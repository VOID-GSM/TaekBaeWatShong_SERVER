package com.example.taekbaewatshongserver.global.security.websocket

import com.example.taekbaewatshongserver.domain.user.repository.UserRepository
import com.example.taekbaewatshongserver.global.security.jwt.JwtTokenProvider
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.MessageDeliveryException
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.stereotype.Component

/**
 * STOMP 인바운드 프레임 인증·인가.
 *
 * - CONNECT/STOMP: `Authorization: Bearer <JWT>` 헤더를 검증해 세션 사용자를 정하고, 토큰 만료 시각에 세션이 닫히도록 예약한다.
 * - SUBSCRIBE: 본인 전용 큐([CHAT_QUEUE])만 허용한다.
 * - SEND: 허용하지 않는다. `/user/{id}/...`로 보내 다른 사용자에게 가짜 이벤트를 넣는 것을 막기 위함이다.
 */
@Component
class StompAuthChannelInterceptor(
    private val jwtTokenProvider: JwtTokenProvider,
    private val userRepository: UserRepository,
    private val stompSessionExpiry: StompSessionExpiry,
) : ChannelInterceptor {

    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*> {
        val accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java)
            ?: return message

        when (accessor.command) {
            StompCommand.CONNECT, StompCommand.STOMP -> accessor.user = authenticate(accessor)
            StompCommand.SUBSCRIBE -> {
                if (accessor.user == null || accessor.destination != CHAT_QUEUE) {
                    throw MessageDeliveryException("구독할 수 없는 경로입니다.")
                }
            }
            StompCommand.SEND -> throw MessageDeliveryException("메시지 전송은 REST API로만 할 수 있습니다.")
            else -> Unit
        }

        return message
    }

    private fun authenticate(accessor: StompHeaderAccessor): StompUserPrincipal {
        val bearer = accessor.getFirstNativeHeader("Authorization")
        val token = bearer?.takeIf { it.startsWith("Bearer ") }?.substring(7)
        val claims = token?.let { jwtTokenProvider.getClaims(it) }
        val userId = claims?.subject?.toLongOrNull()
        val expiresAt = claims?.expiration?.toInstant()
        if (userId == null || expiresAt == null) {
            throw MessageDeliveryException("유효한 인증 토큰이 필요합니다.")
        }

        if (!userRepository.existsById(userId)) {
            throw MessageDeliveryException("유효한 인증 토큰이 필요합니다.")
        }

        stompSessionExpiry.closeAt(requireNotNull(accessor.sessionId), expiresAt)
        return StompUserPrincipal(userId)
    }

    companion object {
        const val CHAT_QUEUE = "/user/queue/chat"
    }
}
