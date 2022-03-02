package no.difi.meldingsutveksling.validation;

import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.ApiType;
import no.difi.meldingsutveksling.MessageType;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

@Slf4j
public class MessageTypeValidator implements ConstraintValidator<no.difi.meldingsutveksling.validation.MessageType, String> {

    @Override
    public void initialize(no.difi.meldingsutveksling.validation.MessageType constraint) {
        // Nothing to initialize
    }

    public boolean isValid(String s, ConstraintValidatorContext context) {
        return MessageType.valueOf(s, ApiType.NEXTMOVE).isPresent();
    }
}
