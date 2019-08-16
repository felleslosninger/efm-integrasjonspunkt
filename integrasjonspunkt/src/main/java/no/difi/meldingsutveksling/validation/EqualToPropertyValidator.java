package no.difi.meldingsutveksling.validation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Objects;

public class EqualToPropertyValidator implements ConstraintValidator<EqualToProperty, String> {

    @Autowired
    private Environment environment;
    private String expectedValue;

    @Override
    public void initialize(EqualToProperty constraintAnnotation) {
        this.expectedValue = environment.getProperty(constraintAnnotation.value());
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return Objects.equals(expectedValue, value);
    }
}
