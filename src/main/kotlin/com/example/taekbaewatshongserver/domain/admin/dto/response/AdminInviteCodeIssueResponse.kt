package com.example.taekbaewatshongserver.domain.admin.dto.response

import com.example.taekbaewatshongserver.domain.admin.entity.AdminInviteCode
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "발급된 관리자 초대 코드")
data class AdminInviteCodeIssueResponse(
    @field:Schema(description = "초대 코드 원문 (`XXXX-XXXX`). 이 응답에서만 볼 수 있으며 다시 조회할 수 없다.", example = "K7Q2-M9XA")
    val code: String,
    @field:Schema(description = "만료 시각 (발급 시각 + 24시간, 서버 시간)", example = "2026-11-27T12:00:00")
    val expiresAt: LocalDateTime,
    @field:Schema(description = "이 코드로 관리자 등록할 수 있는 인원", example = "3")
    val maxUses: Int,
) {
    companion object {
        fun from(inviteCode: AdminInviteCode, code: String): AdminInviteCodeIssueResponse = AdminInviteCodeIssueResponse(
            code = code,
            expiresAt = inviteCode.expiresAt,
            maxUses = inviteCode.maxUses,
        )
    }
}
