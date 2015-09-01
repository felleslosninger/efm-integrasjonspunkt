package no.difi.meldingsutveksling;

import no.difi.meldingsutveksling.altinn.mock.brokerbasic.BrokerServiceAvailableFile;
import no.difi.meldingsutveksling.altinn.mock.brokerbasic.BrokerServiceAvailableFileStatus;
import no.difi.meldingsutveksling.shipping.Request;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class TryAltinnWsClient {

    public static void main(String[] args) {
        AltinnWsClient client;

        AltinnWsConfiguration configuration;
        try {
            configuration = new AltinnWsConfiguration.Builder()
                    .withStreamingServiceUrl(new URL("http://localhost:7777/altinn/streamedmessage"))
                    .withBrokerServiceUrl(new URL("http://localhost:7777/altinn/messages"))
                    .withUsername("2422")
                    .withPassword("ROBSTAD1")
            .build();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        client = new AltinnWsClient(configuration);
        Request request = new MockRequest();
        System.out.println("Testing send");
        client.send(request);

        System.out.println("Testing available files");
        List<BrokerServiceAvailableFile> result = client.availableFiles(request, BrokerServiceAvailableFileStatus.UPLOADED);
        for (BrokerServiceAvailableFile saf : result) {
            System.out.println(saf.getReceiptID());
            System.out.println(saf.getFileStatus().value());
        }
    }
}
