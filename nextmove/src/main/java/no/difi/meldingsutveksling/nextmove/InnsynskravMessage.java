package no.difi.meldingsutveksling.nextmove;

import io.swagger.annotations.ApiModel;
import lombok.*;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;

@Getter
@Setter
@ToString
@Entity
@DiscriminatorValue("innsynskrav")
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "innsynskrav", namespace = "urn:no:difi:meldingsutveksling:2.0")
@ApiModel(value="innsynskrav", parent = BusinessMessage.class)
public class InnsynskravMessage extends BusinessMessage {
    @NotNull
    private String orgnr;
    @NotNull
    private String epost;
}
