package no.difi.meldingsutveksling.validation;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.DocumentIdentification;
import no.difi.meldingsutveksling.domain.sbdh.PartnerIdentification;
import no.difi.meldingsutveksling.domain.sbdh.Receiver;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocumentHeader;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Collections;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static no.difi.meldingsutveksling.ServiceIdentifier.UNKNOWN;

public class ReceiverAcceptableServiceIdentifierValidator implements ConstraintValidator<ReceiverAcceptableServiceIdentifier, StandardBusinessDocumentHeader> {

    private static final Pattern PARTNER_IDENTIFIER_PATTERN = Pattern.compile("^\\d{4}:(\\d{9})$");

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
        if (header.getReceiver() == null || header.getReceiver().size() != 1) {
            return Collections.emptySet();
        }

        Receiver receiver = header.getReceiver().iterator().next();
        PartnerIdentification identifier = receiver.getIdentifier();

        if (identifier == null || identifier.getValue() == null) {
            return Collections.emptySet();
        }

        Matcher matcher = PARTNER_IDENTIFIER_PATTERN.matcher(identifier.getValue());

        if (!matcher.matches()) {
            return Collections.emptySet();
        }

        String orgnr = matcher.group(1);

        Set<ServiceIdentifier> identifiers = sr.getServiceRecords(orgnr)
                .stream()
                .map(ServiceRecord::getServiceIdentifier)
                .collect(Collectors.toSet());

        if (identifiers.contains(ServiceIdentifier.DPF)) {
            identifiers.add(ServiceIdentifier.DPO);
        }

        return identifiers;
    }
}
