package no.difi.meldingsutveksling.receipt;

public interface ReceiptStrategy {
    boolean checkReceived(MessageReceipt receipt);
}
