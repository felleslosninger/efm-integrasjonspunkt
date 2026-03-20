package no.difi.meldingsutveksling.nextmove.nhn;

public enum ApprecStatus {
    OK("Ok"),
    REJECTED("Rejected"),
    OK_ERROR_IN_MESSAGE_PART("OkErrorInMessagePart");

    private String value;

    ApprecStatus(String value) {
        this.value = value;
    }

}
