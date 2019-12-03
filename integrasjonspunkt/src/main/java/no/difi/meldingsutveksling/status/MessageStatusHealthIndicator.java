package no.difi.meldingsutveksling.status;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

@Component
public class MessageStatusHealthIndicator extends AbstractHealthIndicator {

    @Autowired
    private ConversationRepository repo;

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        builder.up();

        Long pollableCount = repo.countByPollable(true);
        builder.withDetail("pollable", pollableCount);
    }
}
