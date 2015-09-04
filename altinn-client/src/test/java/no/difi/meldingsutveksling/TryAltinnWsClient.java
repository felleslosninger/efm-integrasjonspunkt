package no.difi.meldingsutveksling;

import no.difi.meldingsutveksling.shipping.UploadRequest;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Properties;

public class TryAltinnWsClient {

    public static void main(String[] args) {
        AltinnWsClient client;

        AltinnWsConfiguration configuration = configurationFromProperties();

        client = new AltinnWsClient(configuration);
        UploadRequest request = new MockRequest();
        System.out.println("Testing send");
        client.send(request);

        System.out.println("Testing available files");
        List<FileReference> result = client.availableFiles(request.getReceiver());
        for (FileReference fileReference : result) {
            System.out.println(fileReference.getReceiptID());
            System.out.println(fileReference.getValue());

            StandardBusinessDocument sbd = client.download(new DownloadRequest(fileReference.getValue(), request.getReceiver()));
            System.out.println("downloaded sbd " + sbd);
        }
    }

    public static AltinnWsConfiguration configurationFromProperties() {
        Properties properties = new Properties();
        try (InputStream is = AltinnWsConfiguration.class.getResourceAsStream("/altinn.properties")){

            properties.load(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        AltinnWsConfiguration configuration;
        URL brokerServiceUrl;
        try {
            brokerServiceUrl = new URL(properties.getProperty("altinn.brokerservice.url"));
        } catch (MalformedURLException e) {
            throw new RuntimeException("Broker service url is wrong ", e);
        }
        URL streamingServiceUrl;
        try {
            streamingServiceUrl = new URL(properties.getProperty("altinn.streamingservice.url"));
        } catch (MalformedURLException e) {
            throw new RuntimeException("Streaming service url is wrong ", e);
        }
        configuration = new AltinnWsConfiguration.Builder()
                .withBrokerServiceUrl(brokerServiceUrl)
                .withStreamingServiceUrl(streamingServiceUrl)
                .withUsername(properties.getProperty("altinn.username"))
                .withPassword(properties.getProperty("altinn.password"))
                .build();

        return configuration;
    }
}
