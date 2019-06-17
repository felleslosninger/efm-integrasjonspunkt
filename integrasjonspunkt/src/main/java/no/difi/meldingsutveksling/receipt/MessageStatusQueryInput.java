package no.difi.meldingsutveksling.receipt;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel
public class MessageStatusQueryInput {

    @ApiModelProperty(
            value = "convId",
            example = "1")
    Integer convId;

    @ApiModelProperty(
            value = "The conversation ID. Usually a UUID",
            example = "90c0bacf-c233-4a54-96fc-e205b79862d9"
    )
    String conversationId;

    @ApiModelProperty(
            value = "Message status",
            allowableValues = "OPPRETTET, SENDT, MOTTATT, LEVERT, LEST, FEIL, ANNET, INNKOMMENDE_MOTTAT, INNKOMMENDE_LEVERT, LEVETID_UTLOPT",
            example = "LEVERT"
    )
    String status;
}

