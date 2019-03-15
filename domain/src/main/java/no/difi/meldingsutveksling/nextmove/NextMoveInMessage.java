package no.difi.meldingsutveksling.nextmove;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.time.ZonedDateTime;

@Entity
@DiscriminatorValue("in")
@NoArgsConstructor
@Getter
@Setter
@ToString
public class NextMoveInMessage extends NextMoveMessage {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private ZonedDateTime lockTimeout;

    public NextMoveInMessage(String conversationId, String receiverIdentifier, String senderIdentifier, ServiceIdentifier serviceIdentifier, StandardBusinessDocument sbd) {
        super(conversationId, receiverIdentifier, senderIdentifier, serviceIdentifier, sbd);
    }

    public static NextMoveInMessage of(StandardBusinessDocument sbd) {
        return new NextMoveInMessage(
                sbd.getConversationId(),
                sbd.getReceiverOrgNumber(),
                sbd.getSenderOrgNumber(),
                sbd.getServiceIdentifier(),
                sbd);
    }
}
