package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

public class CanNotRetrieveHealthcareStatusException extends HttpStatusCodeException {


    public CanNotRetrieveHealthcareStatusException(HttpStatus statusCode,String message) {
        super(statusCode, CanNotRetrieveHealthcareStatusException.class.getName(), message);
    }
}
