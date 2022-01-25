package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

public class ForsendelseTypeNotFoundException extends HttpStatusCodeException {

    public ForsendelseTypeNotFoundException(String type, String validTypes) {
        super(HttpStatus.BAD_REQUEST, ForsendelseTypeNotFoundException.class.getName(), type, validTypes);
    }
}
