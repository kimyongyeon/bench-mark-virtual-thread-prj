package com.kyy.benchmarkvirtualthreadprj.mdc;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncMdcConfig {
    @Bean(name = "mdcAsyncExecutor")
    public Executor mdcAsyncExecutor() {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("async-");

        executor.setTaskDecorator(new MdcTaskDecorator()); // ★ 적용

        executor.initialize();
        return executor;
    }
}
