package no.difi.meldingsutveksling.shipping.ws;

public class AltinnWsException extends RuntimeException {
    public AltinnWsException(String message, AltinnReason altinnReason, Exception e) {
        super(message + " " + altinnReason, e);
    }

    public AltinnWsException(String message, Exception e) {
        super(message, e);
    }
}
