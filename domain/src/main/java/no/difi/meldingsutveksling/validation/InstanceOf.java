package no.difi.meldingsutveksling.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = {})
@Target(FIELD)
@Retention(RUNTIME)
@Repeatable(InstanceOfList.class)
public @interface InstanceOf {

    String message() default "{no.difi.meldingsutveksling.validation.InstanceOf}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    Class<?> value();
}