package no.difi.meldingsutveksling.serviceregistry;

public class SasKeyException extends RuntimeException {

    public SasKeyException(String s, Exception e) {
        super(s, e);
    }

    SasKeyException(String s) {
        super(s);
    }
}
