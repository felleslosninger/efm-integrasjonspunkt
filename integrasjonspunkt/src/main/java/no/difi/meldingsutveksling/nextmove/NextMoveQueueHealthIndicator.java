package no.difi.meldingsutveksling.nextmove;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageInRepository;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageOutRepository;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NextMoveQueueHealthIndicator extends AbstractHealthIndicator {

    private final NextMoveMessageInRepository inRepository;
    private final NextMoveMessageOutRepository outRepository;

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        builder.up()
                .withDetail("outgoing", outRepository.count())
                .withDetail("incoming", inRepository.count());
    }
}
