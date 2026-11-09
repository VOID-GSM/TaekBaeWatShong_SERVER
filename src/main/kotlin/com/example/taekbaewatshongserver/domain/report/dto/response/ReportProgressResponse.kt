package com.example.taekbaewatshongserver.domain.report.dto.response

import com.example.taekbaewatshongserver.domain.report.entity.Report
import com.example.taekbaewatshongserver.domain.report.entity.ReportProgress
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "진행 상태 변경 결과")
data class ReportProgressResponse(
    @field:Schema(description = "분실 신고 ID", example = "1")
    val id: Long,
    @field:Schema(description = "변경된 진행 상태", example = "IN_PROGRESS")
    val progress: ReportProgress,
    @field:Schema(description = "변경 시각", example = "2026-11-09T14:30:00")
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
