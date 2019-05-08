package no.difi.meldingsutveksling;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.transport.Transport;
import no.difi.meldingsutveksling.transport.TransportFactory;
import no.difi.meldingsutveksling.transport.altinn.AltinnTransport;

import static no.difi.meldingsutveksling.ServiceIdentifier.DPO;

/**
 * Used to create transport based on service registry lookup.
 */
@RequiredArgsConstructor
public class ServiceRegistryTransportFactory implements TransportFactory {

    private final ServiceRegistryLookup serviceRegistryLookup;
    private final AltinnWsClientFactory altinnWsClientFactory;
    private final UUIDGenerator uuidGenerator;

    @Override
    public Transport createTransport(StandardBusinessDocument message) {
        return serviceRegistryLookup.getServiceRecord(message.getReceiverIdentifier(), DPO)
                .map(altinnWsClientFactory::getAltinnWsClient)
                .map(client -> new AltinnTransport(client, uuidGenerator))
                .orElseThrow(() -> new MeldingsUtvekslingRuntimeException(String.format("Failed to create altinn transport, no DPO service record found for %s", message.getReceiverIdentifier())));
    }
}
