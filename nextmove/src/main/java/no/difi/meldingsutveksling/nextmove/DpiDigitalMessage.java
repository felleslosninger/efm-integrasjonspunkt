package no.difi.meldingsutveksling.nextmove;

import lombok.*;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.xml.bind.annotation.XmlRootElement;

@Getter
@Setter
@ToString
@Entity
@DiscriminatorValue("dpi_digital")
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "dpi_digital", namespace = "urn:no:difi:profile:digitalpost:ver1.0")
public class DpiDigitalMessage extends BusinessMessage {

    private String nonSensitiveTitle;
    private String language;
    private DigitalPostInfo digitalPostInfo;
    private Boolean mandatoryNotification;
    private DpiNotification notification;
}
