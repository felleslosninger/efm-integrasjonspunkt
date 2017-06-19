package no.difi.meldingsutveksling.nextmove;

public class NextMoveRuntimeException extends RuntimeException {

    NextMoveRuntimeException(Exception e) {
        super(e);
    }

    public NextMoveRuntimeException(String s, Exception e) {
        super(s, e);
    }
}
