package no.difi.meldingsutveksling.nextmove;

import lombok.*;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.xml.bind.annotation.XmlRootElement;

@Getter
@Setter
@ToString
@Entity
@DiscriminatorValue("dpo")
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "dpo", namespace = "urn:no:difi:meldingsutveksling:2.0")
public class DpoMessage extends BusinessMessage {
    private String dpoField;
}
