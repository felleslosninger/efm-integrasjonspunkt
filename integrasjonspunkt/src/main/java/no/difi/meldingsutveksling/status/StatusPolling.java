package no.difi.meldingsutveksling.status;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.api.StatusStrategy;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.nextmove.v2.ServiceIdentifierService;
import no.difi.meldingsutveksling.receipt.StatusStrategyFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

import static java.lang.String.format;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;

/**
 * Periodically checks non final receipts, and their respective services for updates.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StatusPolling {

    private final IntegrasjonspunktProperties props;
    private final ConversationRepository conversationRepository;
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
                .collect(groupingBy(Conversation::getServiceIdentifier, toSet()))
                .forEach(this::checkReceiptForType);
    }

    private void checkReceiptForType(ServiceIdentifier si, Set<Conversation> conversations) {
        try {
            StatusStrategy strategy = statusStrategyFactory.getStrategy(si);
            strategy.checkStatus(conversations);
        } catch (Exception e) {
            log.error(format("Exception during receipt polling for %s", si), e);
        }
    }

    @Scheduled(fixedRate = 10000)
    public void dpiReceiptsScheduledTask() {
        if (props.getFeature().isEnableReceipts() && props.getFeature().isEnableDPI()) {
            Optional<ExternalReceipt> externalReceipt = dpiReceiptService.checkForReceipts();

            while (externalReceipt.isPresent()) {
                externalReceipt.ifPresent(dpiReceiptService::handleReceipt);
                externalReceipt = dpiReceiptService.checkForReceipts();
            }
        }
    }

}
