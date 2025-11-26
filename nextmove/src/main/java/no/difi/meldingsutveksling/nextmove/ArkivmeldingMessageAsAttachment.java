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
@XmlRootElement(name = "arkivmelding", namespace = "urn:no:difi:meldingsutveksling:2.0")
public class ArkivmeldingMessageAsAttachment extends BusinessMessageAsAttachment<ArkivmeldingMessageAsAttachment> implements HasSikkerhetsNivaa<ArkivmeldingMessageAsAttachment> {
    @NotNull(groups = {
        NextMoveValidationGroups.ServiceIdentifier.DPF.class
    })
    private Integer sikkerhetsnivaa;
    private DpvSettings dpv;
    private DpfSettings dpf;
}
