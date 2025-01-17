package no.difi.meldingsutveksling.nextmove;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import jakarta.persistence.*;
import java.sql.Blob;

@Entity
@Table(name = "next_move_message_entry")
@RequiredArgsConstructor(staticName = "of")
@NoArgsConstructor
@Data
public class NextMoveMessageEntry {

    @Id
    @Column(name = "entry_id")
    @GeneratedValue
    private Integer entryId;

    @Column(name = "message_id")
    @NonNull
    private String messageId;

    @Column(name = "filename")
    @NonNull
    private String filename;

    @Lob
    @Column(name = "content")
    @NonNull
    private Blob content;

    @Column(name = "size")
    @NonNull
    private Long size;
}
