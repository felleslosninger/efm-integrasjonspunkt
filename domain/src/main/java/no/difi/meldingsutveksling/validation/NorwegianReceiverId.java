package no.difi.meldingsutveksling.validation;

import org.hibernate.validator.constraints.CompositionType;
import org.hibernate.validator.constraints.ConstraintComposition;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.Mod11Check;

import javax.validation.Constraint;
import javax.validation.Payload;
import javax.validation.constraints.Digits;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@ConstraintComposition(CompositionType.OR)
@NorwegianIndividualNumber
@NorwegianOrganisationNumber
@Documented
@Constraint(validatedBy = {})
@Target({FIELD, TYPE, PARAMETER, ANNOTATION_TYPE})
@Retention(RUNTIME)
public @interface NorwegianReceiverId {

    String message() default "{no.difi.meldingsutveksling.validation.NorwegianIndividualNumber}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
