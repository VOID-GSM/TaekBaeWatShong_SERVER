package com.example.taekbaewatshongserver.domain.notification.service

import com.example.taekbaewatshongserver.domain.notification.repository.DeviceTokenRepository
import com.example.taekbaewatshongserver.global.config.TaskExecutionConfig
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 알림 저장 트랜잭션이 커밋된 뒤 푸시 전용 실행기([TaskExecutionConfig.PUSH_TASK_EXECUTOR])에서 수신자의 모든 기기로 푸시를 보낸다.
 * 발송이 실패해도 알림 저장이나 택배 처리에는 영향이 없다.
 */
@Component
class PushNotificationListener(
    private val deviceTokenRepository: DeviceTokenRepository,
    private val expoPushClient: ExpoPushClient,
    @Value("\${notification.push.enabled:true}")
    private val enabled: Boolean,
    @Value("\${notification.push.android-channel-id:}")
    androidChannelId: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val channelId = androidChannelId.ifBlank { null }

    @Async(TaskExecutionConfig.PUSH_TASK_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    fun handleNotificationCreated(event: NotificationCreatedEvent) {
        if (!enabled) return

        try {
            send(event)
        } catch (e: Exception) {
            log.error("[푸시 발송 실패] notificationId: {}", event.notificationId, e)
        }
    }

    private fun send(event: NotificationCreatedEvent) {
        val (expoTokens, invalidTokens) = deviceTokenRepository.findAllByOwnerId(event.recipientId)
            .map { it.deviceToken }
            .partition { ExpoPushClient.isExpoPushToken(it) }

        // 등록 시 형식을 검사하기 전에 저장된 토큰은 Expo로 보내지 않아 DeviceNotRegistered로도 지워지지 않으므로 여기서 지운다.
        if (invalidTokens.isNotEmpty()) {
            deviceTokenRepository.deleteAllByDeviceTokenIn(invalidTokens)
            log.info("[무효 토큰 삭제] Expo 형식이 아닌 토큰 {}개 삭제. recipientId: {}", invalidTokens.size, event.recipientId)
        }
        if (expoTokens.isEmpty()) return

        val data = mapOf(
            "notificationId" to event.notificationId,
            "type" to event.type.name,
            "parcelId" to event.parcelId,
        )
        val messages = expoTokens.map {
            ExpoPushMessage(
                to = it,
                title = event.title,
                body = event.message,
                data = data,
                channelId = channelId,
            )
        }

        val tickets = expoPushClient.send(messages)

        val unregisteredTokens = mutableListOf<String>()
        tickets.zip(expoTokens).forEach { (ticket, token) ->
            when {
                ticket.isOk -> Unit
                ticket.isDeviceNotRegistered -> unregisteredTokens += token
                else -> log.warn(
                    "[푸시 발송 실패] notificationId: {} | error: {} | message: {}",
                    event.notificationId,
                    ticket.details?.error,
                    ticket.message,
                )
            }
        }

        if (unregisteredTokens.isNotEmpty()) {
            deviceTokenRepository.deleteAllByDeviceTokenIn(unregisteredTokens)
            log.info("[무효 토큰 삭제] {}개 삭제. recipientId: {}", unregisteredTokens.size, event.recipientId)
        }

        log.info(
            "[푸시 발송] notificationId: {} | 대상 기기: {} | 성공: {}",
            event.notificationId,
            expoTokens.size,
            tickets.count { it.isOk },
        )
    }
}
