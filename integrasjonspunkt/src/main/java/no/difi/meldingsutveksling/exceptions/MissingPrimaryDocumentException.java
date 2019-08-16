package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

public class MissingPrimaryDocumentException extends HttpStatusCodeException {

    public MissingPrimaryDocumentException() {
        super(HttpStatus.BAD_REQUEST, MissingPrimaryDocumentException.class.getName());
    }
}
