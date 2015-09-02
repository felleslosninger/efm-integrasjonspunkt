package no.difi.meldingsutveksling.transport;

import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;

/**
 * Factory class to configure transport based on a SBD document. Implementations of this can use content in the StandardBusinessDocument
 * to determine what transport to create for outgoing messages.
 *
 * @author Glenn Bech
 */

public interface TransportFactory {

    public abstract Transport createTransport(StandardBusinessDocument message);

}

