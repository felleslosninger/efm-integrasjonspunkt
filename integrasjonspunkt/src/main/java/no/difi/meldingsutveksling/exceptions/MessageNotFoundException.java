package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

public class MessageNotFoundException extends HttpStatusCodeException {

    public MessageNotFoundException(String messageId) {
        this("messageId", messageId);
    }

    public MessageNotFoundException(String name, String id) {
        super(HttpStatus.NOT_FOUND,
                MessageNotFoundException.class.getName(),
                name, id);
    }
}
