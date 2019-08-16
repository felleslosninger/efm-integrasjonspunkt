package no.difi.meldingsutveksling.receipt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.nextmove.v2.ServiceIdentifierService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static java.lang.String.format;
import static no.difi.meldingsutveksling.receipt.ConversationMarker.markerFrom;

/**
 * Periodically checks non final receipts, and their respective services for updates.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StatusPolling {

    private final IntegrasjonspunktProperties props;
    private final ConversationRepository conversationRepository;
    private final ConversationService conversationService;
    private final StatusStrategyFactory statusStrategyFactory;
    private final DpiReceiptService dpiReceiptService;
    private final ServiceIdentifierService serviceIdentifierService;

    @Scheduled(fixedRate = 60000)
    public void checkReceiptStatus() {
        if (!props.getFeature().isEnableReceipts()) {
            return;
        }

        conversationRepository.findByPollable(true)
                .stream()
                .filter(c -> serviceIdentifierService.isEnabled(c.getServiceIdentifier()))
                .forEach(this::checkReceiptStatus);
    }

    private void checkReceiptStatus(Conversation c) {
        log.debug(markerFrom(c), "Checking status, conversationId={}", c.getConversationId());
        try {
            StatusStrategy strategy = statusStrategyFactory.getFactory(c);
            strategy.checkStatus(c);
        } catch (Exception e) {
            log.error(format("Exception during receipt polling, conversationId=%s", c.getConversationId()), e);
        }
    }

    @Scheduled(fixedRate = 10000)
    public void dpiReceiptsScheduledTask() {
        if (props.getFeature().isEnableReceipts() && props.getFeature().isEnableDPI()) {
            Optional<ExternalReceipt> externalReceipt = dpiReceiptService.checkForReceipts();

            while (externalReceipt.isPresent()) {
                externalReceipt.ifPresent(this::handleReceipt);
                externalReceipt = dpiReceiptService.checkForReceipts();
            }
        }
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
