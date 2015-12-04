package no.difi.meldingsutveksling.services;

import no.difi.virksert.client.VirksertClientException;

/**
 * @author Glenn Bech
 */
public class CertificateException extends MeldingsUtvekslingException {
    public CertificateException(String message, VirksertClientException e) {
        super(message, e);
    }
}
