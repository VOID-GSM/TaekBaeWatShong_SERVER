package com.example.taekbaewatshongserver.global.security.websocket

import com.example.taekbaewatshongserver.domain.user.repository.UserRepository
import com.example.taekbaewatshongserver.global.security.jwt.JwtTokenProvider
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.MessageDeliveryException
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.MessageBuilder
import org.springframework.messaging.support.MessageHeaderAccessor
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class StompAuthChannelInterceptorTest {

    private val jwtTokenProvider = JwtTokenProvider("test-secret-key-that-is-at-least-32-bytes!!", 60_000L)
    private val userRepository: UserRepository = mock(UserRepository::class.java)
    private val stompSessionExpiry: StompSessionExpiry = mock(StompSessionExpiry::class.java)
    private val interceptor = StompAuthChannelInterceptor(jwtTokenProvider, userRepository, stompSessionExpiry)
    private val channel: MessageChannel = mock(MessageChannel::class.java)

    private fun frame(command: StompCommand, configure: StompHeaderAccessor.() -> Unit = {}): Message<ByteArray> {
        val accessor = StompHeaderAccessor.create(command).apply {
            sessionId = SESSION_ID
            configure()
            setLeaveMutable(true)
        }
        return MessageBuilder.createMessage(ByteArray(0), accessor.messageHeaders)
    }

    @Test
    fun `유효한 토큰으로 CONNECT하면 사용자 ID가 세션 사용자로 설정된다`() {
        `when`(userRepository.existsById(7L)).thenReturn(true)
        val token = jwtTokenProvider.createToken(7L, "user@gsm.hs.kr")

        val result = interceptor.preSend(frame(StompCommand.CONNECT) { addNativeHeader("Authorization", "Bearer $token") }, channel)

        val accessor = MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor::class.java)!!
        assertEquals("7", accessor.user?.name)
    }

    @Test
    fun `CONNECT하면 토큰 만료 시각에 세션이 닫히도록 예약한다`() {
        `when`(userRepository.existsById(7L)).thenReturn(true)
        val token = jwtTokenProvider.createToken(7L, "user@gsm.hs.kr")
        val expiresAt = jwtTokenProvider.getClaims(token)!!.expiration.toInstant()

        interceptor.preSend(frame(StompCommand.CONNECT) { addNativeHeader("Authorization", "Bearer $token") }, channel)

        verify(stompSessionExpiry).closeAt(SESSION_ID, expiresAt)
    }

    @Test
    fun `토큰 없이 CONNECT하면 거부된다`() {
        assertFailsWith<MessageDeliveryException> {
            interceptor.preSend(frame(StompCommand.CONNECT), channel)
        }
    }

    @Test
    fun `토큰 없이 STOMP 프레임으로 연결하면 거부된다`() {
        assertFailsWith<MessageDeliveryException> {
            interceptor.preSend(frame(StompCommand.STOMP), channel)
        }
    }

    @Test
    fun `유효한 토큰으로 STOMP 프레임 연결하면 사용자 ID가 세션 사용자로 설정된다`() {
        `when`(userRepository.existsById(7L)).thenReturn(true)
        val token = jwtTokenProvider.createToken(7L, "user@gsm.hs.kr")

        val result = interceptor.preSend(frame(StompCommand.STOMP) { addNativeHeader("Authorization", "Bearer $token") }, channel)

        val accessor = MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor::class.java)!!
        assertEquals("7", accessor.user?.name)
    }

    @Test
    fun `잘못된 토큰으로 CONNECT하면 거부된다`() {
        assertFailsWith<MessageDeliveryException> {
            interceptor.preSend(frame(StompCommand.CONNECT) { addNativeHeader("Authorization", "Bearer invalid") }, channel)
        }
    }

    @Test
    fun `탈퇴 등으로 사라진 사용자의 토큰은 거부된다`() {
        `when`(userRepository.existsById(7L)).thenReturn(false)
        val token = jwtTokenProvider.createToken(7L, "user@gsm.hs.kr")

        assertFailsWith<MessageDeliveryException> {
            interceptor.preSend(frame(StompCommand.CONNECT) { addNativeHeader("Authorization", "Bearer $token") }, channel)
        }
    }

    @Test
    fun `본인 채팅 큐는 구독할 수 있다`() {
        interceptor.preSend(
            frame(StompCommand.SUBSCRIBE) {
                user = StompUserPrincipal(7L)
                destination = StompAuthChannelInterceptor.CHAT_QUEUE
            },
            channel,
        )
    }

    @Test
    fun `다른 경로는 구독할 수 없다`() {
        assertFailsWith<MessageDeliveryException> {
            interceptor.preSend(
                frame(StompCommand.SUBSCRIBE) {
                    user = StompUserPrincipal(7L)
                    destination = "/user/8/queue/chat"
                },
                channel,
            )
        }
    }

    @Test
    fun `인증되지 않은 세션은 구독할 수 없다`() {
        assertFailsWith<MessageDeliveryException> {
            interceptor.preSend(frame(StompCommand.SUBSCRIBE) { destination = StompAuthChannelInterceptor.CHAT_QUEUE }, channel)
        }
    }

    @Test
    fun `SEND 프레임은 거부된다`() {
        assertFailsWith<MessageDeliveryException> {
            interceptor.preSend(
                frame(StompCommand.SEND) {
                    user = StompUserPrincipal(7L)
                    destination = "/user/8/queue/chat"
                },
                channel,
            )
        }
    }

    companion object {
        private const val SESSION_ID = "session-1"
    }
}
