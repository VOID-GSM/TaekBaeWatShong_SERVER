package com.example.taekbaewatshongserver.domain.chat.service

import com.example.taekbaewatshongserver.domain.chat.dto.response.ChatMessageResponse
import com.example.taekbaewatshongserver.domain.chat.dto.response.ChatSocketEvent
import com.example.taekbaewatshongserver.domain.chat.dto.response.ChatSocketEventType
import com.example.taekbaewatshongserver.domain.chat.entity.ChatMessage
import com.example.taekbaewatshongserver.domain.chat.entity.ChatRoom
import com.example.taekbaewatshongserver.domain.chat.repository.ChatMessageRepository
import com.example.taekbaewatshongserver.domain.user.entity.User
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 채팅 변경을 두 참여자의 STOMP 개인 큐로 보낸다.
 *
 * `publish*`는 호출한 서비스의 트랜잭션 안에서 이벤트 내용을 미리 만들고, 실제 전송([deliver])은 커밋 후에 한다.
 * 전송 단계는 DB에 접근하지 않으므로 커넥션을 추가로 잡지 않는다.
 * 순서를 지키기 위해 @Async 없이 요청 스레드에서 보내며, 전송에 실패해도 이미 커밋된 요청에는 영향을 주지 않는다.
 */
@Component
class ChatSocketPublisher(
    private val chatMessageRepository: ChatMessageRepository,
    private val messagingTemplate: SimpMessagingTemplate,
    private val eventPublisher: ApplicationEventPublisher,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun publishMessageSent(room: ChatRoom, message: ChatMessage) {
        room.participants().forEach { recipient ->
            publish(
                recipient,
                ChatSocketEvent(
                    type = ChatSocketEventType.MESSAGE,
                    roomId = room.id,
                    message = ChatMessageResponse.of(message, recipient),
                    unreadCount = unreadCountOf(room, recipient),
                    totalUnreadCount = totalUnreadCountOf(recipient),
                ),
            )
        }
    }

    fun publishMessagesRead(room: ChatRoom, reader: User) {
        room.participants().forEach { recipient ->
            publish(
                recipient,
                ChatSocketEvent(
                    type = ChatSocketEventType.READ,
                    roomId = room.id,
                    readerId = reader.id,
                    unreadCount = unreadCountOf(room, recipient),
                    totalUnreadCount = totalUnreadCountOf(recipient),
                ),
            )
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun deliver(delivery: ChatSocketDelivery) {
        try {
            messagingTemplate.convertAndSendToUser(delivery.recipientId.toString(), CHAT_QUEUE, delivery.payload)
        } catch (e: Exception) {
            log.error("[채팅 실시간 전달 실패] recipientId: {}, payload: {}", delivery.recipientId, delivery.payload, e)
        }
    }

    private fun ChatRoom.participants(): List<User> = listOf(participantLow, participantHigh)

    private fun publish(recipient: User, payload: ChatSocketEvent) {
        eventPublisher.publishEvent(ChatSocketDelivery(requireNotNull(recipient.id), payload))
    }

    private fun unreadCountOf(room: ChatRoom, user: User): Int = chatMessageRepository
        .countByRoomAndSenderNotAndIsReadFalse(room, user)
        .toInt()

    private fun totalUnreadCountOf(user: User): Int = chatMessageRepository
        .countAllUnreadFor(requireNotNull(user.id))
        .toInt()

    companion object {
        private const val CHAT_QUEUE = "/queue/chat"
    }
}
