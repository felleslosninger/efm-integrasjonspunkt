package no.difi.meldingsutveksling.nextmove;

import io.swagger.annotations.ApiModel;
import lombok.*;

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
