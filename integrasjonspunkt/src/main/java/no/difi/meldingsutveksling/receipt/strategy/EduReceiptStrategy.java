package no.difi.meldingsutveksling.receipt.strategy;

import no.difi.meldingsutveksling.receipt.MessageReceipt;
import no.difi.meldingsutveksling.receipt.ReceiptStrategy;
import org.springframework.stereotype.Component;

@Component
public class EduReceiptStrategy implements ReceiptStrategy {

    @Override
    public boolean checkReceived(MessageReceipt receipt) {
        return false;
    }
}
