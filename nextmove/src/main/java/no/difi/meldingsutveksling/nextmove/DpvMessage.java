package no.difi.meldingsutveksling.nextmove;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.XmlRootElement;

@Data
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "dpv", namespace = "urn:no:difi:meldingsutveksling:2.0")
public class DpvMessage extends BusinessMessage {
    private String dpvField;
}
