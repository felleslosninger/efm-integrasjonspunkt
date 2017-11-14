package no.difi.meldingsutveksling.noarkexchange.altinn;

import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.Markers;
import no.difi.meldingsutveksling.*;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.MessageInfo;
import no.difi.meldingsutveksling.domain.sbdh.EduDocument;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocumentHeader;
import no.difi.meldingsutveksling.kvittering.EduDocumentFactory;
import no.difi.meldingsutveksling.kvittering.xsd.Kvittering;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.nextmove.NextMoveException;
import no.difi.meldingsutveksling.nextmove.NextMoveQueue;
import no.difi.meldingsutveksling.nextmove.NextMoveServiceBus;
import no.difi.meldingsutveksling.noarkexchange.MessageException;
import no.difi.meldingsutveksling.noarkexchange.receive.InternalQueue;
import no.difi.meldingsutveksling.receipt.ConversationService;
import no.difi.meldingsutveksling.receipt.DpoReceiptStatus;
import no.difi.meldingsutveksling.receipt.MessageStatus;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import no.difi.meldingsutveksling.transport.Transport;
import no.difi.meldingsutveksling.transport.TransportFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBElement;
import java.time.LocalDateTime;
import java.util.List;

import static java.lang.String.format;
import static no.difi.meldingsutveksling.logging.MessageMarkerFactory.markerFrom;

/**
 * MessagePolling periodically checks Altinn Formidlingstjeneste for new messages. If new messages are discovered they are
 * downloaded forwarded to the Archive system.
 */
@Component
@Slf4j
public class MessagePolling implements ApplicationContextAware {

    ApplicationContext context;

    @Autowired
    IntegrasjonspunktProperties properties;

    @Autowired
    InternalQueue internalQueue;

    @Autowired
    IntegrasjonspunktNokkel keyInfo;

    @Autowired
    TransportFactory transportFactory;

    @Autowired
    private JmsTemplate jmsTemplate;

    @Autowired
    ServiceRegistryLookup serviceRegistryLookup;

    @Autowired
    ConversationService conversationService;

    @Autowired
    ObjectProvider<List<MessageDownloaderModule>> messageDownloaders;

    @Autowired
    private NextMoveQueue nextMoveQueue;

    private ServiceRecord serviceRecord;

    @Autowired
    private NextMoveServiceBus nextMoveServiceBus;

    @Scheduled(fixedRateString = "${difi.move.nextbest.serviceBus.pollingrate}")
    public void checkForNewNextBestMessages() throws NextMoveException {

        if (properties.getNextbest().getServiceBus().isEnable()) {
            log.debug("Checking for new NextMove messages..");
            nextMoveServiceBus.getAllMessages();
        }
    }

    @Scheduled(fixedRateString = "${difi.move.fiks.pollingrate}")
    public void checkForFiksMessages() {

        if (!properties.getFeature().isEnableDPF()) {
            return;
        }

        log.debug("Checking for new FIKS messages");
        if (messageDownloaders.getIfAvailable() != null) {
            for (MessageDownloaderModule task : messageDownloaders.getObject()) {
                log.debug("performing enabled task");
                task.downloadFiles();
            }
        }

    }

    @Scheduled(fixedRate = 15000)
    public void checkForNewMessages() throws MessageException {

        log.debug("Checking for new messages");

        if (serviceRecord == null) {
            serviceRecord = serviceRegistryLookup.getServiceRecord(properties.getOrg().getNumber());
        }

        if (!properties.getFeature().isEnableDPO()) {
            return;
        }

        // TODO: if ServiceRegistry returns a ServiceRecord to something other than Altinn formidlingstjeneste this
        // will fail
        AltinnWsConfiguration configuration = AltinnWsConfiguration.fromConfiguration(serviceRecord, context);
        AltinnWsClient client = new AltinnWsClient(configuration);

        List<FileReference> fileReferences = client.availableFiles(properties.getOrg().getNumber());

        if (!fileReferences.isEmpty()) {
            log.debug("New message(s) detected");
        }

        for (FileReference reference : fileReferences) {
            try {
                final DownloadRequest request = new DownloadRequest(reference.getValue(), properties.getOrg().getNumber());
                log.debug(format("Downloading message with altinnId=%s", reference.getValue()));
                EduDocument eduDocument = client.download(request);
                Audit.info(format("Downloaded message with id=%s", eduDocument.getConversationId()), eduDocument.createLogstashMarkers());

                if (isNextMove(eduDocument)) {
                    log.debug(format("NextMove message id=%s", eduDocument.getConversationId()));
                    client.confirmDownload(request);
                    if (!properties.getNoarkSystem().getEndpointURL().isEmpty()) {
                        internalQueue.enqueueNoark(eduDocument);
                    } else {
                        nextMoveQueue.enqueueEduDocument(eduDocument);
                    }
                    continue;
                }

                if (!isKvittering(eduDocument)) {
                    sendReceipt(eduDocument.getMessageInfo());
                    log.debug(eduDocument.createLogstashMarkers(), "Delivery receipt sent");
                    internalQueue.enqueueNoark(eduDocument);
                }

                client.confirmDownload(request);
                log.debug(markerFrom(reference).and(eduDocument.createLogstashMarkers()), "Message confirmed downloaded");

                if (isKvittering(eduDocument)) {
                    JAXBElement<Kvittering> jaxbKvit = (JAXBElement<Kvittering>) eduDocument.getAny();
                    Audit.info(format("Message id=%s is a receipt", eduDocument.getConversationId()),
                            eduDocument.createLogstashMarkers().and(getReceiptTypeMarker(jaxbKvit.getValue())));
                    MessageStatus status = statusFromKvittering(jaxbKvit.getValue());
                    conversationService.registerStatus(eduDocument.getConversationId(), status);
                }
            } catch (Exception e) {
                log.error(format("Error during Altinn message polling, message altinnId=%s", reference.getValue()), e);
            }
        }
    }

    private LogstashMarker getReceiptTypeMarker(Kvittering kvittering) {
        final String field = "receipt-type";
        if (kvittering.getLevering() != null) {
            return Markers.append(field, "levering");
        }
        if (kvittering.getAapning() != null) {
            return Markers.append(field, "åpning");
        }
        return Markers.append(field, "unkown");
    }

    private MessageStatus statusFromKvittering(Kvittering kvittering) {
        DpoReceiptStatus status = DpoReceiptStatus.of(kvittering);
        LocalDateTime tidspunkt = kvittering.getTidspunkt().toGregorianCalendar().toZonedDateTime().toLocalDateTime();
        return MessageStatus.of(status, tidspunkt);
    }

    private boolean isKvittering(EduDocument eduDocument) {
        return eduDocument.getStandardBusinessDocumentHeader().getDocumentIdentification().getType().equalsIgnoreCase(StandardBusinessDocumentHeader.KVITTERING_TYPE);
    }

    private boolean isNextMove(EduDocument eduDocument) {
        return eduDocument.getStandardBusinessDocumentHeader().getDocumentIdentification().getType()
                .equalsIgnoreCase(StandardBusinessDocumentHeader.NEXTMOVE_TYPE);
    }

    private void sendReceipt(MessageInfo messageInfo) {
        EduDocument doc = EduDocumentFactory.createLeveringsKvittering(messageInfo, keyInfo.getKeyPair());
        Transport t = transportFactory.createTransport(doc);
        t.send(context, doc);
    }

    @Override
    public void setApplicationContext(ApplicationContext ac) throws BeansException {
        this.context = ac;
    }
}
