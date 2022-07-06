package no.difi.meldingsutveksling.nextmove.v2;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.MessageType;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.domain.capabilities.Capability;
import no.difi.meldingsutveksling.domain.capabilities.DigitalPostAddress;
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
                                .setType(getType(standard, serviceRecord)))
                        .collect(Collectors.toList()))
                .setDigitalPostAddress(getDigitalPostAddress(serviceRecord));
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

    private DigitalPostAddress getDigitalPostAddress(ServiceRecord serviceRecord) {
        return serviceRecord.getServiceIdentifier() == ServiceIdentifier.DPI ?
                new DigitalPostAddress()
                        .setAddress(serviceRecord.getPostkasseAdresse())
                        .setSupplier(serviceRecord.getOrgnrPostkasse())
                : null;
    }

    String getType(String standard, ServiceRecord record) {
        return MessageType.valueOfDocumentType(standard)
                .map(MessageType::getType)
                .orElseGet(() -> record.getServiceIdentifier() == ServiceIdentifier.DPFIO ? MessageType.FIKSIO.getType() : null);
    }
}
