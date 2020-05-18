package no.difi.meldingsutveksling.nextmove;

import io.swagger.annotations.ApiModel;
import lombok.*;
import no.difi.sdp.client2.domain.fysisk_post.Posttype;
import no.difi.sdp.client2.domain.fysisk_post.Utskriftsfarge;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "print", namespace = "urn:no:difi:profile:digitalpost:ver1.0")
@ApiModel(value = "print", parent = BusinessMessage.class)
public class DpiPrintMessage extends BusinessMessage<DpiPrintMessage> {

    private String avsenderId;
    private String fakturaReferanse;
    @NotNull
    @Valid
    private PostAddress mottaker;

    @NotNull
    private Utskriftsfarge utskriftsfarge;

    @NotNull
    private Posttype posttype;

    @NotNull
    @Valid
    private MailReturn retur;
}
