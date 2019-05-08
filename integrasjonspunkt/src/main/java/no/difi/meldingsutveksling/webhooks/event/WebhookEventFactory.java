package no.difi.meldingsutveksling.webhooks.event;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.receipt.MessageStatus;
import org.springframework.stereotype.Component;

import java.time.Clock;

@Component
@RequiredArgsConstructor
public class WebhookEventFactory {

    private final Clock clock;

    public PingEvent pingEvent() {
        return new PingEvent(clock, this);
    }

    public MessageStatusEvent getMessageStatusEvent(MessageStatus messageStatus) {
        return new MessageStatusEvent(clock, this)
                .setStatId(messageStatus.getStatId())
                .setConvId(messageStatus.getConvId())
                .setConversationId(messageStatus.getConversationId())
                .setLastUpdate(messageStatus.getLastUpdate())
                .setStatus(messageStatus.getStatus())
                .setDescription(messageStatus.getDescription());
    }
}
