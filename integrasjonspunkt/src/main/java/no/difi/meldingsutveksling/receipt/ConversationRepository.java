package no.difi.meldingsutveksling.receipt;

import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ConversationRepository  extends CrudRepository<Conversation, String> {
    List<Conversation> findByConversationId(String conversationId);
    List<Conversation> findByPollable(boolean pollable);
}
