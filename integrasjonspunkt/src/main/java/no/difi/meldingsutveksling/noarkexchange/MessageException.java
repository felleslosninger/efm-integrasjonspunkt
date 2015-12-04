package no.difi.meldingsutveksling.noarkexchange;

public class MessageException extends Exception {
    private final StatusMessage statusMessage;

    public MessageException(StatusMessage statusMessage) {
        this.statusMessage = statusMessage;
    }

    public MessageException(Exception exception, StatusMessage statusMessage) {
        super(exception);
        this.statusMessage = statusMessage;
    }

    public StatusMessage getStatusMessage() {
        return statusMessage;
    }
}
