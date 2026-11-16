package com.example.taekbaewatshongserver.domain.report.dto.request

import com.example.taekbaewatshongserver.domain.report.entity.ReportProgress
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "분실 신고 진행 상태 변경 요청")
data class ReportProgressUpdateRequest(
    @field:Schema(description = "바꿀 진행 상태. RECEIVED(접수) / IN_PROGRESS(처리 중) / DONE(완료)", example = "IN_PROGRESS", requiredMode = Schema.RequiredMode.REQUIRED)
    val progress: ReportProgress,
)
