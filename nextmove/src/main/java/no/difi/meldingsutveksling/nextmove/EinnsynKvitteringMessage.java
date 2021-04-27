package no.difi.meldingsutveksling.nextmove;

import lombok.*;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "einnsyn_kvittering", namespace = "urn:no:difi:meldingsutveksling:2.0")
public class EinnsynKvitteringMessage extends BusinessMessage<EinnsynKvitteringMessage> {

    @NotNull
    private String dokumentId;
    @NotNull
    private String status;
    @NotNull
    private EinnsynType referanseType;
}
