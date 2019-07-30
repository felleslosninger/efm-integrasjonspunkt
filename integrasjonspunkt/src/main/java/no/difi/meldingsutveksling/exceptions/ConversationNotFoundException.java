package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

public class ConversationNotFoundException extends HttpStatusCodeException {

    public ConversationNotFoundException(String conversationId) {
        this("conversationId", conversationId);
    }

    public ConversationNotFoundException(String name, String id) {
        super(HttpStatus.NOT_FOUND,
                ConversationNotFoundException.class.getName(),
                name, id);
    }
}
