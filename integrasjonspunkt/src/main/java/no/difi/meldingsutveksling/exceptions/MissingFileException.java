package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

public class MissingFileException extends HttpStatusCodeException {

    public MissingFileException() {
        super(HttpStatus.BAD_REQUEST, MissingFileException.class.getName());
    }
}
