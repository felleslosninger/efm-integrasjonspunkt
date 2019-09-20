package no.difi.meldingsutveksling.webhooks;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.webhooks.event.WebhookEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookEventListener {

    private final WebhookPusher webhookPusher;

    @Async("threadPoolTaskScheduler")
    @EventListener(WebhookEvent.class)
    public void onApplicationEvent(WebhookEvent event) {
        webhookPusher.push(event);
    }
}
