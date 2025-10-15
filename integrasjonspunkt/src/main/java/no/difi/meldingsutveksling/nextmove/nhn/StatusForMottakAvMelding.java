package no.difi.meldingsutveksling.nextmove.nhn;

public enum StatusForMottakAvMelding implements KodeverkVerdi {
    OK("1", "OK"),
    AVVIST("2", "Avvist"),
    OK_FEIL_I_DELMELDING("3", "OK, feil i delmelding");

    private final String verdi;
    private final String navn;
    private final String kodeverk = "2.16.578.1.12.4.1.1.8258";

    StatusForMottakAvMelding(String verdi, String navn) {
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
