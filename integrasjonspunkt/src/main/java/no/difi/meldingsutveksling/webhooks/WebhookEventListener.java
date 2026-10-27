package no.difi.meldingsutveksling.webhooks;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.webhooks.event.WebhookEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookEventListener {

    private final WebhookPusher webhookPusher;

    // Pushed after commit, so subscribers are never notified about a status that is rolled back, nor before the
    // status is visible through the API. fallbackExecution covers events published without an active transaction.
    @Async("threadPoolTaskScheduler")
    @TransactionalEventListener(value = WebhookEvent.class, phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onApplicationEvent(WebhookEvent event) {
        webhookPusher.push(event);
    }
}
