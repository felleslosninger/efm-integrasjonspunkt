package no.difi.meldingsutveksling.domain;


public class Avsender extends Aktor {
	private Noekkelpar noekkelpar;

	public Avsender(Organisasjonsnummer orgNummer, Noekkelpar noekkelpar) {
		super(orgNummer);
		this.setNoekkelpar(noekkelpar);
	}

	/**
	 * @param organisasjonsnummer
	 *            Organisasjonsnummeret til avsender av brevet.
	 * @param noekkelpar
	 *            Avsenders nøkkelpar: signert virksomhetssertifikat og
	 *            tilhørende privatnøkkel.
	 */
	public static Builder builder(Organisasjonsnummer organisasjonsnummer, Noekkelpar noekkelpar) {
		return new Builder(organisasjonsnummer, noekkelpar);
	}

	public Noekkelpar getNoekkelpar() {
		return noekkelpar;
	}

	private void setNoekkelpar(Noekkelpar noekkelpar) {
		this.noekkelpar = noekkelpar;
	}

	public final static class Builder {

		private final Avsender target;
		private boolean built = false;

		private Builder(Organisasjonsnummer orgNummer, Noekkelpar noekkelpar) {
			target = new Avsender(orgNummer, noekkelpar);
		}

		public Avsender build() {
			if (built)
				throw new IllegalStateException("Can't build twice");
			built = true;
			return this.target;
		}
	}
}
