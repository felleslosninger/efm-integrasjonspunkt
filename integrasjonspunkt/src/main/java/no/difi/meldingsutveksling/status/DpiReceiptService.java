package no.difi.meldingsutveksling.status;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerClient;
import org.springframework.scheduling.annotation.Async;
import reactor.core.publisher.Flux;

import java.util.concurrent.CompletableFuture;

@Slf4j
@RequiredArgsConstructor
public class DpiReceiptService {

    private final IntegrasjonspunktProperties properties;
    private final MeldingsformidlerClient meldingsformidlerClient;
    private final ConversationService conversationService;

    @Timed
    @Async("dpiReceiptExecutor")
    public CompletableFuture<Void> handleReceipts(String mpcId) {
        checkForReceipts(mpcId)
                .toStream()
                .forEach(this::handleReceipt);

        return CompletableFuture.completedFuture(null);
    }

    private Flux<ExternalReceipt> checkForReceipts(String mpcId) {
        return meldingsformidlerClient.sjekkEtterKvitteringer(properties.getOrg().getNumber(), mpcId);
    }

    private void handleReceipt(ExternalReceipt externalReceipt) {
        final String id = externalReceipt.getId();
        MessageStatus status = externalReceipt.toMessageStatus();

        conversationService.registerStatus(id, status);

        log.debug(externalReceipt.logMarkers(), "Updated receipt (DPI)");
        externalReceipt.confirmReceipt();
        log.debug(externalReceipt.logMarkers(), "Confirmed receipt (DPI)");
    }
}
