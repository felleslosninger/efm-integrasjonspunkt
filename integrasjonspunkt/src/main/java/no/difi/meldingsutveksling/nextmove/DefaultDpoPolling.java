package no.difi.meldingsutveksling.nextmove;

import com.google.common.collect.Sets;
import io.micrometer.core.annotation.Timed;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.marker.LogstashMarker;
import no.difi.meldingsutveksling.NextMoveConsts;
import no.difi.meldingsutveksling.api.DpoPolling;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.altinnv3.AltinnPackage;
import no.difi.meldingsutveksling.altinnv3.AltinnRestClient;
import no.difi.meldingsutveksling.altinnv3.DownloadRequest;
import no.difi.meldingsutveksling.altinnv3.FileReference;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.noarkexchange.altinn.AltinnNextMoveMessageHandler;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static no.difi.meldingsutveksling.logging.MessageMarkerFactory.markerFrom;

@Slf4j
@Component
@ConditionalOnProperty(name = "difi.move.feature.enableDPO", havingValue = "true")
@Order
@RequiredArgsConstructor
public class DefaultDpoPolling implements DpoPolling {

    private final IntegrasjonspunktProperties properties;
    private final AltinnNextMoveMessageHandler altinnNextMoveMessageHandler;
    private final AltinnRestClient altinnRestClient;

    private Set<String> orgnrs;

    @PostConstruct
    public void init() {
        orgnrs = Sets.newHashSet(properties.getOrg().getNumber());
        orgnrs.addAll(properties.getDpo().getReportees());
    }

    @Override
    @Timed
    public void poll() {
        orgnrs.forEach(o -> {
            log.debug("Polling messages for " + o);
            try {
                if (altinnRestClient.checkIfAvailableFiles(o)) {
                    log.debug("New DPO message(s) detected for " + o);
                    List<FileReference> fileReferences = altinnRestClient.availableFiles(o);
                    fileReferences.forEach(reference -> handleFileReference(altinnRestClient, reference, o));
                }
            } catch (Exception e) {
                log.error("Could not check for available files from Altinn: " + e.getMessage(), e);
            }
        });
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void handleFileReference(AltinnRestClient client, FileReference reference, String orgnr) {
        try {
            final DownloadRequest request = new DownloadRequest(reference.getValue(), orgnr);
            log.debug("Downloading message with altinnId=%s".formatted(reference.getValue()));
            AltinnPackage altinnPackage = client.download(request);
            StandardBusinessDocument sbd = altinnPackage.getSbd();
            MDC.put(NextMoveConsts.CORRELATION_ID, sbd.getMessageId());
            LogstashMarker logstashMarkers = SBDUtil.getMessageInfo(sbd).createLogstashMarkers();
            Audit.info("Downloaded message with id=%s".formatted(sbd.getMessageId()), logstashMarkers);

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
            log.debug(markerFrom(reference).and(logstashMarkers), "Message confirmed downloaded");
        } catch (Exception e) {
            log.error("Error during Altinn message polling, message altinnId=%s".formatted(reference.getValue()), e);
        }
    }
}
