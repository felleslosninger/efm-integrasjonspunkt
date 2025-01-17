package no.difi.meldingsutveksling.validation;

import no.difi.meldingsutveksling.domain.Iso6523;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SenderValueValidator implements ConstraintValidator<SenderValue, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return !Iso6523.parse(value).getOrganizationIdentifier().isEmpty();
    }
}
