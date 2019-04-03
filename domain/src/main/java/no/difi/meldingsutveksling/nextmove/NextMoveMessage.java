package no.difi.meldingsutveksling.nextmove;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import org.hibernate.annotations.DiscriminatorOptions;

import javax.persistence.*;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@DiscriminatorColumn(name = "direction", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorOptions(force = true)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Data
@RequiredArgsConstructor
@NoArgsConstructor
public class NextMoveMessage {

    @Id
    @GeneratedValue
    @Setter(AccessLevel.PRIVATE)
    private Long id;

    @Column(unique = true)
    @NonNull
    private String conversationId;
    @NonNull
    private String receiverIdentifier;
    @NonNull
    private String senderIdentifier;
    @NonNull
    private ServiceIdentifier serviceIdentifier;

    @Version
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Setter(AccessLevel.PRIVATE)
    private Date lastUpdated;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "message_id", nullable = false)
    private Set<BusinessMessageFile> files;

    @NonNull
    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private StandardBusinessDocument sbd;

    public static NextMoveMessage of(StandardBusinessDocument sbd) {
        NextMoveMessage message = new NextMoveMessage(
                sbd.getConversationId(),
                sbd.getReceiverIdentifier(),
                sbd.getSenderIdentifier(),
                sbd.getServiceIdentifier(),
                sbd);
        message.setFiles(new HashSet<>());
        return message;
    }

    @JsonIgnore
    public Set<BusinessMessageFile> getOrCreateFiles() {
        if (files == null) {
            files = new LinkedHashSet<>();
        }
        return files;
    }

    public BusinessMessage getBusinessMessage() {
        if (getSbd().getAny() == null) {
            throw new NextMoveRuntimeException("SBD missing BusinessMessage");
        }
        if (!(getSbd().getAny() instanceof BusinessMessage)) {
            throw new NextMoveRuntimeException("SBD.any not instance of BusinessMessage");
        }
        return (BusinessMessage) getSbd().getAny();
    }
}
