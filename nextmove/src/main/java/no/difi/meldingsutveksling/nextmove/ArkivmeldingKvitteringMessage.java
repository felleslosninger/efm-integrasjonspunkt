package no.difi.meldingsutveksling.nextmove;

import lombok.*;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.xml.bind.annotation.XmlRootElement;

@Getter
@Setter
@ToString
@Entity
@DiscriminatorValue("arkivmelding_kvittering")
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "arkivmelding_kvittering", namespace = "urn:no:difi:meldingsutveksling:2.0")
public class ArkivmeldingKvitteringMessage extends BusinessMessage {
    private String content;
}
