package com.ssafy.happynurse.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 도메인별 정밀 스케줄러용 ThreadPoolTaskScheduler 모음
 */
@Configuration
public class TaskSchedulerConfig {

    @Bean(name = "ivAlertTaskScheduler")
    public TaskScheduler ivAlertTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("iv-alert-");
        scheduler.setRemoveOnCancelPolicy(true); // cancel 시 큐에서 즉시 제거 (메모리 위생)
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(10);
        scheduler.initialize();
        return scheduler;
    }

    @Bean(name = "sttReminderTaskScheduler")
    public TaskScheduler sttReminderTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("stt-reminder-");
        scheduler.setRemoveOnCancelPolicy(true);
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(10);
        scheduler.initialize();
        return scheduler;
    }
}
