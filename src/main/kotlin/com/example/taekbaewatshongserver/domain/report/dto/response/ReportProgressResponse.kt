package com.example.taekbaewatshongserver.domain.report.dto.response

import com.example.taekbaewatshongserver.domain.report.entity.Report
import com.example.taekbaewatshongserver.domain.report.entity.ReportProgress
import java.time.LocalDateTime

data class ReportProgressResponse(
    val id: Long,
    val progress: ReportProgress,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun from(report: Report): ReportProgressResponse = ReportProgressResponse(
            id = report.id,
            progress = report.progress,
            updatedAt = report.updatedAt,
        )
    }
}
