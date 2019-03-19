package no.difi.meldingsutveksling;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.transport.Transport;
import no.difi.meldingsutveksling.transport.TransportFactory;
import no.difi.meldingsutveksling.transport.altinn.AltinnTransport;

import java.util.Optional;

import static no.difi.meldingsutveksling.ServiceIdentifier.DPO;
import static no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord.isServiceIdentifier;

/**
 * Used to create transport based on service registry lookup.
 */
@RequiredArgsConstructor
public class ServiceRegistryTransportFactory implements TransportFactory {

    private final ServiceRegistryLookup serviceRegistryLookup;
    private final AltinnWsClientFactory altinnWsClientFactory;
    private final SenderReferenceGenerator senderReferenceGenerator;

    @Override
    public Transport createTransport(StandardBusinessDocument message) {
        return Optional.of(serviceRegistryLookup.getServiceRecord(message.getReceiverOrgNumber()).getServiceRecord())
                .filter(isServiceIdentifier(DPO))
                .map(altinnWsClientFactory::getAltinnWsClient)
                .map(client -> new AltinnTransport(client, senderReferenceGenerator))
                .orElseThrow(() -> new RuntimeException("Failed to create transport"));
    }
}
