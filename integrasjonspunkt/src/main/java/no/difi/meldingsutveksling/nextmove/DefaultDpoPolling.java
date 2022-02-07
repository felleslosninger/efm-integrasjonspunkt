package no.difi.meldingsutveksling.nextmove;

import com.google.common.collect.Sets;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.marker.LogstashMarker;
import no.difi.meldingsutveksling.*;
import no.difi.meldingsutveksling.altinn.mock.brokerbasic.IBrokerServiceExternalBasicCheckIfAvailableFilesBasicAltinnFaultFaultFaultMessage;
import no.difi.meldingsutveksling.api.DpoPolling;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.noarkexchange.altinn.AltinnNextMoveMessageHandler;
import no.difi.meldingsutveksling.shipping.ws.AltinnReasonFactory;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Set;
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
                if (altinnWsClient.checkIfAvailableFiles(o)) {
                    log.debug("New DPO message(s) detected for " + o);
                    List<FileReference> fileReferences = altinnWsClient.availableFiles(o);
                    fileReferences.forEach(reference -> handleFileReference(altinnWsClient, reference, o));
                }
            } catch (IBrokerServiceExternalBasicCheckIfAvailableFilesBasicAltinnFaultFaultFaultMessage e) {
                log.error("Could not check for available files from Altinn: " + AltinnReasonFactory.from(e), e);
            }
        });
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void handleFileReference(AltinnWsClient client, FileReference reference, String orgnr) {
        try {
            final DownloadRequest request = new DownloadRequest(reference.getValue(), orgnr);
            log.debug(format("Downloading message with altinnId=%s", reference.getValue()));
            AltinnPackage altinnPackage = client.download(request);
            StandardBusinessDocument sbd = altinnPackage.getSbd();
            String messageId = SBDUtil.getMessageId(sbd);
            MDC.put(NextMoveConsts.CORRELATION_ID, messageId);
            LogstashMarker logstashMarkers = SBDUtil.getMessageInfo(sbd).createLogstashMarkers();
            Audit.info(format("Downloaded message with id=%s", messageId), logstashMarkers);

            String conversationId = SBDUtil.getConversationId(sbd);

            try {
                UUID.fromString(messageId);
                UUID.fromString(conversationId);
            } catch (IllegalArgumentException e) {
                log.error("Found invalid UUID in either messageId={} or conversationId={} - discarding message.", messageId, conversationId);
                client.confirmDownload(request);
                return;
            }

            altinnNextMoveMessageHandler.handleAltinnPackage(altinnPackage);
            client.confirmDownload(request);
            log.debug(markerFrom(reference).and(logstashMarkers), "Message confirmed downloaded");
        } catch (Exception e) {
            log.error(format("Error during Altinn message polling, message altinnId=%s", reference.getValue()), e);
        }
    }
}
