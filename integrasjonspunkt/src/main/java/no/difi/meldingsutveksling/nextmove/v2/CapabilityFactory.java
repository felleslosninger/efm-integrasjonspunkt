package no.difi.meldingsutveksling.nextmove.v2;

import no.difi.meldingsutveksling.domain.capabilities.Capability;
import no.difi.meldingsutveksling.domain.capabilities.DocumentType;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class CapabilityFactory {

    Capability getCapability(ServiceRecord serviceRecord) {
        return new Capability()
                .setProcess(serviceRecord.getProcess())
                .setServiceIdentifier(serviceRecord.getServiceIdentifier())
                .setDocumentTypes(serviceRecord.getDocumentTypes()
                        .stream()
                        .map(standard -> new DocumentType()
                                .setStandard(standard)
                                .setType(getType(standard)))
                        .collect(Collectors.toList())
                );
    }

    private String getType(String standard) {
        int lastColon = standard.lastIndexOf(':');
        return lastColon == -1 || lastColon == standard.length() - 1 ? standard : standard.substring(lastColon + 1);
    }
}
