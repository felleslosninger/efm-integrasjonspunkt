package no.difi.meldingsutveksling.receipt.strategy;

import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.receipt.MessageReceipt;
import no.difi.meldingsutveksling.receipt.ReceiptStrategy;

import static no.difi.meldingsutveksling.receipt.MessageReceiptMarker.markerFrom;

public class NoOperationStrategy implements ReceiptStrategy {
    @Override
    public boolean checkReceived(MessageReceipt receipt) {
        Audit.info("Trying to check a receipt that is not handled by receipt strategy", markerFrom(receipt));
        return false;
    }
}
