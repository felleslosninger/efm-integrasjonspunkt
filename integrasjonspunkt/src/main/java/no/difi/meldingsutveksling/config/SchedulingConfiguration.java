package no.difi.meldingsutveksling.config;

import no.difi.meldingsutveksling.nextmove.NextMoveInMessageUnlocker;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageInRepository;
import no.difi.meldingsutveksling.noarkexchange.altinn.DpePolling;
import no.difi.meldingsutveksling.noarkexchange.altinn.DpfPolling;
import no.difi.meldingsutveksling.noarkexchange.altinn.DpoPolling;
import no.difi.meldingsutveksling.noarkexchange.altinn.MessagePolling;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@ConditionalOnProperty(
        value = "app.scheduling.enable", havingValue = "true", matchIfMissing = true
)
@Configuration
@EnableScheduling
public class SchedulingConfiguration {

    @Bean
    public NextMoveInMessageUnlocker nextMoveInMessageUnlocker(NextMoveMessageInRepository repo, Clock clock) {
        return new NextMoveInMessageUnlocker(repo, clock);
    }

    @Bean
    public MessagePolling messagePolling(
            ObjectProvider<DpePolling> dpePolling,
            ObjectProvider<DpfPolling> dpfPolling,
            ObjectProvider<DpoPolling> dpoPolling) {
        return new MessagePolling(dpePolling, dpfPolling, dpoPolling);
    }
}
