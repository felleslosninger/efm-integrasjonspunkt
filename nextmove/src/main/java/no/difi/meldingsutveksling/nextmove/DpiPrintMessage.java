package no.difi.meldingsutveksling.nextmove;

import lombok.*;
import no.difi.meldingsutveksling.config.dpi.ShippingType;
import no.difi.sdp.client2.domain.fysisk_post.Utskriftsfarge;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.xml.bind.annotation.XmlRootElement;

@Getter
@Setter
@ToString
@Entity
@DiscriminatorValue("dpi_print")
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "dpi_print", namespace = "urn:no:difi:profile:digitalpost:ver1.0")
public class DpiPrintMessage extends BusinessMessage {

    private PrintReceiver receiver;
    private Utskriftsfarge color;
    private ShippingType shippingType;
    private MailReturn mailReturn;
}
