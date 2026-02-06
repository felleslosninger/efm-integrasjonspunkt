package no.difi.meldingsutveksling.nextmove;

import lombok.*;
import no.difi.meldingsutveksling.domain.BusinessMessage;
import no.difi.meldingsutveksling.validation.group.NextMoveValidationGroups;

import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public abstract class BusinessMessageAsAttachment<T extends BusinessMessageAsAttachment<T>> implements DocumentAsAttachment<T>, BusinessMessage {


    @NotNull(groups = {
            NextMoveValidationGroups.MessageType.Digital.class,
            NextMoveValidationGroups.MessageType.Print.class
    })
    private String hoveddokument;

    public T setHoveddokument(String hoveddokument) {
        this.hoveddokument = hoveddokument;
        return (T) this;
    }


}
