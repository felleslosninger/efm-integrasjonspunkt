package no.difi.meldingsutveksling.nextbest;

import org.springframework.http.ResponseEntity;

public interface ConversationStrategy {

    ResponseEntity send(ConversationResource conversationResource);
}
