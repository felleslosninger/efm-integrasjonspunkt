package no.difi.meldingsutveksling.validation;

import lombok.extern.slf4j.Slf4j;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Slf4j
public class OneOfValidator implements ConstraintValidator<OneOf, String> {

    private Set<String> acceptedValues;

    @Override
    public void initialize(OneOf constraint) {
        this.acceptedValues = new HashSet<>(Arrays.asList(constraint.value()));
    }

    public boolean isValid(String s, ConstraintValidatorContext context) {
        return s == null || acceptedValues.contains(s);
    }
}
