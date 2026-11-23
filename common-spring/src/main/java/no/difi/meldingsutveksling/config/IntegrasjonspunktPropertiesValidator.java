package no.difi.meldingsutveksling.config;

import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import java.time.Duration;

public class IntegrasjonspunktPropertiesValidator implements Validator {

    private static final String EMPTY_FIELD = "empty_field";
    static final Duration MAX_DPH_NOTIFICATION_RETRY_PERIOD = Duration.ofDays(365);

    @Override
    public boolean supports(Class<?> clazz) {
        return clazz == IntegrasjonspunktProperties.class;
    }

    @Override
    public void validate(Object target, Errors errors) {
        IntegrasjonspunktProperties props = (IntegrasjonspunktProperties) target;

        if (props.getSign().isEnable()) {
            ValidationUtils.rejectIfEmpty(errors, "sign.jwkUrl", EMPTY_FIELD, "Must not be null if JWS is enabled");
        }
        if(props.getFeature().isEnableDPO()){
            ValidationUtils.rejectIfEmpty(errors, "dpo.resource", EMPTY_FIELD, "Resourceid is required for DPO");
            ValidationUtils.rejectIfEmpty(errors, "dpo.system-user.org-id", EMPTY_FIELD, "Systemuser is required for DPO");
            ValidationUtils.rejectIfEmpty(errors, "dpo.system-user.name", EMPTY_FIELD, "Systemuser is required for DPO");
        }
        if (props.getFeature().isEnableDPH() && props.getDph() != null) {
            Duration retryPeriod = props.getDph().getNotificationRetryPeriod();
            if (retryPeriod != null && (retryPeriod.isNegative() || retryPeriod.compareTo(MAX_DPH_NOTIFICATION_RETRY_PERIOD) > 0)) {
                errors.rejectValue("dph.notificationRetryPeriod", "invalid_duration",
                    "Must be between PT0S and " + MAX_DPH_NOTIFICATION_RETRY_PERIOD);
            }
        }
    }
}
