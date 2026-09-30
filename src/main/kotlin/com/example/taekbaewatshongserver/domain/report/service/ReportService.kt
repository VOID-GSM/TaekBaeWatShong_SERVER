package com.example.taekbaewatshongserver.domain.report.service

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
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ReportService(
    private val reportRepository: ReportRepository,
    private val parcelRepository: ParcelRepository,
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
}
