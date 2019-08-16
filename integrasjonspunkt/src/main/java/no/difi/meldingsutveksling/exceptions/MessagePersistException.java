package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

public class MessagePersistException extends HttpStatusCodeException {

    public MessagePersistException(String filename) {
        super(HttpStatus.INTERNAL_SERVER_ERROR,
                MessagePersistException.class.getName(),
                filename);
    }
}
