package no.difi.meldingsutveksling.services;

import java.security.PublicKey;

import org.springframework.stereotype.Component;

import no.difi.meldingsutveksling.adresseregmock.AddressRegister;
import no.difi.meldingsutveksling.adresseregmock.AdressRegisterFactory;

@Component
public class AdresseregisterMock implements AdresseregisterService {

	private AddressRegister adresseRegister = AdressRegisterFactory.createAdressRegister();

	@Override
	public PublicKey getPublicKey(String orgNumber) {
		return adresseRegister.getPublicKey(orgNumber);
	}

	@Override
	public Object getCertificate(String orgNumber) {
		return adresseRegister.getCertificate(orgNumber);
	}

}
