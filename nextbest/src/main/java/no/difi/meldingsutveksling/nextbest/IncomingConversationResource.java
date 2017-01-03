package no.difi.meldingsutveksling.nextbest;

import com.google.common.collect.Maps;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@DiscriminatorValue("incoming")
public class IncomingConversationResource extends ConversationResource {

    IncomingConversationResource() {}

    IncomingConversationResource(String conversationId, String receiverId, String messagetypeId, LocalDateTime
            lastUpdate, Map<Integer, String> fileRefs){
        super(conversationId, receiverId, messagetypeId, lastUpdate, fileRefs);
    }

    public static IncomingConversationResource of(String conversationId, String receiverId, String messagetypeId) {
        return new IncomingConversationResource(conversationId, receiverId, messagetypeId, LocalDateTime.now(),
                Maps.newHashMap());
    }

    public static IncomingConversationResource of(String receiverId, String messagetypeId) {
        String conversationId = UUID.randomUUID().toString();
        return new IncomingConversationResource(conversationId, receiverId, messagetypeId, LocalDateTime.now(),
                Maps.newHashMap());
    }

    public static IncomingConversationResource of(OutgoingConversationResource resource) {
        return new IncomingConversationResource(resource.getConversationId(), resource.getReceiverId(), resource
                .getMessagetypeId(), LocalDateTime.now(), resource.getFileRefs());
    }
}
