package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

public class MissingAddressInformationException extends HttpStatusCodeException {

    public MissingAddressInformationException(String jsonFieldName) {
        super(HttpStatus.BAD_REQUEST, MissingAddressInformationException.class.getName(), jsonFieldName);
    }
}
