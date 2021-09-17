package no.difi.meldingsutveksling.validation;

import no.difi.meldingsutveksling.dpi.MeldingsformidlerClient;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class PartitionIdValidator implements ConstraintValidator<PartitionId, String> {

    @Autowired
    private MeldingsformidlerClient meldingsformidlerClient;

    @Override
    public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
        if (meldingsformidlerClient.shouldValidatePartitionId()) {
            return meldingsformidlerClient.getPartitionIds().contains(s);
        }

        return false;
    }
}
