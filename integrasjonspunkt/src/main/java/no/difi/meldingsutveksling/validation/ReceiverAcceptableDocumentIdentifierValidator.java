package no.difi.meldingsutveksling.validation;

import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.domain.sbdh.*;
import no.difi.meldingsutveksling.elma.DocumentIdentifierLookup;
import no.difi.meldingsutveksling.nextmove.NextMoveRuntimeException;
import no.difi.vefa.peppol.common.model.DocumentTypeIdentifier;
import no.difi.vefa.peppol.common.model.ParticipantIdentifier;
import no.difi.vefa.peppol.common.model.Scheme;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

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

        if (!getExpectedResponseDateTime(header)) {
            return false;
        }

   // header.getBusinessscope()
        DocumentTypeIdentifier standard = getStandard(header);

        List<DocumentTypeIdentifier> acceptedDocumentIdentifiers = documentIdentifierLookup.getDocumentIdentifiers(participantIdentifier);

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
        return header.getReceiver().stream().findFirst()
                .map(Partner::getIdentifier)
                .map(p -> ParticipantIdentifier.of(p.getValue(), Scheme.of(p.getAuthority())))
                .orElse(null);
    }

    private DocumentTypeIdentifier getStandard(StandardBusinessDocumentHeader header) {
        DocumentIdentification documentIdentification = header.getDocumentIdentification();

        if (documentIdentification == null) {
            return null;
        }

        return DocumentTypeIdentifier.of(documentIdentification.getStandard());
    }

    public Boolean getExpectedResponseDateTime(StandardBusinessDocumentHeader header) {
        Set<Scope> scope = header.getBusinessScope().getScope();
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime zonedDateTime;
        zonedDateTime = gettingExpectedResponseDateTime(scope, now);

        if (zonedDateTime.isAfter(now) || now.equals(zonedDateTime)) {
            return true;
        }
        else {
            log.error("ExpectedResponseDateTime is expired. Message will not be handled further. Please resend...");
            return false;}
    }

    public ZonedDateTime gettingExpectedResponseDateTime(Set<Scope> scope, ZonedDateTime now) {
        ZonedDateTime zonedDateTime = scope.stream().flatMap(s-> s.getScopeInformation()
                .stream())
                .map(CorrelationInformation::getExpectedResponseDateTime)
                .findFirst()
                .orElseThrow(() -> new NextMoveRuntimeException("No ExpectedResponseDateTime found"));
        return zonedDateTime;
    }

}
