package no.difi.meldingsutveksling.transport;

import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;

/**
 * If no transport if configured, this will be used. It throws an exception indicating a configuration Error.
 */
public class NullTransport implements Transport {

    /**
     * @param document An SBD document with a payload consisting of an CMS encrypted ASIC package
     */
    @Override
    public void send(StandardBusinessDocument document) {
        throw new MeldingsUtvekslingRuntimeException("no transport configured");
    }
}
