package no.difi.meldingsutveksling.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import no.difi.meldingsutveksling.domain.PartnerIdentifier;

public class SenderValueValidator implements ConstraintValidator<SenderValue, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return PartnerIdentifier.isValid(value);
    }
}
