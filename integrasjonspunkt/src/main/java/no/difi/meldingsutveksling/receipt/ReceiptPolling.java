package no.difi.meldingsutveksling.receipt;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.logging.Audit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
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
    MessageReceiptRepository messageReceiptRepository;

    @PostConstruct
    private void addTestData() {
        messageReceiptRepository.save(MessageReceipt.of("bb8323b9-1023-4046-b620-63c4f9120b62", ServiceIdentifier.DPV));
    }

//    @Scheduled(fixedRate = 30000)
    public void checkReceiptStatus() {
        List<MessageReceipt> receipts = messageReceiptRepository.findByCompleted(false);

        receipts.forEach(r -> {
            log.info(markerFrom(r), "Checking status");
            ReceiptStrategy strategy = ReceiptStrategyFactory.getFactory(r);
            boolean completed = strategy.checkCompleted(r);
            if (completed) {
                Audit.info("Changed status to \"completed\"", markerFrom(r));
                r.setCompleted(true);
                messageReceiptRepository.save(r);
            }
        });

    }
}
