package no.difi.meldingsutveksling.nextmove;

import no.difi.meldingsutveksling.ServiceIdentifier;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ConversationResourceRepository extends CrudRepository<ConversationResource, String> {

    void deleteByConversationId(String conversationId);

    List<ConversationResource> findAllByDirection(ConversationDirection direction);
    Optional<ConversationResource> findByConversationIdAndDirection(String conversationId, ConversationDirection direction);

    List<ConversationResource> findByReceiverIdAndDirection(String receiverId, ConversationDirection direction);
    List<ConversationResource> findByServiceIdentifierAndDirection(ServiceIdentifier serviceIdentifier, ConversationDirection direction);
    List<ConversationResource> findByReceiverIdAndServiceIdentifierAndDirection(String receiverId, ServiceIdentifier serviceIdentifier, ConversationDirection direction);

    List<ConversationResource> findBySenderIdAndDirection(String senderId, ConversationDirection direction);
    List<ConversationResource> findByServiceIdentifierAndSenderIdAndDirection(ServiceIdentifier serviceIdentifier, String senderId, ConversationDirection direction);
    Optional<ConversationResource> findFirstByDirectionOrderByLastUpdateAsc(ConversationDirection direction);
    Optional<ConversationResource> findFirstByDirectionAndLockTimeoutIsNullOrderByLastUpdateAsc(ConversationDirection direction);
    Optional<ConversationResource> findFirstByDirectionAndLockTimeoutIsNotNullOrderByLastUpdateAsc(ConversationDirection direction);
    Optional<ConversationResource> findFirstByServiceIdentifierAndDirectionOrderByLastUpdateAsc(ServiceIdentifier serviceIdentifier, ConversationDirection direction);
    Optional<ConversationResource> findFirstByServiceIdentifierAndDirectionAndLockTimeoutIsNullOrderByLastUpdateAsc(ServiceIdentifier serviceIdentifier, ConversationDirection direction);
    Optional<ConversationResource> findFirstByServiceIdentifierAndDirectionAndLockTimeoutIsNotNullOrderByLastUpdateAsc(ServiceIdentifier serviceIdentifier, ConversationDirection direction);

    List<ConversationResource> findByLockTimeoutLessThanEqual(LocalDateTime now);

    Long countByDirection(ConversationDirection direction);
}
