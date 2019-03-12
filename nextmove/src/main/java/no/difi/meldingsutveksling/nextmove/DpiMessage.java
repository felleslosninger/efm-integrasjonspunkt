package no.difi.meldingsutveksling.nextmove;

import lombok.*;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.xml.bind.annotation.XmlRootElement;

@Getter
@Setter
@ToString
@Entity
@DiscriminatorValue("dpi")
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "dpi", namespace = "urn:no:difi:profile:digitalpost:ver1.0")
public class DpiMessage extends BusinessMessage {

    private String title;

}
