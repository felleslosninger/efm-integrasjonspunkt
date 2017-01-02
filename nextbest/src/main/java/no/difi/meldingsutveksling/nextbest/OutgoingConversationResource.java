package no.difi.meldingsutveksling.nextbest;

import com.google.common.collect.Lists;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@DiscriminatorValue("outgoing")
public class OutgoingConversationResource extends ConversationResource {

    OutgoingConversationResource() {}

    OutgoingConversationResource(String conversationId, String receiverId, String messagetypeId, LocalDateTime
            lastUpdate, List<String> fileRefs){
        super(conversationId, receiverId, messagetypeId, lastUpdate, fileRefs);
    }

    public static OutgoingConversationResource of(String conversationId, String receiverId, String messagetypeId) {
        return new OutgoingConversationResource(conversationId, receiverId, messagetypeId, LocalDateTime.now(),
                Lists.newArrayList());
    }

    public static OutgoingConversationResource of(String receiverId, String messagetypeId) {
        String conversationId = UUID.randomUUID().toString();
        return new OutgoingConversationResource(conversationId, receiverId, messagetypeId, LocalDateTime.now(),
                Lists.newArrayList());
    }

    public static OutgoingConversationResource of(IncomingConversationResource resource) {
        return new OutgoingConversationResource(resource.getConversationId(), resource.getReceiverId(), resource
                .getMessagetypeId(), LocalDateTime.now(), Lists.newArrayList());
    }
}
