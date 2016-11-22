package no.difi.meldingsutveksling.receipt;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.logging.Audit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

import static no.difi.meldingsutveksling.dpi.MeldingsformidlerClient.EMPTY_KVITTERING;
import static no.difi.meldingsutveksling.receipt.ConversationMarker.markerFrom;

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
    ConversationStrategyFactory conversationStrategyFactory;

    @Autowired
    DpiReceiptService dpiReceiptService;

    @Scheduled(fixedRate = 60000)
    public void checkReceiptStatus() {
        if (!props.getFeature().isEnableReceipts()) {
            return;
        }

        List<Conversation> conversations = conversationRepository.findByPollable(true);

        conversations.forEach(c -> {
            log.info(markerFrom(c), "Checking status, conversationId={}", c.getConversationId());
            ConversationStrategy strategy = conversationStrategyFactory.getFactory(c);
            strategy.checkStatus(c);
        });

    }

    @Scheduled(fixedRate = 10000)
    public void dpiReceiptsScheduledTask() {
        if (props.getFeature().isEnableReceipts() && props.getFeature().isEnableDpiReceipts()) {
            final ExternalReceipt externalReceipt = dpiReceiptService.checkForReceipts();
            if (externalReceipt != EMPTY_KVITTERING) {
                externalReceipt.auditLog();
                final String id = externalReceipt.getId();
                Conversation conversation = conversationRepository.findByConversationId(id).stream().findFirst().orElseGet(externalReceipt::createConversation);
                conversation.addMessageReceipt(externalReceipt.toMessageReceipt());
                conversationRepository.save(conversation);
                Audit.info("Updated receipt (DPI)", externalReceipt.logMarkers());
                externalReceipt.confirmReceipt();
                Audit.info("Confirmed receipt (DPI)", externalReceipt.logMarkers());
            }
        }
    }
}
