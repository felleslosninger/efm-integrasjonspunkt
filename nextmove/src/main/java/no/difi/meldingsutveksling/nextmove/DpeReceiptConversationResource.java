package no.difi.meldingsutveksling.nextmove;

import com.google.common.collect.Maps;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.difi.meldingsutveksling.ServiceIdentifier;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.time.LocalDateTime;

@NoArgsConstructor
@Entity
@DiscriminatorValue("DPE_RECEIPT")
@Data
public class DpeReceiptConversationResource extends ConversationResource {

    private DpeReceiptConversationResource(String conversationId, String senderId, String receiverId) {
        super(conversationId, senderId, receiverId, ServiceIdentifier.DPE_RECEIPT, LocalDateTime.now(), Maps.newHashMap(), Maps.newHashMap());
    }
    public static DpeReceiptConversationResource of(ConversationResource cr) {
        DpeReceiptConversationResource dpeCr = new DpeReceiptConversationResource(cr.getConversationId(), cr.getReceiverId(), cr.getSenderId());
        dpeCr.setFileRefs(Maps.newHashMap());
        dpeCr.setCustomProperties(Maps.newHashMap());
        return dpeCr;
    }
}
