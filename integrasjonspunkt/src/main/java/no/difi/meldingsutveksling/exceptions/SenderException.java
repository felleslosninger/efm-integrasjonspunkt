package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

public class SenderException extends HttpStatusCodeException {

    public SenderException(String serviceIdentifier) {
        super(HttpStatus.BAD_REQUEST, SenderException.class.getName(), serviceIdentifier);
    }
}
