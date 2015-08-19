package no.difi.meldingsutveksling;

import no.difi.meldingsutveksling.shipping.Request;

public class TryAltinnWsClient {

    public static void main(String[] args) {
        AltinnWsClient client = new AltinnWsClient();
        Request request = new MockRequest();
        client.send(request);
    }
}
