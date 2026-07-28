package no.difi.meldingsutveksling;

import lombok.Getter;

@Getter
public class QueueInterruptException extends RuntimeException {

    private final boolean clientError;

    public QueueInterruptException(String message) {
        super(message);
        this.clientError = false;
    }

    public QueueInterruptException(String message, boolean clientError) {
        super(message);
        this.clientError = clientError;
    }

}
