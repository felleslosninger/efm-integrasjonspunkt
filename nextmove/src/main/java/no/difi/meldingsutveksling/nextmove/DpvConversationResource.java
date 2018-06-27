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

    private DpvConversationResource(String conversationId, String senderId, String receiverId) {
        super(conversationId, senderId, receiverId, ServiceIdentifier.DPV, LocalDateTime.now(), Maps.newHashMap(), Maps.newHashMap());
    }

    public static DpvConversationResource of(String conversationId, String senderId, String receiverId) {
        return new DpvConversationResource(conversationId, senderId, receiverId);
    }

    public static DpvConversationResource of(DpiConversationResource dpi) {
        DpvConversationResource dpv = of(dpi.getConversationId(), dpi.getSenderId(), dpi.getReceiverId());
        dpv.setMessageContent(dpi.getTitle());
        dpv.setMessageTitle(dpi.getTitle());

        dpv.setSenderName(dpi.getSenderName());
        dpv.setReceiverName(dpi.getReceiverName());
        dpv.setFileRefs(dpi.getFileRefs());
        dpv.setCustomProperties(dpi.getCustomProperties());

        return dpv;
    }
}
