package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

public class ReceiverException extends HttpStatusCodeException {

    public ReceiverException(String serviceIdentifier) {
        super(HttpStatus.BAD_REQUEST, ReceiverException.class.getName(), serviceIdentifier);
    }
}
