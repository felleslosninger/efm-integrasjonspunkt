package no.difi.meldingsutveksling.domain;

import java.security.cert.X509Certificate;

public class Mottaker extends Aktor {

    private X509Certificate sertifikat;

    /**
     * @param organisasjonsnummer Organisasjonsnummeret til avsender av brevet.
     * @param certificate         Sertifikat
     */
    public static Builder builder(Organisasjonsnummer organisasjonsnummer, X509Certificate certificate) {
        return new Builder(organisasjonsnummer, certificate);
    }

    public  Mottaker(Organisasjonsnummer orgNummer, X509Certificate sertifikat) {
        super(orgNummer);
        setSertifikat(sertifikat);
    }

    public X509Certificate getSertifikat() {
        return sertifikat;
    }

    private void setSertifikat(X509Certificate sertifikat) {
        this.sertifikat = sertifikat;
    }


    public final static class Builder {

        private final Mottaker target;
        private boolean built = false;

        private Builder(Organisasjonsnummer orgNummer, X509Certificate certificate) {
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
