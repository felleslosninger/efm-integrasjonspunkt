package no.difi.meldingsutveksling.nextmove;

import com.google.common.collect.Maps;
import lombok.Data;
import no.difi.meldingsutveksling.ServiceIdentifier;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.time.LocalDateTime;

@Entity
@DiscriminatorValue("DPV")
@Data
public class DpvConversationResource extends ConversationResource {

    private String messageTitle;
    private String messageContent;

    DpvConversationResource() {}

    private DpvConversationResource(String conversationId, Sender sender, Receiver receiver) {
        super(conversationId, sender, receiver, ServiceIdentifier.DPV, LocalDateTime.now(), Maps.newHashMap(), Maps.newHashMap());
    }

    public static DpvConversationResource of(String conversationId, Sender sender, Receiver receiver) {
        return new DpvConversationResource(conversationId, sender, receiver);
    }

    public static DpvConversationResource of(DpiConversationResource dpi) {
        DpvConversationResource dpv = of(dpi.getConversationId(), dpi.getSender(), dpi.getReceiver());
        // TODO: replace
//        dpv.setMessageContent(dpi.getTitle());
//        dpv.setMessageTitle(dpi.getTitle());

        dpv.setSender(dpi.getSender());
        dpv.setReceiver(dpi.getReceiver());
        dpv.setHasArkivmelding(dpi.isHasArkivmelding());
        dpv.setFileRefs(dpi.getFileRefs());
        dpv.setCustomProperties(dpi.getCustomProperties());
        dpv.setArkivmelding(dpi.getArkivmelding());

        return dpv;
    }
}
