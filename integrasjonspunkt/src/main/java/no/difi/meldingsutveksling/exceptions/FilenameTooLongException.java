package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

public class FilenameTooLongException extends HttpStatusCodeException {

    public FilenameTooLongException(String filename, int maxLength) {
        super(HttpStatus.BAD_REQUEST, FilenameTooLongException.class.getName(), filename, maxLength);
    }
}
