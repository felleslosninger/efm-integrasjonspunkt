package no.difi.meldingsutveksling.nextmove;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import no.difi.meldingsutveksling.MessageInformable;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.domain.Organisasjonsnummer;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.jpa.StandardBusinessDocumentConverter;
import org.hibernate.annotations.DiscriminatorOptions;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@DiscriminatorColumn(name = "direction", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorOptions(force = true)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Getter
@Setter
@ToString
@Entity
@NoArgsConstructor
public abstract class NextMoveMessage extends AbstractEntity<Long> implements MessageInformable {

    public NextMoveMessage(String conversationId, String messageId, String processIdentifier, String receiverIdentifier, String senderIdentifier, ServiceIdentifier serviceIdentifier, StandardBusinessDocument sbd) {
        this.conversationId = conversationId;
        this.messageId = messageId;
        this.processIdentifier = processIdentifier;
        this.receiverIdentifier = receiverIdentifier;
        this.senderIdentifier = senderIdentifier;
        this.serviceIdentifier = serviceIdentifier;
        this.sbd = sbd;
    }

    @Column(length = 36)
    private String conversationId;

    @Column(length = 36, unique = true)
    private String messageId;

    private String processIdentifier;

    private String receiverIdentifier;

    private String senderIdentifier;

    private ServiceIdentifier serviceIdentifier;

    @UpdateTimestamp
    @Setter(AccessLevel.PRIVATE)
    private OffsetDateTime lastUpdated;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "message")
    private Set<BusinessMessageFile> files;

    @Convert(converter = StandardBusinessDocumentConverter.class)
    @Lob
    private StandardBusinessDocument sbd;

    @JsonIgnore
    public NextMoveMessage addFile(BusinessMessageFile file) {
        Set<BusinessMessageFile> fileSet = getOrCreateFiles();
        file.setMessage(this);
        fileSet.add(file.setDokumentnummer(fileSet.size() + 1));
        return this;
    }

    @JsonIgnore
    public Set<BusinessMessageFile> getOrCreateFiles() {
        if (files == null) {
            files = new LinkedHashSet<>();
        }
        return files;
    }

    @JsonIgnore
    public BusinessMessage getBusinessMessage() {
        if (getSbd().getAny() == null) {
            throw new NextMoveRuntimeException("SBD missing BusinessMessage");
        }
        if (!(getSbd().getAny() instanceof BusinessMessage)) {
            throw new NextMoveRuntimeException("SBD.any not instance of BusinessMessage");
        }
        return (BusinessMessage) getSbd().getAny();
    }

    @Override
    public String getDocumentIdentifier() {
        return getSbd().getDocumentType();
    }

    @Override
    public OffsetDateTime getExpiry() {
        return getSbd().getExpectedResponseDateTime().orElse(null);
    }

    @Override
    public Organisasjonsnummer getSender() {
        return getSbd().getSender();
    }

    @Override
    public Organisasjonsnummer getReceiver() {
        return getSbd().getReceiver();
    }

}
