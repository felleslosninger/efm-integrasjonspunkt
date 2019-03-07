package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

public class ConversationNotLockedException extends HttpStatusCodeException {

    public ConversationNotLockedException(String conversationId) {
        super(HttpStatus.NOT_FOUND,
                ConversationNotLockedException.class.getName(),
                "conversationId", conversationId);
    }
}
