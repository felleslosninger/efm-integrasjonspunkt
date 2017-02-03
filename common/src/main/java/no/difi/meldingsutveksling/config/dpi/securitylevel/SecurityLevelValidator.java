package no.difi.meldingsutveksling.config.dpi.securitylevel;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.List;

class SecurityLevelValidator implements ConstraintValidator<ValidSecurityLevel, Object> {
    private List values;

    @Override
    public void initialize(ValidSecurityLevel value) {
        values = Arrays.asList(value.invalidValues());
    }

    @Override
    public boolean isValid(Object o, ConstraintValidatorContext constraintValidatorContext) {
        return !values.contains(o);
    }
}
