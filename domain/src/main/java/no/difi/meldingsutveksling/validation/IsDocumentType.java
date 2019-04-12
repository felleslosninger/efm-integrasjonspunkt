package no.difi.meldingsutveksling.validation;

import no.difi.meldingsutveksling.ApiType;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = {})
@Target({FIELD, PARAMETER, ANNOTATION_TYPE})
@Retention(RUNTIME)
public @interface IsDocumentType {

    String message() default "{no.difi.meldingsutveksling.validation.IsDocumentType}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    ApiType value();
}

