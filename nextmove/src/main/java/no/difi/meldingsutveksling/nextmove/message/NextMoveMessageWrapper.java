package no.difi.meldingsutveksling.nextmove.message;

import lombok.RequiredArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;

@Entity
@RequiredArgsConstructor(staticName = "of")
public class NextMoveMessageWrapper {

    @Id
    private String conversationId;

    @Lob
    private byte[] content;
}
