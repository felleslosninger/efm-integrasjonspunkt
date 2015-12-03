package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.services.CertificateException;

public class MessageContextException extends MessageException {
    public MessageContextException(Status status) {
        super(status);
    }

    public MessageContextException(CertificateException exception, Status status) {
        super(exception, status);
    }
}