package no.difi.meldingsutveksling.nextbest;

import com.google.common.base.MoreObjects;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "direction")
public abstract class ConversationResource {

    @Id
    private String conversationId;
    private String receiverId;
    private String messagetypeId;
    private LocalDateTime lastUpdate;
    @ElementCollection
    @LazyCollection(value = LazyCollectionOption.FALSE)
    private List<String> fileRefs;

    ConversationResource() {}

    ConversationResource(String conversationId, String receiverId, String messagetypeId, LocalDateTime lastUpdate,
                         List<String> fileRefs){
        this.conversationId = conversationId;
        this.receiverId = receiverId;
        this.messagetypeId = messagetypeId;
        this.lastUpdate = lastUpdate;
        this.fileRefs = fileRefs;
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

    public List<String> getFileRefs() {
        return fileRefs;
    }

    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(LocalDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public void setFileRefs(List<String> fileRefs) {
        this.fileRefs = fileRefs;
    }

    public void addFileRef(String fileRef) {
        this.fileRefs.add(fileRef);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("conversationId", conversationId)
                .add("receiverId", receiverId)
                .add("messagetypeId", messagetypeId)
                .add("lastUpdate", lastUpdate)
                .add("fileRefs", fileRefs)
                .toString();
    }
}
