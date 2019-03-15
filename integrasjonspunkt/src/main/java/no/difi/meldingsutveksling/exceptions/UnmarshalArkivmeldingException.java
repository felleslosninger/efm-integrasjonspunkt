package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

public class UnmarshalArkivmeldingException extends HttpStatusCodeException {

    public UnmarshalArkivmeldingException() {
        super(HttpStatus.BAD_REQUEST, UnmarshalArkivmeldingException.class.getName());
    }

}
