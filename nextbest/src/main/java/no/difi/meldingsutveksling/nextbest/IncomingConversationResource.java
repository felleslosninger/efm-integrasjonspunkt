package no.difi.meldingsutveksling.nextbest;

import com.google.common.collect.Lists;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.util.List;
import java.util.UUID;

@Entity
@DiscriminatorValue("incoming")
public class IncomingConversationResource extends ConversationResource {

    IncomingConversationResource() {}

    IncomingConversationResource(String conversationId, String receiverId, String messagetypeId, List<String> fileRefs){
        super(conversationId, receiverId, messagetypeId, fileRefs);
    }

    public static IncomingConversationResource of(String conversationId, String receiverId, String messagetypeId) {
        return new IncomingConversationResource(conversationId, receiverId, messagetypeId, Lists.newArrayList());
    }

    public static IncomingConversationResource of(String receiverId, String messagetypeId) {
        String conversationId = UUID.randomUUID().toString();
        return new IncomingConversationResource(conversationId, receiverId, messagetypeId, Lists.newArrayList());
    }

    public static IncomingConversationResource of(OutgoingConversationResource resource) {
        return new IncomingConversationResource(resource.getConversationId(), resource.getReceiverId(), resource
                .getMessagetypeId(), Lists.newArrayList());
    }
}
