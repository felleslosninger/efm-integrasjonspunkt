package no.difi.meldingsutveksling.nextmove;

import lombok.Data;
import lombok.NoArgsConstructor;
import no.difi.meldingsutveksling.ServiceIdentifier;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("DPA")
@Data
@NoArgsConstructor
public class DpaConversationResource extends ConversationResource {

    private DpaConversationResource(String conversationId, String senderId, String receiverId) {
        super(conversationId, senderId, receiverId, ServiceIdentifier.DPA);
    }
    public static DpaConversationResource of(String conversationId, String senderId, String receiverId) {
        return new DpaConversationResource(conversationId, senderId, receiverId);
    }

}
