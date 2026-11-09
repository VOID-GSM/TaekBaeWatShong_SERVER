package com.example.taekbaewatshongserver.domain.report.dto.response

import com.example.taekbaewatshongserver.domain.report.entity.Report
import com.example.taekbaewatshongserver.domain.report.entity.ReportProgress
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "분실 신고 상세")
data class ReportDetailResponse(
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
    @field:Schema(description = "분실 상황 설명", example = "보관함에 택배가 없습니다.")
    val content: String,
    @field:Schema(description = "진행 상태. RECEIVED(접수) / IN_PROGRESS(처리 중) / DONE(완료)", example = "RECEIVED")
    val progress: ReportProgress,
    @field:Schema(description = "연결된 채팅방 ID (현재는 항상 null)")
    val chatRoomId: Long? = null,
    @field:Schema(description = "접수 시각", example = "2026-11-09T14:30:00")
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
