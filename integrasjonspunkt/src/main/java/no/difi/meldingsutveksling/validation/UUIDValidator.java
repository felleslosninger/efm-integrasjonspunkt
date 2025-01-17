package no.difi.meldingsutveksling.validation;

import lombok.extern.slf4j.Slf4j;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

@Slf4j
public class UUIDValidator implements ConstraintValidator<UUID, String> {

    @Override
    public void initialize(UUID constraint) {
        // NOOP
    }

    public boolean isValid(String s, ConstraintValidatorContext context) {
        return s == null || isUUID(s);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private boolean isUUID(String s) {
        try {
            java.util.UUID.fromString(s);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
