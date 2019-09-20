package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

public class MissingHttpHeaderException extends HttpStatusCodeException {

    public MissingHttpHeaderException(String name) {
        super(HttpStatus.BAD_REQUEST,
                MissingHttpHeaderException.class.getName(),
                name);
    }
}
