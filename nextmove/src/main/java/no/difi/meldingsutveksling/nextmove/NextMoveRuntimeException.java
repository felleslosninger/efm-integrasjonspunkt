package no.difi.meldingsutveksling.nextmove;

public class NextMoveRuntimeException extends RuntimeException {

    public NextMoveRuntimeException(Exception e) {
        super(e);
    }

    public NextMoveRuntimeException(String s) {
        super(s);
    }

    public NextMoveRuntimeException(String s, Throwable e) {
        super(s, e);
    }
}
