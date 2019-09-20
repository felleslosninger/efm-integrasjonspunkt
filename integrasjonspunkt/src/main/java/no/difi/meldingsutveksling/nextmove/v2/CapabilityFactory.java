package no.difi.meldingsutveksling.nextmove.v2;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.domain.capabilities.Capability;
import no.difi.meldingsutveksling.domain.capabilities.DocumentType;
import no.difi.meldingsutveksling.domain.capabilities.PostalAddress;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CapabilityFactory {

    private final PostalAddressFactory postalAddressFactory;

    Capability getCapability(ServiceRecord serviceRecord) {
        return new Capability()
                .setProcess(serviceRecord.getProcess())
                .setServiceIdentifier(serviceRecord.getServiceIdentifier())
                .setReturnAddress(getReturnAddress(serviceRecord))
                .setPostAddress(getPostAddress(serviceRecord))
                .setDocumentTypes(serviceRecord.getDocumentTypes()
                        .stream()
                        .map(standard -> new DocumentType()
                                .setStandard(standard)
                                .setType(getType(standard)))
                        .collect(Collectors.toList())
                );
    }

    private PostalAddress getReturnAddress(ServiceRecord serviceRecord) {
        return serviceRecord.getServiceIdentifier() == ServiceIdentifier.DPI
                ? postalAddressFactory.getPostalAddress(serviceRecord.getReturnAddress())
                : null;
    }

    private PostalAddress getPostAddress(ServiceRecord serviceRecord) {
        return serviceRecord.getServiceIdentifier() == ServiceIdentifier.DPI
                ? postalAddressFactory.getPostalAddress(serviceRecord.getPostAddress())
                : null;
    }

    private String getType(String standard) {
        int lastColon = standard.lastIndexOf(':');
        return lastColon == -1 || lastColon == standard.length() - 1 ? standard : standard.substring(lastColon + 1);
    }
}
