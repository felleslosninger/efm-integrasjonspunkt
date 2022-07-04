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
        this.expectedValue = properties.getOrg().getIdentifier().getIdentifier();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        String[] sbdhSenderComponents = value.split(":");
        if (sbdhSenderComponents.length >= 3) {
            String[] hostIdentifierComponents = expectedValue.split(":");
            return (sbdhSenderComponents[0] + ":" + sbdhSenderComponents[1])
                    .equals(hostIdentifierComponents[0] + ":" + hostIdentifierComponents[1]);
        }

        return Objects.equals(expectedValue, value);
    }
}
