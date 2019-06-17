package no.difi.meldingsutveksling.receipt;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import no.difi.meldingsutveksling.nextmove.ConversationDirection;

@Data
@ApiModel
public class ConversationQueryInput {

    @ApiModelProperty(
            value = "The conversation ID. Usually a UUID",
            example = "90c0bacf-c233-4a54-96fc-e205b79862d9"
    )
    String conversationId;

    @ApiModelProperty(
            value = "Receiver identifier",
            example = "0192:987654321"
    )
    String receiverIdentifier;

    @ApiModelProperty(
            value = "Sender Identifier",
            example = "0192:987654321"
    )
    String senderIdentifier;

    @ApiModelProperty(
            value = "Service identifier",
            example = "DPO",
            allowableValues = "DPO, DPV, DPI, DPF, DPE"
    )
    String serviceIdentifier;

    @ApiModelProperty(
            value = "Message reference"
    )
    String messageReference;

    @ApiModelProperty(
            value = "Message title"
    )
    String messageTitle;

    @ApiModelProperty(
            value = "pollable"
    )
    Boolean pollable;

    @ApiModelProperty(
            value = "finished"
    )
    Boolean finished;

    @ApiModelProperty(
            value = "Conversation direction",
            example = "OUTGOING",
            allowableValues = "OUTGOING, INCOMING"
    )
    ConversationDirection direction;
}
