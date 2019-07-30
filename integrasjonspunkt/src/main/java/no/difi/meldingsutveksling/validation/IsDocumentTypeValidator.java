package no.difi.meldingsutveksling.validation;

import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.ApiType;
import no.difi.meldingsutveksling.DocumentType;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

@Slf4j
public class IsDocumentTypeValidator implements ConstraintValidator<IsDocumentType, String> {

    private ApiType api;

    public void initialize(IsDocumentType constraint) {
        this.api = constraint.value();
    }

    public boolean isValid(String s, ConstraintValidatorContext context) {
        return DocumentType.valueOf(s, api).isPresent();
    }
}
