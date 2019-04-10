package no.difi.meldingsutveksling.nextmove;

import lombok.*;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.xml.bind.annotation.XmlRootElement;

@Getter
@Setter
@ToString
@Entity
@DiscriminatorValue("digital")
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "digital", namespace = "urn:no:difi:profile:digitalpost:ver1.0")
public class DpiDigitalMessage extends BusinessMessage {

    private String nonSensitiveTitle;
    private String language;

    @Embedded
    private DigitalPostInfo digitalPostInfo;
    private Boolean mandatoryNotification;

    @Embedded
    private DpiNotification notification;
}
