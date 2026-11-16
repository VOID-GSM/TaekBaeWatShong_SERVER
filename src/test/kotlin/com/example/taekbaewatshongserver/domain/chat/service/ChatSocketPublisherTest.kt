package com.example.taekbaewatshongserver.domain.chat.service

import com.example.taekbaewatshongserver.domain.chat.dto.response.ChatSocketEventType
import com.example.taekbaewatshongserver.domain.chat.entity.ChatMessage
import com.example.taekbaewatshongserver.domain.chat.entity.ChatRoom
import com.example.taekbaewatshongserver.domain.chat.entity.ChatRoomType
import com.example.taekbaewatshongserver.domain.chat.repository.ChatMessageRepository
import com.example.taekbaewatshongserver.domain.user.entity.AuthProvider
import com.example.taekbaewatshongserver.domain.user.entity.Role
import com.example.taekbaewatshongserver.domain.user.entity.User
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.springframework.context.ApplicationEventPublisher
import org.springframework.messaging.MessagingException
import org.springframework.messaging.simp.SimpMessagingTemplate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ChatSocketPublisherTest {

    private val driver = User(id = 1L, email = "driver@gsm.hs.kr", name = "김택배", provider = AuthProvider.LOCAL, role = Role.ADMIN)
    private val student = User(id = 2L, email = "student@gsm.hs.kr", name = "이학생", provider = AuthProvider.DATAGSM, role = Role.STUDENT)
    private val room = ChatRoom(id = 5L, type = ChatRoomType.DRIVER, participantLow = driver, participantHigh = student)

    private val chatMessageRepository: ChatMessageRepository = mock(ChatMessageRepository::class.java)
    private val messagingTemplate: SimpMessagingTemplate = mock(SimpMessagingTemplate::class.java)
    private val published = mutableListOf<Any>()
    private val publisher = ChatSocketPublisher(
        chatMessageRepository,
        messagingTemplate,
        ApplicationEventPublisher { published += it },
    )

    private fun deliveries(): List<ChatSocketDelivery> = published.filterIsInstance<ChatSocketDelivery>()

    @Test
    fun `새 메시지는 두 참여자에게 받는 사람 기준 내용으로 발행된다`() {
        val message = ChatMessage(id = 10L, room = room, sender = driver, content = "택배 도착했습니다")

        publisher.publishMessageSent(room, message)

        val byRecipient = deliveries().associateBy { it.recipientId }
        assertEquals(setOf(1L, 2L), byRecipient.keys)
        assertEquals(true, byRecipient.getValue(1L).payload.message?.isMine)
        assertEquals(false, byRecipient.getValue(2L).payload.message?.isMine)
        assertNull(byRecipient.getValue(1L).payload.unreadCount)
        assertNull(byRecipient.getValue(1L).payload.totalUnreadCount)
        assertNotNull(byRecipient.getValue(2L).payload.unreadCount)
        assertNotNull(byRecipient.getValue(2L).payload.totalUnreadCount)
        deliveries().forEach {
            assertEquals(ChatSocketEventType.MESSAGE, it.payload.type)
            assertEquals(5L, it.payload.roomId)
        }
    }

    @Test
    fun `읽음 처리는 두 참여자에게 읽은 사람 ID와 함께 발행된다`() {
        publisher.publishMessagesRead(room, student)

        assertEquals(setOf(1L, 2L), deliveries().map { it.recipientId }.toSet())
        deliveries().forEach {
            assertEquals(ChatSocketEventType.READ, it.payload.type)
            assertEquals(2L, it.payload.readerId)
        }
        val byRecipient = deliveries().associateBy { it.recipientId }
        assertEquals(0, byRecipient.getValue(2L).payload.unreadCount)
        assertNotNull(byRecipient.getValue(2L).payload.totalUnreadCount)
        assertNull(byRecipient.getValue(1L).payload.unreadCount)
        assertNull(byRecipient.getValue(1L).payload.totalUnreadCount)
    }

    @Test
    fun `안 읽은 수는 바뀌는 사람 것만 조회한다`() {
        publisher.publishMessageSent(room, ChatMessage(id = 10L, room = room, sender = driver, content = "안녕하세요"))
        publisher.publishMessagesRead(room, student)

        verify(chatMessageRepository, times(1)).countByRoomAndSenderNotAndIsReadFalse(room, student)
        verify(chatMessageRepository, never()).countByRoomAndSenderNotAndIsReadFalse(room, driver)
        verify(chatMessageRepository, times(2)).countAllUnreadFor(2L)
        verify(chatMessageRepository, never()).countAllUnreadFor(1L)
    }

    @Test
    fun `전송에 실패해도 예외가 호출한 쪽으로 전파되지 않는다`() {
        publisher.publishMessagesRead(room, student)
        doThrow(MessagingException("broker down"))
            .`when`(messagingTemplate).convertAndSendToUser(anyString(), anyString(), any())

        deliveries().forEach { publisher.deliver(it) }
    }
}
