package no.difi.meldingsutveksling.nextmove;

import lombok.*;
import no.difi.meldingsutveksling.config.dpi.ShippingType;
import no.difi.sdp.client2.domain.fysisk_post.Utskriftsfarge;

import javax.persistence.*;
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

    @Embedded
    private PostAddress receiver;
    private Utskriftsfarge color;
    private ShippingType shippingType;

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private MailReturn mailReturn;
}
