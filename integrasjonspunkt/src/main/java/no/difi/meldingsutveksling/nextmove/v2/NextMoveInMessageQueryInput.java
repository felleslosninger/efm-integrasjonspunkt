package no.difi.meldingsutveksling.nextmove.v2;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel
public class NextMoveInMessageQueryInput {

    @ApiModelProperty(value = "conversationId")
    String conversationId;
    @ApiModelProperty(value = "receiverIdentifier")
    String receiverIdentifier;
    @ApiModelProperty(value = "senderIdentifier")
    String senderIdentifier;
    @ApiModelProperty(value = "serviceIdentifier")
    String serviceIdentifier;
}
