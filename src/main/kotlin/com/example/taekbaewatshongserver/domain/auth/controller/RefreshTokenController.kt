package com.example.taekbaewatshongserver.domain.auth.controller

import com.example.taekbaewatshongserver.domain.auth.dto.request.LogoutRequest
import com.example.taekbaewatshongserver.domain.auth.dto.request.TokenRefreshRequest
import com.example.taekbaewatshongserver.domain.auth.service.RefreshTokenService
import com.example.taekbaewatshongserver.domain.user.dto.TokenResponse
import com.example.taekbaewatshongserver.global.config.ApiTags
import com.example.taekbaewatshongserver.global.exception.DefaultErrorResponse
import com.example.taekbaewatshongserver.global.exception.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = ApiTags.TOKEN)
@SecurityRequirements
@RestController
@RequestMapping("/auth")
class RefreshTokenController(
    private val refreshTokenService: RefreshTokenService,
) {

    @Operation(
        summary = "토큰 재발급",
        description = """
            refresh token으로 새 액세스 토큰과 새 refresh token을 받는다. 토큰 없이 호출한다. (`Authorization` 헤더는 보내도 무시된다)

            **언제 호출하나**
            - 로그인이 필요한 API가 **302**(`Location: /login`, `fetch`·`axios`라면 JSON이 아닌 로그인 HTML 페이지) 또는 **401**을 돌려주면 액세스 토큰이 만료된 것으로 보고 이 API를 호출한다. 성공하면 새 `token`으로 원래 요청을 다시 보낸다.
            - 이 API가 401을 돌려주면 사유와 관계없이 로그인 화면으로 보낸다.

            **회전**
            - 호출할 때마다 새 `refreshToken`이 발급되고, 보낸 refresh token은 폐기된다. 앱은 응답의 `refreshToken`으로 **반드시 교체 저장**한다.
            - 새 refresh token의 만료는 발급 시점부터 30일이다(슬라이딩). 30일 동안 한 번도 재발급하지 않으면 다시 로그인해야 한다.

            **동시 재발급**
            - 방금 폐기된 refresh token이 10초 안에 다시 들어오면 정상 요청으로 보고 새 토큰을 발급한다. 그래도 앱은 재발급 요청을 한 번에 하나만 보내고(single-flight) 나머지 요청은 그 결과를 기다리게 하는 것을 권장한다.
            - 폐기된 지 10초가 지난 refresh token이 다시 들어오면 탈취로 보고, 그 로그인(같은 family)의 refresh token을 모두 폐기한다. 다른 기기의 로그인은 영향받지 않는다.
        """,
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "재발급 성공. 새 액세스 토큰(JWT)과 새 refresh token을 반환한다."),
        ApiResponse(
            responseCode = "400",
            description = "요청 본문이 JSON이 아니거나 `refreshToken` 필드가 없음",
            content = [Content(schema = Schema(implementation = DefaultErrorResponse::class))],
        ),
        ApiResponse(
            responseCode = "401",
            description = "형식이 틀렸거나, 없거나, 만료됐거나, 로그아웃으로 폐기된 refresh token / 이미 사용된 refresh token이 다시 들어옴(그 로그인 전체 폐기). 앱은 로그인 화면으로 보낸다.",
            content = [
                Content(
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [
                        ExampleObject(name = "유효하지 않거나 만료됨", value = """{"message": "유효하지 않거나 만료된 refresh token입니다. 다시 로그인해주세요."}"""),
                        ExampleObject(name = "재사용 감지", value = """{"message": "이미 사용된 refresh token입니다. 보안을 위해 다시 로그인해주세요."}"""),
                    ],
                ),
            ],
        ),
    )
    @PostMapping("/refresh")
    fun refresh(
        @RequestBody request: TokenRefreshRequest,
    ): ResponseEntity<TokenResponse> {
        val response = refreshTokenService.refresh(request.refreshToken)
        return ResponseEntity.ok(response)
    }

    @Operation(
        summary = "로그아웃 (refresh token 폐기)",
        description = """
            이 기기의 로그인을 끝낸다. 보낸 refresh token이 속한 로그인(family)의 refresh token을 모두 폐기한다. 토큰 없이 호출한다.

            - 멱등이다. 형식이 틀린 값, 모르는 값, 이미 폐기됐거나 만료된 refresh token이어도 똑같이 204로 응답한다. (로그아웃을 막지 않도록)
            - 이미 재발급으로 교체된 옛 refresh token을 보내도 그 로그인 전체가 폐기된다.
            - 다른 기기의 로그인은 영향받지 않는다.
            - 푸시 디바이스 토큰 해제(`DELETE /notification/token`)는 따로 호출한다. 둘의 순서는 상관없다.
            - 로그아웃한 뒤 이 refresh token으로 `POST /auth/refresh`를 호출하면 401이다.
            - 액세스 토큰은 만료(최대 1시간)될 때까지 유효하므로 앱에서 함께 지운다.
        """,
    )
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "로그아웃 처리됨 (폐기할 refresh token이 없어도 204)", content = [Content()]),
        ApiResponse(
            responseCode = "400",
            description = "요청 본문이 JSON이 아니거나 `refreshToken` 필드가 없음",
            content = [Content(schema = Schema(implementation = DefaultErrorResponse::class))],
        ),
    )
    @PostMapping("/logout")
    fun logout(
        @RequestBody request: LogoutRequest,
    ): ResponseEntity<Void> {
        refreshTokenService.logout(request.refreshToken)
        return ResponseEntity.noContent().build()
    }
}
