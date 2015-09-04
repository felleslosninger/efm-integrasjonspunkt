package no.difi.meldingsutveksling;

public class FileReference {
    String value;
    private Integer receiptID;

    public FileReference(String value, Integer receiptID) {
        this.value = value;
        this.receiptID = receiptID;
    }

    public String getValue() {
        return value;
    }

    public Integer getReceiptID() {
        return receiptID;
    }
}
