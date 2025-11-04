package no.difi.meldingsutveksling.nextmove;

import com.google.common.collect.Sets;
import io.micrometer.core.annotation.Timed;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.marker.LogstashMarker;
import no.difi.meldingsutveksling.NextMoveConsts;
import no.difi.meldingsutveksling.altinnv3.dpo.AltinnDPODownloadService;
import no.difi.meldingsutveksling.altinnv3.dpo.DownloadRequest;
import no.difi.meldingsutveksling.altinnv3.dpo.payload.AltinnPackage;
import no.difi.meldingsutveksling.api.DpoPolling;
import no.difi.meldingsutveksling.config.AltinnAuthorizationDetails;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.noarkexchange.altinn.AltinnNextMoveMessageHandler;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;
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
    private final AltinnDPODownloadService altinnDownloadService;

    private Set<AltinnAuthorizationDetails> systemUsers;

    @PostConstruct
    public void init() {
        systemUsers = Sets.newHashSet(properties.getDpo().getAuthorizationDetails());
        systemUsers.addAll(properties.getDpo().getReportees());
    }

    @Override
    @Timed
    public void poll() {
        systemUsers.forEach(system -> {
            log.debug("Polling messages for " + system.getSystemuserOrgId());
            try {
                UUID[] fileTransferIds = altinnDownloadService.getAvailableFiles(system);
                if (fileTransferIds.length > 0) log.debug("New DPO message(s) detected for {}", system.getSystemuserOrgId());
                Arrays.stream(fileTransferIds).forEach(fileTransferId -> {
                    handleFileReference(system, fileTransferId);
                });
            } catch (Exception e) {
                log.error("Could not check for available files from Altinn: " + e.getMessage(), e);
            }
        });
    }

    private void handleFileReference(AltinnAuthorizationDetails system, UUID fileTransferId) {
        try {
            String orgNumber = system.getSystemuserOrgId().substring(5);
            final DownloadRequest request = new DownloadRequest(fileTransferId, orgNumber);
            log.debug("Downloading message with altinnId={}", fileTransferId);
            AltinnPackage altinnPackage = altinnDownloadService.download(system, request);
            StandardBusinessDocument sbd = altinnPackage.getSbd();
            MDC.put(NextMoveConsts.CORRELATION_ID, sbd.getMessageId());
            LogstashMarker logstashMarkers = SBDUtil.getMessageInfo(sbd).createLogstashMarkers();
            Audit.info("Downloaded message with id=%s".formatted(sbd.getMessageId()), logstashMarkers);

            try {
                UUID.fromString(sbd.getMessageId());
                UUID.fromString(sbd.getConversationId());
            } catch (IllegalArgumentException e) {
                log.error("Found invalid UUID in either messageId={} or conversationId={} - discarding message.", sbd.getMessageId(), sbd.getConversationId());
                altinnDownloadService.confirmDownload(system, request);
                return;
            }

            altinnNextMoveMessageHandler.handleAltinnPackage(altinnPackage);
            altinnDownloadService.confirmDownload(system, request);
            log.debug(markerFrom("altinn-reference-value", fileTransferId).and(logstashMarkers), "Message confirmed downloaded");
        } catch (Exception e) {
            log.error("Error during Altinn message polling, message altinnId=%s".formatted(fileTransferId), e);
        }
    }
}
