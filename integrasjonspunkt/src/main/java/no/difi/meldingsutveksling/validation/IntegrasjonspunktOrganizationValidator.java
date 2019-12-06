package no.difi.meldingsutveksling.validation;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Objects;

public class IntegrasjonspunktOrganizationValidator implements ConstraintValidator<IntegrasjonspunktOrganization, String> {

    @Autowired
    private IntegrasjonspunktProperties properties;
    private String expectedValue;

    @Override
    public void initialize(IntegrasjonspunktOrganization constraintAnnotation) {
        this.expectedValue = "0192:" + properties.getOrg().getNumber();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        return Objects.equals(expectedValue, value);
    }
}
