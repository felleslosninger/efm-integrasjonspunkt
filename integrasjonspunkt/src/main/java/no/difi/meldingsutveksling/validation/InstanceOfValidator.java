package no.difi.meldingsutveksling.validation;

import lombok.extern.slf4j.Slf4j;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

@Slf4j
public class InstanceOfValidator implements ConstraintValidator<InstanceOf, Object> {

    private Class<?> expectedClass;

    @Override
    public void initialize(InstanceOf constraint) {
        this.expectedClass = constraint.value();
    }

    public boolean isValid(Object object, ConstraintValidatorContext context) {
        return object == null || expectedClass.isInstance(object);
    }
}
