package no.difi.meldingsutveksling.domain;

import java.security.cert.X509Certificate;

public class Mottaker extends Aktor {

	private X509Certificate sertifikat;
	
	public Mottaker(Organisasjonsnummer orgNummer, X509Certificate sertifikat) {
		super(orgNummer);
		setSertifikat(sertifikat);
	}

	public X509Certificate getSertifikat() {
		return sertifikat;
	}

	private void setSertifikat(X509Certificate sertifikat) {
		this.sertifikat = sertifikat;
	}

}
