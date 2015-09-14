package no.difi.meldingsutveksling;

import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.shipping.UploadRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Properties;

public class TryAltinnWsClient {
    static Properties properties;
    static {
        properties = new Properties();
        try (InputStream is = AltinnWsConfiguration.class.getResourceAsStream("/altinn.properties")){

            properties.load(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        AltinnWsClient client;

        AltinnWsConfiguration senderConfiguration = configurationFromProperties("altinn.username", "altinn.password");

        client = new AltinnWsClient(senderConfiguration);
        UploadRequest request = new MockRequest();
        System.out.println("Uploading as " + request.getSender() + " with credentials " + senderConfiguration.getUsername() + "/" + senderConfiguration.getPassword() + " sender reference: " + request.getSenderReference());
        System.out.println("Recieving orgnumber: " + request.getReceiver());
        client.send(request);
        System.out.println("Testing available files");
        AltinnWsConfiguration receiverConfiguration = configurationFromProperties("receiver.username", "receiver.password");
        AltinnWsClient recieverClient = new AltinnWsClient(receiverConfiguration);
        List<FileReference> result = recieverClient.availableFiles(request.getReceiver());
        for (FileReference fileReference : result) {
            System.out.println(fileReference.getReceiptID());
            System.out.println(fileReference.getValue());

            StandardBusinessDocument sbd = recieverClient.download(new DownloadRequest(fileReference.getValue(), request.getReceiver()));
            System.out.println("downloaded sbd " + sbd);
        }
    }

    public static AltinnWsConfiguration configurationFromProperties(String username, String password) {


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
                .withUsername(properties.getProperty(username))
                .withPassword(properties.getProperty(password))
                .build();

        return configuration;
    }
}
