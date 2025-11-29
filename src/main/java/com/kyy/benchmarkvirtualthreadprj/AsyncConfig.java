package com.kyy.benchmarkvirtualthreadprj;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("platformExecutor")
    public Executor platformExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(50);      // 일부러 작게
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);    // 큐도 한정
        executor.setThreadNamePrefix("platform-");
        executor.initialize();
        return executor;
    }

    @Bean("virtualExecutor")
    public Executor virtualExecutor() {
        // JDK 21 Virtual Thread 기반
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}