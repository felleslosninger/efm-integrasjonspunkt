package no.difi.meldingsutveksling.cucumber;

import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.Date;
import java.util.concurrent.ScheduledFuture;

public class NoopTaskScheduler extends ThreadPoolTaskScheduler {

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long period) {
        return null;
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Date startTime, long period) {
        return null;
    }
}
