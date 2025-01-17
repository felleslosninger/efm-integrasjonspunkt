package no.difi.meldingsutveksling.nextmove;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import jakarta.xml.bind.annotation.XmlRootElement;

@Getter
@Setter
@ToString
@NoArgsConstructor
@XmlRootElement(name = "avtalt", namespace = "urn:no:difi:meldingsutveksling:2.0")
public class AvtaltMessage extends BusinessMessage<AvtaltMessage> {
    String identifier;
    Object content;
}