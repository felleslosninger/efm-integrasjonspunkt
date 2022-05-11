package no.difi.meldingsutveksling.nextmove.v2;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.domain.capabilities.Capabilities;
import no.difi.meldingsutveksling.serviceregistry.SRParameter;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CapabilitiesFactory {

    private final ServiceRegistryLookup sr;
    private final CapabilityFactory capabilityFactory;

    public Capabilities getCapabilities(String receiverIdentifier, Integer securityLevel, String process) {
        List<ServiceRecord> serviceRecords = sr.getServiceRecords(SRParameter.builder(receiverIdentifier)
                .securityLevel(securityLevel)
                .process(process).build());

        return new Capabilities()
                .setCapabilities(serviceRecords
                        .stream()
                        .map(capabilityFactory::getCapability)
                        .collect(Collectors.toList()));
    }
}
