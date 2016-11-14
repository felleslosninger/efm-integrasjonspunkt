package no.difi.meldingsutveksling.receipt;

import org.springframework.data.repository.CrudRepository;

public interface ConversationRepository  extends CrudRepository<Conversation, String> {
    Conversation findByConversationId(String conversationId);
}
