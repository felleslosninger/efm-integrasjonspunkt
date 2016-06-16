package no.difi.meldingsutveksling.noarkexchange.altinn;


import no.difi.meldingsutveksling.*;
import no.difi.meldingsutveksling.config.IntegrasjonspunktConfiguration;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.MessageInfo;
import no.difi.meldingsutveksling.domain.sbdh.EduDocument;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocumentHeader;
import no.difi.meldingsutveksling.elma.ELMALookup;
import no.difi.meldingsutveksling.kvittering.EduDocumentFactory;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.noarkexchange.receive.InternalQueue;
import no.difi.meldingsutveksling.transport.Transport;
import no.difi.meldingsutveksling.transport.TransportFactory;
import no.difi.vefa.peppol.common.model.Endpoint;
import no.difi.vefa.peppol.lookup.api.LookupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

import static no.difi.meldingsutveksling.logging.MessageMarkerFactory.markerFrom;

/**
 * MessagePolling periodically checks Altinn Formidlingstjeneste for new messages. If new messages are discovered they
 * are downloaded forwarded to the Archive system.
 */
@Component
public class MessagePolling {
    private static final String PREFIX_NORWAY = "9908:";
    private Logger logger = LoggerFactory.getLogger(MessagePolling.class);

    @Autowired
    IntegrasjonspunktConfiguration config;

    @Autowired
    InternalQueue internalQueue;

    @Autowired
    ELMALookup elmaLookup;

    @Autowired
    IntegrasjonspunktNokkel keyInfo;

    @Autowired
    TransportFactory transportFactory;

    @Autowired
    private JmsTemplate jmsTemplate;

    @Scheduled(fixedRate = 15000)
    public void checkForNewMessages() {
        MDC.put(IntegrasjonspunktConfiguration.KEY_ORGANISATION_NUMBER, config.getOrganisationNumber());
        logger.debug("Checking for new messages");
        Endpoint endpoint;
        try {
            endpoint = elmaLookup.lookup(PREFIX_NORWAY + config.getOrganisationNumber());
        } catch (LookupException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }
        AltinnWsConfiguration configuration = AltinnWsConfiguration.fromConfiguration(endpoint.getAddress(), config.getConfiguration());
        AltinnWsClient client = new AltinnWsClient(configuration);

        List<FileReference> fileReferences = client.availableFiles(config.getOrganisationNumber());

        if(!fileReferences.isEmpty()) {
            Audit.info("New message(s) detected");
        }

        for (FileReference reference : fileReferences) {
            final DownloadRequest request = new DownloadRequest(reference.getValue(), config.getOrganisationNumber());
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
        t.send(config.getConfiguration(), doc);
    }
}

