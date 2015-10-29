package no.difi.meldingsutveksling.adresseregister.client;

import no.difi.virksert.client.VirksertClient;
import no.difi.virksert.client.VirksertClientBuilder;
import no.difi.virksert.client.VirksertClientException;

import java.security.cert.Certificate;

public class AdresseregisterVirksertClient implements AdresseRegisterClient {
    private final String uri;

    public AdresseregisterVirksertClient(String uri) {
        this.uri = uri;
    }

    @Override
    public Certificate getCertificate(String orgNr) {
        VirksertClient client = VirksertClientBuilder.newInstance().setUri(uri)
                .setScope("demotrust")
                .setIntermediateAliases(new String[]{"difi_demo"})
                .setRootAliases(new String[]{"mykey"}).build();
        try {
            return client.fetch(orgNr);
        } catch (VirksertClientException e) {
            throw new CertificateException(e);
        }
    }
}
