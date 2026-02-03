package no.difi.meldingsutveksling.altinnv3.dpo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class TaskExecutorConfig {

    @Bean(destroyMethod = "shutdown")
    public ThreadPoolTaskExecutor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(getCorePoolSize());
        taskExecutor.setThreadNamePrefix("TaskExecutor");
        /*
            If corePoolSize or more threads are running, the Executor always prefers queuing a request rather than adding a new thread.
         */
        taskExecutor.setQueueCapacity(0);
        taskExecutor.initialize();
        return taskExecutor;
    }

    @Bean
    @Primary
    public TaskExecutor taskExecutor(ThreadPoolTaskExecutor threadPoolTaskExecutor) {
        return threadPoolTaskExecutor;
    }

    private int getCorePoolSize() {
        return 1;
    }
}
