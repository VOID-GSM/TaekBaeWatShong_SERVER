package com.example.taekbaewatshongserver.domain.chat.service

import com.example.taekbaewatshongserver.domain.chat.dto.response.ChatMessageResponse
import com.example.taekbaewatshongserver.domain.chat.dto.response.ChatSocketEvent
import com.example.taekbaewatshongserver.domain.chat.dto.response.ChatSocketEventType
import com.example.taekbaewatshongserver.domain.chat.entity.ChatRoom
import com.example.taekbaewatshongserver.domain.chat.repository.ChatMessageRepository
import com.example.taekbaewatshongserver.domain.chat.repository.ChatRoomRepository
import com.example.taekbaewatshongserver.domain.user.entity.User
import org.slf4j.LoggerFactory
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 커밋된 채팅 변경을 두 참여자의 STOMP 개인 큐로 보낸다.
 *
 * 순서를 지키기 위해 @Async 없이 요청 스레드에서 커밋 직후 보낸다.
 * 전달에 실패해도 이미 커밋된 REST 요청은 성공으로 응답해야 하므로 예외는 로그만 남긴다.
 */
@Component
class ChatSocketPublisher(
    private val chatRoomRepository: ChatRoomRepository,
    private val chatMessageRepository: ChatMessageRepository,
    private val messagingTemplate: SimpMessagingTemplate,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    fun handleMessageSent(event: ChatMessageSentEvent) = publishSafely(event) {
        val message = chatMessageRepository.findWithSenderById(event.messageId) ?: return@publishSafely
        val room = chatRoomRepository.findWithParticipantsById(message.room.id) ?: return@publishSafely

        room.participants().forEach { recipient ->
            send(
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

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    fun handleMessagesRead(event: ChatMessagesReadEvent) = publishSafely(event) {
        val room = chatRoomRepository.findWithParticipantsById(event.roomId) ?: return@publishSafely

        room.participants().forEach { recipient ->
            send(
                recipient,
                ChatSocketEvent(
                    type = ChatSocketEventType.READ,
                    roomId = room.id,
                    readerId = event.readerId,
                    unreadCount = unreadCountOf(room, recipient),
                    totalUnreadCount = totalUnreadCountOf(recipient),
                ),
            )
        }
    }

    private fun ChatRoom.participants(): List<User> = listOf(participantLow, participantHigh)

    private fun send(recipient: User, payload: ChatSocketEvent) {
        messagingTemplate.convertAndSendToUser(requireNotNull(recipient.id).toString(), CHAT_QUEUE, payload)
    }

    private fun unreadCountOf(room: ChatRoom, user: User): Int = chatMessageRepository
        .countByRoomAndSenderNotAndIsReadFalse(room, user)
        .toInt()

    private fun totalUnreadCountOf(user: User): Int = chatMessageRepository
        .countAllUnreadFor(requireNotNull(user.id))
        .toInt()

    private inline fun publishSafely(event: Any, block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            log.error("[채팅 실시간 전달 실패] event: {}", event, e)
        }
    }

    companion object {
        private const val CHAT_QUEUE = "/queue/chat"
    }
}
