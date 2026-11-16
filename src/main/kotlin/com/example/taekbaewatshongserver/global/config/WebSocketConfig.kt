package com.example.taekbaewatshongserver.global.config

import com.example.taekbaewatshongserver.global.security.websocket.StompAuthChannelInterceptor
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

/**
 * 실시간 채팅용 STOMP 설정.
 *
 * 클라이언트는 `/ws-chat`에 연결하고 CONNECT 프레임의 `Authorization` 헤더로 JWT를 보낸 뒤
 * 본인 전용 큐(`/user/queue/chat`)를 구독한다. 메시지 전송은 REST API로만 한다.
 */
@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig(
    private val stompAuthChannelInterceptor: StompAuthChannelInterceptor,
    @Value("#{'\${app.cors.allowed-origins}'.split(',')}") private val allowedOrigins: List<String>,
) : WebSocketMessageBrokerConfigurer {

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint(ENDPOINT)
            .setAllowedOriginPatterns(*allowedOrigins.map { it.trim() }.toTypedArray())
    }

    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        registry.enableSimpleBroker("/queue")
            .setHeartbeatValue(longArrayOf(HEARTBEAT_MS, HEARTBEAT_MS))
            .setTaskScheduler(heartbeatScheduler())
        registry.setUserDestinationPrefix("/user")
    }

    override fun configureClientInboundChannel(registration: ChannelRegistration) {
        registration.interceptors(stompAuthChannelInterceptor)
    }

    private fun heartbeatScheduler(): ThreadPoolTaskScheduler = ThreadPoolTaskScheduler().apply {
        poolSize = 1
        setThreadNamePrefix("ws-heartbeat-")
        initialize()
    }

    companion object {
        const val ENDPOINT = "/ws-chat"
        private const val HEARTBEAT_MS = 10_000L
    }
}
