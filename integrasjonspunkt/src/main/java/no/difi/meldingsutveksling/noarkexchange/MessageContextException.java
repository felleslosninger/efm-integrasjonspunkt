package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.services.CertificateException;

public class MessageContextException extends MessageException {
    public MessageContextException(StatusMessage statusMessage) {
        super(statusMessage);
    }

    public MessageContextException(CertificateException exception, StatusMessage statusMessage) {
        super(exception, statusMessage);
    }
}