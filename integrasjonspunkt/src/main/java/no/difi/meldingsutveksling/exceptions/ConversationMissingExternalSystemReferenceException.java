package no.difi.meldingsutveksling.exceptions;

import org.springframework.http.HttpStatus;

import java.io.Serializable;

public class ConversationMissingExternalSystemReferenceException extends HttpStatusCodeException {

    public ConversationMissingExternalSystemReferenceException(String messageId) {
        this("messageId", messageId);
    }

    private ConversationMissingExternalSystemReferenceException(String name, Serializable id) {
        super(HttpStatus.NOT_FOUND,
                ConversationMissingExternalSystemReferenceException.class.getName(),
                name, id);
    }
}
