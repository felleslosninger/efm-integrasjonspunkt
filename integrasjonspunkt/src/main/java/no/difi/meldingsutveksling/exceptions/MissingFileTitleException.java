package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

public class MissingFileTitleException extends HttpStatusCodeException {

    public MissingFileTitleException(String serviceIdentifier) {
        super(HttpStatus.BAD_REQUEST, MissingFileTitleException.class.getName(), serviceIdentifier);
    }
}
