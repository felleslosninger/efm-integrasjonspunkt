package no.difi.meldingsutveksling.validation;

import no.difi.meldingsutveksling.domain.sbdh.Scope;
import no.difi.meldingsutveksling.domain.sbdh.ScopeType;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class SbdScopeConditionalInstanceIdentifierUuidValidator implements
        ConstraintValidator<SbdScopeConditionalInstanceIdentifierUuid, Scope> {

    private final UUIDValidator uuidValidator = new UUIDValidator();

    @Override
    public boolean isValid(Scope s, ConstraintValidatorContext constraintValidatorContext) {
        if (ScopeType.CONVERSATION_ID.getFullname().equals(s.getType())) {
            return uuidValidator.isValid(s.getInstanceIdentifier(), constraintValidatorContext);
        }
        // No UUID validation for other scope types
        return true;
    }

    @Override
    public void initialize(SbdScopeConditionalInstanceIdentifierUuid constraintAnnotation) {
    }

}
