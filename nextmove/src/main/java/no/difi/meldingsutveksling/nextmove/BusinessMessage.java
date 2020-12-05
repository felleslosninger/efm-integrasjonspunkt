package no.difi.meldingsutveksling.nextmove;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import no.difi.meldingsutveksling.validation.group.ValidationGroups;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public abstract class BusinessMessage<T extends BusinessMessage<T>> extends AbstractEntity<Long> {

    @JsonIgnore
    private String type;

    @NotNull(groups = {
            ValidationGroups.ServiceIdentifier.DPF.class,
            ValidationGroups.DocumentType.Digital.class
    })
    private Integer sikkerhetsnivaa;

    @NotNull(groups = {
            ValidationGroups.DocumentType.Digital.class,
            ValidationGroups.DocumentType.Print.class
    })
    private String hoveddokument;

    public T setSikkerhetsnivaa(Integer sikkerhetsnivaa) {
        this.sikkerhetsnivaa = sikkerhetsnivaa;
        return (T) this;
    }

    public T setHoveddokument(String hoveddokument) {
        this.hoveddokument = hoveddokument;
        return (T) this;
    }
}
