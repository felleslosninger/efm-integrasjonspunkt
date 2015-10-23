package no.difi.meldingsutveksling.adresseregister.client;

import java.security.cert.Certificate;

public interface AdresseRegisterClient {
    /**
     * Gets the certificate for the given organisation. The returned class is a X509 certificate.
     *
     * @param orgNr organisation number (9 digits)
     * @return The certificate for the organisation
     * @see java.security.cert.X509Certificate
     */
    public Certificate getCertificate(String orgNr);
}
