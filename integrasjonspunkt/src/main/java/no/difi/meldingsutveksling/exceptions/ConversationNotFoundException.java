package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

public class ConversationNotFoundException extends HttpStatusCodeException {

    public ConversationNotFoundException(String conversationId) {
        super(HttpStatus.NOT_FOUND,
                "no.difi.meldingsutveksling.nextmove.message.notFound",
                "conversationId", conversationId);
    }
}
