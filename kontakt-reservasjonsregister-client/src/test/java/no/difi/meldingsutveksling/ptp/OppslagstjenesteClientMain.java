package no.difi.meldingsutveksling.ptp;

import static no.difi.meldingsutveksling.ptp.OppslagstjenesteClient.Configuration;

public class OppslagstjenesteClientMain {
    public static void main(String[] args) {
        final Configuration configuration = new Configuration("https://kontaktinfo-ws-ver2.difi.no/kontaktinfo-external/ws-v5", "changeit", "client_alias", "ver2");
        OppslagstjenesteClient client = new OppslagstjenesteClient(configuration);
        final KontaktInfo kontaktInfo = client.hentKontaktInformasjon("06068700602");
        System.out.println(kontaktInfo);
    }
}
