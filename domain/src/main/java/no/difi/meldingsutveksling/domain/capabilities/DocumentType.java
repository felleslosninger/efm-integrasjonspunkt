package no.difi.meldingsutveksling.domain.capabilities;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "Document type information")
public class DocumentType {

    @ApiModelProperty(
            value = "Document type. This is always identical to the last part of the standard.",
            example = "arkivmelding"
    )
    private String type;

    @ApiModelProperty(
            value = "Document standard",
            example = "urn:no:difi:arkivmelding:xsd::arkivmelding"
    )
    private String standard;
}
