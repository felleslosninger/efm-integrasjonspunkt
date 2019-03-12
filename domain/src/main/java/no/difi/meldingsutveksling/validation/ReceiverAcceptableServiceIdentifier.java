package no.difi.meldingsutveksling.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = {})
@Target({TYPE, PARAMETER, ANNOTATION_TYPE})
@Retention(RUNTIME)
public @interface ReceiverAcceptableServiceIdentifier {

    String message() default "{no.difi.meldingsutveksling.validation.ReceiverAcceptableServiceIdentifier}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
