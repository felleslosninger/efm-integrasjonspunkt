package no.difi.meldingsutveksling.services;

import no.difi.meldingsutveksling.config.IntegrasjonspunktConfig;
import no.difi.virksert.client.VirksertClient;
import no.difi.virksert.client.VirksertClientBuilder;
import no.difi.virksert.client.VirksertClientException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.security.cert.Certificate;


@Component
public class AdresseregisterVirksert implements AdresseregisterService {

    @Autowired
    IntegrasjonspunktConfig configuration;

    private VirksertClient virksertClient;

    public AdresseregisterVirksert() {
    }

    @PostConstruct
    public void init() {
        String adresseRegisterEndPointURL = configuration.getAdresseRegisterEndPointURL();
        //todo we hard code this for now. See MIIF-219& MIIF-220
        virksertClient = VirksertClientBuilder.newInstance().setUri(adresseRegisterEndPointURL)
                .setScope("test-certificates")
                .setTrustedIntermediateAliases("intermediate")
                .setTrustedRootAliases("rootcert").build();
    }

    @Override
    public Certificate getCertificate(String orgNumber) {
        try {
            return virksertClient.fetch(orgNumber);
        } catch (VirksertClientException e) {
            throw new CertificateException(e);
        }
    }

    public IntegrasjonspunktConfig getConfiguration() {
        return configuration;
    }

    public void setConfiguration(IntegrasjonspunktConfig configuration) {
        this.configuration = configuration;
    }
}
