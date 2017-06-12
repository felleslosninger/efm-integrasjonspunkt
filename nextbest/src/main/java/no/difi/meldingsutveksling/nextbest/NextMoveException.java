package no.difi.meldingsutveksling.nextbest;

public class NextMoveException extends Exception {

    NextMoveException(Exception e) {
        super(e);
    }

    public NextMoveException(String s, Exception e) {
        super(s, e);
    }
}
