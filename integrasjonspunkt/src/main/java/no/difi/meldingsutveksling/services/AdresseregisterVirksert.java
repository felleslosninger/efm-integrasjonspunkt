package no.difi.meldingsutveksling.services;

import no.difi.meldingsutveksling.config.IntegrasjonspunktConfig;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
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
        virksertClient = VirksertClientBuilder.newInstance()
                .setScope("no.difi.virksert.scope.DemoScope")
                .setUri(adresseRegisterEndPointURL).build();
    }

    public IntegrasjonspunktConfig getConfiguration() {
        return configuration;
    }

    public void setConfiguration(IntegrasjonspunktConfig configuration) {
        this.configuration = configuration;
    }

    @Override
    public Certificate getCertificate(String orgNumber) {
        try {
            return virksertClient.fetch(orgNumber);
        } catch (VirksertClientException e) {
            throw new MeldingsUtvekslingRuntimeException(e.getMessage(), e);
        }
    }
}
