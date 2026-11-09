package com.example.taekbaewatshongserver.domain.notification.controller

import com.example.taekbaewatshongserver.domain.notification.dto.request.DeviceTokenRegisterRequest
import com.example.taekbaewatshongserver.domain.notification.dto.response.DeviceTokenResponse
import com.example.taekbaewatshongserver.domain.notification.dto.response.NotificationListResponse
import com.example.taekbaewatshongserver.domain.notification.dto.response.NotificationReadResponse
import com.example.taekbaewatshongserver.domain.notification.service.DeviceTokenService
import com.example.taekbaewatshongserver.domain.notification.service.NotificationService
import com.example.taekbaewatshongserver.domain.user.entity.User
import com.example.taekbaewatshongserver.global.config.ApiTags
import com.example.taekbaewatshongserver.global.exception.DefaultErrorResponse
import com.example.taekbaewatshongserver.global.exception.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@Tag(name = ApiTags.NOTIFICATION)
@RestController
@RequestMapping("/notification")
class NotificationController(
    private val notificationService: NotificationService,
    private val deviceTokenService: DeviceTokenService,
) {

    @Operation(
        summary = "내 알림 목록 조회",
        description = """
            로그인한 사용자의 알림을 최신순으로 조회한다. 커서 기반 페이지네이션을 쓴다.

            - 첫 페이지는 `cursor` 없이 요청하고, 다음 페이지는 응답의 `nextCursor` 값을 `cursor`로 보낸다.
            - `nextCursor`가 `null`이면 마지막 페이지다.
            - `unreadCount`는 페이지와 관계없이 안 읽은 알림 전체 개수다.
        """,
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공"),
    )
    @GetMapping
    fun getNotifications(
        @AuthenticationPrincipal user: User,
        @Parameter(description = "이전 페이지 응답의 `nextCursor` (첫 페이지는 생략). 이 ID보다 오래된 알림을 조회한다.", example = "120")
        @RequestParam(required = false)
        cursor: Long?,
        @Parameter(description = "페이지 크기 (1~100, 기본 20). 범위를 벗어나면 가까운 값으로 맞춰진다.", example = "20")
        @RequestParam(required = false)
        size: Int?,
    ): ResponseEntity<NotificationListResponse> {
        val response = notificationService.getNotifications(user, cursor, size)
        return ResponseEntity.ok(response)
    }

    @Operation(
        summary = "알림 읽음 처리",
        description = """
            알림 한 건을 읽음 처리한다. 이미 읽은 알림이어도 성공으로 응답한다.

            - 본인의 알림만 처리할 수 있다.
            - 응답의 `unreadCount`로 배지 숫자를 바로 갱신할 수 있다.
        """,
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "읽음 처리 성공"),
        ApiResponse(
            responseCode = "403",
            description = "본인의 알림이 아님",
            content = [Content(schema = Schema(implementation = DefaultErrorResponse::class))],
        ),
        ApiResponse(
            responseCode = "404",
            description = "존재하지 않는 알림",
            content = [
                Content(
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(value = """{"message": "해당 알림을 찾을 수 없습니다."}""")],
                ),
            ],
        ),
    )
    @PatchMapping("/{notificationId}/read")
    fun markAsRead(
        @AuthenticationPrincipal user: User,
        @Parameter(description = "알림 ID", example = "1")
        @PathVariable
        notificationId: Long,
    ): ResponseEntity<NotificationReadResponse> {
        val response = notificationService.markAsRead(user, notificationId)
        return ResponseEntity.ok(response)
    }

    @Operation(
        summary = "디바이스 토큰 등록",
        description = """
            푸시 알림을 받을 기기의 토큰(FCM 등)을 등록한다. 앱 실행·로그인 시마다 호출해도 된다.

            - 이미 등록된 토큰이면 새로 만들지 않고, 소유자를 현재 사용자로·플랫폼을 요청 값으로 바꾼다. (기기에서 계정을 바꿔 로그인한 경우)
        """,
    )
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "등록 성공 (기존 토큰을 갱신한 경우에도 201)"),
        ApiResponse(
            responseCode = "400",
            description = "`platform`이 ANDROID / IOS / WEB 중 하나가 아님",
            content = [
                Content(
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(value = """{"message": "유효하지 않은 플랫폼입니다. (WINDOWS)"}""")],
                ),
            ],
        ),
        ApiResponse(
            responseCode = "409",
            description = "같은 토큰이 동시에 등록되는 중. 잠시 후 다시 요청하면 된다.",
            content = [
                Content(
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(value = """{"message": "이미 등록 처리 중인 디바이스 토큰입니다. 다시 시도해주세요."}""")],
                ),
            ],
        ),
    )
    @PostMapping("/token")
    fun registerDeviceToken(
        @AuthenticationPrincipal user: User,
        @RequestBody request: DeviceTokenRegisterRequest,
    ): ResponseEntity<DeviceTokenResponse> {
        val response = deviceTokenService.registerDeviceToken(user, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }
}
