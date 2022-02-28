package no.difi.meldingsutveksling.nextmove;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.OptimisticLocking;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.util.HashSet;

@Entity
@DiscriminatorValue("out")
@NoArgsConstructor
@Getter
@Setter
@ToString
@OptimisticLocking(type = OptimisticLockType.DIRTY)
@DynamicUpdate
public class NextMoveOutMessage extends NextMoveMessage {

    public NextMoveOutMessage(String conversationId,
                              String messageId,
                              String processIdentifier,
                              String receiverIdentifier,
                              String senderIdentifier,
                              ServiceIdentifier serviceIdentifier,
                              StandardBusinessDocument sbd) {
        super(conversationId, messageId, processIdentifier, receiverIdentifier, senderIdentifier, serviceIdentifier, sbd);
    }


    public static NextMoveOutMessage of(StandardBusinessDocument sbd, ServiceIdentifier serviceIdentifier) {
        NextMoveOutMessage message = new NextMoveOutMessage(
                SBDUtil.getConversationId(sbd),
                SBDUtil.getMessageId(sbd),
                SBDUtil.getProcess(sbd),
                sbd.getReceiverIdentifier().getPrimaryIdentifier(),
                sbd.getSenderIdentifier().getPrimaryIdentifier(),
                serviceIdentifier,
                sbd);
        message.setFiles(new HashSet<>());
        return message;
    }

    public boolean isPrimaryDocument(String filename) {
        return filename != null && filename.equals(getBusinessMessage().getHoveddokument());
    }

    @Override
    public ConversationDirection getDirection() {
        return ConversationDirection.OUTGOING;
    }
}
