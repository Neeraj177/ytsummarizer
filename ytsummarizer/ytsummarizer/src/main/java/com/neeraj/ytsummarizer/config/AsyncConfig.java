package com.neeraj.ytsummarizer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync // Activates Spring's asynchronous multi-threading capabilities
public class AsyncConfig {

    @Bean(name = "videoJobExecutor")
    public Executor videoJobExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Starts with 3 threads running immediately
        executor.setCorePoolSize(3);

        // Can scale up to 10 threads maximum if under heavy load
        executor.setMaxPoolSize(10);

        // Allows 50 video jobs to sit in a queue waiting if all threads are busy
        executor.setQueueCapacity(50);

        // Labels our background threads in logs (e.g., [YT-Async-1]) for easy tracking
        executor.setThreadNamePrefix("YT-Async-");

        executor.initialize();
        return executor;
    }
}