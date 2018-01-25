package no.difi.meldingsutveksling.nextmove;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

@Component
public class NextMoveQueueHealthIndicator extends AbstractHealthIndicator {

    @Autowired
    private ConversationResourceRepository repo;

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        builder.up();

        Long outgoingCount = repo.countByDirection(ConversationDirection.OUTGOING);
        builder.withDetail("outgoing", outgoingCount);

        Long incomingCount = repo.countByDirection(ConversationDirection.INCOMING);
        builder.withDetail("incoming", incomingCount);
    }
}
