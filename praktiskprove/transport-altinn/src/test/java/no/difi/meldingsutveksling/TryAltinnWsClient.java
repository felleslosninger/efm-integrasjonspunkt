package no.difi.meldingsutveksling;

import no.difi.meldingsutveksling.shipping.Request;

import java.net.MalformedURLException;

public class TryAltinnWsClient {

    public static void main(String[] args) {
        AltinnWsClient client;
        try {
            client = new AltinnWsClient("http://localhost:7777/altinn/messages");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        Request request = new MockRequest();
        client.send(request);
    }
}
