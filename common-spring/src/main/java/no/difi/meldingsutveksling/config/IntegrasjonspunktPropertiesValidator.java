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

        if (props.getSign().isEnable()) {
            ValidationUtils.rejectIfEmpty(errors, "sign.jwkUrl", EMPTY_FIELD, "Must not be null if JWS is enabled");
        }
        if(props.getFeature().isEnableDPO()){
            ValidationUtils.rejectIfEmpty(errors, "dpo.resource", EMPTY_FIELD, "Resourceid is required for DPO");
            ValidationUtils.rejectIfEmpty(errors, "dpo.system-user.org-id", EMPTY_FIELD, "Systemuser is required for DPO");
            ValidationUtils.rejectIfEmpty(errors, "dpo.system-user.name", EMPTY_FIELD, "Systemuser is required for DPO");
        }
    }
}
