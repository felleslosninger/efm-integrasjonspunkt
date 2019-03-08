package no.difi.meldingsutveksling.nextmove;

import lombok.NoArgsConstructor;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("in")
@NoArgsConstructor
public class NextMoveInMessage extends NextMoveMessage {

    public NextMoveInMessage(String conversationId, String receiverIdentifier, String senderIdentifier, StandardBusinessDocument sbd) {
        super(conversationId, receiverIdentifier, senderIdentifier, sbd);
    }

    public static NextMoveInMessage of(StandardBusinessDocument sbd) {
        return new NextMoveInMessage(sbd.getConversationId(), sbd.getReceiverOrgNumber(), sbd.getSenderOrgNumber(), sbd);
    }
}
