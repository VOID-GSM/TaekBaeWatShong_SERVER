package com.example.taekbaewatshongserver.global.config

import com.example.taekbaewatshongserver.domain.chat.dto.response.ChatSocketEvent
import com.example.taekbaewatshongserver.domain.chat.dto.response.ChatSocketEventType
import com.example.taekbaewatshongserver.domain.user.repository.UserRepository
import com.example.taekbaewatshongserver.global.security.jwt.JwtTokenProvider
import com.example.taekbaewatshongserver.global.security.websocket.StompAuthChannelInterceptor
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration
import org.springframework.boot.autoconfigure.websocket.servlet.WebSocketMessagingAutoConfiguration
import org.springframework.boot.autoconfigure.websocket.servlet.WebSocketServletAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.messaging.converter.MappingJackson2MessageConverter
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.messaging.simp.stomp.StompFrameHandler
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.messaging.simp.stomp.StompSession
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter
import org.springframework.web.socket.WebSocketHttpHeaders
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.messaging.WebSocketStompClient
import java.lang.reflect.Type
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@SpringBootTest(
    classes = [WebSocketConfigTest.TestApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "app.cors.allowed-origins=http://localhost:3000",
        "jwt.secret=test-secret-key-that-is-at-least-32-bytes!!",
        "jwt.access-token-validity-ms=60000",
    ],
)
class WebSocketConfigTest {

    @SpringBootConfiguration
    @ImportAutoConfiguration(
        ServletWebServerFactoryAutoConfiguration::class,
        DispatcherServletAutoConfiguration::class,
        WebSocketServletAutoConfiguration::class,
        WebSocketMessagingAutoConfiguration::class,
        JacksonAutoConfiguration::class,
    )
    @Import(WebSocketConfig::class, StompAuthChannelInterceptor::class, JwtTokenProvider::class)
    class TestApplication

    @LocalServerPort
    private var port: Int = 0

    @MockBean
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @Autowired
    private lateinit var messagingTemplate: SimpMessagingTemplate

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private fun connect(token: String?): StompSession {
        val client = WebSocketStompClient(StandardWebSocketClient()).apply {
            messageConverter = MappingJackson2MessageConverter().apply { objectMapper = this@WebSocketConfigTest.objectMapper }
        }
        val handshakeHeaders = WebSocketHttpHeaders().apply { origin = "http://localhost:3000" }
        val connectHeaders = StompHeaders().apply { token?.let { add("Authorization", "Bearer $it") } }

        return client.connectAsync(
            "ws://localhost:$port${WebSocketConfig.ENDPOINT}",
            handshakeHeaders,
            connectHeaders,
            object : StompSessionHandlerAdapter() {},
        ).get(5, TimeUnit.SECONDS)
    }

    @Test
    fun `인증한 사용자는 본인 큐로 보낸 이벤트만 받는다`() {
        `when`(userRepository.existsById(7L)).thenReturn(true)
        val session = connect(jwtTokenProvider.createToken(7L, "user@gsm.hs.kr"))

        val received = CompletableFuture<ChatSocketEvent>()
        session.subscribe(
            StompAuthChannelInterceptor.CHAT_QUEUE,
            object : StompFrameHandler {
                override fun getPayloadType(headers: StompHeaders): Type = ChatSocketEvent::class.java
                override fun handleFrame(headers: StompHeaders, payload: Any?) {
                    received.complete(payload as ChatSocketEvent)
                }
            },
        )
        // SUBSCRIBE가 브로커에 등록될 시간을 준다.
        Thread.sleep(300)

        val event = ChatSocketEvent(
            type = ChatSocketEventType.READ,
            roomId = 1L,
            readerId = 8L,
            unreadCount = 0,
            totalUnreadCount = 2,
        )
        messagingTemplate.convertAndSendToUser("8", "/queue/chat", event.copy(roomId = 99L))
        messagingTemplate.convertAndSendToUser("7", "/queue/chat", event)

        assertEquals(event, received.get(5, TimeUnit.SECONDS))
        session.disconnect()
    }

    @Test
    fun `토큰 없이 연결하면 세션이 수립되지 않는다`() {
        assertFailsWith<ExecutionException> { connect(token = null) }
    }
}
