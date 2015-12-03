package no.difi.meldingsutveksling.noarkexchange;

public class MessageException extends Exception {
    private final Status status;

    public MessageException(Status status) {
        this.status = status;
    }

    public MessageException(Exception exception, Status status) {
        super(exception);
        this.status = status;
    }

    public Status getStatus() {
        return status;
    }
}
