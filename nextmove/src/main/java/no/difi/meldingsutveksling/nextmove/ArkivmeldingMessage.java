package no.difi.meldingsutveksling.nextmove;

import lombok.*;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;

@Getter
@Setter
@ToString
@Entity
@DiscriminatorValue("arkivmelding")
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "arkivmelding", namespace = "urn:no:difi:meldingsutveksling:2.0")
public class ArkivmeldingMessage extends BusinessMessage {

    private String dpoField;
}
