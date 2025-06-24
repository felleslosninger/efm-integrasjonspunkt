package no.difi.meldingsutveksling.nextmove;

import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@XmlRootElement(name = "dialogmelding", namespace = "urn:no:difi:meldingsutveksling:2.0")
public class Dialogmelding extends BusinessMessage<Dialogmelding> {
}
