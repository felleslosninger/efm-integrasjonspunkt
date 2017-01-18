package no.difi.meldingsutveksling.nextbest;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.MoreObjects;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "direction")
public abstract class ConversationResource {

    @Id
    private String conversationId;
    private String senderId;
    private String receiverId;
    private String messagetypeId;
    @JsonIgnore
    private LocalDateTime lastUpdate;
    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name = "fileid")
    @Column(name = "filename")
    @CollectionTable(name = "filerefs", joinColumns = @JoinColumn(name = "ids"))
    private Map<Integer, String> fileRefs;

    ConversationResource() {}

    ConversationResource(String conversationId, String senderId, String receiverId, String messagetypeId,
                         LocalDateTime lastUpdate, Map fileRefs){
        this.conversationId = conversationId;
        this.senderId = senderId;
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

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getMessagetypeId() {
        return messagetypeId;
    }

    public void setMessagetypeId(String messagetypeId) {
        this.messagetypeId = messagetypeId;
    }

    public Map<Integer, String> getFileRefs() {
        return fileRefs;
    }

    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(LocalDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public void setFileRefs(Map<Integer, String> fileRefs) {
        this.fileRefs = fileRefs;
    }

    public void addFileRef(String fileRef) {
        Optional<Integer> max = this.fileRefs.keySet().stream().max(Integer::compare);
        this.fileRefs.put(max.isPresent() ? max.get()+1 : 0, fileRef);
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
