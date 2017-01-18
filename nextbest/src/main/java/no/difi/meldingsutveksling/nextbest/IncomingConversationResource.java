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

    IncomingConversationResource(String conversationId, String senderId, String receiverId, String messagetypeId,
                                 LocalDateTime
            lastUpdate, Map<Integer, String> fileRefs){
        super(conversationId, senderId, receiverId, messagetypeId, lastUpdate, fileRefs);
    }

    public static IncomingConversationResource of(String conversationId, String senderId, String receiverId,
                                                  String messagetypeId) {
        return new IncomingConversationResource(conversationId, senderId, receiverId, messagetypeId,
                LocalDateTime.now(), Maps.newHashMap());
    }

    public static IncomingConversationResource of(String senderId, String receiverId, String messagetypeId) {
        String conversationId = UUID.randomUUID().toString();
        return new IncomingConversationResource(conversationId, senderId, receiverId, messagetypeId,
                LocalDateTime.now(), Maps.newHashMap());
    }

    public static IncomingConversationResource of(OutgoingConversationResource resource) {
        return new IncomingConversationResource(resource.getConversationId(), resource.getSenderId(),
                resource.getReceiverId(), resource.getMessagetypeId(), LocalDateTime.now(), resource.getFileRefs());
    }
}
