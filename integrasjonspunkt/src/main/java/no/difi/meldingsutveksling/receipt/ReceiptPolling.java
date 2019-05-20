package no.difi.meldingsutveksling.receipt;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.nextmove.v2.ServiceIdentifierService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

import static java.lang.String.format;
import static no.difi.meldingsutveksling.receipt.ConversationMarker.markerFrom;
import static no.difi.meldingsutveksling.receipt.ReceiptStatus.FEIL;
import static no.difi.meldingsutveksling.receipt.ReceiptStatus.LEST;

/**
 * Periodically checks non final receipts, and their respective services for updates.
 */
@Component
public class ReceiptPolling {


    private static final Logger log = LoggerFactory.getLogger(ReceiptPolling.class);

    @Autowired
    private IntegrasjonspunktProperties props;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private StatusStrategyFactory statusStrategyFactory;

    @Autowired
    private DpiReceiptService dpiReceiptService;

    @Autowired
    private ServiceIdentifierService serviceIdentifierService;

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

        conversationService.registerStatus(id, status)
                .filter(c -> Arrays.asList(LEST, FEIL).contains(ReceiptStatus.valueOf(status.getStatus())))
                .ifPresent(c -> conversationService.markFinished(c));

        Audit.info("Updated receipt (DPI)", externalReceipt.logMarkers());
        externalReceipt.confirmReceipt();
        Audit.info("Confirmed receipt (DPI)", externalReceipt.logMarkers());
    }
}
