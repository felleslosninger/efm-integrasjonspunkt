package no.difi.meldingsutveksling.nextmove;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
import lombok.*;
import no.difi.meldingsutveksling.validation.group.ValidationGroups;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type")
@ApiModel(subTypes = {
        ArkivmeldingMessage.class,
        DigitalDpvMessage.class,
        DpiDigitalMessage.class,
        DpiPrintMessage.class,
        InnsynskravMessage.class,
        PubliseringMessage.class
}, discriminator = "type")
public abstract class BusinessMessage extends AbstractEntity<Long> {

    @Column(name = "type", insertable = false, updatable = false)
    @JsonIgnore
    private String type;

    @NotNull(groups = {
            ValidationGroups.ServiceIdentifier.DPF.class,
            ValidationGroups.DocumentType.Digital.class
    })
    private Integer sikkerhetsnivaa;

    private String primaerDokumentNavn;
}
