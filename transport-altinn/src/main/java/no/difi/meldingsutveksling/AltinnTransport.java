package no.difi.meldingsutveksling;

import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.Organisasjonsnummer;
import no.difi.meldingsutveksling.domain.sbdh.Document;
import no.difi.meldingsutveksling.domain.sbdh.Partner;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocumentHeader;
import no.difi.meldingsutveksling.shipping.UploadRequest;
import no.difi.meldingsutveksling.transport.Transport;
import no.difi.vefa.peppol.common.api.EndpointNotFoundException;
import no.difi.vefa.peppol.common.model.*;
import no.difi.vefa.peppol.lookup.LookupClient;
import no.difi.vefa.peppol.lookup.LookupClientBuilder;
import no.difi.vefa.peppol.lookup.api.LookupException;
import no.difi.vefa.peppol.security.api.PeppolSecurityException;
import org.apache.commons.configuration.Configuration;

import java.util.List;

import static no.difi.meldingsutveksling.domain.Organisasjonsnummer.fromIso6523;


/**
 * Transport implementation for Altinn message service.
 */
public class AltinnTransport implements Transport {

    public static final String DOCUMENT_IDENTIFIER = "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:www.cenbii.eu:transaction:biitrns010:ver2.0:extended:urn:www.peppol.eu:bis:peppol4a:ver2.0::2.1";
    public static final String PROCESS_IDENTIFIER = "urn:www.cenbii.eu:profile:bii04:ver2.0";

    /**
     * @param configuration a configuration object given by the integrasjonspunkt
     * @param document      An SBD document with a payload consisting of an CMS encrypted ASIC package
     */
    @Override
    public void send(Configuration configuration, final Document document) {
        LookupClient lClient = LookupClientBuilder.forTest().build();
        Endpoint endpoint;
        try {
            endpoint = lClient.getEndpoint(
                    new ParticipantIdentifier(getReceiverOrgNr(document)),
                    new DocumentIdentifier(DOCUMENT_IDENTIFIER),
                    new ProcessIdentifier(PROCESS_IDENTIFIER),
                    TransportProfile.AS2_1_0
            );
        } catch (LookupException | PeppolSecurityException | EndpointNotFoundException e) {
            throw new MeldingsUtvekslingRuntimeException(e.getMessage(), e);
        }

        String endPointHostName = endpoint.getAddress();
        AltinnWsClient client = new AltinnWsClient(endPointHostName, AltinnWsConfiguration.fromConfiguration(configuration));

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

    private String getReceiverOrgNr(Document document) {
        StandardBusinessDocumentHeader standardBusinessDocumentHeader = document.getStandardBusinessDocumentHeader();
        if (standardBusinessDocumentHeader == null) {
            throw new IllegalStateException();
        }
        final List<Partner> receiver = standardBusinessDocumentHeader.getReceiver();
        if (receiver == null || receiver.size() != 1) {
            throw new IllegalStateException("receiver is not valid " + receiver);
        }
        final String value = receiver.get(0).getIdentifier().getValue();
        return value;
    }


    private class AltinnTransportException extends RuntimeException {
        public AltinnTransportException(String message, Exception exception) {
            super(message, exception);
        }
    }
}
