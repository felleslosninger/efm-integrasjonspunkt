package no.difi.meldingsutveksling.receipt;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import no.difi.meldingsutveksling.nextmove.ConversationDirection;

@Data
@ApiModel
public class ConversationQueryInput {

    @ApiModelProperty(value = "conversationId")
    String conversationId;

    @ApiModelProperty(value = "receiverIdentifier")
    String receiverIdentifier;

    @ApiModelProperty(value = "senderIdentifier")
    String senderIdentifier;

    @ApiModelProperty(value = "serviceIdentifier")
    String serviceIdentifier;

    @ApiModelProperty(value = "messageReference")
    String messageReference;

    @ApiModelProperty(value = "messageTitle")
    String messageTitle;

    @ApiModelProperty(value = "pollable")
    Boolean pollable;

    @ApiModelProperty(value = "finished")
    Boolean finished;

    @ApiModelProperty(value = "serviceIdentifier")
    ConversationDirection direction;
}
