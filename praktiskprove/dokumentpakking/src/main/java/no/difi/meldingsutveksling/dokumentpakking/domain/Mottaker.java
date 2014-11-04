package no.difi.meldingsutveksling.dokumentpakking.domain;

import no.difi.meldingsutveksling.dokumentpakking.crypto.Sertifikat;

public class Mottaker extends Aktor {

	private Sertifikat sertifikat;
	
	public Mottaker(Organisasjonsnummer orgNummer, Sertifikat sertifikat) {
		super(orgNummer);
		setSertifikat(sertifikat);
	}

	public Sertifikat getSertifikat() {
		return sertifikat;
	}

	private void setSertifikat(Sertifikat sertifikat) {
		this.sertifikat = sertifikat;
	}

}
