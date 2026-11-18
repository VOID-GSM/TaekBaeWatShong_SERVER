package com.example.taekbaewatshongserver.domain.chat.service

import com.example.taekbaewatshongserver.domain.chat.dto.response.ChatMessageResponse
import com.example.taekbaewatshongserver.domain.chat.dto.response.ChatSocketEvent
import com.example.taekbaewatshongserver.domain.chat.dto.response.ChatSocketEventType
import com.example.taekbaewatshongserver.domain.chat.entity.ChatMessage
import com.example.taekbaewatshongserver.domain.chat.entity.ChatRoom
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
    private val chatUnreadCounter: ChatUnreadCounter,
    private val messagingTemplate: SimpMessagingTemplate,
    private val eventPublisher: ApplicationEventPublisher,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /** 안 읽은 수는 받는 쪽(보낸 사람의 상대)만 바뀌므로 그쪽만 계산하고, 보낸 사람에게는 null로 보낸다. */
    fun publishMessageSent(room: ChatRoom, message: ChatMessage) {
        val sender = message.sender
        val receiver = room.partnerOf(sender)

        publish(
            sender,
            ChatSocketEvent(
                type = ChatSocketEventType.MESSAGE,
                roomId = room.id,
                message = ChatMessageResponse.of(message, sender),
            ),
        )
        publish(
            receiver,
            ChatSocketEvent(
                type = ChatSocketEventType.MESSAGE,
                roomId = room.id,
                message = ChatMessageResponse.of(message, receiver),
                unreadCount = chatUnreadCounter.ofRoom(room, receiver),
                totalUnreadCount = chatUnreadCounter.total(receiver),
            ),
        )
    }

    /** 안 읽은 수는 읽은 사람만 바뀌므로(이 방은 0) 그쪽만 계산하고, 상대에게는 null로 보낸다. */
    fun publishMessagesRead(room: ChatRoom, reader: User) {
        publish(
            reader,
            ChatSocketEvent(
                type = ChatSocketEventType.READ,
                roomId = room.id,
                readerId = reader.id,
                unreadCount = 0,
                totalUnreadCount = chatUnreadCounter.total(reader),
            ),
        )
        publish(
            room.partnerOf(reader),
            ChatSocketEvent(
                type = ChatSocketEventType.READ,
                roomId = room.id,
                readerId = reader.id,
            ),
        )
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun deliver(delivery: ChatSocketDelivery) {
        try {
            messagingTemplate.convertAndSendToUser(delivery.recipientId.toString(), CHAT_QUEUE, delivery.payload)
        } catch (e: Exception) {
            val payload = delivery.payload
            log.error(
                "[채팅 실시간 전달 실패] recipientId: {}, type: {}, roomId: {}, messageId: {}",
                delivery.recipientId,
                payload.type,
                payload.roomId,
                payload.message?.id,
                e,
            )
        }
    }

    private fun publish(recipient: User, payload: ChatSocketEvent) {
        eventPublisher.publishEvent(ChatSocketDelivery(requireNotNull(recipient.id), payload))
    }

    companion object {
        private const val CHAT_QUEUE = "/queue/chat"
    }
}
