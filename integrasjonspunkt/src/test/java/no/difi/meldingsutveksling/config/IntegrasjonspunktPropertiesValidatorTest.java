package no.difi.meldingsutveksling.config;

import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class IntegrasjonspunktPropertiesValidatorTest {

    private final IntegrasjonspunktPropertiesValidator validator = new IntegrasjonspunktPropertiesValidator();

    @Test
    void dph_notification_retry_period_must_not_be_negative() {
        Errors errors = validate(dphWithRetryPeriod(Duration.ofHours(-1)));

        assertThat(errors.getFieldError("dph.notificationRetryPeriod")).isNotNull();
    }

    @Test
    void dph_notification_retry_period_must_not_be_too_long() {
        Errors errors = validate(dphWithRetryPeriod(Duration.ofDays(366)));

        assertThat(errors.getFieldError("dph.notificationRetryPeriod")).isNotNull();
    }

    @Test
    void dph_notification_retry_period_may_be_zero() {
        Errors errors = validate(dphWithRetryPeriod(Duration.ZERO));

        assertThat(errors.hasErrors()).isFalse();
    }

    private Errors validate(IntegrasjonspunktProperties properties) {
        Errors errors = new BeanPropertyBindingResult(properties, "difi.move");
        validator.validate(properties, errors);
        return errors;
    }

    private static IntegrasjonspunktProperties dphWithRetryPeriod(Duration retryPeriod) {
        IntegrasjonspunktProperties properties = new IntegrasjonspunktProperties();
        properties.setSign(new IntegrasjonspunktProperties.Sign());
        properties.setFeature(new IntegrasjonspunktProperties.FeatureToggle().setEnableDPH(true));
        properties.setDph(new DphProperties().setNotificationRetryPeriod(retryPeriod));
        return properties;
    }
}
