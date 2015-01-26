package no.difi.meldingsutveksling.noark;

import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.transport.NullTransport;
import no.difi.meldingsutveksling.transport.OxalisTransport;
import no.difi.meldingsutveksling.transport.Transport;
import no.difi.meldingsutveksling.transport.TransportFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;


/**
 * Proof of concept transport factory that creates An Oxalis transport for persons, and a File based transport for
 * businesses.
 *
 * @author Glenn Bech
 */

@Component
@Qualifier("multiTransport")
public class MultiTransportFactory implements TransportFactory {

    @Override
    public Transport createTransport(StandardBusinessDocument message) {
        // organisations
        if (message.getStandardBusinessDocumentHeader().getSender().get(0).getIdentifier().getValue().length() < 9) {
            return new OxalisTransport();
        } else {
            return new NullTransport();
        }
    }
}