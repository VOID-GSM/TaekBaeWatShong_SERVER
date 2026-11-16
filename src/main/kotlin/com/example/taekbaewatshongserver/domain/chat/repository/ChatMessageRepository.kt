package com.example.taekbaewatshongserver.domain.chat.repository

import com.example.taekbaewatshongserver.domain.chat.entity.ChatMessage
import com.example.taekbaewatshongserver.domain.chat.entity.ChatRoom
import com.example.taekbaewatshongserver.domain.user.entity.User
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ChatMessageRepository : JpaRepository<ChatMessage, Long> {

    @EntityGraph(attributePaths = ["sender"])
    fun findByRoomOrderByIdDesc(room: ChatRoom, pageable: Pageable): List<ChatMessage>

    @EntityGraph(attributePaths = ["sender"])
    fun findByRoomAndIdLessThanOrderByIdDesc(room: ChatRoom, id: Long, pageable: Pageable): List<ChatMessage>

    fun countByRoomAndSenderNotAndIsReadFalse(room: ChatRoom, sender: User): Long

    @Query(
        """
        select m.room.id as roomId, count(m) as unreadCount
        from ChatMessage m
        where m.room.id in :roomIds and m.sender.id <> :readerId and m.isRead = false
        group by m.room.id
        """,
    )
    fun countUnreadByRoomIds(
        @Param("roomIds") roomIds: Collection<Long>,
        @Param("readerId") readerId: Long,
    ): List<RoomUnreadCount>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ChatMessage m set m.isRead = true where m.room = :room and m.sender <> :reader and m.isRead = false")
    fun markAllAsReadBy(@Param("room") room: ChatRoom, @Param("reader") reader: User): Int
}
