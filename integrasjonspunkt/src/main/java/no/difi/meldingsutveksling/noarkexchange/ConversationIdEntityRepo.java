package no.difi.meldingsutveksling.noarkexchange;

import org.springframework.data.repository.CrudRepository;

public interface ConversationIdEntityRepo extends CrudRepository<ConversationIdEntity, Long> {

    ConversationIdEntity findByNewConversationId(String newConversationId);
}
