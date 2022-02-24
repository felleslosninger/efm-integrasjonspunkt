package no.difi.meldingsutveksling.nextmove;

import com.google.common.collect.Maps;
import lombok.*;
import no.difi.sdp.client2.domain.fysisk_post.Posttype;
import no.difi.sdp.client2.domain.fysisk_post.Utskriftsfarge;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Map;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "print", namespace = "urn:no:difi:profile:digitalpost:ver1.0")
public class DpiPrintMessage extends BusinessMessage<DpiPrintMessage> {

    private String avsenderId;
    private String fakturaReferanse;
    @Valid
    @NotNull
    private PostAddress mottaker;
    private Utskriftsfarge utskriftsfarge;
    private Posttype posttype;
    @Valid
    @NotNull
    private MailReturn retur;

    private Map<String, String> printinstruksjoner = Maps.newHashMap();
}
