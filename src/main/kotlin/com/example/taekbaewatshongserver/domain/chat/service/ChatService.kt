package com.example.taekbaewatshongserver.domain.chat.service

import com.example.taekbaewatshongserver.domain.chat.dto.request.ChatMessageSendRequest
import com.example.taekbaewatshongserver.domain.chat.dto.request.ChatRoomCreateRequest
import com.example.taekbaewatshongserver.domain.chat.dto.response.*
import com.example.taekbaewatshongserver.domain.chat.entity.ChatMessage
import com.example.taekbaewatshongserver.domain.chat.entity.ChatRoom
import com.example.taekbaewatshongserver.domain.chat.repository.ChatMessageRepository
import com.example.taekbaewatshongserver.domain.chat.repository.ChatRoomRepository
import com.example.taekbaewatshongserver.domain.user.entity.User
import com.example.taekbaewatshongserver.domain.user.repository.UserRepository
import com.example.taekbaewatshongserver.global.exception.ChatException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional

private const val DEFAULT_SIZE = 30
private const val MAX_SIZE = 100

@Service
@Transactional(readOnly = true)
class ChatService(
    private val chatRoomRepository: ChatRoomRepository,
    private val chatMessageRepository: ChatMessageRepository,
    private val userRepository: UserRepository,
    private val chatRoomCreator: ChatRoomCreator,
) {

    fun getPartners(user: User, keyword: String?, page: Int?, size: Int?): ChatPartnerListResponse {
        val condition = ChatPolicy.partnerCondition(user)
        val pageable = PageRequest.of((page ?: 0).coerceAtLeast(0), (size ?: DEFAULT_SIZE).coerceIn(1, MAX_SIZE))

        val partners = userRepository.findChatPartners(
            userId = requireNotNull(user.id),
            roles = condition.roles,
            dataGsmOnlyRole = condition.dataGsmOnlyRole,
            pattern = "%${keyword?.trim().orEmpty()}%",
            pageable = pageable,
        )

        return ChatPartnerListResponse(
            partners = partners.content.map { ChatPartnerResponse.from(it) },
            hasNext = partners.hasNext(),
        )
    }

    // 동시 생성 경합에서 진 경우, 상대 트랜잭션이 커밋한 방을 재조회로 볼 수 있어야 하므로 READ_COMMITTED를 쓴다.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    fun createOrGetRoom(user: User, request: ChatRoomCreateRequest): ChatRoomCreateResult {
        val partner = userRepository.findById(request.partnerId)
            .orElseThrow { ChatException.PartnerNotFound() }

        val type = ChatPolicy.resolveRoomType(user, partner)
        val (low, high) = ChatRoom.orderPair(user, partner)

        chatRoomRepository.findByParticipantLowAndParticipantHigh(low, high)?.let {
            return ChatRoomCreateResult(ChatRoomResponse.of(it, user, unreadCountOf(it, user)), created = false)
        }

        val savedRoom = try {
            chatRoomCreator.create(type, user, partner)
        } catch (e: DataIntegrityViolationException) {
            val existingRoom = chatRoomRepository.findByParticipantLowAndParticipantHigh(low, high)
                ?: throw e
            return ChatRoomCreateResult(
                ChatRoomResponse.of(existingRoom, user, unreadCountOf(existingRoom, user)),
                created = false,
            )
        }

        return ChatRoomCreateResult(ChatRoomResponse.of(savedRoom, user), created = true)
    }

    fun getMyRooms(user: User): ChatRoomListResponse {
        val rooms = chatRoomRepository.findAllByParticipant(user)

        if (rooms.isEmpty()) {
            return ChatRoomListResponse(totalUnreadCount = 0, rooms = emptyList())
        }

        val unreadCounts = chatMessageRepository
            .countUnreadByRoomIds(rooms.map { it.id }, requireNotNull(user.id))
            .associate { it.roomId to it.unreadCount.toInt() }

        return ChatRoomListResponse(
            totalUnreadCount = unreadCounts.values.sum(),
            rooms = rooms.map { ChatRoomResponse.of(it, user, unreadCounts[it.id] ?: 0) },
        )
    }

    fun getRoom(user: User, roomId: Long): ChatRoomResponse {
        val room = findParticipatingRoom(user, roomId)
        return ChatRoomResponse.of(room, user, unreadCountOf(room, user))
    }

    fun getMessages(user: User, roomId: Long, cursor: Long?, size: Int?): ChatMessageListResponse {
        val room = findParticipatingRoom(user, roomId)

        val pageSize = (size ?: DEFAULT_SIZE).coerceIn(1, MAX_SIZE)
        val pageable = PageRequest.of(0, pageSize + 1)

        val messages = if (cursor != null) {
            chatMessageRepository.findByRoomAndIdLessThanOrderByIdDesc(room, cursor, pageable)
        } else {
            chatMessageRepository.findByRoomOrderByIdDesc(room, pageable)
        }

        val hasNext = messages.size > pageSize
        val content = if (hasNext) messages.subList(0, pageSize) else messages
        val nextCursor = if (hasNext) content.last().id else null

        return ChatMessageListResponse(
            messages = content.map { ChatMessageResponse.of(it, user) },
            nextCursor = nextCursor,
        )
    }

    @Transactional
    fun sendMessage(user: User, roomId: Long, request: ChatMessageSendRequest): ChatMessageResponse {
        val room = findParticipatingRoom(user, roomId)

        ChatPolicy.resolveRoomType(user, room.partnerOf(user))

        val message = chatMessageRepository.save(
            ChatMessage(
                room = room,
                sender = user,
                content = request.content.trim(),
            ),
        )
        chatRoomRepository.updateLastMessageIfNewer(room.id, ChatRoom.previewOf(message.content), message.createdAt)

        return ChatMessageResponse.of(message, user)
    }

    @Transactional
    fun markAsRead(user: User, roomId: Long): ChatMessageReadResponse {
        val room = findParticipatingRoom(user, roomId)
        val readCount = chatMessageRepository.markAllAsReadBy(room, user)

        return ChatMessageReadResponse(roomId = roomId, readCount = readCount, unreadCount = 0)
    }

    private fun findParticipatingRoom(user: User, roomId: Long): ChatRoom {
        val room = chatRoomRepository.findWithParticipantsById(roomId)
            ?: throw ChatException.RoomNotFound()

        if (!room.contains(user)) {
            throw ChatException.NotParticipant()
        }

        return room
    }

    private fun unreadCountOf(room: ChatRoom, user: User): Int = chatMessageRepository
        .countByRoomAndSenderNotAndIsReadFalse(room, user)
        .toInt()
}
