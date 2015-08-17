package no.difi.meldingsutveksling;

public class TryAltinnClient {
    public static void main(String[] args) {
        AltinnClient altinnClient = new AltinnClient();
        altinnClient.download("test.zip");
    }
}
