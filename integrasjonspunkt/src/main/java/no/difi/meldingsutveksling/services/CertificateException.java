package no.difi.meldingsutveksling.services;

import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;

/**
 * @author Glenn Bech
 */
public class CertificateException extends MeldingsUtvekslingRuntimeException {
    public CertificateException(Exception cause) {
        super(cause);
    }
}
