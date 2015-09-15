package no.difi.meldingsutveksling.services;

import no.difi.meldingsutveksling.adresseregister.client.AdresseRegisterClient;
import no.difi.meldingsutveksling.config.IntegrasjonspunktConfig;
import org.springframework.stereotype.Component;

import java.security.PublicKey;
import java.security.cert.Certificate;


@Component
public class AdresseregisterRest implements AdresseregisterService {

    AdresseRegisterClient client = new AdresseRegisterClient(IntegrasjonspunktConfig.getInstance().getAdresseRegisterEndPointURL());

    @Override
    public PublicKey getPublicKey(String orgNumber) {
        return client.getCertificate(orgNumber).getPublicKey();
    }

    @Override
    public Certificate getCertificate(String orgNumber) {
        return client.getCertificate(orgNumber);
    }

}
