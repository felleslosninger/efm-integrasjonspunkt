package no.difi.meldingsutveksling.validation;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.DocumentIdentification;
import no.difi.meldingsutveksling.domain.sbdh.Partner;
import no.difi.meldingsutveksling.domain.sbdh.PartnerIdentification;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocumentHeader;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import static no.difi.meldingsutveksling.ServiceIdentifier.UNKNOWN;

public class ReceiverAcceptableServiceIdentifierValidator implements ConstraintValidator<ReceiverAcceptableServiceIdentifier, StandardBusinessDocumentHeader> {

    @Autowired
    private ServiceRegistryLookup sr;

    @Override
    public void initialize(ReceiverAcceptableServiceIdentifier receiverAcceptableServiceIdentifier) {
        // NOOP
    }

    @Override
    public boolean isValid(StandardBusinessDocumentHeader header, ConstraintValidatorContext context) {
        if (header == null) {
            return true;
        }

        ServiceIdentifier serviceIdentifier = getServiceIdentifier(header);
        Set<ServiceIdentifier> acceptableServiceIdentifiers = getAcceptableServiceIdentifiers(header);

        if (acceptableServiceIdentifiers.contains(serviceIdentifier)) {
            return true;
        }

        context.buildConstraintViolationWithTemplate(
                String.format("%s %s",
                        context.getDefaultConstraintMessageTemplate(),
                        acceptableServiceIdentifiers))
                .addConstraintViolation()
                .disableDefaultConstraintViolation();

        return false;
    }

    private ServiceIdentifier getServiceIdentifier(StandardBusinessDocumentHeader header) {
        DocumentIdentification documentIdentification = header.getDocumentIdentification();

        if (documentIdentification == null || documentIdentification.getType() == null) {
            return ServiceIdentifier.UNKNOWN;
        }

        return ServiceIdentifier.safeValueOf(documentIdentification.getType()).orElse(UNKNOWN);
    }

    private Set<ServiceIdentifier> getAcceptableServiceIdentifiers(StandardBusinessDocumentHeader header) {
        Set<ServiceIdentifier> serviceIdentifiers = header.getFirstReceiver()
                .map(Partner::getIdentifier)
                .map(PartnerIdentification::getStrippedValue)
                .map(p -> sr.getServiceRecords(p))
                .orElse(Collections.emptyList())
                .stream()
                .map(ServiceRecord::getServiceIdentifier)
                .collect(Collectors.toSet());

        if (serviceIdentifiers.contains(ServiceIdentifier.DPF)) {
            serviceIdentifiers.add(ServiceIdentifier.DPO);
        }

        return serviceIdentifiers;
    }
}
