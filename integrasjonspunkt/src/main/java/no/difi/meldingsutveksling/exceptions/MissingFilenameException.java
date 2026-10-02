package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

public class MissingFilenameException extends HttpStatusCodeException {

    public MissingFilenameException() {
        super(HttpStatus.BAD_REQUEST, MissingFilenameException.class.getName());
    }
}
