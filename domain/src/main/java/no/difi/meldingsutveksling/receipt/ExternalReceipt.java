package no.difi.meldingsutveksling.receipt;

public interface ExternalReceipt {
    void update(MessageReceipt messageReceipt);
    void confirmReceipt();
}
