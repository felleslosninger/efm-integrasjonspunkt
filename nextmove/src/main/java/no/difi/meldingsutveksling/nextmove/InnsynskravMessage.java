package no.difi.meldingsutveksling.nextmove;

import lombok.*;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "innsynskrav", namespace = "urn:no:difi:meldingsutveksling:2.0")
public class InnsynskravMessage extends BusinessMessage<InnsynskravMessage> {
    @NotNull
    private String orgnr;
    @NotNull
    private String epost;
}
