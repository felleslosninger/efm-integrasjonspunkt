package no.difi.meldingsutveksling.validation;

import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.nextmove.BusinessMessageUtil;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

@Slf4j
public class IsMessageTypeValidator implements ConstraintValidator<IsMessageType, String> {

    public boolean isValid(String s, ConstraintValidatorContext context) {
        return BusinessMessageUtil.getMessageTypes().contains(s);
    }

}
