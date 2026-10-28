package com.example.taekbaewatshongserver.domain.chat.repository

import com.example.taekbaewatshongserver.domain.chat.entity.ChatRoom
import com.example.taekbaewatshongserver.domain.user.entity.User
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface ChatRoomRepository : JpaRepository<ChatRoom, Long> {

    @EntityGraph(attributePaths = ["participantLow", "participantHigh"])
    fun findByParticipantLowAndParticipantHigh(participantLow: User, participantHigh: User): ChatRoom?

    @EntityGraph(attributePaths = ["participantLow", "participantHigh"])
    @Query(
        """
        select r from ChatRoom r
        where r.participantLow = :user or r.participantHigh = :user
        order by coalesce(r.lastMessageAt, r.createdAt) desc
        """,
    )
    fun findAllByParticipant(@Param("user") user: User): List<ChatRoom>

    @EntityGraph(attributePaths = ["participantLow", "participantHigh"])
    fun findWithParticipantsById(id: Long): ChatRoom?

    /** 동시에 전송된 메시지의 커밋 순서가 뒤바뀌어도 더 오래된 메시지로 덮어쓰지 않는다. */
    @Modifying(flushAutomatically = true)
    @Query(
        """
        update ChatRoom r
        set r.lastMessage = :lastMessage, r.lastMessageAt = :lastMessageAt
        where r.id = :roomId and (r.lastMessageAt is null or r.lastMessageAt <= :lastMessageAt)
        """,
    )
    fun updateLastMessageIfNewer(
        @Param("roomId") roomId: Long,
        @Param("lastMessage") lastMessage: String,
        @Param("lastMessageAt") lastMessageAt: LocalDateTime,
    ): Int
}
