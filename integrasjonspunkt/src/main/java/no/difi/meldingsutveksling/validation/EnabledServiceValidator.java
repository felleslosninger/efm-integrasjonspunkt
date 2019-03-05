package no.difi.meldingsutveksling.validation;

import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.nextmove.ConversationStrategyFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.stream.Collectors;

@Slf4j
public class EnabledServiceValidator implements ConstraintValidator<EnabledService, String> {

    @Autowired
    private ConversationStrategyFactory strategyFactory;

    public void initialize(EnabledService constraint) {
        // NOOP
    }

    public boolean isValid(String s, ConstraintValidatorContext context) {
        if (s == null) {
            return true;
        }

        try {
            if (strategyFactory.getEnabledServices().contains(ServiceIdentifier.valueOf(s))) {
                return true;
            }
        } catch (Exception e) {
            log.warn(e.getLocalizedMessage());
            // catch exception
        }

        context.buildConstraintViolationWithTemplate(
                String.format("%s %s",
                        context.getDefaultConstraintMessageTemplate(),
                        Arrays.stream(ServiceIdentifier.values())
                                .map(Enum::name)
                                .collect(Collectors.joining(", "))
                )
        )
                .addConstraintViolation()
                .disableDefaultConstraintViolation();

        return false;
    }
}
