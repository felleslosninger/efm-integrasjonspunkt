package no.difi.meldingsutveksling.nextmove;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("out")
@NoArgsConstructor
@Getter
@Setter
@ToString
public class NextMoveOutMessage extends NextMoveMessage {

    public NextMoveOutMessage(String conversationId, String receiverIdentifier, String senderIdentifier, ServiceIdentifier serviceIdentifier, StandardBusinessDocument sbd) {
        super(conversationId, receiverIdentifier, senderIdentifier, serviceIdentifier, sbd);
    }

    public static NextMoveOutMessage of(StandardBusinessDocument sbd) {
        return new NextMoveOutMessage(
                sbd.getConversationId(),
                sbd.getReceiverOrgNumber(),
                sbd.getSenderOrgNumber(),
                sbd.getServiceIdentifier(),
                sbd);
    }
}
