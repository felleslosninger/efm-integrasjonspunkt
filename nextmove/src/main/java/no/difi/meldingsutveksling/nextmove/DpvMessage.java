package no.difi.meldingsutveksling.nextmove;

import lombok.*;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.xml.bind.annotation.XmlRootElement;

@Getter
@Setter
@ToString
@Entity
@DiscriminatorValue("dpv")
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "dpv", namespace = "urn:no:difi:profile:eformidling:ver2.0")
public class DpvMessage extends BusinessMessage {
    private String title;
    private String content;
}
