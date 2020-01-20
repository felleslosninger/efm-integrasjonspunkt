package no.difi.meldingsutveksling;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.transport.Transport;
import no.difi.meldingsutveksling.transport.TransportFactory;
import no.difi.meldingsutveksling.transport.altinn.AltinnTransport;

/**
 * Used to create transport based on service registry lookup.
 */
@RequiredArgsConstructor
public class ServiceRegistryTransportFactory implements TransportFactory {

    private final AltinnWsClient altinnWsClient;
    private final UUIDGenerator uuidGenerator;

    @Override
    public Transport createTransport(StandardBusinessDocument message) {
        return new AltinnTransport(altinnWsClient, uuidGenerator);
    }
}
