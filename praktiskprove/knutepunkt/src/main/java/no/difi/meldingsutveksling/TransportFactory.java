package no.difi.meldingsutveksling;

import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import org.springframework.stereotype.Component;

/**
 * Factory class to configure transportation. Implementations of this can use content in the StandardBusinessDocument
 * to determine what transport to create for outgoing messages. A running instance has one instance of a transport factory,
 * configured by Spring.
 *
 * @author Glenn Bech
 */
@Component
public interface TransportFactory {

    public abstract Transport createTransport(StandardBusinessDocument message);

}

