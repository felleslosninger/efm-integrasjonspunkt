package no.difi.meldingsutveksling.noark;


import no.difi.meldingsutveksling.AltinnTransport;
import no.difi.meldingsutveksling.domain.sbdh.Document;
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
    public Transport createTransport(Document message) {
        return new AltinnTransport();
    }
}