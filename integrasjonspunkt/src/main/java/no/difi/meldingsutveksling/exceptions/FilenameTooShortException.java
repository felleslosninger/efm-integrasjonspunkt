package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

public class FilenameTooShortException extends HttpStatusCodeException {

    public FilenameTooShortException(String filename, int minLength) {
        super(HttpStatus.BAD_REQUEST, FilenameTooShortException.class.getName(), filename, minLength);
    }
}
