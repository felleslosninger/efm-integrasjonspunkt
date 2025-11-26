package no.difi.meldingsutveksling.nextmove;

import com.google.common.collect.Maps;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import no.difi.meldingsutveksling.validation.Avsenderidentifikator;
import no.difi.meldingsutveksling.validation.group.NextMoveValidationGroups;

import java.util.Map;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "digital", namespace = "urn:no:difi:profile:digitalpost:ver1.0")
public class DpiDigitalMessageAsAttachment extends BusinessMessageAsAttachment<DpiDigitalMessageAsAttachment> implements DpiMessage,HasSikkerhetsNivaa<DpiDigitalMessageAsAttachment> {
    @NotNull(groups = {
        NextMoveValidationGroups.MessageType.Digital.class
    })
    private Integer sikkerhetsnivaa;

    @Avsenderidentifikator
    private String avsenderId;
    private String fakturaReferanse;
    @NotNull
    private String tittel;
    @NotNull
    private String spraak;

    @NotNull
    @Valid
    private DigitalPostInfo digitalPostInfo;

    private DpiNotification varsler;

    private Map<String, String> metadataFiler = Maps.newHashMap();
}
