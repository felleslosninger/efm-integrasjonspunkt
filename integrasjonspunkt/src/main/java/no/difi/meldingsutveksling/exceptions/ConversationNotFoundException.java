package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

public class ConversationNotFoundException extends HttpStatusCodeException {

    public ConversationNotFoundException(String conversationId) {
        super(HttpStatus.NOT_FOUND,
                ConversationNotFoundException.class.getName(),
                "conversationId", conversationId);
    }
}
