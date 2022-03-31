package no.difi.meldingsutveksling.validation;

import no.difi.meldingsutveksling.status.AvsenderidentifikatorHolder;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class AvsenderidentifikatorValidator implements ConstraintValidator<Avsenderidentifikator, String> {

    @Autowired
    private AvsenderidentifikatorHolder avsenderidentifikatorHolder;

    @Override
    public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
        if (s == null) {
            return avsenderidentifikatorHolder.pollWithoutAvsenderidentifikator();
        }

        return avsenderidentifikatorHolder.getAvsenderidentifikatorListe().contains(s);
    }
}
