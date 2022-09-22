package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

import java.io.Serializable;

public class AsicReadException extends HttpStatusCodeException {
    public AsicReadException(String messageId) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, AsicReadException.class.getName(), messageId);
    }
}
