package no.difi.meldingsutveksling.config.dpi.securitylevel;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(
        validatedBy = {SecurityLevelValidator.class}
)
public @interface ValidSecurityLevel {
    String message() default "{package.Value.message}";

    Class<?>[] groups() default
            {};

    Class<? extends Payload>[] payload() default
            {};

    SecurityLevel[] invalidValues() default {};

    SecurityLevel[] values() default
            {};
}
