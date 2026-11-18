package com.example.taekbaewatshongserver.domain.report.service

import com.example.taekbaewatshongserver.domain.chat.dto.request.ChatRoomCreateRequest
import com.example.taekbaewatshongserver.domain.chat.dto.response.ChatRoomCreateResult
import com.example.taekbaewatshongserver.domain.chat.dto.response.ChatRoomResponse
import com.example.taekbaewatshongserver.domain.chat.entity.ChatRoom
import com.example.taekbaewatshongserver.domain.chat.entity.ChatRoomType
import com.example.taekbaewatshongserver.domain.chat.repository.ChatRoomRepository
import com.example.taekbaewatshongserver.domain.chat.service.ChatService
import com.example.taekbaewatshongserver.domain.parcel.entity.Parcel
import com.example.taekbaewatshongserver.domain.parcel.repository.ParcelRepository
import com.example.taekbaewatshongserver.domain.report.entity.Report
import com.example.taekbaewatshongserver.domain.report.repository.ReportRepository
import com.example.taekbaewatshongserver.domain.user.entity.AuthProvider
import com.example.taekbaewatshongserver.domain.user.entity.Role
import com.example.taekbaewatshongserver.domain.user.entity.User
import com.example.taekbaewatshongserver.global.exception.ReportException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.LocalDateTime
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ReportServiceTest {

    private val student = User(
        id = 1L,
        email = "student@gsm.hs.kr",
        name = "이학생",
        provider = AuthProvider.DATAGSM,
        role = Role.STUDENT,
        studentNumber = "2401",
    )
    private val teacher = User(id = 2L, email = "teacher@gsm.hs.kr", name = "김선생", provider = AuthProvider.DATAGSM, role = Role.TEACHER)
    private val admin = User(id = 10L, email = "admin@gsm.hs.kr", name = "관리자", provider = AuthProvider.LOCAL, role = Role.ADMIN)
    private val otherAdmin = User(id = 11L, email = "admin2@gsm.hs.kr", name = "관리자2", provider = AuthProvider.LOCAL, role = Role.ADMIN)

    private val reportRepository: ReportRepository = mock(ReportRepository::class.java)
    private val parcelRepository: ParcelRepository = mock(ParcelRepository::class.java)
    private val chatService: ChatService = mock(ChatService::class.java)
    private val chatRoomRepository: ChatRoomRepository = mock(ChatRoomRepository::class.java)
    private val service = ReportService(reportRepository, parcelRepository, chatService, chatRoomRepository)

    private fun reportOf(reporter: User, chatRoom: ChatRoom? = null) = Report(
        id = 7L,
        parcel = Parcel(id = 3L, deliveryCompany = "CJ대한통운", invoiceNumber = "604112345678", alias = "운동화", owner = reporter),
        reporter = reporter,
        lostEstimatedAt = LocalDateTime.of(2026, 11, 8, 18, 0),
        content = "보관함에 택배가 없습니다.",
        chatRoom = chatRoom,
    )

    private fun roomOf(id: Long, user: User, partner: User) = ChatRoom(
        id = id,
        type = ChatRoomType.DRIVER,
        participantLow = ChatRoom.orderPair(user, partner).first,
        participantHigh = ChatRoom.orderPair(user, partner).second,
    )

    private fun roomResponseOf(room: ChatRoom, viewer: User) = ChatRoomResponse.of(room, viewer)

    @Test
    fun `전체 목록은 학번·역할·택배사·신고 내용·채팅방 ID를 채운다`() {
        val room = roomOf(5L, admin, student)
        `when`(reportRepository.findAllByOrderByCreatedAtDesc()).thenReturn(listOf(reportOf(student, room)))

        val summary = service.getAllReports(null).reports.single()

        assertEquals("2401", summary.studentNumber)
        assertEquals(Role.STUDENT, summary.reporterRole)
        assertEquals("CJ대한통운", summary.deliveryCompany)
        assertEquals("보관함에 택배가 없습니다.", summary.content)
        assertEquals(5L, summary.chatRoomId)
    }

    @Test
    fun `선생님 신고는 학번이 null이고 채팅 시작 전에는 채팅방 ID도 null이다`() {
        `when`(reportRepository.findAllByOrderByCreatedAtDesc()).thenReturn(listOf(reportOf(teacher)))

        val summary = service.getAllReports(null).reports.single()

        assertEquals(Role.TEACHER, summary.reporterRole)
        assertNull(summary.studentNumber)
        assertNull(summary.chatRoomId)
    }

    @Test
    fun `상세와 내 신고 목록도 학번과 채팅방 ID를 채운다`() {
        val report = reportOf(student, roomOf(5L, admin, student))
        `when`(reportRepository.findById(7L)).thenReturn(Optional.of(report))
        `when`(reportRepository.findAllByReporterOrderByCreatedAtDesc(student)).thenReturn(listOf(report))

        val detail = service.getReport(admin, 7L)
        assertEquals("2401", detail.studentNumber)
        assertEquals(5L, detail.chatRoomId)

        assertEquals(5L, service.getMyReports(student).reports.single().chatRoomId)
    }

    @Test
    fun `채팅을 시작하면 신고자와의 채팅방을 만들고 신고에 연결한다`() {
        val report = reportOf(student)
        val room = roomOf(5L, admin, student)
        val result = ChatRoomCreateResult(roomResponseOf(room, admin), created = true)
        `when`(reportRepository.findById(7L)).thenReturn(Optional.of(report))
        `when`(chatService.createOrGetRoom(admin, ChatRoomCreateRequest(partnerId = 1L))).thenReturn(result)
        `when`(chatRoomRepository.getReferenceById(5L)).thenReturn(room)

        val actual = service.startChat(admin, 7L)

        assertTrue(actual.created)
        assertSame(room, report.chatRoom)
    }

    @Test
    fun `이미 이 관리자와 연결된 신고면 연결된 방을 그대로 반환한다`() {
        val room = roomOf(5L, admin, student)
        val report = reportOf(student, room)
        `when`(reportRepository.findById(7L)).thenReturn(Optional.of(report))
        `when`(chatService.getRoom(admin, 5L)).thenReturn(roomResponseOf(room, admin))

        val actual = service.startChat(admin, 7L)

        assertFalse(actual.created)
        assertEquals(5L, actual.room.id)
        verify(chatRoomRepository, never()).getReferenceById(anyLong())
    }

    @Test
    fun `다른 관리자가 이미 채팅을 시작한 신고면 409 예외를 던진다`() {
        val report = reportOf(student, roomOf(5L, otherAdmin, student))
        `when`(reportRepository.findById(7L)).thenReturn(Optional.of(report))

        assertThrows<ReportException.ChatAlreadyStarted> { service.startChat(admin, 7L) }
    }

    @Test
    fun `존재하지 않는 신고로 채팅을 시작하면 404 예외를 던진다`() {
        `when`(reportRepository.findById(7L)).thenReturn(Optional.empty())

        assertThrows<ReportException.NotFound> { service.startChat(admin, 7L) }
    }
}
