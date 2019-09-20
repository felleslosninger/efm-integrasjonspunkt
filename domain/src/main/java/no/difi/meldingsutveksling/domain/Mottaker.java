package no.difi.meldingsutveksling.domain;

import java.security.cert.Certificate;

public class Mottaker extends Aktor {

    private Certificate sertifikat;

    /**
     * @param organisasjonsnummer Organisasjonsnummeret til avsender av brevet.
     * @param certificate         Sertifikat
     */
    public static Builder builder(Organisasjonsnummer organisasjonsnummer, Certificate certificate) {
        return new Builder(organisasjonsnummer, certificate);
    }

    public Mottaker(Organisasjonsnummer orgNummer, Certificate sertifikat) {
        super(orgNummer);
        setSertifikat(sertifikat);
    }

    public Certificate getSertifikat() {
        return sertifikat;
    }

    private void setSertifikat(Certificate sertifikat) {
        this.sertifikat = sertifikat;
    }


    public static final class Builder {

        private final Mottaker target;
        private boolean built = false;

        private Builder(Organisasjonsnummer orgNummer, Certificate certificate) {
            target = new Mottaker(orgNummer, certificate);
        }

        public Mottaker build() {
            if (built)
                throw new IllegalStateException("Can't build twice");
            built = true;
            return this.target;
        }
    }

}
