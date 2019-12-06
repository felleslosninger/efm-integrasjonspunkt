package no.difi.meldingsutveksling.nextmove;

import io.swagger.annotations.ApiModel;
import lombok.*;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "digital_dpv", namespace = "urn:no:difi:profile:digitalpost:ver1.0")
@ApiModel(value = "digital_dpv", parent = BusinessMessage.class)
public class DigitalDpvMessage extends BusinessMessage<DigitalDpvMessage> {

    @NotNull
    private String tittel;
    @NotNull
    private String sammendrag;
    @NotNull
    private String innhold;

}
