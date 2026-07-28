package no.difi.meldingsutveksling;

import lombok.Getter;

@Getter
public class QueueInterruptException extends RuntimeException {

    private final Integer httpCode;
    private final boolean clientError;

    public QueueInterruptException(String message) {
        super(message);
        this.httpCode = null;
        this.clientError = false;
    }

    public QueueInterruptException(String message, int httpCode, boolean clientError) {
        super(message);
        this.httpCode = httpCode;
        this.clientError = clientError;
    }

}
