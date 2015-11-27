package no.difi.meldingsutveksling.services;

import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.virksert.client.VirksertClientException;

/**
 * @author Glenn Bech
 */
public class CertificateException extends MeldingsUtvekslingRuntimeException {
    public CertificateException(String message, VirksertClientException e) {
        super(message, e);
    }
}
