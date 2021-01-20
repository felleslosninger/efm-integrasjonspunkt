package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

public class IdentifierNotFoundException extends HttpStatusCodeException {

    public IdentifierNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, IdentifierNotFoundException.class.getName(), message);
    }
}
