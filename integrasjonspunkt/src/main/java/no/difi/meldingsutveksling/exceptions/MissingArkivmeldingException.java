package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

public class MissingArkivmeldingException extends HttpStatusCodeException {

    public MissingArkivmeldingException() {
        super(HttpStatus.BAD_REQUEST, MissingArkivmeldingException.class.getName());
    }
}
