package no.difi.meldingsutveksling.status;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel
public class MessageStatusQueryInput {

    @ApiModelProperty(
            value = "id",
            example = "1")
    Long id;

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
            value = "Message status",
            allowableValues = "OPPRETTET, SENDT, MOTTATT, LEVERT, LEST, FEIL, ANNET, INNKOMMENDE_MOTTAT, INNKOMMENDE_LEVERT, LEVETID_UTLOPT",
            example = "LEVERT"
    )
    String status;
}

