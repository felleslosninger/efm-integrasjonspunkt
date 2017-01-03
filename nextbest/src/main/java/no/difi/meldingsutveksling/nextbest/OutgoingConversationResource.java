package no.difi.meldingsutveksling.nextbest;

import com.google.common.collect.Maps;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.UUID;

@Entity
@DiscriminatorValue("outgoing")
public class OutgoingConversationResource extends ConversationResource {

    OutgoingConversationResource() {}

    OutgoingConversationResource(String conversationId, String receiverId, String messagetypeId, LocalDateTime
            lastUpdate, HashMap<Integer, String> fileRefs){
        super(conversationId, receiverId, messagetypeId, lastUpdate, fileRefs);
    }

    public static OutgoingConversationResource of(String conversationId, String receiverId, String messagetypeId) {
        return new OutgoingConversationResource(conversationId, receiverId, messagetypeId, LocalDateTime.now(),
                Maps.newHashMap());
    }

    public static OutgoingConversationResource of(String receiverId, String messagetypeId) {
        String conversationId = UUID.randomUUID().toString();
        return new OutgoingConversationResource(conversationId, receiverId, messagetypeId, LocalDateTime.now(),
                Maps.newHashMap());
    }

    public static OutgoingConversationResource of(IncomingConversationResource resource) {
        return new OutgoingConversationResource(resource.getConversationId(), resource.getReceiverId(), resource
                .getMessagetypeId(), LocalDateTime.now(), Maps.newHashMap());
    }
}
