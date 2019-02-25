package no.difi.meldingsutveksling.nextmove.message;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import javax.persistence.*;
import java.sql.Blob;

@Entity
@RequiredArgsConstructor(staticName = "of")
@NoArgsConstructor
@Data
public class NextMoveMessageEntry {

    @Id
    @Column(name = "entry_id")
    @GeneratedValue
    private Integer entryId;

    @Column(name = "conversation_id")
    @NonNull
    private String conversationId;

    @Column(name = "filename")
    @NonNull
    private String filename;

    @Lob
    @Column(name ="content")
    @NonNull
    private Blob content;

    @Column(name = "size")
    @NonNull
    private long size;
}
