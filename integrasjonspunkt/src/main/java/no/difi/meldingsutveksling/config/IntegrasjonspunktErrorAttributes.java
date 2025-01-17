package no.difi.meldingsutveksling.config;

import no.difi.meldingsutveksling.exceptions.ErrorDescriber;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.context.request.WebRequest;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class IntegrasjonspunktErrorAttributes extends DefaultErrorAttributes {

    private final Clock clock;

    public IntegrasjonspunktErrorAttributes(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
        final Map<String, Object> errorAttributes = super.getErrorAttributes(webRequest, options);
        errorAttributes.put("timestamp", OffsetDateTime.now(clock));

        final Throwable error = super.getError(webRequest);

        if (error instanceof ConstraintViolationException cve) {
            addErrorMessage(errorAttributes, cve);
        }

        if (error instanceof ErrorDescriber describer) {
            errorAttributes.put("description", describer.getDescription());
        }

        return errorAttributes;
    }

    private void addErrorMessage(Map<String, Object> errorAttributes, ConstraintViolationException error) {
        errorAttributes.put("errors", error.getConstraintViolations().stream()
                .map(this::getObjectError)
                .collect(Collectors.toList())
        );
    }

    private ObjectError getObjectError(ConstraintViolation<?> violation) {
        String field = determineField(violation);

        return new FieldError(
                field,
                violation.getPropertyPath().toString(),
                violation.getInvalidValue(),
                false,
                new String[]{violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName()},
                null,
                violation.getMessage());
    }

    private String determineField(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        int elementIndex = path.indexOf(".<");
        return (elementIndex >= 0 ? path.substring(0, elementIndex) : path);
    }
}
