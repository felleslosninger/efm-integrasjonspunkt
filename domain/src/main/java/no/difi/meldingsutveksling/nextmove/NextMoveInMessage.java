package no.difi.meldingsutveksling.nextmove;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.time.ZonedDateTime;

@Entity
@DiscriminatorValue("in")
@NoArgsConstructor
@Data
public class NextMoveInMessage extends NextMoveMessage {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private ZonedDateTime lockTimeout;


    public NextMoveInMessage(String conversationId, String receiverIdentifier, String senderIdentifier, StandardBusinessDocument sbd) {
        super(conversationId, receiverIdentifier, senderIdentifier, sbd);
    }

    public static NextMoveInMessage of(StandardBusinessDocument sbd) {
        return new NextMoveInMessage(sbd.getConversationId(), sbd.getReceiverOrgNumber(), sbd.getSenderOrgNumber(), sbd);
    }
}
