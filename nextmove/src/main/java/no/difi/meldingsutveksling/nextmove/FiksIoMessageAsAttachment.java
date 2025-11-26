package no.difi.meldingsutveksling.nextmove;

import jakarta.validation.constraints.NotNull;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import no.difi.meldingsutveksling.validation.group.NextMoveValidationGroups;

@Getter
@Setter
@ToString
@NoArgsConstructor
@XmlRootElement(name = "fiksio", namespace = "urn:no:difi:meldingsutveksling:2.0")
public class FiksIoMessageAsAttachment extends BusinessMessageAsAttachment<FiksIoMessageAsAttachment> implements HasSikkerhetsNivaa<FiksIoMessageAsAttachment> {
    @NotNull(groups = {
        NextMoveValidationGroups.ServiceIdentifier.DPF.class
    })
    private Integer sikkerhetsnivaa;
}
