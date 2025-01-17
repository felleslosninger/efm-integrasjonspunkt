package no.difi.meldingsutveksling.validation;

import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.webhooks.filter.WebhookFilterParser;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

@Slf4j
public class WebhookFilterValidator implements ConstraintValidator<WebhookFilter, String> {

    @Autowired
    private WebhookFilterParser webhookFilterParser;

    @Override
    public void initialize(WebhookFilter constraint) {
        // NOOP
    }

    public boolean isValid(String s, ConstraintValidatorContext context) {
        if (s == null) {
            return true;
        }

        try {
            webhookFilterParser.parse(s).forEach(p -> {
            });
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
