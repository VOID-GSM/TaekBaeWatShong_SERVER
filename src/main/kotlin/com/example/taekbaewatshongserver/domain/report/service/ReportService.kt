package com.example.taekbaewatshongserver.domain.report.service

import com.example.taekbaewatshongserver.domain.chat.dto.request.ChatRoomCreateRequest
import com.example.taekbaewatshongserver.domain.chat.dto.response.ChatRoomCreateResult
import com.example.taekbaewatshongserver.domain.chat.repository.ChatRoomRepository
import com.example.taekbaewatshongserver.domain.chat.service.ChatService
import com.example.taekbaewatshongserver.domain.parcel.repository.ParcelRepository
import com.example.taekbaewatshongserver.domain.report.dto.request.ReportCreateRequest
import com.example.taekbaewatshongserver.domain.report.dto.request.ReportProgressUpdateRequest
import com.example.taekbaewatshongserver.domain.report.dto.response.*
import com.example.taekbaewatshongserver.domain.report.entity.Report
import com.example.taekbaewatshongserver.domain.report.entity.ReportProgress
import com.example.taekbaewatshongserver.domain.report.repository.ReportRepository
import com.example.taekbaewatshongserver.domain.user.entity.Role
import com.example.taekbaewatshongserver.domain.user.entity.User
import com.example.taekbaewatshongserver.global.exception.ReportException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class ReportService(
    private val reportRepository: ReportRepository,
    private val parcelRepository: ParcelRepository,
    private val chatService: ChatService,
    private val chatRoomRepository: ChatRoomRepository,
) {

    @Transactional
    fun createReport(user: User, request: ReportCreateRequest): ReportCreateResponse {
        val parcel = parcelRepository.findByInvoiceNumber(request.invoiceNumber)
            ?: throw ReportException.ParcelNotFound()

        if (parcel.owner.id != user.id) {
            throw AccessDeniedException("본인이 등록한 택배만 분실 신고할 수 있습니다.")
        }

        if (reportRepository.existsByParcel(parcel)) {
            throw ReportException.Conflict()
        }

        val report = Report(
            parcel = parcel,
            reporter = user,
            lostEstimatedAt = request.lostEstimatedAt,
            content = request.content,
        )

        return try {
            val savedReport = reportRepository.save(report)
            ReportCreateResponse.from(savedReport)
        } catch (e: DataIntegrityViolationException) {
            throw ReportException.Conflict()
        }
    }

    fun getMyReports(user: User): MyReportListResponse {
        val reports = reportRepository.findAllByReporterOrderByCreatedAtDesc(user)
        return MyReportListResponse(reports.map { MyReportListResponse.MyReportDetail.from(it) })
    }

    fun getReport(user: User, reportId: Long): ReportDetailResponse {
        val report = reportRepository.findById(reportId)
            .orElseThrow { ReportException.NotFound() }

        if (user.role != Role.ADMIN && report.reporter.id != user.id) {
            throw AccessDeniedException("본인의 분실 신고만 조회할 수 있습니다.")
        }

        return ReportDetailResponse.from(report)
    }

    fun getAllReports(progress: ReportProgress?): ReportListResponse {
        val reports = if (progress != null) {
            reportRepository.findAllByProgressOrderByCreatedAtDesc(progress)
        } else {
            reportRepository.findAllByOrderByCreatedAtDesc()
        }

        return ReportListResponse(reports.map { ReportListResponse.ReportSummary.from(it) })
    }

    @Transactional
    fun updateProgress(reportId: Long, request: ReportProgressUpdateRequest): ReportProgressResponse {
        val report = reportRepository.findById(reportId)
            .orElseThrow { ReportException.NotFound() }

        report.updateProgress(request.progress)

        return ReportProgressResponse.from(report)
    }

    // ChatService.createOrGetRoom이 이 트랜잭션에 합류하므로, 동시 생성 경합 후 재조회를 위해 같은 격리 수준을 쓴다.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    fun startChat(admin: User, reportId: Long): ChatRoomCreateResult {
        val report = reportRepository.findById(reportId)
            .orElseThrow { ReportException.NotFound() }

        report.chatRoom?.let { linkedRoom ->
            if (!linkedRoom.contains(admin)) {
                throw ReportException.ChatAlreadyStarted()
            }
            return ChatRoomCreateResult(chatService.getRoom(admin, linkedRoom.id), created = false)
        }

        val reporterId = requireNotNull(report.reporter.id)
        val result = chatService.createOrGetRoom(admin, ChatRoomCreateRequest(partnerId = reporterId))
        val linked = reportRepository.linkChatRoomIfAbsent(reportId, chatRoomRepository.getReferenceById(result.room.id), LocalDateTime.now())

        if (linked == 0) {
            val linkedRoomId = reportRepository.findById(reportId)
                .orElseThrow { ReportException.NotFound() }
                .chatRoom?.id
            if (linkedRoomId != result.room.id) {
                throw ReportException.ChatAlreadyStarted()
            }
        }

        return result
    }
}
