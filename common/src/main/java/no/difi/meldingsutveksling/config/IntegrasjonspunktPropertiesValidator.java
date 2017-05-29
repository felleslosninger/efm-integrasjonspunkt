package no.difi.meldingsutveksling.config;

import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

public class IntegrasjonspunktPropertiesValidator implements Validator {

    private static final String EMPTY_FIELD = "empty_field";

    @Override
    public boolean supports(Class<?> clazz) {
        return clazz == IntegrasjonspunktProperties.class;
    }

    @Override
    public void validate(Object target, Errors errors) {
        IntegrasjonspunktProperties props = (IntegrasjonspunktProperties) target;

        if (props.getFeature().isEnableDPO()) {
            ValidationUtils.rejectIfEmpty(errors, "noarkSystem.type", EMPTY_FIELD, "DPO enabled - cannot be null");
            ValidationUtils.rejectIfEmpty(errors, "noarkSystem.endpointURL", EMPTY_FIELD, "DPO enabled - cannot be null");
        }

        if (props.getFeature().isEnableDPV()) {
            ValidationUtils.rejectIfEmpty(errors, "altinnPTV.externalServiceCode", EMPTY_FIELD, "DPV enabled - cannot be null");
            ValidationUtils.rejectIfEmpty(errors, "altinnPTV.externalServiceEditionCode", EMPTY_FIELD, "DPV enabled - cannot be null");
            ValidationUtils.rejectIfEmpty(errors, "altinnPTV.username", EMPTY_FIELD, "DPV enabled - cannot be null");
            ValidationUtils.rejectIfEmpty(errors, "altinnPTV.password", EMPTY_FIELD, "DPV enabled - cannot be null");
        }

    }
}
