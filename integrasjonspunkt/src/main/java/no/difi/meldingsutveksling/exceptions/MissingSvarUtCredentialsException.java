package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

public class MissingSvarUtCredentialsException extends HttpStatusCodeException {

    public MissingSvarUtCredentialsException(String orgnr) {
        super(HttpStatus.BAD_REQUEST, MissingSvarUtCredentialsException.class.getName(), orgnr);
    }

}
