package no.difi.meldingsutveksling.validation;

import no.difi.meldingsutveksling.status.AvsenderindikatorHolder;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Set;

public class AvsenderidentifikatorValidator implements ConstraintValidator<Avsenderidentifikator, String> {

    @Autowired
    private AvsenderindikatorHolder avsenderindikatorHolder;

    @Override
    public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
        Set<String> avsenderindikatorListe = avsenderindikatorHolder.getAvsenderindikatorListe();

        if (avsenderindikatorListe.isEmpty()) {
            return true;
        }

        if (s == null) {
            return false;
        }

        return avsenderindikatorListe.contains(s);
    }
}
