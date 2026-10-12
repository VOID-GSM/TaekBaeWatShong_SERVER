package com.example.taekbaewatshongserver.domain.chat.repository

import com.example.taekbaewatshongserver.domain.chat.entity.ChatRoom
import com.example.taekbaewatshongserver.domain.user.entity.User
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

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
}
