package no.difi.meldingsutveksling.config;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.api.DpePolling;
import no.difi.meldingsutveksling.api.DpfPolling;
import no.difi.meldingsutveksling.api.DpoPolling;
import no.difi.meldingsutveksling.nextmove.NextMoveInMessageUnlocker;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageInRepository;
import no.difi.meldingsutveksling.nextmove.MessagePollingScheduler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.time.Clock;

@ConditionalOnProperty(
        value = "app.scheduling.enable", havingValue = "true", matchIfMissing = true
)
@EnableScheduling
@Configuration
@Import(TaskSchedulerConfiguration.class)
@RequiredArgsConstructor
public class SchedulingConfiguration implements SchedulingConfigurer {

    private final TaskScheduler taskScheduler;

    @Bean
    public NextMoveInMessageUnlocker nextMoveInMessageUnlocker(NextMoveMessageInRepository repo, Clock clock) {
        return new NextMoveInMessageUnlocker(repo, clock);
    }

    @Bean
    public MessagePollingScheduler messagePolling(
            ObjectProvider<DpePolling> dpePolling,
            ObjectProvider<DpfPolling> dpfPolling,
            ObjectProvider<DpoPolling> dpoPolling) {
        return new MessagePollingScheduler(dpePolling, dpfPolling, dpoPolling);
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setScheduler(taskScheduler);
    }
}
