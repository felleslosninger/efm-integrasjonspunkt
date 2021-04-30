package no.difi.meldingsutveksling.nextmove;

import com.google.common.collect.Maps;
import lombok.*;
import no.difi.sdp.client2.domain.fysisk_post.Posttype;
import no.difi.sdp.client2.domain.fysisk_post.Utskriftsfarge;

import javax.validation.Valid;
import java.util.Map;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@NextMoveBusinessMessage("print")
public class DpiPrintMessage extends BusinessMessage<DpiPrintMessage> {

    private String avsenderId;
    private String fakturaReferanse;
    @Valid
    private PostAddress mottaker;
    private Utskriftsfarge utskriftsfarge;
    private Posttype posttype;
    @Valid
    private MailReturn retur;

    private Map<String, String> printinstruksjoner = Maps.newHashMap();
}
