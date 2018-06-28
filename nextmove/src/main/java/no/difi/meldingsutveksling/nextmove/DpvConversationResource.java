package no.difi.meldingsutveksling.nextmove;

import lombok.Data;
import lombok.NoArgsConstructor;
import no.difi.meldingsutveksling.ServiceIdentifier;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@DiscriminatorValue("DPV")
@NoArgsConstructor
@Data
public class DpvConversationResource extends ConversationResource {

    private String messageTitle;
    private String messageContent;

    private DpvConversationResource(String conversationId, String senderId, String receiverId) {
        super(conversationId, senderId, receiverId, ServiceIdentifier.DPV);
    }

    private DpvConversationResource(String conversationId,
                                    String senderId,
                                    String senderName,
                                    String receiverId,
                                    String receiverName,
                                    LocalDateTime lastUpdate,
                                    Map fileRefs,
                                    Map customProperties) {
        super(conversationId, senderId, senderName, receiverId, receiverName, ServiceIdentifier.DPV, lastUpdate, fileRefs, customProperties);
    }

    public static DpvConversationResource of(String conversationId, String senderId, String receiverId) {
        return new DpvConversationResource(conversationId, senderId, receiverId);
    }

    public static DpvConversationResource of(ConversationResource cr) {
        DpvConversationResource dpv = new DpvConversationResource(cr.getConversationId(),
                cr.getSenderId(),
                cr.getSenderName(),
                cr.getReceiverId(),
                cr.getReceiverName(),
                cr.getLastUpdate(),
                cr.getFileRefs(),
                cr.getCustomProperties());

        // Special case, direct conversion from DPI -> DPV
        if (cr instanceof DpiConversationResource) {
            DpiConversationResource dpi = (DpiConversationResource) cr;
            dpv.setMessageContent(dpi.getTitle());
            dpv.setMessageTitle(dpi.getTitle());
        }

        return dpv;
    }

}
