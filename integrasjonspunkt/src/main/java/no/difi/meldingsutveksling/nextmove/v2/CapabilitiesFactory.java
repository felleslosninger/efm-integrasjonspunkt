package no.difi.meldingsutveksling.nextmove.v2;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.domain.capabilities.Capabilities;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CapabilitiesFactory {

    private final CapabilityFactory capabilityFactory;

    public Capabilities getCapabilities(List<ServiceRecord> serviceRecords) {
        return new Capabilities()
                .setCapabilities(serviceRecords
                        .stream()
                        .map(capabilityFactory::getCapability)
                        .collect(Collectors.toList()));
    }
}
