package no.difi.meldingsutveksling.status;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class DpiReceiptService {

    private final IntegrasjonspunktProperties properties;
    private final MeldingsformidlerClient meldingsformidlerClient;
    private final ConversationService conversationService;

    @Async("dpiReceiptExecutor")
    public CompletableFuture<Void> handleReceipts(String mpcId) {
        Optional<ExternalReceipt> externalReceipt = checkForReceipts(mpcId);

        while (externalReceipt.isPresent()) {
            externalReceipt.ifPresent(this::handleReceipt);
            externalReceipt = checkForReceipts(mpcId);
        }
        return CompletableFuture.completedFuture(null);
    }

    private Optional<ExternalReceipt> checkForReceipts(String mpcId) {
        return meldingsformidlerClient.sjekkEtterKvittering(properties.getOrg().getNumber(), mpcId);
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
