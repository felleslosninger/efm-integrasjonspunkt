package no.difi.meldingsutveksling.receipt;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiParam;
import lombok.Data;
import no.difi.meldingsutveksling.nextmove.ConversationDirection;

@Data
@ApiModel
public class ConversationQueryInput {

    @ApiParam(value = "conversationId")
    String conversationId;

    @ApiParam(value = "receiverIdentifier")
    String receiverIdentifier;

    @ApiParam(value = "senderIdentifier")
    String senderIdentifier;

    @ApiParam(value = "serviceIdentifier")
    String serviceIdentifier;

    @ApiParam(value = "messageReference")
    String messageReference;

    @ApiParam(value = "messageTitle")
    String messageTitle;

    @ApiParam(value = "pollable")
    Boolean pollable;

    @ApiParam(value = "finished")
    Boolean finished;

    @ApiParam(value = "msh")
    Boolean msh;

    @ApiParam(value = "serviceIdentifier")
    ConversationDirection direction;
}
