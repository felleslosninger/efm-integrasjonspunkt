package no.difi.meldingsutveksling.nextbest;

import com.google.common.base.MoreObjects;

import javax.persistence.*;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "direction")
public abstract class ConversationResource {

    @Id
    private String conversationId;
    private String receiverId;
    private String messagetypeId;

    ConversationResource() {}

    ConversationResource(String conversationId, String receiverId, String messagetypeId){
        this.conversationId = conversationId;
        this.receiverId = receiverId;
        this.messagetypeId = messagetypeId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getMessagetypeId() {
        return messagetypeId;
    }

    public void setMessagetypeId(String messagetypeId) {
        this.messagetypeId = messagetypeId;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("conversationId", conversationId)
                .add("receiverId", receiverId)
                .add("messagetypeId", messagetypeId)
                .toString();
    }

}
