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

    private DpeReceiptConversationResource(String conversationId, Sender sender, Receiver receiver) {
        super(conversationId, sender, receiver, ServiceIdentifier.DPE_RECEIPT, LocalDateTime.now(), Maps.newHashMap(), Maps.newHashMap());
    }
    public static DpeReceiptConversationResource of(ConversationResource cr) {
        Receiver receiver = Receiver.of(cr.getSender().getSenderId(), cr.getSender().getSenderName());
        Sender sender = Sender.of(cr.getReceiver().getReceiverId(), cr.getReceiver().getReceiverName());
        DpeReceiptConversationResource dpeCr = new DpeReceiptConversationResource(cr.getConversationId(), sender, receiver);
        dpeCr.addFileRef(cr.getFileRefs().get(0));
        return dpeCr;
    }
}
