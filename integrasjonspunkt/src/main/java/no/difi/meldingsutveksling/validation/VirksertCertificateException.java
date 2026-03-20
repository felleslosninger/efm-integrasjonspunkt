package no.difi.meldingsutveksling.validation;

public class VirksertCertificateException extends RuntimeException {
    public VirksertCertificateException(Throwable t) {
        super(t);
    }
    public VirksertCertificateException(String s) {
        super(s);
    }
    public VirksertCertificateException(String s, Throwable t) {
        super(s, t);
    }
}
