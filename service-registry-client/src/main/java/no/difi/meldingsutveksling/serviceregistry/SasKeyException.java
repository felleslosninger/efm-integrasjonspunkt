package no.difi.meldingsutveksling.serviceregistry;

public class SasKeyException extends Exception {

    public SasKeyException(String s, Exception e) {
        super(s, e);
    }

    public SasKeyException(String s) {
        super(s);
    }
}
