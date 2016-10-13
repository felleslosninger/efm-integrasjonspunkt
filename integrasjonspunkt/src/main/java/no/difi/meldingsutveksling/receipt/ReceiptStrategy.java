package no.difi.meldingsutveksling.receipt;

public interface ReceiptStrategy {
    boolean checkCompleted(MessageReceipt receipt);
}
