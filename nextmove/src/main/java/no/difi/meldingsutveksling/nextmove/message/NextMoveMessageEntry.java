package no.difi.meldingsutveksling.nextmove.message;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import javax.persistence.*;

@Entity
@RequiredArgsConstructor(staticName = "of")
@NoArgsConstructor
@Data
public class NextMoveMessageEntry {

    @Id
    @Column(name = "entry_id")
    @GeneratedValue
    private String entryId;

    @Column(name = "conversation_id")
    @NonNull
    private String conversationId;

    @NonNull
    private String filename;

    @Lob
    @NonNull
    private byte[] content;
}
