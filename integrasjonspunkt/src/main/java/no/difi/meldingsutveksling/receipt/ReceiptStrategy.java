package no.difi.meldingsutveksling.receipt;

public interface ReceiptStrategy {
    public boolean checkCompleted(MessageReceipt receipt);
}
