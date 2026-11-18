package com.example.taekbaewatshongserver.domain.report.repository

import com.example.taekbaewatshongserver.domain.chat.entity.ChatRoom
import com.example.taekbaewatshongserver.domain.parcel.entity.Parcel
import com.example.taekbaewatshongserver.domain.report.entity.Report
import com.example.taekbaewatshongserver.domain.report.entity.ReportProgress
import com.example.taekbaewatshongserver.domain.user.entity.User
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface ReportRepository : JpaRepository<Report, Long> {

    fun existsByParcel(parcel: Parcel): Boolean

    @EntityGraph(attributePaths = ["parcel", "chatRoom"])
    fun findAllByReporterOrderByCreatedAtDesc(reporter: User): List<Report>

    @EntityGraph(attributePaths = ["parcel", "reporter", "chatRoom"])
    fun findAllByOrderByCreatedAtDesc(): List<Report>

    @EntityGraph(attributePaths = ["parcel", "reporter", "chatRoom"])
    fun findAllByProgressOrderByCreatedAtDesc(progress: ReportProgress): List<Report>

    /** 동시에 채팅을 시작해도 먼저 연결한 채팅방을 덮어쓰지 않는다. 0이면 다른 요청이 이미 연결했다. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        """
        update Report r
        set r.chatRoom = :room, r.updatedAt = :now
        where r.id = :id and r.chatRoom is null
        """,
    )
    fun linkChatRoomIfAbsent(
        @Param("id") id: Long,
        @Param("room") room: ChatRoom,
        @Param("now") now: LocalDateTime,
    ): Int
}
