package no.difi.meldingsutveksling.validation;

import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.domain.Organisasjonsnummer;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

@Slf4j
public class InServiceRegistryValidator implements ConstraintValidator<InServiceRegistry, String> {

    @Autowired
    private ServiceRegistryLookup serviceRegistryLookup;

    public void initialize(InServiceRegistry constraint) {
        // NOOP
    }

    public boolean isValid(String s, ConstraintValidatorContext context) {
        if (s == null) {
            return true;
        }

        try {
            serviceRegistryLookup.getServiceRecord(getStrippedIdentifier(s));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String getStrippedIdentifier(String s) {
        return Organisasjonsnummer.isIso6523(s) ? Organisasjonsnummer.fromIso6523(s).toString() : s;
    }
}
