package no.difi.meldingsutveksling.receipt;

import no.difi.meldingsutveksling.nextmove.ConversationDirection;
import org.springframework.context.annotation.Profile;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

@Profile("!test")
public interface ConversationRepository  extends CrudRepository<Conversation, String> {
    Optional<Conversation> findByConvIdAndDirection(Integer convId, ConversationDirection direction);
    List<Conversation> findByConversationId(String conversationId);
    List<Conversation> findByPollable(boolean pollable);
    List<Conversation> findByFinishedAndDirection(boolean finished, ConversationDirection direction);
    List<Conversation> findByDirection(ConversationDirection direction);
    Long countByPollable(boolean pollable);
}
