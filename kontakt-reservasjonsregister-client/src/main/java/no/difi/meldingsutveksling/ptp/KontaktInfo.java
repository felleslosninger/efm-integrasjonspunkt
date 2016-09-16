package no.difi.meldingsutveksling.ptp;

import com.google.common.base.MoreObjects;
import no.difi.ptp.sikkerdigitalpost.HentPersonerRespons;
import no.difi.ptp.sikkerdigitalpost.Person;

import java.util.Optional;
import java.util.function.Function;

public class KontaktInfo {
    private static Function<? super Person, Optional<KontaktInfo>> personMapperKontaktInfo = (Function<Person, Optional<KontaktInfo>>) person -> Optional.of(new KontaktInfo(person.getX509Sertifikat(), person.getSikkerDigitalPostAdresse().getPostkasseadresse(), person.getSikkerDigitalPostAdresse().getPostkasseleverandoerAdresse()));
    byte[] certificate;
    String orgnrPostkasse;
    String postkasseAdresse;

    public KontaktInfo(byte[] certificate, String orgnrPostkasse, String postkasseAdresse) {
        this.certificate = certificate;
        this.orgnrPostkasse = orgnrPostkasse;
        this.postkasseAdresse = postkasseAdresse;
    }

    public byte[] getCertificate() {
        return certificate;
    }

    public String getOrgnrPostkasse() {
        return orgnrPostkasse;
    }

    public String getPostkasseAdresse() {
        return postkasseAdresse;
    }

    public static KontaktInfo from(HentPersonerRespons hentPersonerRespons) {
        return hentPersonerRespons.getPerson().stream().findFirst().flatMap(personMapperKontaktInfo).orElseThrow(() -> new KontaktInfoException("Mangler kontaktinformasjon"));
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("certificate", certificate)
                .add("orgnrPostkasse", orgnrPostkasse)
                .add("postkasseAdresse", postkasseAdresse)
                .toString();
    }
}
