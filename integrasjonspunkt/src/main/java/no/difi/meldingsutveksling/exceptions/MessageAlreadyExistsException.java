package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

public class MessageAlreadyExistsException extends HttpStatusCodeException {

    public MessageAlreadyExistsException(String messageId) {
        super(HttpStatus.BAD_REQUEST,
                MessageAlreadyExistsException.class.getName(),
                "messageId", messageId);
    }
}
