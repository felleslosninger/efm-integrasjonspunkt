package no.difi.meldingsutveksling.ks.svarut;

public class SvarUtServiceException extends RuntimeException {
    public SvarUtServiceException(String s, Exception e) {
        super(s, e);
    }
}
