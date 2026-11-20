package com.ryo.ryopicturebackend.Config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程池配置
 */
@Configuration
public class ThreadPoolExecutorConfig {

    /**
     * 通用线程池
     * 核心线程数 2，最大线程数 4，空闲 100 秒回收，任务队列容量 4
     *
     * @return 线程池
     */
    @Bean
    public ThreadPoolExecutor threadPoolExecutor() {
        return new ThreadPoolExecutor(
                2, 4, 100, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(4),
                new ThreadFactory() {
                    private final AtomicInteger count = new AtomicInteger(1);

                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thread = new Thread(r);
                        thread.setName("线程" + count.getAndIncrement());
                        return thread;
                    }
                });
    }
}
