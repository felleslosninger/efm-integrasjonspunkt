package no.difi.meldingsutveksling.dokumentpakking.domain;

import java.security.PublicKey;

public class Mottaker extends Aktor {

	private PublicKey publicKey;
	
	public Mottaker(Organisasjonsnummer orgNummer, PublicKey publicKey) {
		super(orgNummer);
		setPublicKey(publicKey);
	}

	public PublicKey getPublicKey() {
		return publicKey;
	}

	private void setPublicKey(PublicKey publicKey) {
		this.publicKey = publicKey;
	}

}
