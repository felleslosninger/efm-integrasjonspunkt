package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

public class InvalidDocumentException extends HttpStatusCodeException {

    public InvalidDocumentException(String filename) {
        super(HttpStatus.BAD_REQUEST, InvalidDocumentException.class.getName(), filename);
    }
}
