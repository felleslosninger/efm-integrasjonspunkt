package no.difi.meldingsutveksling.nextmove;

import lombok.Data;
import lombok.NoArgsConstructor;
import no.difi.meldingsutveksling.ServiceIdentifier;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@DiscriminatorValue("DPO")
@NoArgsConstructor
@Data
public class DpoConversationResource extends ConversationResource {

    private DpoConversationResource(String conversationId, String senderId, String receiverId) {
        super(conversationId, senderId, receiverId, ServiceIdentifier.DPO);
    }

    private DpoConversationResource(String conversationId,
                                    String senderId,
                                    String senderName,
                                    String receiverId,
                                    String receiverName,
                                    LocalDateTime lastUpdate,
                                    Map fileRefs,
                                    Map customProperties) {
        super(conversationId, senderId, senderName, receiverId, receiverName, ServiceIdentifier.DPO, lastUpdate, fileRefs, customProperties);
    }

    public static DpoConversationResource of(String conversationId, String senderId, String receiverId) {
        return new DpoConversationResource(conversationId, senderId, receiverId);
    }

    public static DpoConversationResource of(ConversationResource cr) {
        DpoConversationResource dpo = new DpoConversationResource(cr.getConversationId(),
                cr.getSenderId(),
                cr.getSenderName(),
                cr.getReceiverId(),
                cr.getReceiverName(),
                cr.getLastUpdate(),
                cr.getFileRefs(),
                cr.getCustomProperties());

        return dpo;
    }
}
