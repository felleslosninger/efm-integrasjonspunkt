package no.difi.meldingsutveksling.nextmove.nhn;

interface KodeverkVerdi {
    String getVerdi();
    String getNavn();
    String getKodeverk();
}

public enum FeilmeldingForApplikasjonskvittering implements KodeverkVerdi {
    UKJENT("Ukjent", "Ukjent"),
    UGYLDIG_ID("E10", "Ugyldig meldingsidentifikator"),
    LEGE_FINNES_IKKE("E20", "Lege finnes ikke"),
    MOTTAKER_FINNES_IKKE("E21", "Mottaker finnes ikke"),
    PASIENT_MANGLER_FNR("E30", "Pasientens fødselsnummer mangler"),
    PASIENT_FEIL_FNR("E31", "Pasientens fødselsnummer er feil"),
    PASIENT_MANGLER_NAVN("E32", "Pasientens navn mangler"),
    PASIENT_EKSISTERER_IKKE_HOS_MOTTAKER("E35", "Pasienten finnes ikke i mottakersystemet"),
    UTILSTREKKELIG_PASIENTOPPLYSNING("E36", "Pasientopplysninger er utilstrekkelige"),
    PASIENT_FNR_IKKE_REGISTRERT_FREG("E53", "Pasientens fødselsnummer eller D-nummer finnes ikke registrert i Folkeregisteret."),
    PASIENT_FNR_IKKE_REGISTRERT_MOTTAKER("E54", "Pasientens fødselsnummer er ikke registrert i mottagersystemet"),
    SIGNATURFEIL("S01", "Feil på signatur"),
    UGYLIG_SERTIFIKAT("S02", "Ugyldig sertifikat"),
    TILBAKETRUKKET_SERTIFIKAT("S03", "Tilbaketrukket sertifikat"),
    UGYLDIG_XML("T01", "Ikke XML / ikke 'well formed' / uleselig"),
    VALIDERINGSFEIL_XML("T02", "XML validerer ikke"),
    IKKE_STOTTET_FORMAT("T10", "Støtter ikke meldingsformatet"),
    ANNEN_FEIL_FORMAT("T99", "Annen feil på format"),
    ANNEN_FEIL("X99", "Annen feil");

    private final String verdi;
    private final String navn;
    private final String kodeverk = "2.16.578.1.12.4.1.1.8221";

    FeilmeldingForApplikasjonskvittering(String verdi, String navn) {
        this.verdi = verdi;
        this.navn = navn;
    }

    @Override
    public String getVerdi() {
        return verdi;
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String getKodeverk() {
        return kodeverk;
    }
}
