package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

public class NoContentException extends HttpStatusCodeException {

    public NoContentException() {
        super(HttpStatus.NO_CONTENT,
                NoContentException.class.getName());
    }
}
