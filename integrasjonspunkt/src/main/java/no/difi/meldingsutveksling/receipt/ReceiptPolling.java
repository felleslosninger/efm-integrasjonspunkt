package no.difi.meldingsutveksling.receipt;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.dpi.DpiReceiptStatus;
import no.difi.meldingsutveksling.logging.Audit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

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
    private ConversationService conversationService;

    @Autowired
    StatusStrategyFactory statusStrategyFactory;

    @Autowired
    DpiReceiptService dpiReceiptService;

    @Scheduled(fixedRate = 60000)
    public void checkReceiptStatus() {
        if (!props.getFeature().isEnableReceipts()) {
            return;
        }

        List<Conversation> conversations = conversationRepository.findByPollable(true);

        conversations.forEach(c -> {
            if (serviceEnabled(c.getServiceIdentifier())) {
                log.debug(markerFrom(c), "Checking status, conversationId={}", c.getConversationId());
                StatusStrategy strategy = statusStrategyFactory.getFactory(c);
                strategy.checkStatus(c);
            }
        });

    }

    @Scheduled(fixedRate = 10000)
    public void dpiReceiptsScheduledTask() {
        if (props.getFeature().isEnableReceipts() && props.getFeature().isEnableDPI()) {
            final ExternalReceipt externalReceipt = dpiReceiptService.checkForReceipts();
            if (externalReceipt != EMPTY_KVITTERING) {
                externalReceipt.auditLog();
                final String id = externalReceipt.getId();
                MessageStatus status = externalReceipt.toMessageStatus();
                Optional<Conversation> c = conversationService.registerStatus(id, status);
                if (c.isPresent() && DpiReceiptStatus.LEST.toString().equals(status.getStatus())) {
                    conversationService.markFinished(c.get());
                }
                Audit.info("Updated receipt (DPI)", externalReceipt.logMarkers());
                externalReceipt.confirmReceipt();
                Audit.info("Confirmed receipt (DPI)", externalReceipt.logMarkers());
            }
        }
    }

    private boolean serviceEnabled(ServiceIdentifier si) {
        switch (si) {
            case DPO:
                return props.getFeature().isEnableDPO();
            case DPV:
                return props.getFeature().isEnableDPV();
            case DPF:
                return props.getFeature().isEnableDPF();
            case DPI:
                return props.getFeature().isEnableDPI();
            default:
                return false;
        }
    }

}
