package com.example.taekbaewatshongserver.global.security.websocket

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Lazy
import org.springframework.scheduling.TaskScheduler
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.WebSocketHandlerDecorator
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory
import java.io.IOException
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * 연결할 때 쓴 JWT가 만료되는 시각에 웹소켓 세션을 닫는다.
 *
 * STOMP 세션 ID는 웹소켓 세션 ID와 같으므로, 열린 세션을 ID로 기억해 두었다가 [closeAt]에서 예약한 시각에 닫는다.
 * 클라이언트는 새 토큰으로 다시 연결해야 한다.
 */
@Component
class StompSessionExpiry(
    // 브로커 설정이 웹소켓 설정을 참조하므로, 순환 참조를 피하려고 @Lazy로 주입한다.
    @Lazy @Qualifier("messageBrokerTaskScheduler") private val taskScheduler: TaskScheduler,
) : WebSocketHandlerDecoratorFactory {
    private val log = LoggerFactory.getLogger(javaClass)
    private val sessions = ConcurrentHashMap<String, WebSocketSession>()

    override fun decorate(handler: WebSocketHandler): WebSocketHandler = object : WebSocketHandlerDecorator(handler) {
        override fun afterConnectionEstablished(session: WebSocketSession) {
            sessions[session.id] = session
            super.afterConnectionEstablished(session)
        }

        override fun afterConnectionClosed(session: WebSocketSession, closeStatus: CloseStatus) {
            sessions.remove(session.id)
            super.afterConnectionClosed(session, closeStatus)
        }
    }

    fun closeAt(sessionId: String, expiresAt: Instant) {
        taskScheduler.schedule({ close(sessionId) }, expiresAt)
    }

    private fun close(sessionId: String) {
        val session = sessions[sessionId]?.takeIf { it.isOpen } ?: return
        try {
            session.close(TOKEN_EXPIRED)
        } catch (e: IOException) {
            log.warn("[웹소켓 세션 만료 종료 실패] sessionId: {}", sessionId, e)
        }
    }

    companion object {
        val TOKEN_EXPIRED = CloseStatus(4001, "token expired")
    }
}
