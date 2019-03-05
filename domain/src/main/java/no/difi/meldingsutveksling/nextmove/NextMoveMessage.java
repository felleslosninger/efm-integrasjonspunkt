package no.difi.meldingsutveksling.nextmove;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;

import javax.persistence.*;

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

}
