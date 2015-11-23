package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.services.CertificateException;

public class AvsenderException extends Throwable {
    public AvsenderException(CertificateException e) {
    }
}
