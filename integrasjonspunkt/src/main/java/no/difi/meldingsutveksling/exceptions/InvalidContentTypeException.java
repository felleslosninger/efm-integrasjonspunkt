package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

public class InvalidContentTypeException extends HttpStatusCodeException {

    public InvalidContentTypeException(String filename) {
        super(HttpStatus.BAD_REQUEST, InvalidContentTypeException.class.getName(), filename);
    }
}
