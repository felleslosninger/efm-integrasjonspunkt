package no.difi.meldingsutveksling.elma;

import no.difi.meldingsutveksling.AltinnTransport;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.sbdh.Document;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocumentHeader;
import no.difi.meldingsutveksling.transport.Transport;
import no.difi.meldingsutveksling.transport.TransportFactory;
import no.difi.vefa.peppol.common.model.Endpoint;
import no.difi.vefa.peppol.lookup.api.LookupException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * TransportFactory that uses the Peppol ELMA adressing mechanism to look up Endpoints for
 * the Altinn document storage service. The endPoint can vary between
 *
 * @author Glenn Bech
 */

@Component
public class ELMATransportFactory implements TransportFactory {

    @Autowired
    private ELMALookup elmaLookup;

    @Override
    public Transport createTransport(Document message) {
        try {
            StandardBusinessDocumentHeader standardBusinessDocumentHeader = message.getStandardBusinessDocumentHeader();
            Endpoint endpoint = elmaLookup.lookup(standardBusinessDocumentHeader.getReceiverOrganisationNumber());
            return new AltinnTransport(endpoint.getAddress());
        } catch (LookupException e) {
            throw new MeldingsUtvekslingRuntimeException(e.getMessage(), e);
        }
    }
}
