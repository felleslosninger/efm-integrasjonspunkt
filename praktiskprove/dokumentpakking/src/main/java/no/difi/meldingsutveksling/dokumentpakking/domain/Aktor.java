package no.difi.meldingsutveksling.dokumentpakking.domain;

public abstract class Aktor {
	private Organisasjonsnummer orgNummer;

	public Aktor(Organisasjonsnummer orgNummer) {
		this.orgNummer = orgNummer;
	}

	public Organisasjonsnummer getOrgNummer() {
		return orgNummer;
	}
}
