package no.difi.meldingsutveksling.receipt;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel
public class MessageStatusQueryInput {

    @ApiModelProperty(value = "convId", example = "1")
    Integer convId;

    @ApiModelProperty(value = "conversationId")
    String conversationId;

    @ApiModelProperty(value = "status")
    String status;
}
