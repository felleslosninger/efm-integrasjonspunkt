package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

import java.util.Collection;

public class ForsendelseTypeNotFoundException extends HttpStatusCodeException {

    public ForsendelseTypeNotFoundException(String type, Collection<String> validTypes) {
        super(HttpStatus.BAD_REQUEST, ForsendelseTypeNotFoundException.class.getName(), type, validTypes.toArray());
    }
}
