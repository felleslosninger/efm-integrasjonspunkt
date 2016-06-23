package no.difi.meldingsutveksling.noarkexchange;

public class MessageContextException extends MessageException {
    public MessageContextException(StatusMessage statusMessage) {
        super(statusMessage);
    }

    public MessageContextException(Exception exception, StatusMessage statusMessage) {
        super(exception, statusMessage);
    }

    public MessageContextException(java.security.cert.CertificateException e, StatusMessage missingSenderCertificate) {
        super(e, missingSenderCertificate);
    }
}