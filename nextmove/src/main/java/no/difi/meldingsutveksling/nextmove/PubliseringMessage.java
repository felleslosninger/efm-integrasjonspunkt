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
@DiscriminatorValue("publisering")
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "publisering", namespace = "urn:no:difi:meldingsutveksling:2.0")
public class PubliseringMessage extends BusinessMessage {
    @NotNull
    private String orgnr;
}
