package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

public class MissingSaksmappeException extends HttpStatusCodeException {

    public MissingSaksmappeException() {
        super(HttpStatus.BAD_REQUEST, MissingSaksmappeException.class.getName());
    }

}
