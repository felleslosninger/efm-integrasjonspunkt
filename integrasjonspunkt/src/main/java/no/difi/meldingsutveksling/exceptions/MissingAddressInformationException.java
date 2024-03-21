package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

public class MissingAddressInformationException extends HttpStatusCodeException {

    public MissingAddressInformationException(String errorMessage) {
        super(HttpStatus.BAD_REQUEST, MissingAddressInformationException.class.getName(), errorMessage);
    }
}
