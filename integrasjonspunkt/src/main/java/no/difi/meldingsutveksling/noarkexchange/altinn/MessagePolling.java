package no.difi.meldingsutveksling.noarkexchange.altinn;

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
import no.difi.meldingsutveksling.logging.MoveLogMarkers;
import no.difi.meldingsutveksling.nextbest.NextBestQueue;
import no.difi.meldingsutveksling.noarkexchange.MessageException;
import no.difi.meldingsutveksling.noarkexchange.receive.InternalQueue;
import no.difi.meldingsutveksling.receipt.Conversation;
import no.difi.meldingsutveksling.receipt.ConversationRepository;
import no.difi.meldingsutveksling.receipt.MessageReceipt;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import no.difi.meldingsutveksling.transport.Transport;
import no.difi.meldingsutveksling.transport.TransportFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBElement;
import java.time.LocalDateTime;
import java.util.List;

import static no.difi.meldingsutveksling.logging.MessageMarkerFactory.markerFrom;

/**
 * MessagePolling periodically checks Altinn Formidlingstjeneste for new messages. If new messages are discovered they are
 * downloaded forwarded to the Archive system.
 */
@Component
public class MessagePolling implements ApplicationContextAware {

    private static final String PREFIX_NORWAY = "9908:";
    private Logger logger = LoggerFactory.getLogger(MessagePolling.class);

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
    ConversationRepository conversationRepository;

    @Autowired
    private NextBestQueue nextBestQueue;

    private ServiceRecord serviceRecord;

    @Scheduled(fixedRate = 15000)
    public void checkForNewMessages() throws MessageException {
        MDC.put(MoveLogMarkers.KEY_ORGANISATION_NUMBER, properties.getOrg().getNumber());
        logger.debug("Checking for new messages");

        // TODO: if ServiceRegistry returns a ServiceRecord to something other than Altinn formidlingstjeneste this
        // will fail
        if (serviceRecord == null) {
            serviceRecord = serviceRegistryLookup.getServiceRecord(properties.getOrg().getNumber());
        }

        AltinnWsConfiguration configuration = AltinnWsConfiguration.fromConfiguration(serviceRecord.getEndPointURL(), context);
        AltinnWsClient client = new AltinnWsClient(configuration);

        List<FileReference> fileReferences = client.availableFiles(properties.getOrg().getNumber());

        if (!fileReferences.isEmpty()) {
            Audit.info("New message(s) detected");
        }

        for (FileReference reference : fileReferences) {
            final DownloadRequest request = new DownloadRequest(reference.getValue(), properties.getOrg().getNumber());
            EduDocument eduDocument = client.download(request);

            if (isNextBest(eduDocument)) {
                logger.info("NextBest Message received");
                client.confirmDownload(request);
                nextBestQueue.enqueueEduDocument(eduDocument);
                return;
            }

            if (!isKvittering(eduDocument)) {
                sendReceipt(eduDocument.getMessageInfo());
                Audit.info("Delivery receipt sent", eduDocument.createLogstashMarkers());
                internalQueue.enqueueNoark(eduDocument);
            }

            client.confirmDownload(request);
            Audit.info("Message downloaded", markerFrom(reference).and(eduDocument.createLogstashMarkers()));

            if (isKvittering(eduDocument)) {
                JAXBElement<Kvittering> jaxbKvit = (JAXBElement<Kvittering>) eduDocument.getAny();
                Audit.info("Message is a receipt", eduDocument.createLogstashMarkers().and(getReceiptTypeMarker
                        (jaxbKvit.getValue())));
                MessageReceipt receipt = receiptFromKvittering(jaxbKvit.getValue());
                Conversation conversation = conversationRepository.findByConversationId(eduDocument.getConversationId())
                        .stream()
                        .findFirst()
                        .orElse(Conversation.of(eduDocument.getConversationId(),
                                "unknown", eduDocument
                                .getReceiverOrgNumber(),
                                "unknown", ServiceIdentifier.EDU));
                conversation.addMessageReceipt(receipt);
                conversationRepository.save(conversation);
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

    private MessageReceipt receiptFromKvittering(Kvittering kvittering) {
        if (kvittering.getAapning() != null) {
            return MessageReceipt.of(ReceiptStatus.OTHER, LocalDateTime.now(), "Åpningskvittering");
        }
        if (kvittering.getLevering() != null) {
            return MessageReceipt.of(ReceiptStatus.DELIVERED, LocalDateTime.now());
        }
        return MessageReceipt.of(ReceiptStatus.OTHER, LocalDateTime.now());
    }

    private boolean isKvittering(EduDocument eduDocument) {
        return eduDocument.getStandardBusinessDocumentHeader().getDocumentIdentification().getType().equalsIgnoreCase(StandardBusinessDocumentHeader.KVITTERING_TYPE);
    }

    private boolean isNextBest(EduDocument eduDocument) {
        return eduDocument.getStandardBusinessDocumentHeader().getDocumentIdentification().getType()
                .equalsIgnoreCase(StandardBusinessDocumentHeader.NEXTBEST_TYPE);
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
