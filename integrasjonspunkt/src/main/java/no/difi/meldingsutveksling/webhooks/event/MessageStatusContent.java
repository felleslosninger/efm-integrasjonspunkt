package no.difi.meldingsutveksling.webhooks.event;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.nextmove.ConversationDirection;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class MessageStatusContent extends WebhookContentBase<MessageStatusContent> {

    private String messageId;
    private String conversationId;
    private ConversationDirection direction;
    private ServiceIdentifier serviceIdentifier;
    private String status;
    private String description;
}
