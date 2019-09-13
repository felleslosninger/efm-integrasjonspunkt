package no.difi.meldingsutveksling.domain.capabilities;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import no.difi.meldingsutveksling.ServiceIdentifier;

import java.util.List;

@Data
@ApiModel(value = "capability")
public class Capability {

    @ApiModelProperty(
            value = "Process",
            example = "urn:no:difi:profile:arkivmelding:administrasjon:ver1.0",
            required = true
    )
    private String process;

    @ApiModelProperty(
            value = "Service identifier",
            example = "DPO",
            allowableValues = "DPO, DPV, DPI, DPF, DPE",
            required = true
    )
    private ServiceIdentifier serviceIdentifier;

    private PostalAddress postAddress;

    private PostalAddress returnAddress;

    private List<DocumentType> documentTypes;
}
