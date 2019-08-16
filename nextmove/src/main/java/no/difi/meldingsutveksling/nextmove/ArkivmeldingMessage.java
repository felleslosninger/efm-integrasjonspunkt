package no.difi.meldingsutveksling.nextmove;

import io.swagger.annotations.ApiModel;
import lombok.*;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.xml.bind.annotation.XmlRootElement;

@Getter
@Setter
@ToString
@Entity
@DiscriminatorValue("arkivmelding")
@NoArgsConstructor
@XmlRootElement(name = "arkivmelding", namespace = "urn:no:difi:meldingsutveksling:2.0")
@ApiModel(value="arkivmelding", parent = BusinessMessage.class)
public class ArkivmeldingMessage extends BusinessMessage {
}
