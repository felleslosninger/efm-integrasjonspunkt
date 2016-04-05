package no.difi.meldingsutveksling.transport.altinn;

import no.difi.meldingsutveksling.domain.sbdh.EduDocument;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocumentHeader;
import no.difi.meldingsutveksling.elma.ELMALookup;
import no.difi.meldingsutveksling.transport.Transport;
import no.difi.meldingsutveksling.transport.TransportFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * TransportFactory that uses the Peppol ELMA adressing mechanism to look up Endpoints for
 * the Altinn document storage service. The endPoint can vary between
 *
 * @author Glenn Bech
 */

@Component
public class AltinnTransportFactory implements TransportFactory {

    @Autowired
    ELMALookup elmaLookup;

    @Override
    public Transport createTransport(EduDocument message) {
        StandardBusinessDocumentHeader standardBusinessDocumentHeader = message.getStandardBusinessDocumentHeader();
        final String receiverOrganisationNumber = standardBusinessDocumentHeader.getReceiverOrganisationNumber();
        return new AltinnTransport(receiverOrganisationNumber, elmaLookup);
    }

}
