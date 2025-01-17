package no.difi.meldingsutveksling.nextmove;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.time.OffsetDateTime;

@Entity
@DiscriminatorValue("in")
@NoArgsConstructor
@Getter
@Setter
@ToString
public class NextMoveInMessage extends NextMoveMessage {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private OffsetDateTime lockTimeout;

    public NextMoveInMessage(String conversationId,
                             String messageId,
                             String processIdentifier,
                             String receiverIdentifier,
                             String senderIdentifier,
                             ServiceIdentifier serviceIdentifier,
                             StandardBusinessDocument sbd) {
        super(conversationId, messageId, processIdentifier, receiverIdentifier, senderIdentifier, serviceIdentifier, sbd);
    }

    public static NextMoveInMessage of(StandardBusinessDocument sbd, ServiceIdentifier serviceIdentifier) {
        return new NextMoveInMessage(
                sbd.getConversationId(),
                sbd.getMessageId(),
                sbd.getProcess(),
                sbd.getReceiverIdentifier().getPrimaryIdentifier(),
                sbd.getSenderIdentifier().getPrimaryIdentifier(),
                serviceIdentifier,
                sbd);
    }

    @Override
    public ConversationDirection getDirection() {
        return ConversationDirection.INCOMING;
    }
}
