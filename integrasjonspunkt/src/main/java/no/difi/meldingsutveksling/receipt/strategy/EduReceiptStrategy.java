package no.difi.meldingsutveksling.receipt.strategy;

import no.difi.meldingsutveksling.receipt.MessageReceipt;
import no.difi.meldingsutveksling.receipt.ReceiptStrategy;

public class EduReceiptStrategy implements ReceiptStrategy {

    @Override
    public boolean checkCompleted(MessageReceipt receipt) {
        return false;
    }
}
