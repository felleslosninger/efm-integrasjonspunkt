package no.difi.meldingsutveksling.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = {})
@Target({FIELD, PARAMETER, ANNOTATION_TYPE})
@Retention(RUNTIME)
public @interface InServiceRegistry {

    String message() default "{no.difi.meldingsutveksling.validation.InServiceRegistry}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
