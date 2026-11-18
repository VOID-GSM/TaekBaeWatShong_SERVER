package com.example.taekbaewatshongserver.global.config

import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration
import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder
import org.springframework.boot.task.ThreadPoolTaskSchedulerBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.AsyncAnnotationBeanPostProcessor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler

/**
 * `@Async`·`@Scheduled` 실행기를 명시적으로 등록한다.
 *
 * `@EnableWebSocketMessageBroker`가 채널용 Executor·TaskScheduler 빈을 등록하면 Boot의 기본 실행기
 * (`applicationTaskExecutor`, `taskScheduler`)가 만들어지지 않는다. 그러면 `@Async`는 풀 없이 매번 새 스레드를 만들고
 * `@Scheduled`는 브로커 스케줄러에서 돈다. Boot의 빌더로 만들어 `spring.task.*` 설정도 그대로 적용된다.
 */
@Configuration
class TaskExecutionConfig {

    @Bean(
        name = [
            TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME,
            AsyncAnnotationBeanPostProcessor.DEFAULT_TASK_EXECUTOR_BEAN_NAME,
        ],
    )
    fun applicationTaskExecutor(builder: ThreadPoolTaskExecutorBuilder): ThreadPoolTaskExecutor = builder.build()

    @Bean
    fun taskScheduler(builder: ThreadPoolTaskSchedulerBuilder): ThreadPoolTaskScheduler = builder.build()
}
