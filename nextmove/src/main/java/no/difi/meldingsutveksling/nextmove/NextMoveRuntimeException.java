package no.difi.meldingsutveksling.nextmove;

public class NextMoveRuntimeException extends RuntimeException {

    NextMoveRuntimeException(Exception e) {
        super(e);
    }

    NextMoveRuntimeException(String s) {
        super(s);
    }

    public NextMoveRuntimeException(String s, Exception e) {
        super(s, e);
    }
}
