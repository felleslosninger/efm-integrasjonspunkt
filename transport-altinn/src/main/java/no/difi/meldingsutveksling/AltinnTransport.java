package no.difi.meldingsutveksling;

import no.difi.meldingsutveksling.common.PropertyLoader;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.shipping.UploadRequest;
import no.difi.meldingsutveksling.transport.Transport;

import java.net.MalformedURLException;
import java.net.URL;

public class AltinnTransport implements Transport {
    PropertyLoader propertyLoader;

    public AltinnTransport() {

    }

    public static void main(String[] args) {
        new AltinnTransport().send(new StandardBusinessDocument());
    }

    // create zip from standard business document
    // upload zip file (need to merge in Ftp branch)
    // get receipt <-- ok
    @Override
    public void send(StandardBusinessDocument document) {
        try {
            new AltinnWsConfiguration.Builder().withBrokerServiceUrl(new URL("")).withStreamingServiceUrl(new URL("")).withUsername("donald").withPassword("password").build();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        AltinnWsClient client = new AltinnWsClient(AltinnWsConfiguration.fromProperties());

        UploadRequest request1 = new UploadRequest() {

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
        };

        client.send(request1);
    }



    private class AltinnTransportException extends RuntimeException {
        public AltinnTransportException(String message, Exception exception) {
            super(message, exception);
        }
    }
}
