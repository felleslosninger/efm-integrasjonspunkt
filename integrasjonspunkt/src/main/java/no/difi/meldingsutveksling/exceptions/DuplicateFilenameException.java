package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

public class DuplicateFilenameException extends HttpStatusCodeException {

    public DuplicateFilenameException(String filename) {
        super(HttpStatus.BAD_REQUEST, DuplicateFilenameException.class.getName(), filename);
    }
}
