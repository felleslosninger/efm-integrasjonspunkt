package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

import java.io.Serializable;

public class HealthcareValidationException extends HttpStatusCodeException {


    public HealthcareValidationException(String message) {
        super(HttpStatus.BAD_REQUEST, HealthcareValidationException.class.getName(), message);
    }
}
