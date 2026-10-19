package no.difi.meldingsutveksling.cucumber;

import org.jspecify.annotations.NonNull;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.Date;
import java.util.concurrent.ScheduledFuture;

public class NoopTaskScheduler extends ThreadPoolTaskScheduler {

    @Override
    public @NonNull ScheduledFuture<?> scheduleAtFixedRate(@NonNull Runnable task, long period) {
        return null;
    }

    @Override
    public @NonNull ScheduledFuture<?> scheduleAtFixedRate(@NonNull Runnable task, @NonNull Date startTime, long period) {
        return null;
    }
}
