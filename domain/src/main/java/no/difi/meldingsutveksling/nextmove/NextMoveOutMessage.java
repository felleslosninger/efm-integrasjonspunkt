package no.difi.meldingsutveksling.nextmove;

import lombok.NoArgsConstructor;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("out")
@NoArgsConstructor
public class NextMoveOutMessage extends NextMoveMessage {

    public NextMoveOutMessage(String conversationId, String receiverIdentifier, String senderIdentifier, StandardBusinessDocument sbd) {
        super(conversationId, receiverIdentifier, senderIdentifier, sbd);
    }

    public static NextMoveOutMessage of(StandardBusinessDocument sbd) {
        return new NextMoveOutMessage(sbd.getConversationId(), sbd.getReceiverOrgNumber(), sbd.getSenderOrgNumber(), sbd);
    }
}
