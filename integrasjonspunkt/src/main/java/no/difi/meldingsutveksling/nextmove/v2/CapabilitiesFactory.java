package no.difi.meldingsutveksling.nextmove.v2;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.DocumentType;
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

    public Capabilities getCapabilities(String receiverIdentifier, Integer securityLevel) {
        List<ServiceRecord> serviceRecords = sr.getServiceRecords(SRParameter.builder(receiverIdentifier)
                .securityLevel(securityLevel).build());

        // Temporary hack to support old "arkivmelding" beta receivers - will be removed in a future update.
        // Integrasjonspunktet will in this case downgrade "arkivmelding" to beta format before sending.
        // See https://difino.atlassian.net/browse/MOVE-1952
        serviceRecords.parallelStream()
                .filter(r -> r.getProcess().contains(DocumentType.ARKIVMELDING.getType()))
                .peek(r -> r.setProcess(r.getProcess().replace("ver1.0", "ver5.5")))
                .forEach(r -> r.setDocumentTypes(r.getDocumentTypes().stream()
                        .map(dt -> dt.replace("arkivmelding:xsd::", "arkivmelding:xsd:arkivmelding55::"))
                        .collect(Collectors.toList())));

        return new Capabilities()
                .setCapabilities(serviceRecords
                        .stream()
                        .map(capabilityFactory::getCapability)
                        .collect(Collectors.toList()));
    }
}
