package no.difi.meldingsutveksling.nextmove;

import no.difi.meldingsutveksling.ServiceIdentifier;

import java.util.List;
import java.util.Optional;

/**
 * Wrapper repository for conversations.
 */
public class DirectionalConversationResourceRepository {

    private ConversationDirection direction;

    private ConversationResourceRepository repo;

    public DirectionalConversationResourceRepository(ConversationResourceRepository repo, ConversationDirection direction) {
        this.repo = repo;
        this.direction = direction;
    }


    public List<ConversationResource> findAll() {
        return repo.findAllByDirection(direction);
    }

    public ConversationResource save(ConversationResource cr) {
        cr.setDirection(direction);
        return repo.save(cr);
    }

    public void delete(ConversationResource cr) {
        repo.deleteByConversationId(cr.getConversationId());
    }

    public Optional<ConversationResource> findByConversationId(String cId) {
        return repo.findByConversationIdAndDirection(cId, direction);
    }

    public List<ConversationResource> findByServiceIdentifierAndSenderId(ServiceIdentifier serviceIdentifier, String senderId) {
        return repo.findByServiceIdentifierAndSenderIdAndDirection(serviceIdentifier, senderId, direction);
    }

    public List<ConversationResource> findByServiceIdentifier(ServiceIdentifier serviceIdentifier) {
        return repo.findByServiceIdentifierAndDirection(serviceIdentifier, direction);
    }

    public List<ConversationResource> findBySenderId(String senderId) {
        return repo.findBySenderIdAndDirection(senderId, direction);
    }

    public Optional<ConversationResource> findFirstByOrderByLastUpdateAsc() {
        return repo.findFirstByDirectionOrderByLastUpdateAsc(direction);
    }

    public Optional<ConversationResource> findFirstByLockTimeoutIsNullOrderByLastUpdateAsc() {
        return repo.findFirstByDirectionAndLockTimeoutIsNullOrderByLastUpdateAsc(direction);
    }

    public Optional<ConversationResource> findFirstByLockTimeoutIsNotNullOrderByLastUpdateAsc() {
        return repo.findFirstByDirectionAndLockTimeoutIsNotNullOrderByLastUpdateAsc(direction);
    }

    public Optional<ConversationResource> findFirstByServiceIdentifierOrderByLastUpdateAsc(ServiceIdentifier serviceIdentifier) {
        return repo.findFirstByServiceIdentifierAndDirectionOrderByLastUpdateAsc(serviceIdentifier, direction);
    }

    public Optional<ConversationResource> findFirstByServiceIdentifierAndLockTimeoutIsNullOrderByLastUpdateAsc(ServiceIdentifier serviceIdentifier) {
        return repo.findFirstByServiceIdentifierAndDirectionAndLockTimeoutIsNullOrderByLastUpdateAsc(serviceIdentifier, direction);
    }

    public Optional<ConversationResource> findFirstByServiceIdentifierAndLockTimeoutIsNotNullOrderByLastUpdateAsc(ServiceIdentifier serviceIdentifier) {
        return repo.findFirstByServiceIdentifierAndDirectionAndLockTimeoutIsNotNullOrderByLastUpdateAsc(serviceIdentifier, direction);
    }

    public List<ConversationResource> findByReceiverIdAndServiceIdentifier(String receiverId, ServiceIdentifier serviceIdentifier) {
        return repo.findByReceiverIdAndServiceIdentifierAndDirection(receiverId, serviceIdentifier, direction);
    }

    public List<ConversationResource> findByReceiverId(String receiverId) {
        return repo.findByReceiverIdAndDirection(receiverId, direction);
    }
}
