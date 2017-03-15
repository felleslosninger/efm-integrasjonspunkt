package no.difi.meldingsutveksling;

import no.difi.meldingsutveksling.domain.sbdh.EduDocument;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import no.difi.meldingsutveksling.transport.Transport;
import no.difi.meldingsutveksling.transport.TransportFactory;
import no.difi.meldingsutveksling.transport.altinn.AltinnTransport;

import java.util.Optional;

import static no.difi.meldingsutveksling.ServiceIdentifier.DPO;
import static no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord.isServiceIdentifier;

/**
 * Used to create transport based on service registry lookup.
 */
public class ServiceRegistryTransportFactory implements TransportFactory {
    private ServiceRegistryLookup serviceRegistryLookup;

    /**
     * Creates instance of factory with needed dependency to determine the transport to create
     * @param serviceRegistryLookup service to lookup service record
     */
    public ServiceRegistryTransportFactory(ServiceRegistryLookup serviceRegistryLookup) {
        this.serviceRegistryLookup = serviceRegistryLookup;
    }

    @Override
    public Transport createTransport(EduDocument message) {

        Optional<ServiceRecord> serviceRecord = Optional.of(serviceRegistryLookup.getServiceRecord(message.getReceiverOrgNumber()));

        Optional<Transport> transport = serviceRecord.filter(isServiceIdentifier(DPO)).map(AltinnTransport::new);
        return transport.orElseThrow(() -> new RuntimeException("Failed to create transport"));
    }



}
