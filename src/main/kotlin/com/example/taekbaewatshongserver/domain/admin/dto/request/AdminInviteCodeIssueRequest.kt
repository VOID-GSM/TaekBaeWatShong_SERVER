package com.example.taekbaewatshongserver.domain.admin.dto.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "관리자 초대 코드 발급 요청")
data class AdminInviteCodeIssueRequest(
    @field:Schema(
        description = "이 코드로 관리자 등록할 수 있는 인원 (1~20). 생략하면 1명이다.",
        example = "3",
        minimum = "1",
        maximum = "20",
        defaultValue = "1",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    )
    val maxUses: Int? = null,
)
