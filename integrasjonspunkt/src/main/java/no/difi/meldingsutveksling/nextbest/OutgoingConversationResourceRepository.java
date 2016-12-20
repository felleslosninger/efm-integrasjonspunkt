package no.difi.meldingsutveksling.nextbest;

import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface OutgoingConversationResourceRepository extends CrudRepository<OutgoingConversationResource, String> {

    List<OutgoingConversationResource> findByReceiverId(String receiverId);
    List<OutgoingConversationResource> findByMessagetypeId(String messagetypeId);
    List<OutgoingConversationResource> findByReceiverIdAndMessagetypeId(String receiverId, String messagetypeId);
}
