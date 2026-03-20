package no.difi.meldingsutveksling.nextmove.nhn;

public enum TransportStatus {

    UNCONFIRMED("Unconfirmed"),
    ACKNOWLEDGED("Acknowledged"),
    REJECTED("Rejected");

    private String value;

    private TransportStatus(String value) {
        this.value = value;
    }

}
