package com.gauzynote.framework.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 自定义线程
 *
 */
@Configuration
@EnableAsync
public class TaskExecutePool {

    // 核心线程数
    @Value("${thread-pool.core-size}")
    private int coreSize;
    // 最大线程数
    @Value("${thread-pool.max-size}")
    private int maxSize;
    // 任务队列容量
    @Value("${thread-pool.queue-capacity}")
    private int queueCapacity;
    // 线程空闲时间（存活时间）
    @Value("${thread-pool.keep-alive-seconds}")
    private int keepAliveSeconds;
    // 线程名称前缀
    @Value("${thread-pool.thread-name-prefix}")
    private String threadNamePrefix;

    @Bean("OperLogServiceInsertAsync")
    public ThreadPoolTaskExecutor OperLogServiceInsertAsync() {
        return createThreadPoolTaskExecutor();
    }


    private ThreadPoolTaskExecutor createThreadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(coreSize);
        executor.setMaxPoolSize(maxSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
