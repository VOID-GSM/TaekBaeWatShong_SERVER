package com.example.taekbaewatshongserver.domain.report.dto.response

import com.example.taekbaewatshongserver.domain.report.entity.Report
import com.example.taekbaewatshongserver.domain.report.entity.ReportProgress
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "전체 분실 신고 목록")
data class ReportListResponse(
    @field:Schema(description = "분실 신고 목록 (접수 최신순)")
    val reports: List<ReportSummary>,
) {
    @Schema(description = "분실 신고 요약")
    data class ReportSummary(
        @field:Schema(description = "분실 신고 ID", example = "1")
        val id: Long,
        @field:Schema(description = "신고자 이름", example = "홍길동")
        val reporterName: String,
        @field:Schema(description = "신고자 학번 (현재는 항상 null)")
        val studentNumber: String? = null,
        @field:Schema(description = "택배 별칭", example = "운동화")
        val alias: String,
        @field:Schema(description = "운송장 번호", example = "123456789012")
        val invoiceNumber: String,
        @field:Schema(description = "분실 추정 시각", example = "2026-11-08T18:00:00")
        val lostEstimatedAt: LocalDateTime,
        @field:Schema(description = "진행 상태. RECEIVED(접수) / IN_PROGRESS(처리 중) / DONE(완료)", example = "RECEIVED")
        val progress: ReportProgress,
        @field:Schema(description = "연결된 채팅방 ID (현재는 항상 null)")
        val chatRoomId: Long? = null,
        @field:Schema(description = "접수 시각", example = "2026-11-09T14:30:00")
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
