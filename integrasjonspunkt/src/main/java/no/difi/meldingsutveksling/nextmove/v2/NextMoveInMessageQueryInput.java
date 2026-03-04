package no.difi.meldingsutveksling.nextmove.v2;

import lombok.Data;

@Data
public class NextMoveInMessageQueryInput {

    String conversationId;
    String messageId;
    String receiverIdentifier;
    String senderIdentifier;
    String serviceIdentifier;
    String process;
    String herId2;

}
