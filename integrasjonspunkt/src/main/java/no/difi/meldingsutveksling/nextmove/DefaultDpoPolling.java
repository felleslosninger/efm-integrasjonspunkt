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
import no.difi.meldingsutveksling.altinnv3.DPO.altinn2.AltinnPackage;
import no.difi.meldingsutveksling.altinnv3.DPO.AltinnDownloadService;
import no.difi.meldingsutveksling.altinnv3.DPO.DownloadRequest;
import no.difi.meldingsutveksling.altinnv3.DPO.FileReference;
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
    private final AltinnDownloadService altinnDownloadService;

    private Set<String> orgnrs;

    @PostConstruct
    public void init() {
        orgnrs = Sets.newHashSet(properties.getOrg().getNumber());
        orgnrs.addAll(properties.getDpo().getReportees());
    }

    @Override
    @Timed
    public void poll() {
        orgnrs.forEach(o -> { //todo reportees greia, korleis funka det? kva er det?
            log.debug("Polling messages for " + o);
            try {
                List<FileReference> fileReferences = altinnDownloadService.getAvailableFiles();
                if (!fileReferences.isEmpty()) log.debug("New DPO message(s) detected for " + o);
                fileReferences.forEach(reference -> handleFileReference(reference, o));

            } catch (Exception e) {
                log.error("Could not check for available files from Altinn: " + e.getMessage(), e);
            }
        });
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void handleFileReference(FileReference reference, String orgnr) {
        try {
            final DownloadRequest request = new DownloadRequest(reference.getFileReferenceId(), orgnr);
            log.debug("Downloading message with altinnId=%s".formatted(reference.getFileReferenceId()));
            AltinnPackage altinnPackage = altinnDownloadService.download(request);
            StandardBusinessDocument sbd = altinnPackage.getSbd();
            MDC.put(NextMoveConsts.CORRELATION_ID, sbd.getMessageId());
            LogstashMarker logstashMarkers = SBDUtil.getMessageInfo(sbd).createLogstashMarkers();
            Audit.info("Downloaded message with id=%s".formatted(sbd.getMessageId()), logstashMarkers);

            try {
                UUID.fromString(sbd.getMessageId());
                UUID.fromString(sbd.getConversationId());
            } catch (IllegalArgumentException e) {
                log.error("Found invalid UUID in either messageId={} or conversationId={} - discarding message.", sbd.getMessageId(), sbd.getConversationId());
                altinnDownloadService.confirmDownload(request);
                return;
            }

            altinnNextMoveMessageHandler.handleAltinnPackage(altinnPackage);
            altinnDownloadService.confirmDownload(request);
            log.debug(markerFrom(reference).and(logstashMarkers), "Message confirmed downloaded");
        } catch (Exception e) {
            log.error("Error during Altinn message polling, message altinnId=%s".formatted(reference.getFileReferenceId()), e);
        }
    }
}
