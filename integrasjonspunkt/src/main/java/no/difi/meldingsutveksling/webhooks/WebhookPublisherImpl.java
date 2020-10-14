package no.difi.meldingsutveksling.webhooks;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.status.Conversation;
import no.difi.meldingsutveksling.status.MessageStatus;
import no.difi.meldingsutveksling.webhooks.event.WebhookContentFactory;
import no.difi.meldingsutveksling.webhooks.event.WebhookEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WebhookPublisherImpl implements WebhookPublisher {

    private final WebhookContentFactory webhookContentFactory;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publish(Conversation conversation, MessageStatus messageStatus) {
        applicationEventPublisher.publishEvent(
                new WebhookEvent<>(webhookContentFactory.getMessageStatusContent(conversation, messageStatus), this)
        );
    }
}
