package no.difi.meldingsutveksling.webhooks;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.receipt.MessageStatus;
import no.difi.meldingsutveksling.webhooks.event.WebhookEventFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WebhookPublisherImpl implements WebhookPublisher {

    private final WebhookEventFactory webhookEventFactory;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publish(MessageStatus messageStatus) {
        applicationEventPublisher.publishEvent(webhookEventFactory.getMessageStatusEvent(messageStatus));
    }
}
