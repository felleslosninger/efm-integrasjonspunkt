package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

public class ConversationAlreadyExistsException extends HttpStatusCodeException {

    public ConversationAlreadyExistsException(String conversationId) {
        super(HttpStatus.BAD_REQUEST,
                ConversationAlreadyExistsException.class.getName(),
                "conversationId", conversationId);
    }
}
