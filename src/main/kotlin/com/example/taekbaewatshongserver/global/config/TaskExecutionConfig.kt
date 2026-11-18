package com.example.taekbaewatshongserver.global.config

import org.slf4j.LoggerFactory
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
 *
 * 푸시 발송은 외부 HTTP 대기로 스레드를 오래 붙잡으므로 [PUSH_TASK_EXECUTOR]를 따로 둔다.
 * Expo가 느려져도 기본 실행기에서 도는 알림 생성(`ParcelEventListener`)이 밀리지 않게 하기 위해서다.
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

    @Bean(PUSH_TASK_EXECUTOR)
    fun pushTaskExecutor(): ThreadPoolTaskExecutor = ThreadPoolTaskExecutor().apply {
        corePoolSize = PUSH_POOL_SIZE
        maxPoolSize = PUSH_POOL_SIZE
        queueCapacity = PUSH_QUEUE_CAPACITY
        setThreadNamePrefix("push-")
        setRejectedExecutionHandler { _, _ ->
            log.warn("[푸시 발송 스킵] 푸시 대기열({}건)이 가득 차 발송 작업을 버렸습니다.", PUSH_QUEUE_CAPACITY)
        }
        setWaitForTasksToCompleteOnShutdown(true)
        setAwaitTerminationSeconds(PUSH_AWAIT_TERMINATION_SECONDS)
    }

    @Bean
    fun taskScheduler(builder: ThreadPoolTaskSchedulerBuilder): ThreadPoolTaskScheduler = builder.build()

    companion object {
        const val PUSH_TASK_EXECUTOR = "pushTaskExecutor"

        private const val PUSH_POOL_SIZE = 4
        private const val PUSH_QUEUE_CAPACITY = 1000
        private const val PUSH_AWAIT_TERMINATION_SECONDS = 20

        private val log = LoggerFactory.getLogger(TaskExecutionConfig::class.java)
    }
}
