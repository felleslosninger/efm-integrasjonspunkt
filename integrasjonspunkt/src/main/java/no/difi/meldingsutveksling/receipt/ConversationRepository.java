package no.difi.meldingsutveksling.receipt;

import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository  extends CrudRepository<Conversation, String> {
    Optional<Conversation> findByConvId(Integer convId);
    List<Conversation> findByConversationId(String conversationId);
    List<Conversation> findByPollable(boolean pollable);
}
