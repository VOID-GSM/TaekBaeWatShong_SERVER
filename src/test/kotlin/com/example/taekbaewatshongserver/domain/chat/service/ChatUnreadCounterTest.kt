package com.example.taekbaewatshongserver.domain.chat.service

import com.example.taekbaewatshongserver.domain.chat.repository.ChatMessageRepository
import com.example.taekbaewatshongserver.domain.chat.repository.ChatRoomRepository
import com.example.taekbaewatshongserver.domain.chat.repository.RoomUnreadCount
import com.example.taekbaewatshongserver.domain.user.entity.AuthProvider
import com.example.taekbaewatshongserver.domain.user.entity.Role
import com.example.taekbaewatshongserver.domain.user.entity.User
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyCollection
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import kotlin.test.assertEquals

class ChatUnreadCounterTest {

    private val reader = User(id = 2L, email = "student@gsm.hs.kr", name = "이학생", provider = AuthProvider.DATAGSM, role = Role.STUDENT)

    private val chatRoomRepository: ChatRoomRepository = mock(ChatRoomRepository::class.java)
    private val chatMessageRepository: ChatMessageRepository = mock(ChatMessageRepository::class.java)
    private val counter = ChatUnreadCounter(chatRoomRepository, chatMessageRepository)

    private fun unread(roomId: Long, count: Long) = object : RoomUnreadCount {
        override val roomId = roomId
        override val unreadCount = count
    }

    @Test
    fun `전체 안 읽은 수는 방별 안 읽은 수와 같은 쿼리로 집계한 합계다`() {
        `when`(chatRoomRepository.findIdsByParticipantId(2L)).thenReturn(listOf(5L, 6L, 7L))
        `when`(chatMessageRepository.countUnreadByRoomIds(listOf(5L, 6L, 7L), 2L))
            .thenReturn(listOf(unread(5L, 2L), unread(7L, 3L)))

        assertEquals(mapOf(5L to 2, 7L to 3), counter.byRooms(listOf(5L, 6L, 7L), reader))
        assertEquals(5, counter.total(reader))
    }

    @Test
    fun `참여한 방이 없으면 메시지 집계 쿼리를 실행하지 않는다`() {
        `when`(chatRoomRepository.findIdsByParticipantId(2L)).thenReturn(emptyList())

        assertEquals(0, counter.total(reader))
        verify(chatMessageRepository, never()).countUnreadByRoomIds(anyCollection(), anyLong())
    }
}
