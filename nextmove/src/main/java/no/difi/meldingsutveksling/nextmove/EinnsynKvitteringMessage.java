package no.difi.meldingsutveksling.nextmove;

import lombok.*;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.xml.bind.annotation.XmlRootElement;

@Getter
@Setter
@ToString
@Entity
@DiscriminatorValue("einnsyn_kvittering")
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "einnsyn_kvittering", namespace = "urn:no:difi:meldingsutveksling:2.0")
public class EinnsynKvitteringMessage extends BusinessMessage {
    private String content;
}