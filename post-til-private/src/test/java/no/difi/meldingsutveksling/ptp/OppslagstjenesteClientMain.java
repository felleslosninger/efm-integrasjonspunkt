package no.difi.meldingsutveksling.ptp;

public class OppslagstjenesteClientMain {
    public static void main(String[] args) {
        OppslagstjenesteClient client = new OppslagstjenesteClient();
        client.hentKontaktInformasjon("23079421936");
    }
}
