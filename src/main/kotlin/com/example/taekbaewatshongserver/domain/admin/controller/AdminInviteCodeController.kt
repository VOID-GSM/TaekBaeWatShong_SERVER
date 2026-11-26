package com.example.taekbaewatshongserver.domain.admin.controller

import com.example.taekbaewatshongserver.domain.admin.dto.request.AdminInviteCodeIssueRequest
import com.example.taekbaewatshongserver.domain.admin.dto.response.AdminInviteCodeIssueResponse
import com.example.taekbaewatshongserver.domain.admin.service.AdminInviteCodeService
import com.example.taekbaewatshongserver.domain.user.entity.User
import com.example.taekbaewatshongserver.global.config.ApiTags
import com.example.taekbaewatshongserver.global.exception.DefaultErrorResponse
import com.example.taekbaewatshongserver.global.exception.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = ApiTags.ADMIN_INVITE)
@RestController
@RequestMapping("/admin/invite-codes")
class AdminInviteCodeController(
    private val adminInviteCodeService: AdminInviteCodeService,
) {

    @Operation(
        summary = "관리자 초대 코드 발급 (관리자)",
        description = """
            새 관리자를 등록할 때 쓸 초대 코드를 발급한다.

            - 코드 하나를 발급할 때 정한 인원(기본 1명, 최대 20명)까지 쓸 수 있고, 24시간 뒤 만료된다.
            - 요청 본문은 선택이다. 본문이 없거나 `maxUses`가 없으면 1명이다. 유효 기간은 바꿀 수 없다.
            - 호출할 때마다 새 코드가 만들어진다. 이전에 발급한 코드는 그대로 유효하다.
            - 코드 원문은 이 응답에서만 볼 수 있다. 서버에는 해시만 저장되므로 다시 조회할 수 없다.
            - 받은 사람은 관리자 소셜 로그인(`GET /auth/admin/login?inviteCode=`) 또는 관리자 이메일 가입(`POST /auth/email/admin/signup`의 `inviteCode`)에 이 코드를 넣는다. 하이픈·공백·대소문자는 무시된다.
            - 이미 ADMIN인 계정이 로그인할 때는 코드를 보내도 인원이 차감되지 않는다.
        """,
    )
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "발급 성공"),
        ApiResponse(
            responseCode = "400",
            description = "`maxUses`가 1~20 범위를 벗어남",
            content = [
                Content(
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(value = """{"message": "초대 인원은 1명 이상 20명 이하로 지정해주세요."}""")],
                ),
            ],
        ),
        ApiResponse(
            responseCode = "403",
            description = "관리자가 아님",
            content = [Content(schema = Schema(implementation = DefaultErrorResponse::class))],
        ),
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    fun issue(
        @AuthenticationPrincipal user: User,
        @RequestBody(required = false) request: AdminInviteCodeIssueRequest?,
    ): ResponseEntity<AdminInviteCodeIssueResponse> {
        val response = adminInviteCodeService.issue(user, request?.maxUses ?: AdminInviteCodeService.DEFAULT_MAX_USES)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }
}
