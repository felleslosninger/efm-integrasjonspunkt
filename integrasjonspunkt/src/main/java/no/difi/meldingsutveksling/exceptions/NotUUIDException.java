package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

public class NotUUIDException extends HttpStatusCodeException {

    public NotUUIDException(String field, String value) {
        super(HttpStatus.BAD_REQUEST, NotUUIDException.class.getName(), field, value);
    }

}
