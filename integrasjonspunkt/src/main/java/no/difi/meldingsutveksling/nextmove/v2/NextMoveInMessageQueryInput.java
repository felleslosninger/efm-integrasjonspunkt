package no.difi.meldingsutveksling.nextmove.v2;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel
public class NextMoveInMessageQueryInput {

    @ApiModelProperty(
            value = "The conversation ID. Usually a UUID",
            example = "90c0bacf-c233-4a54-96fc-e205b79862d9"
    )
    String conversationId;

    @ApiModelProperty(
            value = "The message ID. Usually a UUID",
            example = "90c0bacf-c233-4a54-96fc-e205b79862d9"
    )
    String messageId;

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
            value = "Process",
            example = "urn:no:difi:profile:arkivmelding:administrasjon:ver1.0"
    )
    String process;
}
