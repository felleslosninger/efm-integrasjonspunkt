package no.difi.meldingsutveksling.noarkexchange.altinn;

import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.Markers;
import no.difi.meldingsutveksling.*;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.MessageInfo;
import no.difi.meldingsutveksling.domain.sbdh.Scope;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocumentHeader;
import no.difi.meldingsutveksling.kvittering.SBDReceiptFactory;
import no.difi.meldingsutveksling.kvittering.xsd.Kvittering;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.nextmove.NextMoveQueue;
import no.difi.meldingsutveksling.nextmove.NextMoveServiceBus;
import no.difi.meldingsutveksling.nextmove.message.MessagePersister;
import no.difi.meldingsutveksling.noarkexchange.MessageException;
import no.difi.meldingsutveksling.noarkexchange.receive.InternalQueue;
import no.difi.meldingsutveksling.receipt.*;
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
import java.util.concurrent.CompletableFuture;

import static java.lang.String.format;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPO;
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
    @Autowired
    private NextMoveServiceBus nextMoveServiceBus;

    private MessagePersister messagePersister;
    private ServiceRecord serviceRecord;
    private CompletableFuture batchRead;

    public MessagePolling(ObjectProvider<MessagePersister> messagePersister) {
        this.messagePersister = messagePersister.getIfUnique();
    }

    @Scheduled(fixedRateString = "${difi.move.nextmove.serviceBus.pollingrate}")
    public void checkForNewNextMoveMessages() {
        if (properties.getNextmove().getServiceBus().isEnable() &&
                !properties.getNextmove().getServiceBus().isBatchRead()) {
            log.debug("Checking for new NextMove messages..");
            nextMoveServiceBus.getAllMessagesRest();
        }
        if (properties.getNextmove().getServiceBus().isEnable() &&
                properties.getNextmove().getServiceBus().isBatchRead()) {
            if (this.batchRead == null || this.batchRead.isDone()) {
                log.debug("Checking for new NextMove messages (batch)..");
                this.batchRead = nextMoveServiceBus.getAllMessagesBatch();
            } else {
                log.debug("Batch still processing..");
            }
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
        if (!properties.getFeature().isEnableDPO()) {
            return;
        }
        log.debug("Checking for new messages");

        if (serviceRecord == null) {
            serviceRecord = serviceRegistryLookup.getServiceRecord(properties.getOrg().getNumber(), DPO)
                    .orElseThrow(() -> new MeldingsUtvekslingRuntimeException(String.format("DPO ServiceRecord not found for %s", properties.getOrg().getNumber())));
        }

        AltinnWsConfiguration configuration = AltinnWsConfiguration.fromConfiguration(serviceRecord, context);
        AltinnWsClient client = new AltinnWsClient(configuration, context);

        List<FileReference> fileReferences = client.availableFiles(properties.getOrg().getNumber());

        if (!fileReferences.isEmpty()) {
            log.debug("New message(s) detected");
        }

        for (FileReference reference : fileReferences) {
            try {
                final DownloadRequest request = new DownloadRequest(reference.getValue(), properties.getOrg().getNumber());
                log.debug(format("Downloading message with altinnId=%s", reference.getValue()));
                StandardBusinessDocument sbd = client.download(request, messagePersister);
                Audit.info(format("Downloaded message with id=%s", sbd.getConversationId()), sbd.createLogstashMarkers());

                if (isNextMove(sbd)) {
                    log.debug(format("NextMove message id=%s", sbd.getConversationId()));
                    client.confirmDownload(request);
                    if (properties.getNoarkSystem().isEnable() && !properties.getNoarkSystem().getEndpointURL().isEmpty()) {
                        internalQueue.enqueueNoark(sbd);
                    } else {
                        nextMoveQueue.enqueue(sbd);
                    }
                    continue;
                }

                if (!isKvittering(sbd)) {
                    sendReceipt(sbd.getMessageInfo());
                    log.debug(sbd.createLogstashMarkers(), "Delivery receipt sent");
                    Conversation c = conversationService.registerConversation(sbd);
                    internalQueue.enqueueNoark(sbd);
                    conversationService.registerStatus(c, MessageStatus.of(GenericReceiptStatus.INNKOMMENDE_MOTTATT));
                }

                client.confirmDownload(request);
                log.debug(markerFrom(reference).and(sbd.createLogstashMarkers()), "Message confirmed downloaded");

                if (isKvittering(sbd)) {
                    JAXBElement<Kvittering> jaxbKvit = (JAXBElement<Kvittering>) sbd.getAny();
                    Audit.info(format("Message id=%s is a receipt", sbd.getConversationId()),
                            sbd.createLogstashMarkers().and(getReceiptTypeMarker(jaxbKvit.getValue())));
                    MessageStatus status = statusFromKvittering(jaxbKvit.getValue());
                    conversationService.registerStatus(sbd.getConversationId(), status);
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

    private boolean isKvittering(StandardBusinessDocument sbd) {
        return sbd.getStandardBusinessDocumentHeader().getDocumentIdentification().getType().equalsIgnoreCase(StandardBusinessDocumentHeader.KVITTERING_TYPE);
    }

    private boolean isNextMove(StandardBusinessDocument sbd) {
        return sbd.getConversationScope()
                .map(Scope::getIdentifier)
                .filter(s -> s.equals(StandardBusinessDocumentHeader.NEXTMOVE_STANDARD))
                .isPresent();
//        return sbd.getStandardBusinessDocumentHeader().getDocumentIdentification().getStandard()
//                .equalsIgnoreCase(StandardBusinessDocumentHeader.NEXTMOVE_STANDARD);
    }

    private void sendReceipt(MessageInfo messageInfo) {
        StandardBusinessDocument doc = SBDReceiptFactory.createLeveringsKvittering(messageInfo, keyInfo);
        Transport t = transportFactory.createTransport(doc);
        t.send(context, doc);
    }

    @Override
    public void setApplicationContext(ApplicationContext ac) throws BeansException {
        this.context = ac;
    }
}
