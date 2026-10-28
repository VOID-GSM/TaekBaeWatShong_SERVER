package com.example.taekbaewatshongserver.domain.report.dto.response

import com.example.taekbaewatshongserver.domain.report.entity.Report
import com.example.taekbaewatshongserver.domain.report.entity.ReportProgress
import java.time.LocalDateTime

data class ReportListResponse(
    val reports: List<ReportSummary>,
) {
    data class ReportSummary(
        val id: Long,
        val reporterName: String,
        val studentNumber: String? = null,
        val alias: String,
        val invoiceNumber: String,
        val lostEstimatedAt: LocalDateTime,
        val progress: ReportProgress,
        val chatRoomId: Long? = null,
        val createdAt: LocalDateTime,
    ) {
        companion object {
            fun from(report: Report): ReportSummary = ReportSummary(
                id = report.id,
                reporterName = report.reporter.name,
                alias = report.parcel.alias,
                invoiceNumber = report.parcel.invoiceNumber,
                lostEstimatedAt = report.lostEstimatedAt,
                progress = report.progress,
                createdAt = report.createdAt,
            )
        }
    }
}
