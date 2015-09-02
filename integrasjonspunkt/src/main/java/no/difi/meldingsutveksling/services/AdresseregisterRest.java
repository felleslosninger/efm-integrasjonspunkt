package no.difi.meldingsutveksling.services;

import java.security.PublicKey;
import java.security.cert.Certificate;

import org.springframework.stereotype.Component;

import no.difi.meldingsutveksling.adresseregister.client.AdresseRegisterClient;

@Component
public class AdresseregisterRest implements AdresseregisterService {

	AdresseRegisterClient client = new AdresseRegisterClient();

	@Override
	public PublicKey getPublicKey(String orgNumber) {
		return client.getCertificate(orgNumber).getPublicKey();
	}

	@Override
	public Certificate getCertificate(String orgNumber) {
		return client.getCertificate(orgNumber);
	}

}
