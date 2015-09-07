package no.difi.meldingsutveksling;

import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.Organisasjonsnummer;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.shipping.UploadRequest;
import no.difi.meldingsutveksling.transport.Transport;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import static no.difi.meldingsutveksling.domain.Organisasjonsnummer.*;


/**
 * Transport implementation for Altinn message service.
 *
 * @author Glenn Bech
 */
public class AltinnTransport implements Transport {

    private Properties properties;

    @Override
    public void send(final StandardBusinessDocument document) {

        System.setProperty("com.sun.xml.internal.ws.transport.http.client.HttpTransportP‌​ipe.dump", "true");

        properties=new Properties();
        try (InputStream is = AltinnWsConfiguration.class.getResourceAsStream("/altinn.properties")) {
            properties.load(is);
        } catch (IOException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }
        final UploadRequest request1 = new UploadRequest() {
            @Override
            public String getSender() {
                Organisasjonsnummer orgNumberSender = fromIso6523(document.getStandardBusinessDocumentHeader().getSender().get(0).getIdentifier().getValue());
                return orgNumberSender.toString();
            }

            @Override
            public String getReceiver() {
                Organisasjonsnummer orgNumberReceiver = fromIso6523(document.getStandardBusinessDocumentHeader().getReceiver().get(0).getIdentifier().getValue());
                return orgNumberReceiver.toString();
            }

            @Override
            public String getSenderReference() {
                return String.valueOf(Math.random() * 3000);
            }

            @Override
            public StandardBusinessDocument getPayload() {
                return document;
            }
        };

        AltinnWsConfiguration config = configurationFromProperties();
        AltinnWsClient client = new AltinnWsClient(config);
        client.send(request1);
    }

    public AltinnWsConfiguration configurationFromProperties() {

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
