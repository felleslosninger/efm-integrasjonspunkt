package no.difi.meldingsutveksling.services;

import no.difi.meldingsutveksling.adresseregister.client.AdresseRegisterClient;
import no.difi.meldingsutveksling.adresseregister.client.AdresseRegisterPilotClient;
import no.difi.meldingsutveksling.config.IntegrasjonspunktConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.security.PublicKey;
import java.security.cert.Certificate;


@Component
public class AdresseregisterRest implements AdresseregisterService {

    @Autowired
    IntegrasjonspunktConfig configuration;

    AdresseRegisterClient client;

    public AdresseregisterRest() {
    }

    @PostConstruct
    public void init() {
        String adresseRegisterEndPointURL = configuration.getAdresseRegisterEndPointURL();
        client = new AdresseRegisterPilotClient(adresseRegisterEndPointURL);
    }

    @Override
    public PublicKey getPublicKey(String orgNumber) {
        return client.getCertificate(orgNumber).getPublicKey();
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
