package no.difi.meldingsutveksling.receipt;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.logging.Audit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

import static no.difi.meldingsutveksling.ptp.MeldingsformidlerClient.EMPTY_KVITTERING;
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

    // TODO: fjernes etter test!
    @PostConstruct
    private void addTestData() {
//        MessageReceipt r = MessageReceipt.of(ReceiptStatus.SENT, LocalDateTime.now());
//        conversationRepository.save(Conversation.of("bb8323b9-1023-4046-b620-63c4f9120b62", "foo", "123",
//                "fooTitle", ServiceIdentifier.EDU, r));
//        MessageReceipt ra1 = MessageReceipt.of(ReceiptStatus.SENT, LocalDateTime.now());
//        MessageReceipt ra2 = MessageReceipt.of(ReceiptStatus.DELIVERED, LocalDateTime.now().plusMinutes(1));
//        conversationRepository.save(Conversation.of("bb8323b9-1023-4046-b620-63c4f9120b63", "bar", "456",
//                "barTitle", ServiceIdentifier.EDU, ra1, ra2));
//        MessageReceipt rb1 = MessageReceipt.of(ReceiptStatus.SENT, LocalDateTime.now());
//        MessageReceipt rb2 = MessageReceipt.of(ReceiptStatus.DELIVERED, LocalDateTime.now().plusMinutes(1));
//        MessageReceipt rb3 = MessageReceipt.of(ReceiptStatus.READ, LocalDateTime.now().plusMinutes(5));
//        Conversation c = Conversation.of("bb8323b9-1023-4046-b620-63c4f9120b64", "foobar",  "123456",
//                "foobarTitle", ServiceIdentifier.EDU, rb1, rb2, rb3);
//        c.setPollable(false);
//        conversationRepository.save(c);
    }

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
        final ExternalReceipt externalReceipt = dpiReceiptService.checkForReceipts();
        if(externalReceipt != EMPTY_KVITTERING) {
            Audit.info("Got receipt (DPI)", externalReceipt.logMarkers());
            final String id = externalReceipt.getId();
            Conversation conversation = conversationRepository.findOne(id);
            // TODO: add receipt update to conversation
//            receipt = externalReceipt.update(receipt);
            conversationRepository.save(conversation);
            Audit.info("Updated receipt (DPI)", externalReceipt.logMarkers());
            externalReceipt.confirmReceipt();
            Audit.info("Confirmed receipt (DPI)", externalReceipt.logMarkers());
        }
    }
}
