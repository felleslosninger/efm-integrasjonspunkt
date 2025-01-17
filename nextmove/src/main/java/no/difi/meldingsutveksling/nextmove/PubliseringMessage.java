package no.difi.meldingsutveksling.nextmove;

import lombok.*;

import jakarta.validation.constraints.NotNull;
import jakarta.xml.bind.annotation.XmlRootElement;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "publisering", namespace = "urn:no:difi:meldingsutveksling:2.0")
public class PubliseringMessage extends BusinessMessage<PubliseringMessage> {
    @NotNull
    private String orgnr;
}
