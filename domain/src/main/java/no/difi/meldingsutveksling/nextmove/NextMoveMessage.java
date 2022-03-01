package no.difi.meldingsutveksling.nextmove;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import no.difi.meldingsutveksling.MessageInformable;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.domain.PartnerIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.jpa.StandardBusinessDocumentConverter;
import org.hibernate.annotations.DiscriminatorOptions;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

@DiscriminatorColumn(name = "direction", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorOptions(force = true)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Getter
@Setter
@ToString
@Entity
@RequiredArgsConstructor
@NoArgsConstructor
public abstract class NextMoveMessage extends AbstractEntity<Long> implements MessageInformable {

    @Column(length = 36)
    @NonNull
    private String conversationId;

    @NonNull
    @Column(length = 36, unique = true)
    private String messageId;

    @NonNull
    private String processIdentifier;
    @NonNull
    private String receiverIdentifier;
    @NonNull
    private String senderIdentifier;
    @NonNull
    private ServiceIdentifier serviceIdentifier;

    @UpdateTimestamp
    @Setter(AccessLevel.PRIVATE)
    private OffsetDateTime lastUpdated;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "message")
    private Set<BusinessMessageFile> files;

    @NonNull
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
    public BusinessMessage<?> getBusinessMessage() {
        return getSbd().getBusinessMessage(BusinessMessage.class)
                .orElseThrow(() -> new NextMoveRuntimeException("SBD.any not instance of BusinessMessage"));
    }

    @JsonIgnore
    public <T> Optional<T> getBusinessMessage(Class<T> clazz) {
        return getSbd().getBusinessMessage(clazz);
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
    public PartnerIdentifier getSender() {
        return getSbd().getSenderIdentifier();
    }

    @Override
    public PartnerIdentifier getReceiver() {
        return getSbd().getReceiverIdentifier();
    }

}
