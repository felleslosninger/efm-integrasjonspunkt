package no.difi.meldingsutveksling.webhooks.event;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.receipt.Conversation;
import no.difi.meldingsutveksling.receipt.MessageStatus;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class WebhookContentFactory {

    private final Clock clock;

    public PingContent pingContent() {
        return new PingContent()
                .setCreatedTs(OffsetDateTime.now(clock))
                .setEvent("ping");
    }

    public MessageStatusContent getMessageStatusContent(Conversation conversation, MessageStatus messageStatus) {
        return new MessageStatusContent()
                .setCreatedTs(OffsetDateTime.now(clock))
                .setResource("messages")
                .setEvent("status")
                .setConversationId(conversation.getConversationId())
                .setDirection(conversation.getDirection())
                .setServiceIdentifier(conversation.getServiceIdentifier())
                .setStatus(messageStatus.getStatus())
                .setDescription(messageStatus.getDescription());
    }
}
