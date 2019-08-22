package no.difi.meldingsutveksling.domain.capabilities;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel(value = "capabilities")
public class Capabilities {

    @SuppressWarnings("squid:S1700")
    @ApiModelProperty(value = "List of capabilities")
    private List<Capability> capabilities;
}
