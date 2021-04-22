package no.difi.meldingsutveksling.validation;

import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.ApiType;
import no.difi.meldingsutveksling.MessageType;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

@Slf4j
public class IsMessageTypeValidator implements ConstraintValidator<IsMessageType, String> {

    private ApiType api;

    @Override
    public void initialize(IsMessageType constraint) {
        this.api = constraint.value();
    }

    public boolean isValid(String s, ConstraintValidatorContext context) {
        return MessageType.valueOf(s, api).isPresent();
    }
}
