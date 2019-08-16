package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

public class MessageStatusNotFoundException extends HttpStatusCodeException {

    public MessageStatusNotFoundException(String messageId) {
        super(HttpStatus.NOT_FOUND,
                MessageStatusNotFoundException.class.getName(),
                "messageId", messageId);
    }
}
