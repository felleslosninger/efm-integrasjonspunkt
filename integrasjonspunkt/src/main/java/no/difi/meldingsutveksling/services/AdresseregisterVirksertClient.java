package no.difi.meldingsutveksling.services;

import no.difi.meldingsutveksling.adresseregister.client.AdresseRegisterClient;
import no.difi.meldingsutveksling.adresseregister.client.CertificateException;
import no.difi.virksert.client.VirksertClient;
import no.difi.virksert.client.VirksertClientBuilder;
import no.difi.virksert.client.VirksertClientException;

import java.security.cert.Certificate;

public class AdresseregisterVirksertClient implements AdresseRegisterClient {

    private final String url;
    private final String keyStoreName;
    private final String trustedIntermediateCerts;
    private final String trustedRootCertificateAlias;
    private final VirksertClient virksertClient;

    /**
     * @param url                         the URL of the Viksert server
     * @param keyStoreName                the name of the keystore, available on the classpath, without the .jks suffix (example: trust-certificates)
     * @param trustedRootCertificateAlias the alias of a trusted root certificate in the keystore identified by the keystore name
     * @param trustedIntermediateCertificateAlias
     *                                    the alias of a trusted intermediate certifiate int the keystore identified by the keystore name
     */
    public AdresseregisterVirksertClient(String url, String keyStoreName, String trustedRootCertificateAlias, String trustedIntermediateCertificateAlias) {
        this.url = url;
        this.keyStoreName = keyStoreName;
        this.trustedIntermediateCerts = trustedIntermediateCertificateAlias;
        this.trustedRootCertificateAlias = trustedRootCertificateAlias;
        virksertClient = VirksertClientBuilder.newInstance().setUri(url)
                .setScope(keyStoreName)
                .setTrustedIntermediateAliases(trustedIntermediateCerts)
                .setTrustedRootAliases(trustedRootCertificateAlias).build();

    }

    @Override
    public Certificate getCertificate(String orgNr) {
        try {
            return virksertClient.fetch(orgNr);
        } catch (VirksertClientException e) {
            throw new CertificateException(e);
        }
    }
}
