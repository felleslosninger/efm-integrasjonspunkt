package no.difi.meldingsutveksling.status;

import lombok.Data;
import no.difi.meldingsutveksling.nextmove.ConversationDirection;

@Data
public class ConversationQueryInput {

    String conversationId;
    String messageId;
    String receiver;
    String receiverIdentifier;
    String sender;
    String senderIdentifier;
    String serviceIdentifier;
    String messageReference;
    String messageTitle;
    Boolean pollable;
    Boolean finished;
    ConversationDirection direction;

}
