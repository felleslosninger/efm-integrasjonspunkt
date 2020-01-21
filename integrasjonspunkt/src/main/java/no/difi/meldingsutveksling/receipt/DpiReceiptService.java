package no.difi.meldingsutveksling.receipt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerClient;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DpiReceiptService {

    private final IntegrasjonspunktProperties properties;
    private final MeldingsformidlerClient meldingsformidlerClient;
    private final ConversationService conversationService;

    Optional<ExternalReceipt> checkForReceipts() {
        return meldingsformidlerClient.sjekkEtterKvittering(properties.getOrg().getNumber());
    }

    public void handleReceipt(ExternalReceipt externalReceipt) {
        final String id = externalReceipt.getId();
        MessageStatus status = externalReceipt.toMessageStatus();

        conversationService.registerStatus(id, status);

        log.debug(externalReceipt.logMarkers(), "Updated receipt (DPI)");
        externalReceipt.confirmReceipt();
        log.debug(externalReceipt.logMarkers(), "Confirmed receipt (DPI)");
    }
}
