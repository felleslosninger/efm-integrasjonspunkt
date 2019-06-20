package no.difi.meldingsutveksling.nextmove;

import io.swagger.annotations.ApiModel;
import lombok.*;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;

@Getter
@Setter
@ToString
@Entity
@DiscriminatorValue("digital")
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "digital", namespace = "urn:no:difi:profile:digitalpost:ver1.0")
@ApiModel(value="digital", parent = BusinessMessage.class)
public class DpiDigitalMessage extends BusinessMessage {

    @NotNull
    private String tittel;
    @NotNull
    private String spraak;

    @Embedded
    @NotNull
    @Valid
    private DigitalPostInfo digitalPostInfo;

    @Embedded
    @NotNull
    @Valid
    private DpiNotification varsler;
}
