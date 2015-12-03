package no.difi.meldingsutveksling.queue.exception;

public class QueueException extends RuntimeException {
    public QueueException(String message, Throwable exception) {
        super(message, exception);
    }
}
