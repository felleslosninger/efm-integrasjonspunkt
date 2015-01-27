package no.difi.meldingsutveksling.services;

import java.security.PublicKey;
import java.security.cert.Certificate;

import no.difi.meldingsutveksling.adresseregister.AddressRegister;
import no.difi.meldingsutveksling.adresseregister.AdressRegisterFactory;

public class AdresseregisterMock implements AdresseregisterService {

	private AddressRegister adresseRegister = AdressRegisterFactory.createAdressRegister();

	@Override
	public PublicKey getPublicKey(String orgNumber) {
		return adresseRegister.getPublicKey(orgNumber);
	}

	@Override
	public Certificate getCertificate(String orgNumber) {
		return adresseRegister.getCertificate(orgNumber);
	}

}
