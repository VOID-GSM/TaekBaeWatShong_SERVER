package com.example.taekbaewatshongserver.domain.report.dto.response

import com.example.taekbaewatshongserver.domain.report.entity.Report
import com.example.taekbaewatshongserver.domain.report.entity.ReportProgress
import java.time.LocalDateTime

data class ReportCreateResponse(
    val id: Long,
    val invoiceNumber: String,
    val alias: String,
    val lostEstimatedAt: LocalDateTime,
    val content: String,
    val progress: ReportProgress,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(report: Report): ReportCreateResponse {
            return ReportCreateResponse(
                id = report.id,
                invoiceNumber = report.parcel.invoiceNumber,
                alias = report.parcel.alias,
                lostEstimatedAt = report.lostEstimatedAt,
                content = report.content,
                progress = report.progress,
                createdAt = report.createdAt
            )
        }
    }
}
