package no.difi.meldingsutveksling.dokumentpakking.domain;

import java.security.PrivateKey;

public class Avsender extends Aktor {
	private PrivateKey privatNokkel;

	public Avsender(Organisasjonsnummer orgNummer, PrivateKey privatNokkel) {
		super(orgNummer);
		this.setPrivatnokkel(privatNokkel);
	}

	/**
	 * @param organisasjonsnummer
	 *            Organisasjonsnummeret til avsender av brevet.
	 * @param noekkelpar
	 *            Avsenders nøkkelpar: signert virksomhetssertifikat og
	 *            tilhørende privatnøkkel.
	 */
	public static Builder builder(Organisasjonsnummer organisasjonsnummer, PrivateKey noekkelpar) {
		return new Builder(organisasjonsnummer, noekkelpar);
	}

	public PrivateKey getPrivatnokkel() {
		return privatNokkel;
	}

	private void setPrivatnokkel(PrivateKey privatNokkel) {
		this.privatNokkel = privatNokkel;
	}

	public static class Builder {

		private final Avsender target;
		private boolean built = false;

		private Builder(Organisasjonsnummer orgNummer, PrivateKey privatNokkel) {
			target = new Avsender(orgNummer, privatNokkel);
		}

		public Avsender build() {
			if (built)
				throw new IllegalStateException("Can't build twice");
			built = true;
			return this.target;
		}
	}
}
