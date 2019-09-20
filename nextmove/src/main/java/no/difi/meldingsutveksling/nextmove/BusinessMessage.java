package no.difi.meldingsutveksling.nextmove;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
import lombok.*;
import no.difi.meldingsutveksling.validation.group.ValidationGroups;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(subTypes = {
        ArkivmeldingMessage.class,
        DigitalDpvMessage.class,
        DpiDigitalMessage.class,
        DpiPrintMessage.class,
        InnsynskravMessage.class,
        PubliseringMessage.class
}, discriminator = "type")
public abstract class BusinessMessage extends AbstractEntity<Long> {

    @JsonIgnore
    private String type;

    @NotNull(groups = {
            ValidationGroups.ServiceIdentifier.DPF.class,
            ValidationGroups.DocumentType.Digital.class
    })
    private Integer sikkerhetsnivaa;

    private String hoveddokument;
}
