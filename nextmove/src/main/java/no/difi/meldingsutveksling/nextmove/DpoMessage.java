package no.difi.meldingsutveksling.nextmove;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.XmlRootElement;

@Data
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "dpo", namespace = "urn:no:difi:meldingsutveksling:2.0")
public class DpoMessage extends BusinessMessage {
    private String dpoField;
}
