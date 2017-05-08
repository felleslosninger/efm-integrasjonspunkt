package no.difi.meldingsutveksling.nextbest;

import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface ConversationResourceRepository extends CrudRepository<ConversationResource, String> {

    List<ConversationResource> findAllByDirection(ConversationDirection direction);
    Optional<ConversationResource> findByConversationIdAndDirection(String conversationId, ConversationDirection direction);

    List<ConversationResource> findByReceiverIdAndDirection(String receiverId, ConversationDirection direction);
    List<ConversationResource> findByMessagetypeIdAndDirection(String messagetypeId, ConversationDirection direction);
    List<ConversationResource> findByReceiverIdAndMessagetypeIdAndDirection(String receiverId, String messagetypeId, ConversationDirection direction);

    List<ConversationResource> findBySenderIdAndDirection(String senderId, ConversationDirection direction);
    List<ConversationResource> findByMessagetypeIdAndSenderIdAndDirection(String messagetypeId, String senderId, ConversationDirection direction);
    Optional<ConversationResource> findFirstByDirectionOrderByLastUpdateAsc(ConversationDirection direction);
    Optional<ConversationResource> findFirstByMessagetypeIdAndDirectionOrderByLastUpdateAsc(String messagetypeId, ConversationDirection direction);
}
