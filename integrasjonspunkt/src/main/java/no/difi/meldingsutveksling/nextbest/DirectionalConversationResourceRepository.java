package no.difi.meldingsutveksling.nextbest;

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
        repo.delete(cr);
    }

    public Optional<ConversationResource> findByConversationId(String cId) {
        return repo.findByConversationIdAndDirection(cId, direction);
    }

    public List<ConversationResource> findByMessagetypeIdAndSenderId(String messagetypeId, String senderId) {
        return repo.findByMessagetypeIdAndSenderIdAndDirection(messagetypeId, senderId, direction);
    }

    public List<ConversationResource> findByMessagetypeId(String messagetypeId) {
        return repo.findByMessagetypeIdAndDirection(messagetypeId, direction);
    }

    public List<ConversationResource> findBySenderId(String senderId) {
        return repo.findBySenderIdAndDirection(senderId, direction);
    }

    public Optional<ConversationResource> findFirstByOrderByLastUpdateAsc() {
        return repo.findFirstByDirectionOrderByLastUpdateAsc(direction);
    }

    public Optional<ConversationResource> findFirstByMessagetypeIdOrderByLastUpdateAsc(String messagetypeId) {
        return repo.findFirstByMessagetypeIdAndDirectionOrderByLastUpdateAsc(messagetypeId, direction);
    }

    public List<ConversationResource> findByReceiverIdAndMessagetypeId(String receiverId, String messagetypeId) {
        return repo.findByReceiverIdAndMessagetypeIdAndDirection(receiverId, messagetypeId, direction);
    }

    public List<ConversationResource> findByReceiverId(String receiverId) {
        return repo.findByReceiverIdAndDirection(receiverId, direction);
    }
}
