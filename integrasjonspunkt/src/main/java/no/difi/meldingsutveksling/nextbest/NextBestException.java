package no.difi.meldingsutveksling.nextbest;

public class NextBestException extends Exception {

    NextBestException(String s) {
        super(s);
    }

    NextBestException(Exception e) {
        super(e);
    }

    NextBestException(String s, Exception e) {
        super(s, e);
    }
}
