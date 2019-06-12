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
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.time.Clock;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@ConditionalOnProperty(
        value = "app.scheduling.enable", havingValue = "true", matchIfMissing = true
)
@Configuration
@EnableScheduling
public class SchedulingConfiguration implements SchedulingConfigurer {

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

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setScheduler(taskExecutor());
    }

    @Bean(destroyMethod="shutdown")
    public ScheduledExecutorService taskExecutor() {
        return Executors.newScheduledThreadPool(100);
    }
}
