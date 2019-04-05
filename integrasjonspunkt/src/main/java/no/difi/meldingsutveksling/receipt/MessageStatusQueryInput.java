package no.difi.meldingsutveksling.receipt;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiParam;
import lombok.Data;

@Data
@ApiModel
public class MessageStatusQueryInput {

    @ApiParam(value = "convId")
    Integer convId;

    @ApiParam(value = "conversationId")
    String conversationId;

    @ApiParam(value = "status")
    String status;
}
