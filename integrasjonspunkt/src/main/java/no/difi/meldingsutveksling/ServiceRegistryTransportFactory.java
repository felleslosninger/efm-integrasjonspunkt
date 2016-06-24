package no.difi.meldingsutveksling;

import no.difi.meldingsutveksling.config.IntegrasjonspunktConfiguration;
import no.difi.meldingsutveksling.domain.sbdh.EduDocument;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import no.difi.meldingsutveksling.transport.PostVirksomhetTransport;
import no.difi.meldingsutveksling.transport.Transport;
import no.difi.meldingsutveksling.transport.TransportFactory;
import no.difi.meldingsutveksling.transport.altinn.AltinnTransport;

import java.util.Optional;
import java.util.function.Predicate;

public class ServiceRegistryTransportFactory implements TransportFactory {

    ServiceRegistryLookup serviceRegistryLookup;

    public ServiceRegistryTransportFactory(ServiceRegistryLookup serviceRegistryLookup, IntegrasjonspunktConfiguration configuration) {
        this.serviceRegistryLookup = serviceRegistryLookup;
    }

    @Override
    public Transport createTransport(EduDocument message) {
        final ServiceRecord primaryServiceRecord = serviceRegistryLookup.getPrimaryServiceRecord(message.getReceiverOrgNumber());
        primaryServiceRecord.getServiceIdentifier();

        Optional<ServiceRecord> serviceRecord = Optional.of(primaryServiceRecord);

        Optional<Transport> transport = serviceRecord.filter(isServiceIdentifier("edu")).map(s -> new AltinnTransport(s.getEndPointURL()));
        if(transport.isPresent()) {
            transport = serviceRecord.filter(isServiceIdentifier("post")).map(s -> new PostVirksomhetTransport(s.getEndPointURL()));
        }
        return transport.orElseThrow(() -> new RuntimeException("Failed to create transport"));
    }

    private Predicate<? super ServiceRecord> isServiceIdentifier(String identifier) {
        return s -> s.getServiceIdentifier().equalsIgnoreCase(identifier);
    }

}
