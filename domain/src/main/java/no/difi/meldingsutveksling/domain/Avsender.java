package no.difi.meldingsutveksling.domain;


public class Avsender extends Aktor {

    public Avsender(Organisasjonsnummer orgNummer) {
        super(orgNummer);
    }

    /**
     * @param organisasjonsnummer Pakk
     *                            Organisasjonsnummeret til avsender av brevet.
     */
    public static Builder builder(Organisasjonsnummer organisasjonsnummer) {
        return new Builder(organisasjonsnummer);
    }

    public static final class Builder {

        private final Avsender target;
        private boolean built = false;

        private Builder(Organisasjonsnummer orgNummer) {
            target = new Avsender(orgNummer);
        }

        public Avsender build() {
            if (built)
                throw new IllegalStateException("Can't build twice");
            built = true;
            return this.target;
        }
    }
}
