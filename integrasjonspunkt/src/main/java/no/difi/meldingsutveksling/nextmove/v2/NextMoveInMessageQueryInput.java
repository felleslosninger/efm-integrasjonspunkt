package no.difi.meldingsutveksling.nextmove.v2;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiParam;
import lombok.Data;
import no.difi.meldingsutveksling.validation.EnabledService;

@Data
@ApiModel
public class NextMoveInMessageQueryInput {

    @ApiParam(value = "conversationId")
    String conversationId;
    @ApiParam(value = "receiverIdentifier")
    String receiverIdentifier;
    @ApiParam(value = "senderIdentifier")
    String senderIdentifier;
    @ApiParam(value = "serviceIdentifier")
    @EnabledService
    String serviceIdentifier;
}
