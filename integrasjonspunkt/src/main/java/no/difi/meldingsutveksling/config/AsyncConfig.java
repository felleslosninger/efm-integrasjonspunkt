package no.difi.meldingsutveksling.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;

@Slf4j
@EnableAsync
@Configuration
@Import(TaskExecutorConfig.class)
@RequiredArgsConstructor
public class AsyncConfig implements AsyncConfigurer {

    private final TaskExecutor threadPoolTaskExecutor;

    @Override
    public Executor getAsyncExecutor() {
        return threadPoolTaskExecutor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> {
            Class<?> targetClass = method.getDeclaringClass();
            Logger logger = LoggerFactory.getLogger(targetClass);
            logger.error(ex.getMessage(), ex);
        };
    }

    @Bean
    public Executor dpiReceiptExecutor() {
        return new SimpleAsyncTaskExecutor();
    }
}
