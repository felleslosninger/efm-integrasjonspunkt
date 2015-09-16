package no.difi.meldingsutveksling.transport;

import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import org.apache.commons.configuration.Configuration;

/**
 * If no transport if configured, this will be used. It throws an exception indicating a configuration Error.
 */
public class NullTransport implements Transport {

    /**
     * @param document An SBD document with a payload consisting of an CMS encrypted ASIC package
     * @param config   configuration object, not relevant for the null transport
     */
    @Override
    public void send(Configuration config, StandardBusinessDocument document) {
        throw new MeldingsUtvekslingRuntimeException("no transport configured");
    }
}
