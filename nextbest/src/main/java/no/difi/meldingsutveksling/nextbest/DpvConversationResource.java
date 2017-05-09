package no.difi.meldingsutveksling.nextbest;

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

    private String serviceCode;
    private String serviceEditionCode;

    DpvConversationResource() {}

    private DpvConversationResource(String conversationId, String senderId, String receiverId) {
        super(conversationId, senderId, receiverId, ServiceIdentifier.DPV, LocalDateTime.now(), Maps.newHashMap());
    }

    public static DpvConversationResource of(String conversationId, String senderId, String receiverId) {
        return new DpvConversationResource(conversationId, senderId, receiverId);
    }
}
