package no.difi.meldingsutveksling.validation;

import no.difi.meldingsutveksling.MimeTypeExtensionMapper;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class AcceptableMimeTypeValidator implements ConstraintValidator<AcceptableMimeType, String> {

    @Override
    public void initialize(AcceptableMimeType acceptableMimeType) {
        // NOOP
    }

    @Override
    public boolean isValid(String s, ConstraintValidatorContext context) {
        if (s == null) {
            return true;
        }

        if (MimeTypeExtensionMapper.isSupportedMimeType(s)) {
            return true;
        }

        context.buildConstraintViolationWithTemplate(
                String.format("%s %s",
                        context.getDefaultConstraintMessageTemplate(),
                        String.join(", ", MimeTypeExtensionMapper.getSupportedMimeTypes())
                )
        )
                .addConstraintViolation()
                .disableDefaultConstraintViolation();

        return false;
    }
}
