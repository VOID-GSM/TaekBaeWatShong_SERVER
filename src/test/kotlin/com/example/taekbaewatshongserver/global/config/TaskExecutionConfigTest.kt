package com.example.taekbaewatshongserver.global.config

import com.example.taekbaewatshongserver.domain.user.repository.UserRepository
import com.example.taekbaewatshongserver.global.security.jwt.JwtTokenProvider
import com.example.taekbaewatshongserver.global.security.websocket.StompAuthChannelInterceptor
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration
import org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration
import org.springframework.boot.autoconfigure.websocket.servlet.WebSocketMessagingAutoConfiguration
import org.springframework.boot.autoconfigure.websocket.servlet.WebSocketServletAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.test.assertTrue

@SpringBootTest(
    classes = [TaskExecutionConfigTest.TestApplication::class],
    properties = [
        "app.cors.allowed-origins=http://localhost:3000",
        "jwt.secret=test-secret-key-that-is-at-least-32-bytes!!",
        "jwt.access-token-validity-ms=60000",
    ],
)
class TaskExecutionConfigTest {

    @SpringBootConfiguration
    @EnableAsync
    @EnableScheduling
    @ImportAutoConfiguration(
        TaskExecutionAutoConfiguration::class,
        TaskSchedulingAutoConfiguration::class,
        WebSocketServletAutoConfiguration::class,
        WebSocketMessagingAutoConfiguration::class,
    )
    @Import(
        WebSocketConfig::class,
        CorsAllowedOrigins::class,
        TaskExecutionConfig::class,
        StompAuthChannelInterceptor::class,
        JwtTokenProvider::class,
    )
    class TestApplication {
        @Bean
        fun threadProbe() = ThreadProbe()
    }

    open class ThreadProbe {
        open val scheduledThread = CompletableFuture<String>()

        @Async
        open fun asyncThread(): CompletableFuture<String> = CompletableFuture.completedFuture(Thread.currentThread().name)

        @Scheduled(fixedDelay = 60_000)
        open fun scheduled() {
            scheduledThread.complete(Thread.currentThread().name)
        }
    }

    @MockBean
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var threadProbe: ThreadProbe

    @Test
    fun `웹소켓 브로커가 있어도 @Async는 Boot 기본 스레드 풀에서 실행된다`() {
        val threadName = threadProbe.asyncThread().get(5, TimeUnit.SECONDS)
        assertTrue(threadName.startsWith("task-"), "실제 스레드: $threadName")
    }

    @Test
    fun `웹소켓 브로커가 있어도 @Scheduled는 Boot 기본 스케줄러에서 실행된다`() {
        val threadName = threadProbe.scheduledThread.get(5, TimeUnit.SECONDS)
        assertTrue(threadName.startsWith("scheduling-"), "실제 스레드: $threadName")
    }
}
