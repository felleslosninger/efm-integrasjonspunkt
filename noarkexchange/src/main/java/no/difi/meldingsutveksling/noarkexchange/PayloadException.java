package no.difi.meldingsutveksling.noarkexchange;

public class PayloadException extends Exception {

    PayloadException(String s) {
        super(s);
    }

    public PayloadException(String s, Exception e) {
        super(s, e);
    }
}
