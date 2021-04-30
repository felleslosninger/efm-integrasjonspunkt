package no.difi.meldingsutveksling.nextmove;

import com.google.common.collect.Maps;
import lombok.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Map;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@NextMoveBusinessMessage("digital")
public class DpiDigitalMessage extends BusinessMessage<DpiDigitalMessage> {

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
