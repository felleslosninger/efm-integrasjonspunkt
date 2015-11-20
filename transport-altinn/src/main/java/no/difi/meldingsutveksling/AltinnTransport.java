package no.difi.meldingsutveksling;

import no.difi.meldingsutveksling.domain.Organisasjonsnummer;
import no.difi.meldingsutveksling.domain.sbdh.Document;
import no.difi.meldingsutveksling.shipping.UploadRequest;
import no.difi.meldingsutveksling.transport.Transport;
import org.apache.commons.configuration.Configuration;

import static no.difi.meldingsutveksling.domain.Organisasjonsnummer.fromIso6523;


/**
 * Transport implementation for Altinn message service.
 */
public class AltinnTransport implements Transport {

    private final String hostName;

    public AltinnTransport(String hostName) {
        this.hostName = hostName;
    }

    /**
     * @param configuration a configuration object given by the integrasjonspunkt
     * @param document      An SBD document with a payload consisting of an CMS encrypted ASIC package
     */
    @Override
    public void send(Configuration configuration, final Document document) {
        AltinnWsClient client = new AltinnWsClient(AltinnWsConfiguration.fromConfiguration(hostName, configuration));
        UploadRequest request1 = new UploadRequest() {

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
            public Document getPayload() {
                return document;
            }
        };

        client.send(request1);
    }
}
