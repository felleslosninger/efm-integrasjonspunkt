package no.difi.meldingsutveksling.nextmove;

import lombok.*;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.xml.bind.annotation.XmlRootElement;

@Getter
@Setter
@ToString
@Entity
@DiscriminatorValue("dpe")
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "dpe", namespace = "urn:no:difi:meldingsutveksling:2.0")
public class DpeMessage extends BusinessMessage {
    private String customProperties;
}
