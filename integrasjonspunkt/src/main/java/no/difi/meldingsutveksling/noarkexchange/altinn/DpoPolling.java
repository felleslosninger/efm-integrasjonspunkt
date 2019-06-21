package no.difi.meldingsutveksling.noarkexchange.altinn;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.AltinnWsClient;
import no.difi.meldingsutveksling.AltinnWsClientFactory;
import no.difi.meldingsutveksling.DownloadRequest;
import no.difi.meldingsutveksling.FileReference;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.nextmove.TimeToLiveHelper;
import no.difi.meldingsutveksling.nextmove.message.MessagePersister;
import no.difi.meldingsutveksling.receipt.ConversationService;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookupException;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

import static java.lang.String.format;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPO;
import static no.difi.meldingsutveksling.logging.MessageMarkerFactory.markerFrom;
import static no.difi.meldingsutveksling.nextmove.ConversationDirection.INCOMING;

@Slf4j
@Component
@ConditionalOnProperty(name = "difi.move.feature.enableDPO", havingValue = "true")
@RequiredArgsConstructor
public class DpoPolling {

    private final IntegrasjonspunktProperties properties;
    private final AltinnNextMoveMessageHandler altinnNextMoveMessageHandler;
    private final ServiceRegistryLookup serviceRegistryLookup;
    private final MessagePersister messagePersister;
    private final AltinnWsClientFactory altinnWsClientFactory;
    private final TimeToLiveHelper timeToLiveHelper;
    private final SBDUtil sbdUtil;
    private final ConversationService conversationService;

    private ServiceRecord serviceRecord;

    public void poll() {
        log.debug("Checking for new messages");

        AltinnWsClient client = getAltinnWsClient();
        List<FileReference> fileReferences = client.availableFiles(properties.getOrg().getNumber());

        if (!fileReferences.isEmpty()) {
            log.debug("New message(s) detected");
        }

        fileReferences.forEach(reference -> handleFileReference(client, reference));
    }

    private AltinnWsClient getAltinnWsClient() {
        return altinnWsClientFactory.getAltinnWsClient(getServiceRecord());
    }

    private ServiceRecord getServiceRecord() {
        if (serviceRecord == null) {
            try {
                serviceRecord = serviceRegistryLookup.getServiceRecord(properties.getOrg().getNumber(), DPO);
            } catch (ServiceRegistryLookupException e) {
                throw new MeldingsUtvekslingRuntimeException(String.format("DPO ServiceRecord not found for %s", properties.getOrg().getNumber()), e);
            }
        }

        return serviceRecord;
    }

    private void handleFileReference(AltinnWsClient client, FileReference reference) {
        try {
            final DownloadRequest request = new DownloadRequest(reference.getValue(), properties.getOrg().getNumber());
            log.debug(format("Downloading message with altinnId=%s", reference.getValue()));
            StandardBusinessDocument sbd = client.download(request, messagePersister);
            Audit.info(format("Downloaded message with id=%s", sbd.getConversationId()), sbd.createLogstashMarkers());

            if (sbdUtil.isExpired(sbd)) {
                timeToLiveHelper.registerErrorStatusAndMessage(sbd, DPO, INCOMING);
            } else {
                altinnNextMoveMessageHandler.handleStandardBusinessDocument(sbd);
            }

            client.confirmDownload(request);
            log.debug(markerFrom(reference).and(sbd.createLogstashMarkers()), "Message confirmed downloaded");
        } catch (Exception e) {
            log.error(format("Error during Altinn message polling, message altinnId=%s", reference.getValue()), e);
        }
    }
}
