package no.difi.meldingsutveksling.validation;

import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.domain.sbdh.DocumentIdentification;
import no.difi.meldingsutveksling.domain.sbdh.Partner;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocumentHeader;
import no.difi.meldingsutveksling.elma.DocumentIdentifierLookup;
import no.difi.vefa.peppol.common.model.DocumentTypeIdentifier;
import no.difi.vefa.peppol.common.model.ParticipantIdentifier;
import no.difi.vefa.peppol.common.model.Scheme;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class ReceiverAcceptableDocumentIdentifierValidator implements ConstraintValidator<ReceiverAcceptableDocumentIdentifier, StandardBusinessDocumentHeader> {

    @Autowired
    private DocumentIdentifierLookup documentIdentifierLookup;

    @Override
    public void initialize(ReceiverAcceptableDocumentIdentifier receiverAcceptableDocumentIdentifier) {
        // NOOP
    }

    @Override
    public boolean isValid(StandardBusinessDocumentHeader header, ConstraintValidatorContext context) {
        if (header == null) {
            return true;
        }

        ParticipantIdentifier participantIdentifier = getParticipantIdentifier(header);

        if (participantIdentifier == null) {
            return true;
        }

        String standard = getStandard(header);

        List<String> acceptedDocumentIdentifiers = documentIdentifierLookup.getDocumentIdentifiers(participantIdentifier)
                .stream()
                .map(DocumentTypeIdentifier::getIdentifier)
                .collect(Collectors.toList());

        if (acceptedDocumentIdentifiers.contains(standard)) {
            return true;
        }

        context.buildConstraintViolationWithTemplate(
                String.format("%s %s",
                        context.getDefaultConstraintMessageTemplate(),
                        acceptedDocumentIdentifiers))
                .addConstraintViolation()
                .disableDefaultConstraintViolation();

        return false;
    }

    private ParticipantIdentifier getParticipantIdentifier(StandardBusinessDocumentHeader header) {
        return header.getFirstReceiver()
                .map(Partner::getIdentifier)
                .map(p -> ParticipantIdentifier.of(p.getValue(), Scheme.of(p.getAuthority())))
                .orElse(null);
    }

    private String getStandard(StandardBusinessDocumentHeader header) {
        DocumentIdentification documentIdentification = header.getDocumentIdentification();

        if (documentIdentification == null) {
            return null;
        }

        return documentIdentification.getStandard();
    }
}
