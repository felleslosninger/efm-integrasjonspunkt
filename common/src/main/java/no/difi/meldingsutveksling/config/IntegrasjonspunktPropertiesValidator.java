package no.difi.meldingsutveksling.config;

import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

public class IntegrasjonspunktPropertiesValidator implements Validator {

    private static final String EMPTY_FIELD = "empty_field";
    private static final String DPO_ERROR_MSG = "DPO enabled - cannot be null";
    private static final String DPV_ERROR_MSG = "DPV enabled - cannot be null";

    @Override
    public boolean supports(Class<?> clazz) {
        return clazz == IntegrasjonspunktProperties.class;
    }

    @Override
    public void validate(Object target, Errors errors) {
        IntegrasjonspunktProperties props = (IntegrasjonspunktProperties) target;

        if (props.getFeature().isEnableDPO()) {
            ValidationUtils.rejectIfEmpty(errors, "noarkSystem.type", EMPTY_FIELD, DPO_ERROR_MSG);
            ValidationUtils.rejectIfEmpty(errors, "noarkSystem.endpointURL", EMPTY_FIELD, DPO_ERROR_MSG);
        }

        if (props.getFeature().isEnableDPV()) {
            ValidationUtils.rejectIfEmpty(errors, "dpv.externalServiceCode", EMPTY_FIELD, DPV_ERROR_MSG);
            ValidationUtils.rejectIfEmpty(errors, "dpv.externalServiceEditionCode", EMPTY_FIELD, DPV_ERROR_MSG);
            ValidationUtils.rejectIfEmpty(errors, "dpv.username", EMPTY_FIELD, DPV_ERROR_MSG);
            ValidationUtils.rejectIfEmpty(errors, "dpv.password", EMPTY_FIELD, DPV_ERROR_MSG);
        }

        if (props.getFeature().isEnableDPE() && !props.getSign().isEnable()) {
            errors.rejectValue("sign.enable", "cannot_be_false", "DPE enabled - cannot be false");
        }

        if (props.getSign().isEnable()) {
            ValidationUtils.rejectIfEmpty(errors, "sign.certificate", EMPTY_FIELD, "Sign verification enabled, must" +
                    " specify certificate");
        }

    }
}
