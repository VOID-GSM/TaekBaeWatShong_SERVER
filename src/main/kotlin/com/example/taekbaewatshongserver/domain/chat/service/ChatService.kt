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
import org.springframework.transaction.annotation.Transactional

private const val DEFAULT_SIZE = 30
private const val MAX_SIZE = 100

@Service
@Transactional(readOnly = true)
class ChatService(
    private val chatRoomRepository: ChatRoomRepository,
    private val chatMessageRepository: ChatMessageRepository,
    private val userRepository: UserRepository,
) {

    fun getPartners(user: User): ChatPartnerListResponse {
        val partners = userRepository.findAllByRoleIn(ChatPolicy.candidateRoles(user))
            .filter { it.id != user.id && ChatPolicy.isAllowed(user, it) }
            .map { ChatPartnerResponse.from(it) }

        return ChatPartnerListResponse(partners)
    }

    @Transactional
    fun createOrGetRoom(user: User, request: ChatRoomCreateRequest): ChatRoomCreateResult {
        val partner = userRepository.findById(request.partnerId)
            .orElseThrow { ChatException.PartnerNotFound() }

        val type = ChatPolicy.resolveRoomType(user, partner)
        val (low, high) = ChatRoom.orderPair(user, partner)

        chatRoomRepository.findByParticipantLowAndParticipantHigh(low, high)?.let {
            return ChatRoomCreateResult(ChatRoomResponse.of(it, user, unreadCountOf(it, user)), created = false)
        }

        return try {
            val savedRoom = chatRoomRepository.save(ChatRoom.of(type, user, partner))
            ChatRoomCreateResult(ChatRoomResponse.of(savedRoom, user), created = true)
        } catch (e: DataIntegrityViolationException) {
            val existingRoom = chatRoomRepository.findByParticipantLowAndParticipantHigh(low, high)
                ?: throw e
            ChatRoomCreateResult(
                ChatRoomResponse.of(existingRoom, user, unreadCountOf(existingRoom, user)),
                created = false,
            )
        }
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
        room.updateLastMessage(message)

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
