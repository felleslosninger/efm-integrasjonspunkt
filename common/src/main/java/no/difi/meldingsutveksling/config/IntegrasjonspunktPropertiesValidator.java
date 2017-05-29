package no.difi.meldingsutveksling.config;

import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

public class IntegrasjonspunktPropertiesValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return clazz == IntegrasjonspunktProperties.class;
    }

    @Override
    public void validate(Object target, Errors errors) {
        IntegrasjonspunktProperties props = (IntegrasjonspunktProperties) target;

        if (props.getFeature().isEnableDPO()) {
            ValidationUtils.rejectIfEmpty(errors, "noarkSystem.type", "empty_field", "DPO enabled - cannot be null");
            ValidationUtils.rejectIfEmpty(errors, "noarkSystem.endpointURL", "empty_field", "DPO enabled - cannot be null");
        }

        if (props.getFeature().isEnableDPV()) {
            ValidationUtils.rejectIfEmpty(errors, "altinnPTV.externalServiceCode", "empty_field", "DPV enabled - cannot be null");
            ValidationUtils.rejectIfEmpty(errors, "altinnPTV.externalServiceEditionCode", "empty_field", "DPV enabled - cannot be null");
            ValidationUtils.rejectIfEmpty(errors, "altinnPTV.username", "empty_field", "DPV enabled - cannot be null");
            ValidationUtils.rejectIfEmpty(errors, "altinnPTV.password", "empty_field", "DPV enabled - cannot be null");
        }

    }
}
