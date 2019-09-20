package no.difi.meldingsutveksling.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class TaskSchedulerConfiguration {

    @Bean(destroyMethod = "shutdown")
    public ThreadPoolTaskScheduler threadPoolTaskScheduler() {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setThreadNamePrefix("TaskScheduler");
        taskScheduler.setPoolSize(getPoolSize());
        taskScheduler.setWaitForTasksToCompleteOnShutdown(true);
        return taskScheduler;
    }

    private int getPoolSize() {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        return Math.max(6, availableProcessors - 1);
    }
}
