package no.difi.meldingsutveksling.nextmove;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.*;
import no.difi.meldingsutveksling.altinn.mock.brokerbasic.IBrokerServiceExternalBasicCheckIfAvailableFilesBasicAltinnFaultFaultFaultMessage;
import no.difi.meldingsutveksling.api.DpoPolling;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.noarkexchange.altinn.AltinnNextMoveMessageHandler;
import no.difi.meldingsutveksling.shipping.ws.AltinnReasonFactory;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

import static java.lang.String.format;
import static no.difi.meldingsutveksling.logging.MessageMarkerFactory.markerFrom;

@Slf4j
@Component
@ConditionalOnProperty(name = "difi.move.feature.enableDPO", havingValue = "true")
@Order
@RequiredArgsConstructor
public class DefaultDpoPolling implements DpoPolling {

    private final IntegrasjonspunktProperties properties;
    private final AltinnNextMoveMessageHandler altinnNextMoveMessageHandler;
    private final AltinnWsClient altinnWsClient;

    @Override
    @Timed
    public void poll() {
        log.trace("Checking for new DPO messages");

        try {
            if (altinnWsClient.checkIfAvailableFiles()) {
                log.debug("New DPO message(s) detected");
                List<FileReference> fileReferences = altinnWsClient.availableFiles();
                fileReferences.forEach(reference -> handleFileReference(altinnWsClient, reference));
            }
        } catch (IBrokerServiceExternalBasicCheckIfAvailableFilesBasicAltinnFaultFaultFaultMessage e) {
            log.error("Could not check for available files from Altinn: " + AltinnReasonFactory.from(e), e);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void handleFileReference(AltinnWsClient client, FileReference reference) {
        try {
            final DownloadRequest request = new DownloadRequest(reference.getValue(), properties.getOrg().getNumber());
            log.debug(format("Downloading message with altinnId=%s", reference.getValue()));
            AltinnPackage altinnPackage = client.download(request);
            StandardBusinessDocument sbd = altinnPackage.getSbd();
            MDC.put(NextMoveConsts.CORRELATION_ID, sbd.getMessageId());
            Audit.info(format("Downloaded message with id=%s", sbd.getDocumentId()), sbd.createLogstashMarkers());

            try {
                UUID.fromString(sbd.getMessageId());
                UUID.fromString(sbd.getConversationId());
            } catch (IllegalArgumentException e) {
                log.error("Found invalid UUID in either messageId={} or conversationId={} - discarding message.", sbd.getMessageId(), sbd.getConversationId());
                client.confirmDownload(request);
                return;
            }

            altinnNextMoveMessageHandler.handleAltinnPackage(altinnPackage);
            client.confirmDownload(request);
            log.debug(markerFrom(reference).and(sbd.createLogstashMarkers()), "Message confirmed downloaded");
        } catch (Exception e) {
            log.error(format("Error during Altinn message polling, message altinnId=%s", reference.getValue()), e);
        }
    }
}
