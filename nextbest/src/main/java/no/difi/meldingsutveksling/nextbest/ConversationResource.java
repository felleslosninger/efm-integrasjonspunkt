package no.difi.meldingsutveksling.nextbest;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.base.MoreObjects;
import lombok.Data;
import no.difi.meldingsutveksling.xml.LocalDateTimeAdapter;

import javax.persistence.*;
import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type")
@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "messagetypeId",
        visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = DpoConversationResource.class, name = "DPO"),
        @JsonSubTypes.Type(value = DpvConversationResource.class, name = "DPV"),
        @JsonSubTypes.Type(value = DpiConversationResource.class, name = "DPI"),
        @JsonSubTypes.Type(value = DpfConversationResource.class, name = "DPF"),
        @JsonSubTypes.Type(value = DpeInnsynConversationResource.class, name = "DPE_innsyn"),
        @JsonSubTypes.Type(value = DpeDataConversationResource.class, name = "DPE_data")
})
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlSeeAlso({DpeInnsynConversationResource.class, DpoConversationResource.class})
public abstract class ConversationResource {

    @Id
    @XmlElement
    private String conversationId;
    @XmlElement
    private String messagetypeId;
    @XmlElement
    private String senderId;
    @XmlElement
    private String receiverId;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @XmlElement
    @XmlJavaTypeAdapter(LocalDateTimeAdapter.class)
    private LocalDateTime lastUpdate;
    @JsonIgnore
    private ConversationDirection direction;
    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name = "fileid")
    @Column(name = "filename")
    @CollectionTable(name = "filerefs", joinColumns = @JoinColumn(name = "ids"))
    @XmlElement
    private Map<Integer, String> fileRefs;
    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name = "propId")
    @Column(name = "prop")
    @CollectionTable(name = "props", joinColumns = @JoinColumn(name = "pids"))
    @XmlElement
    private Map<String, String> customProperties;

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
