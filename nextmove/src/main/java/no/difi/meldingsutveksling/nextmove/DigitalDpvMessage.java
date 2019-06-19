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
@DiscriminatorValue("digital_dpv")
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "digital_dpv", namespace = "urn:no:difi:profile:digitalpost:ver1.0")
@ApiModel(value = "digital_dpv", parent = BusinessMessage.class)
public class DigitalDpvMessage extends BusinessMessage {

    @NotNull
    private String title;
    @NotNull
    private String summary;
    @NotNull
    private String body;
}
