package no.difi.meldingsutveksling.nextmove;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
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
    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private StandardBusinessDocument sbd;

    @JsonIgnore
    public Set<BusinessMessageFile> getFiles() throws NextMoveException {
        return getBusinessMessage().getFiles();
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
