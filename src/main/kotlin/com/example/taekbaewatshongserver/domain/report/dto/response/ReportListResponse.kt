package com.example.taekbaewatshongserver.domain.report.dto.response

import com.example.taekbaewatshongserver.domain.report.entity.Report
import com.example.taekbaewatshongserver.domain.report.entity.ReportProgress
import com.example.taekbaewatshongserver.domain.user.entity.Role
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
        @field:Schema(description = "신고자 역할. STUDENT(학생) / TEACHER(선생님) / ADMIN(관리자·택배기사)", example = "STUDENT")
        val reporterRole: Role,
        @field:Schema(description = "신고자 학번. 선생님·관리자와 Google로 가입한 학생처럼 학번이 없으면 null", example = "2401")
        val studentNumber: String? = null,
        @field:Schema(description = "택배 별칭", example = "운동화")
        val alias: String,
        @field:Schema(description = "택배사", example = "CJ대한통운")
        val deliveryCompany: String,
        @field:Schema(description = "운송장 번호", example = "123456789012")
        val invoiceNumber: String,
        @field:Schema(description = "분실 추정 시각", example = "2026-11-08T18:00:00")
        val lostEstimatedAt: LocalDateTime,
        @field:Schema(description = "분실 상황 설명", example = "보관함에 택배가 없습니다.")
        val content: String,
        @field:Schema(description = "진행 상태. RECEIVED(접수) / IN_PROGRESS(처리 중) / DONE(완료)", example = "RECEIVED")
        val progress: ReportProgress,
        @field:Schema(description = "이 신고로 시작한 1:1 채팅방 ID (`POST /report/{reportId}/chat` 전에는 null)", example = "3")
        val chatRoomId: Long? = null,
        @field:Schema(description = "접수 시각", example = "2026-11-09T14:30:00")
        val createdAt: LocalDateTime,
    ) {
        companion object {
            fun from(report: Report): ReportSummary = ReportSummary(
                id = report.id,
                reporterName = report.reporter.name,
                reporterRole = report.reporter.role,
                studentNumber = report.reporter.studentNumber,
                alias = report.parcel.alias,
                deliveryCompany = report.parcel.deliveryCompany,
                invoiceNumber = report.parcel.invoiceNumber,
                lostEstimatedAt = report.lostEstimatedAt,
                content = report.content,
                progress = report.progress,
                chatRoomId = report.chatRoom?.id,
                createdAt = report.createdAt,
            )
        }
    }
}
