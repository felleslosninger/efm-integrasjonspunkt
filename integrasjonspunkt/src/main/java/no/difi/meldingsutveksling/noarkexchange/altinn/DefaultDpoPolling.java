package no.difi.meldingsutveksling.noarkexchange.altinn;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.AltinnWsClient;
import no.difi.meldingsutveksling.DownloadRequest;
import no.difi.meldingsutveksling.FileReference;
import no.difi.meldingsutveksling.api.DpoPolling;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.nextmove.TimeToLiveHelper;
import no.difi.meldingsutveksling.api.MessagePersister;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

import static java.lang.String.format;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPO;
import static no.difi.meldingsutveksling.logging.MessageMarkerFactory.markerFrom;
import static no.difi.meldingsutveksling.nextmove.ConversationDirection.INCOMING;

@Slf4j
@Component
@ConditionalOnProperty(name = "difi.move.feature.enableDPO", havingValue = "true")
@Order
@RequiredArgsConstructor
public class DefaultDpoPolling implements DpoPolling {

    private final IntegrasjonspunktProperties properties;
    private final AltinnNextMoveMessageHandler altinnNextMoveMessageHandler;
    private final MessagePersister messagePersister;
    private final AltinnWsClient altinnWsClient;
    private final TimeToLiveHelper timeToLiveHelper;
    private final SBDUtil sbdUtil;

    @Override
    public void poll() {
        log.trace("Checking for new DPO messages");

        List<FileReference> fileReferences = altinnWsClient.availableFiles(properties.getOrg().getNumber());

        if (!fileReferences.isEmpty()) {
            log.debug("New DPO message(s) detected");
        }

        fileReferences.forEach(reference -> handleFileReference(altinnWsClient, reference));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void handleFileReference(AltinnWsClient client, FileReference reference) {
        try {
            final DownloadRequest request = new DownloadRequest(reference.getValue(), properties.getOrg().getNumber());
            log.debug(format("Downloading message with altinnId=%s", reference.getValue()));
            StandardBusinessDocument sbd = client.download(request, messagePersister);
            Audit.info(format("Downloaded message with id=%s", sbd.getDocumentId()), sbd.createLogstashMarkers());

            try {
                UUID.fromString(sbd.getMessageId());
                UUID.fromString(sbd.getConversationId());
            } catch (IllegalArgumentException e) {
                log.error("Found invalid UUID in either messageId={} or conversationId={} - discarding message.", sbd.getMessageId(), sbd.getConversationId());
                client.confirmDownload(request);
                return;
            }

            if (sbdUtil.isExpired(sbd)) {
                timeToLiveHelper.registerErrorStatusAndMessage(sbd, DPO, INCOMING);
                messagePersister.delete(sbd.getMessageId());
            } else {
                altinnNextMoveMessageHandler.handleStandardBusinessDocument(sbd);
            }

            client.confirmDownload(request);
            log.debug(markerFrom(reference).and(sbd.createLogstashMarkers()), "Message confirmed downloaded");

            if (!sbdUtil.isStatus(sbd)) {
                altinnNextMoveMessageHandler.sendReceivedStatusToSender(sbd);
            }
        } catch (Exception e) {
            log.error(format("Error during Altinn message polling, message altinnId=%s", reference.getValue()), e);
        }
    }
}
