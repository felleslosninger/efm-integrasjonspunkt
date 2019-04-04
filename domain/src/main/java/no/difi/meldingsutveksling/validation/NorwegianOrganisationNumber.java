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

@ConstraintComposition(CompositionType.AND)
@Digits(integer = 9, fraction = 0)
@Length(min = 9, max = 9)
@Mod11Check(threshold = 7)
@Documented
@Constraint(validatedBy = {})
@Target({FIELD, TYPE, PARAMETER, ANNOTATION_TYPE})
@Retention(RUNTIME)
public @interface NorwegianOrganisationNumber {

    String message() default "{no.difi.meldingsutveksling.validation.NorwegianOrganisationNumber}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
