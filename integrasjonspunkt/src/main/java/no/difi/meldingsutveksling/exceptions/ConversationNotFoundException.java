package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

import java.io.Serializable;

public class ConversationNotFoundException extends HttpStatusCodeException {

    public ConversationNotFoundException(Long id) {
        this("id", id);
    }

    public ConversationNotFoundException(String messageId) {
        this("messageId", messageId);
    }

    private ConversationNotFoundException(String name, Serializable id) {
        super(HttpStatus.NOT_FOUND,
                ConversationNotFoundException.class.getName(),
                name, id);
    }
}
