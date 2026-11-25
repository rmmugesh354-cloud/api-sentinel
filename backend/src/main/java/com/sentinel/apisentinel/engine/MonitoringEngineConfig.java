package com.sentinel.apisentinel.engine;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class MonitoringEngineConfig {

    @Value("${app.monitoring.pool.core-size:10}")
    private int corePoolSize;

    @Value("${app.monitoring.pool.max-size:25}")
    private int maxPoolSize;

    @Value("${app.monitoring.pool.queue-capacity:200}")
    private int queueCapacity;

    @Value("${app.monitoring.pool.keep-alive-seconds:60}")
    private int keepAliveSeconds;

    @Bean(name = "healthCheckExecutor")
    public ThreadPoolTaskExecutor healthCheckExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setThreadNamePrefix("sentinel-worker-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
