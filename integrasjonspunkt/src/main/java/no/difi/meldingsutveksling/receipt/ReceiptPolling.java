package no.difi.meldingsutveksling.receipt;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

import static no.difi.meldingsutveksling.receipt.MessageReceiptMarker.markerFrom;

/**
 * Periodically checks non final receipts, and their respective services for updates.
 */
@Component
public class ReceiptPolling {


    private static final Logger log = LoggerFactory.getLogger(ReceiptPolling.class);

    @Autowired
    private IntegrasjonspunktProperties props;

    @Autowired
    private MessageReceiptRepository messageReceiptRepository;

    @Autowired
    ReceiptStrategyFactory receiptStrategyFactory;

    // TODO: fjernes etter test!
    // TODO: fjernes etter test!
    @PostConstruct
    private void addTestData() {
//        messageReceiptRepository.save(MessageReceipt.of("bb8323b9-1023-4046-b620-63c4f9120b62",
//                "123", "foo", ServiceIdentifier.EDU));
//        messageReceiptRepository.save(MessageReceipt.of("bb8323b9-1023-4046-b620-63c4f9120b63",
//                "456", "bar", ServiceIdentifier.EDU));
//        messageReceiptRepository.save(MessageReceipt.of("bb8323b9-1023-4046-b620-63c4f9120b64",
//                "123456", "foobar", ServiceIdentifier.EDU));
    }

    @Scheduled(fixedRate = 60000)
    public void checkReceiptStatus() {
        if (!props.getFeature().isEnableReceipts()) {
            return;
        }

        List<MessageReceipt> receipts = messageReceiptRepository.findByReceived(false);

        receipts.forEach(receipt -> {
            log.debug(markerFrom(receipt), "Checking status, messageId={}", receipt.getMessageId());
            ReceiptStrategy strategy = receiptStrategyFactory.getFactory(receipt);
            final ExternalReceipt externalReceipt = strategy.getReceipt();
            externalReceipt.update(receipt);
            // Save regardless due to possible change to lastUpdate
            messageReceiptRepository.save(receipt);
            externalReceipt.confirmReceipt();
        });

    }
}
