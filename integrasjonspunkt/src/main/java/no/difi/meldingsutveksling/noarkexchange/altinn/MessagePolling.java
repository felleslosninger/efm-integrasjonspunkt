package no.difi.meldingsutveksling.noarkexchange.altinn;

import java.util.List;
import no.difi.meldingsutveksling.*;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.MessageInfo;
import no.difi.meldingsutveksling.domain.sbdh.EduDocument;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocumentHeader;
import no.difi.meldingsutveksling.kvittering.EduDocumentFactory;
import no.difi.meldingsutveksling.logging.Audit;
import static no.difi.meldingsutveksling.logging.MessageMarkerFactory.markerFrom;
import no.difi.meldingsutveksling.logging.MoveLogMarkers;
import no.difi.meldingsutveksling.noarkexchange.receive.InternalQueue;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import no.difi.meldingsutveksling.transport.Transport;
import no.difi.meldingsutveksling.transport.TransportFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * MessagePolling periodically checks Altinn Formidlingstjeneste for new
 * messages. If new messages are discovered they are downloaded forwarded to the
 * Archive system.
 */
@Component
public class MessagePolling {

    private static final String PREFIX_NORWAY = "9908:";
    private Logger logger = LoggerFactory.getLogger(MessagePolling.class);

    @Autowired
    Environment environment;

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

    private ServiceRecord primaryServiceRecord;

    @Scheduled(fixedRate = 15000)
    public void checkForNewMessages() {
        MDC.put(MoveLogMarkers.KEY_ORGANISATION_NUMBER, properties.getOrgnumber());
        logger.debug("Checking for new messages");

        // TODO: if ServiceRegistry returns a ServiceRecord to something other than Altinn formidlingstjeneste this
        // will fail
        if (primaryServiceRecord == null) {
            primaryServiceRecord = serviceRegistryLookup.getPrimaryServiceRecord(properties.getOrgnumber());
        }

        AltinnWsConfiguration configuration = AltinnWsConfiguration.fromConfiguration(primaryServiceRecord.getEndPointURL(), environment);
        AltinnWsClient client = new AltinnWsClient(configuration);

        List<FileReference> fileReferences = client.availableFiles(properties.getOrgnumber());

        if (!fileReferences.isEmpty()) {
            Audit.info("New message(s) detected");
        }

        for (FileReference reference : fileReferences) {
            final DownloadRequest request = new DownloadRequest(reference.getValue(), properties.getOrgnumber());
            EduDocument eduDocument = client.download(request);

            if (!isKvittering(eduDocument)) {
                internalQueue.enqueueNoark(eduDocument);
            }
            client.confirmDownload(request);
            Audit.info("Message downloaded", markerFrom(reference).and(eduDocument.createLogstashMarkers()));
            if (!isKvittering(eduDocument)) {
                sendReceipt(eduDocument.getMessageInfo());
                Audit.info("Delivery receipt sent", eduDocument.createLogstashMarkers());
            } else {
                Audit.info("Message is a receipt", eduDocument.createLogstashMarkers());
            }
        }
    }

    private boolean isKvittering(EduDocument eduDocument) {
        return eduDocument.getStandardBusinessDocumentHeader().getDocumentIdentification().getType().equalsIgnoreCase(StandardBusinessDocumentHeader.KVITTERING_TYPE);
    }

    private void sendReceipt(MessageInfo messageInfo) {
        EduDocument doc = EduDocumentFactory.createLeveringsKvittering(messageInfo, keyInfo.getKeyPair());
        Transport t = transportFactory.createTransport(doc);
        t.send(environment, doc);
    }
}
