package no.difi.meldingsutveksling.nextmove;

public class NextMoveException extends Exception {

    NextMoveException(Exception e) {
        super(e);
    }

    NextMoveException(String s) {
        super(s);
    }

    public NextMoveException(String s, Exception e) {
        super(s, e);
    }
}
