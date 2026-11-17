package com.example.taekbaewatshongserver.domain.chat.service

import com.example.taekbaewatshongserver.domain.chat.entity.ChatRoom
import com.example.taekbaewatshongserver.domain.chat.repository.ChatMessageRepository
import com.example.taekbaewatshongserver.domain.chat.repository.ChatRoomRepository
import com.example.taekbaewatshongserver.domain.user.entity.User
import org.springframework.stereotype.Component

/**
 * 안 읽은 메시지 수 계산을 한 곳에 모은다. REST 응답과 실시간 이벤트가 같은 정의를 쓰게 하기 위함이다.
 *
 * 안 읽은 메시지 = 그 방에서 상대가 보냈고 아직 `isRead = false`인 메시지.
 */
@Component
class ChatUnreadCounter(
    private val chatRoomRepository: ChatRoomRepository,
    private val chatMessageRepository: ChatMessageRepository,
) {

    fun ofRoom(room: ChatRoom, reader: User): Int = chatMessageRepository
        .countByRoomAndSenderNotAndIsReadFalse(room, reader)
        .toInt()

    fun byRooms(roomIds: Collection<Long>, reader: User): Map<Long, Int> {
        if (roomIds.isEmpty()) {
            return emptyMap()
        }
        return chatMessageRepository
            .countUnreadByRoomIds(roomIds, requireNotNull(reader.id))
            .associate { it.roomId to it.unreadCount.toInt() }
    }

    fun total(reader: User): Int {
        val roomIds = chatRoomRepository.findIdsByParticipantId(requireNotNull(reader.id))
        return byRooms(roomIds, reader).values.sum()
    }
}
