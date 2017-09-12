package no.difi.meldingsutveksling.receipt;

import org.springframework.context.annotation.Profile;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

@Profile("!test")
public interface ConversationRepository  extends CrudRepository<Conversation, String> {
    Optional<Conversation> findByConvId(Integer convId);
    List<Conversation> findByConversationId(String conversationId);
    List<Conversation> findByPollable(boolean pollable);
    List<Conversation> findByFinished(boolean finished);
}
