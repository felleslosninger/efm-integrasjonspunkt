package no.difi.meldingsutveksling.services;

import no.difi.meldingsutveksling.adresseregister.client.AdresseRegisterClient;
import no.difi.meldingsutveksling.adresseregister.client.AdresseregisterVirksertClient;
import no.difi.meldingsutveksling.config.IntegrasjonspunktConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.security.cert.Certificate;


@Component
public class AdresseregisterVirksert implements AdresseregisterService {

    @Autowired
    IntegrasjonspunktConfig configuration;

    AdresseRegisterClient client;

    public AdresseregisterVirksert() {
    }

    @PostConstruct
    public void init() {
        String adresseRegisterEndPointURL = configuration.getAdresseRegisterEndPointURL();
        client = new AdresseregisterVirksertClient(adresseRegisterEndPointURL);
    }

    @Override
    public Certificate getCertificate(String orgNumber) {
        return client.getCertificate(orgNumber);
    }

    public IntegrasjonspunktConfig getConfiguration() {
        return configuration;
    }

    public void setConfiguration(IntegrasjonspunktConfig configuration) {
        this.configuration = configuration;
    }
}
