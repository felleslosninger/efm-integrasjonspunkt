package no.difi.meldingsutveksling.nextmove;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;

import javax.persistence.*;
import java.util.Set;

@Entity
@Data
@RequiredArgsConstructor(staticName = "of")
@NoArgsConstructor
public class NextMoveMessage {

    @Id
    @GeneratedValue
    private Long id;

    @Column(unique = true)
    @NonNull
    private String conversationId;
    @NonNull
    private String receiverIdentifier;
    @NonNull
    private String senderIdentifier;
    @NonNull
    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private StandardBusinessDocument sbd;

    public static NextMoveMessage of(StandardBusinessDocument sbd) {
        return new NextMoveMessage(sbd.getConversationId(), sbd.getReceiverOrgNumber(), sbd.getSenderOrgNumber(), sbd);
    }

    @JsonIgnore
    public Set<BusinessMessageFile> getFiles() throws NextMoveException {
        return getBusinessMessage().getFiles();
    }

    @JsonIgnore
    public ServiceIdentifier getServiceIdentifier() {
        String diType = sbd.getStandardBusinessDocumentHeader().getDocumentIdentification().getType();
        return ServiceIdentifier.safeValueOf(diType).orElseThrow(() ->
                new NextMoveRuntimeException(String.format("Could not create ServiceIdentifier from documentIdentification.type=%s for message with id=%s", diType, getConversationId())));
    }

    private BusinessMessage getBusinessMessage() throws NextMoveException {
        if (getSbd().getAny() == null) {
            throw new NextMoveException("SBD missing BusinessMessage");
        }
        if (!(getSbd().getAny() instanceof BusinessMessage)) {
            throw new NextMoveException("SBD.any not instance of BusinessMessage");
        }
        return (BusinessMessage) getSbd().getAny();
    }
}
