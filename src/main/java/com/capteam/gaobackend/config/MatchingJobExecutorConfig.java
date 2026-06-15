package com.capteam.gaobackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class MatchingJobExecutorConfig {

    // 장시간 실행되는 AI 매칭이 일반 HTTP 요청 스레드를 점유하지 않도록 전용 풀을 사용합니다.
    @Bean("matchingJobExecutor")
    public AsyncTaskExecutor matchingJobExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("matching-job-");
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.initialize();
        return executor;
    }
}
