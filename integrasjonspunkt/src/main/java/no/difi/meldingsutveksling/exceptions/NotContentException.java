package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

public class NotContentException extends HttpStatusCodeException {

    public NotContentException() {
        super(HttpStatus.NO_CONTENT,
                NotContentException.class.getName());
    }
}
