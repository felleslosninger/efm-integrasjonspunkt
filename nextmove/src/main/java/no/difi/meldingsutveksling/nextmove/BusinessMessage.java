package no.difi.meldingsutveksling.nextmove;

import lombok.*;
import no.difi.meldingsutveksling.validation.group.ValidationGroups;

import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type")
public abstract class BusinessMessage extends AbstractEntity<Long> {

    @NotNull(groups = {
            ValidationGroups.ServiceIdentifier.DPF.class,
            ValidationGroups.DocumentType.Digital.class
    })
    private Integer sikkerhetsnivaa;

    private String primaerDokumentNavn;
}
