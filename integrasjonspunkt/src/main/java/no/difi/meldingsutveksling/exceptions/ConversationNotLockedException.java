package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

public class ConversationNotLockedException extends HttpStatusCodeException {

    public ConversationNotLockedException(String conversationId) {
        super(HttpStatus.BAD_REQUEST,
                ConversationNotLockedException.class.getName(),
                "conversationId", conversationId);
    }
}
