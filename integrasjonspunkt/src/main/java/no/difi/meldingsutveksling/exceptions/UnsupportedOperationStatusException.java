package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

public class UnsupportedOperationStatusException extends HttpStatusCodeException {


    public UnsupportedOperationStatusException(String message) {
        super(HttpStatus.BAD_REQUEST, UnsupportedOperationStatusException.class.getName(), message);
    }
}
