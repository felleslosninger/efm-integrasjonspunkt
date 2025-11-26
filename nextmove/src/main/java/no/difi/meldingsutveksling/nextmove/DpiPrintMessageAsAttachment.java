package no.difi.meldingsutveksling.nextmove;

import com.google.common.collect.Maps;
import lombok.*;
import no.difi.meldingsutveksling.validation.Avsenderidentifikator;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.Map;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "print", namespace = "urn:no:difi:profile:digitalpost:ver1.0")
public class DpiPrintMessageAsAttachment extends BusinessMessageAsAttachment<DpiPrintMessageAsAttachment> implements DpiMessage {

    @Avsenderidentifikator
    private String avsenderId;
    private String fakturaReferanse;
    @Valid
    @NotNull
    private PostAddress mottaker;
    private PrintColor utskriftsfarge;
    private PostalCategory posttype;
    @Valid
    @NotNull
    private MailReturn retur;

    private Map<String, String> printinstruksjoner = Maps.newHashMap();
}
