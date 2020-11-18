package no.difi.meldingsutveksling.config;

import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

public class IntegrasjonspunktPropertiesValidator implements Validator {

    private static final String EMPTY_FIELD = "empty_field";
    private static final String DPFIO_ERROR_MSG = "DPFIO enabled - cannot be null";
    private static final String DPV_ERROR_MSG = "DPV enabled - cannot be null";

    @Override
    public boolean supports(Class<?> clazz) {
        return clazz == IntegrasjonspunktProperties.class;
    }

    @Override
    public void validate(Object target, Errors errors) {
        IntegrasjonspunktProperties props = (IntegrasjonspunktProperties) target;

        if (props.getFeature().isEnableDPV()) {
            ValidationUtils.rejectIfEmpty(errors, "dpv.username", EMPTY_FIELD, DPV_ERROR_MSG);
            ValidationUtils.rejectIfEmpty(errors, "dpv.password", EMPTY_FIELD, DPV_ERROR_MSG);
        }

        if (props.getFeature().isEnableDPFIO()) {
            ValidationUtils.rejectIfEmpty(errors, "fiks.io.konto-id", EMPTY_FIELD, DPFIO_ERROR_MSG);
            ValidationUtils.rejectIfEmpty(errors, "fiks.io.integrasjons-id", EMPTY_FIELD, DPFIO_ERROR_MSG);
            ValidationUtils.rejectIfEmpty(errors, "fiks.io.integrasjons-passord", EMPTY_FIELD, DPFIO_ERROR_MSG);
        }

        if (props.getSign().isEnable()) {
            ValidationUtils.rejectIfEmpty(errors, "sign.jwkUrl", EMPTY_FIELD, "Must not be null if JWS is enabled");
        }

        if (props.getNoarkSystem() != null && "mail".equals(props.getNoarkSystem().getType())) {
            String errorMsg = "Noark system type set to mail - cannot be null";
            ValidationUtils.rejectIfEmpty(errors, "mail.smtpHost", EMPTY_FIELD, errorMsg);
            ValidationUtils.rejectIfEmpty(errors, "mail.smtpPort", EMPTY_FIELD, errorMsg);
            ValidationUtils.rejectIfEmpty(errors, "mail.receiverAddress", EMPTY_FIELD, errorMsg);
        }

        if (props.getFeature().isEnableDPF() && props.getFiks().getInn().isMailOnError()) {
            String errorMsg = "DPF and mailOnError enabled - cannot be null";
            ValidationUtils.rejectIfEmpty(errors, "mail.smtpHost", EMPTY_FIELD, errorMsg);
            ValidationUtils.rejectIfEmpty(errors, "mail.smtpPort", EMPTY_FIELD, errorMsg);
            ValidationUtils.rejectIfEmpty(errors, "mail.receiverAddress", EMPTY_FIELD, errorMsg);
        }

    }
}
