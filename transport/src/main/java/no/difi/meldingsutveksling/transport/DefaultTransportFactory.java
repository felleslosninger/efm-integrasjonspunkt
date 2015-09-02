package no.difi.meldingsutveksling.transport;

import org.springframework.stereotype.Component;

/**
 * The running instance of the "integrationspunkt" module may specify its own implementation of the TransportFactory.
 * This implementation is scans the classpath for transport.properties. The properties should contain one line
 * per content-type/transport class tuple.
 * <p/>
 * <pre>
 * transport=no.difi.meldingsutveksling.OxalisTransport
 * transport.urn:no:difi:meldingsutveksling:1.0=no.difi.meldingsutveksling.OxalisTransport
 * </pre>
 *
 * @author Glenn Bech
 */
@Component
public class DefaultTransportFactory implements TransportFactory {

    @Override
    public Transport createTransport(no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument message) {
        // try to find transport.properties on classpath
        // try to find integrasjonspunkt.transport environment variable
        // create nulltransport (Exception)

        return new NullTransport();
    }
}
