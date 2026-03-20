package no.difi.meldingsutveksling.nextmove;

import lombok.*;

import jakarta.validation.constraints.NotNull;
import jakarta.xml.bind.annotation.XmlRootElement;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "innsynskrav", namespace = "urn:no:difi:meldingsutveksling:2.0")
public class InnsynskravMessage extends BusinessMessageAsAttachment<InnsynskravMessage> {
    @NotNull
    private String orgnr;
    @NotNull
    private String epost;
}
