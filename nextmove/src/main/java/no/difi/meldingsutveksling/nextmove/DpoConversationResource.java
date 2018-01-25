package no.difi.meldingsutveksling.nextmove;

import com.google.common.collect.Maps;
import lombok.Data;
import no.difi.meldingsutveksling.ServiceIdentifier;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.time.LocalDateTime;

@Entity
@DiscriminatorValue("DPO")
@Data
public class DpoConversationResource extends ConversationResource {

    private String jpId;

    DpoConversationResource() {}

    private DpoConversationResource(String conversationId, Sender sender, Receiver receiver) {
        super(conversationId, sender, receiver, ServiceIdentifier.DPO, LocalDateTime.now(), Maps.newHashMap(), Maps.newHashMap());
    }

    public static DpoConversationResource of(String conversationId, Sender sender, Receiver receiver) {
        return new DpoConversationResource(conversationId, sender, receiver);
    }
}
