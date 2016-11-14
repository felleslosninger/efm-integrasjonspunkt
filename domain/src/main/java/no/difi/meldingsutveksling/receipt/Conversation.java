package no.difi.meldingsutveksling.receipt;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.List;

@Entity
public class Conversation {

    @Id
    @GeneratedValue
    private String id;

    private String conversationId;

    @Cascade(CascadeType.PERSIST)
    @OneToMany
    List<MessageReceipt> messageReceipt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public List<MessageReceipt> getMessageReceipt() {
        return messageReceipt;
    }

    public void setMessageReceipt(List<MessageReceipt> messageReceipt) {
        this.messageReceipt = messageReceipt;
    }
}
