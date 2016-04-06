package no.difi.meldingsutveksling.transport.altinn;

import no.difi.meldingsutveksling.AltinnWsClient;
import no.difi.meldingsutveksling.AltinnWsConfiguration;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.Organisasjonsnummer;
import no.difi.meldingsutveksling.domain.sbdh.EduDocument;
import no.difi.meldingsutveksling.elma.ELMALookup;
import no.difi.meldingsutveksling.shipping.UploadRequest;
import no.difi.meldingsutveksling.transport.Transport;
import no.difi.vefa.peppol.common.model.Endpoint;
import no.difi.vefa.peppol.lookup.api.LookupException;
import org.springframework.core.env.Environment;

import static no.difi.meldingsutveksling.domain.Organisasjonsnummer.fromIso6523;


/**
 * Transport implementation for Altinn message service.
 */
public class AltinnTransport implements Transport {

    private final String organisationNumber;
    private final ELMALookup elmaLookup;

    public AltinnTransport(String organisationNumber, ELMALookup elmaLookup) {
        this.organisationNumber = organisationNumber;
        this.elmaLookup = elmaLookup;
    }

    /**
     * @param environment a configuration object given by the integrasjonspunkt
     * @param eduDocument      An eduDocument with a payload consisting of an CMS encrypted ASIC package
     */
    @Override
    public void send(Environment environment, final EduDocument eduDocument) {
        Endpoint ep;
        try {
            ep = elmaLookup.lookup(organisationNumber);
        } catch (LookupException e) {
            throw new MeldingsUtvekslingRuntimeException(e.getMessage(), e);
        }
        AltinnWsClient client = new AltinnWsClient(AltinnWsConfiguration.fromConfiguration(ep.getAddress(), environment));
        UploadRequest request1 = new UploadRequest() {

            @Override
            public String getSender() {
                Organisasjonsnummer orgNumberSender = fromIso6523(eduDocument.getStandardBusinessDocumentHeader().getSender().get(0).getIdentifier().getValue());
                return orgNumberSender.toString();
            }

            @Override
            public String getReceiver() {
                Organisasjonsnummer orgNumberReceiver = fromIso6523(eduDocument.getStandardBusinessDocumentHeader().getReceiver().get(0).getIdentifier().getValue());
                return orgNumberReceiver.toString();
            }

            @Override
            public String getSenderReference() {
                return String.valueOf(Math.random() * 3000);
            }

            @Override
            public EduDocument getPayload() {
                return eduDocument;
            }
        };

        client.send(request1);
    }
}
