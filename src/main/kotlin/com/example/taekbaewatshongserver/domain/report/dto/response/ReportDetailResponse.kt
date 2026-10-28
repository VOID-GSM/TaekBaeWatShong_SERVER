package com.example.taekbaewatshongserver.domain.report.dto.response

import com.example.taekbaewatshongserver.domain.report.entity.Report
import com.example.taekbaewatshongserver.domain.report.entity.ReportProgress
import java.time.LocalDateTime

data class ReportDetailResponse(
    val id: Long,
    val reporterName: String,
    val studentNumber: String? = null,
    val alias: String,
    val invoiceNumber: String,
    val lostEstimatedAt: LocalDateTime,
    val content: String,
    val progress: ReportProgress,
    val chatRoomId: Long? = null,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(report: Report): ReportDetailResponse = ReportDetailResponse(
            id = report.id,
            reporterName = report.reporter.name,
            alias = report.parcel.alias,
            invoiceNumber = report.parcel.invoiceNumber,
            lostEstimatedAt = report.lostEstimatedAt,
            content = report.content,
            progress = report.progress,
            createdAt = report.createdAt,
        )
    }
}
