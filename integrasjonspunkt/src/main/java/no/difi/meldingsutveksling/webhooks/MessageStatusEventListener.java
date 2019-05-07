package no.difi.meldingsutveksling.webhooks;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.webhooks.event.MessageStatusEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageStatusEventListener {

    private final WebhookPusher webhookPusher;

    @Async
    @EventListener(MessageStatusEvent.class)
    public void onApplicationEvent(MessageStatusEvent event) {
        webhookPusher.push(event);
    }
}
