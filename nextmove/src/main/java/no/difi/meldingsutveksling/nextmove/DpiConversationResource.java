package no.difi.meldingsutveksling.nextmove;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.difi.meldingsutveksling.ServiceIdentifier;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@DiscriminatorValue("DPI")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DpiConversationResource extends ConversationResource {

    private String title;

    private DpiConversationResource(String conversationId,
                                    String senderId,
                                    String senderName,
                                    String receiverId,
                                    String receiverName,
                                    LocalDateTime lastUpdate,
                                    Map fileRefs,
                                    Map customProperties) {
        super(conversationId, senderId, senderName, receiverId, receiverName, ServiceIdentifier.DPI, lastUpdate, fileRefs, customProperties);
    }

    public static DpiConversationResource of(ConversationResource cr) {
        return new DpiConversationResource(cr.getConversationId(),
                cr.getSenderId(),
                cr.getSenderName(),
                cr.getReceiverId(),
                cr.getReceiverName(),
                cr.getLastUpdate(),
                cr.getFileRefs(),
                cr.getCustomProperties());
    }
}
