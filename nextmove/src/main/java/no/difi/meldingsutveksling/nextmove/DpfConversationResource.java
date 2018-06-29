package no.difi.meldingsutveksling.nextmove;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.time.LocalDateTime;
import java.util.Map;

import static no.difi.meldingsutveksling.ServiceIdentifier.DPF;

@Entity
@DiscriminatorValue("DPF")
@NoArgsConstructor
@Data
public class DpfConversationResource extends ConversationResource {

    private DpfConversationResource(String conversationId, String senderId, String receiverId) {
        super(conversationId, senderId, receiverId, DPF);
    }

    private DpfConversationResource(String conversationId,
                                    String senderId,
                                    String senderName,
                                    String receiverId,
                                    String receiverName,
                                    LocalDateTime lastUpdate,
                                    Map fileRefs,
                                    Map customProperties) {
        super(conversationId, senderId, senderName, receiverId, receiverName, DPF, lastUpdate, fileRefs, customProperties);
    }

    public static DpfConversationResource of(String conversationId, String senderId, String receiverId) {
        return new DpfConversationResource(conversationId, senderId, receiverId);
    }

    public static DpfConversationResource of(ConversationResource cr) {
        return new DpfConversationResource(cr.getConversationId(),
                cr.getSenderId(),
                cr.getSenderName(),
                cr.getReceiverId(),
                cr.getReceiverName(),
                cr.getLastUpdate(),
                cr.getFileRefs(),
                cr.getCustomProperties());
    }
}
