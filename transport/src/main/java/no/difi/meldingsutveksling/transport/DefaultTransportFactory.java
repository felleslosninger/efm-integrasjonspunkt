package no.difi.meldingsutveksling.transport;

import org.springframework.stereotype.Component;

/**
 * The running instance of the "integrationspunkt" module should specify its own implementation of the TransportFactory.
 * @author Glenn Bech
 */
@Component
public class DefaultTransportFactory implements TransportFactory {

    @Override
    public Transport createTransport(no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument message) {
        return new NullTransport();
    }
}
