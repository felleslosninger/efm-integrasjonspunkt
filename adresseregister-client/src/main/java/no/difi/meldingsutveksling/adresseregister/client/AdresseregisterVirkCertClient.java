package no.difi.meldingsutveksling.adresseregister.client;

import no.difi.virksert.client.VirksertClient;
import no.difi.virksert.client.VirksertClientBuilder;
import no.difi.virksert.client.VirksertClientException;

import java.security.cert.Certificate;

public class AdresseregisterVirkCertClient implements AdresseRegisterClient{
    private final String uri;
    private final String scope;

    public AdresseregisterVirkCertClient(String uri, String scope) {
        this.uri = uri;
        this.scope = scope;
    }

    @Override
    public Certificate getCertificate(String orgNr) {
        VirksertClient client = VirksertClientBuilder.newInstance().setUri(uri).setScope(scope).build();
        try {
            return client.fetch(orgNr);
        } catch (VirksertClientException e) {
            throw new CertificateNotFoundException(e);
        }
    }
}
