package no.difi.meldingsutveksling.nextmove;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import no.arkivverket.standarder.noark5.arkivmelding.Arkivmelding;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.config.dpi.securitylevel.SecurityLevel;
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
        property = "serviceIdentifier",
        visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = DpoConversationResource.class, name = "DPO"),
        @JsonSubTypes.Type(value = DpvConversationResource.class, name = "DPV"),
        @JsonSubTypes.Type(value = DpiConversationResource.class, name = "DPI"),
        @JsonSubTypes.Type(value = DpfConversationResource.class, name = "DPF"),
        @JsonSubTypes.Type(value = DpeInnsynConversationResource.class, name = "DPE_INNSYN"),
        @JsonSubTypes.Type(value = DpeDataConversationResource.class, name = "DPE_DATA"),
        @JsonSubTypes.Type(value = DpeReceiptConversationResource.class, name = "DPE_RECEIPT")
})
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlSeeAlso({DpoConversationResource.class,
        DpvConversationResource.class,
        DpiConversationResource.class,
        DpfConversationResource.class,
        DpeInnsynConversationResource.class,
        DpeDataConversationResource.class,
        DpeReceiptConversationResource.class})
public abstract class ConversationResource {

    @Id
    @XmlElement
    private String conversationId;
    @XmlElement
    private ServiceIdentifier serviceIdentifier;
    @Embedded
    @XmlElement
    private Sender sender;
    @Embedded
    @XmlElement
    private Receiver receiver;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @XmlElement
    @XmlJavaTypeAdapter(LocalDateTimeAdapter.class)
    private LocalDateTime lastUpdate;
    @JsonIgnore
    private ConversationDirection direction;
    @XmlElement
    @JsonIgnore
    private boolean hasArkivmelding;
    @JsonIgnore
    private boolean locked;
    @XmlElement
    private SecurityLevel securityLevel;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private LocalDateTime lockTimeout;
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
    @JsonIgnore
    @Transient
    private Arkivmelding arkivmelding;

    ConversationResource() {}

    ConversationResource(String conversationId, Sender sender, Receiver receiver, ServiceIdentifier serviceIdentifier,
                         LocalDateTime lastUpdate, Map fileRefs, Map customProperties){
        this.conversationId = conversationId;
        this.sender = sender;
        this.receiver = receiver;
        this.serviceIdentifier = serviceIdentifier;
        this.lastUpdate = lastUpdate;
        this.fileRefs = fileRefs;
        this.customProperties = customProperties;
    }

    public void addFileRef(String fileRef) {
        Optional<Integer> max = this.fileRefs.keySet().stream().max(Integer::compare);
        this.fileRefs.put(max.isPresent() ? max.get()+1 : 0, fileRef);
    }

}
