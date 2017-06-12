package no.difi.meldingsutveksling.nextmove;

import org.springframework.http.ResponseEntity;

public interface ConversationStrategy {

    ResponseEntity send(ConversationResource conversationResource);
}
