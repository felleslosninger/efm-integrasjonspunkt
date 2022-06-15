package no.difi.meldingsutveksling.validation;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Objects;

public class SenderValueValidator implements ConstraintValidator<SenderValue, String> {

    @Autowired
    private IntegrasjonspunktProperties properties;
    private String expectedValue;

    @Override
    public void initialize(SenderValue constraintAnnotation) {
        this.expectedValue = "0192:" + properties.getOrg().getIdentifier();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        String[] split = value.split(":");
        if (split.length == 3) {
            return (split[0]+":"+split[1]).equals(expectedValue);
        }

        return Objects.equals(expectedValue, value);
    }
}
