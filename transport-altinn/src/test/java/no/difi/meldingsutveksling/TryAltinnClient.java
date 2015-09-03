package no.difi.meldingsutveksling;

import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.shipping.Request;

public class TryAltinnClient {

    static class MockRequest implements Request {

        @Override
        public String getSender() {
            return "123456789";
        }

        @Override
        public String getReceiver() {
            return "987654321";
        }

        @Override
        public String getSenderReference() {
            return "my reference";
        }

        @Override
        public StandardBusinessDocument getPayload() {
            return new StandardBusinessDocument();
        }

    }

    public static void main(String[] args) {
        AltinnFTPClient altinnClient = new AltinnFTPClient();

        System.out.println(".... Uploading test.zip");
        altinnClient.send(new TryAltinnClient.MockRequest());
        System.out.println(".... Downloading test.zip");
        altinnClient.download("test.zip");
    }
}
