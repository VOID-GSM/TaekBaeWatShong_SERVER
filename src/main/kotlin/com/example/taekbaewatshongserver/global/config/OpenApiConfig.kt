package com.example.taekbaewatshongserver.global.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import io.swagger.v3.oas.models.tags.Tag
import org.springdoc.core.customizers.GlobalOpenApiCustomizer
import org.springdoc.core.customizers.OperationCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun openAPI(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("택배왔숑 API")
                .version("v1")
                .description(API_DESCRIPTION),
        )
        // 프록시(HTTPS) 뒤에서도 Try it out이 현재 접속한 주소로 요청하도록 상대 경로를 쓴다.
        .servers(listOf(Server().url("/").description("현재 서버")))
        .components(
            Components().addSecuritySchemes(
                BEARER_AUTH,
                SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("로그인 API로 받은 `token` 값을 입력한다. (`Bearer ` 접두사는 자동으로 붙는다)"),
            ),
        )
        .addSecurityItem(SecurityRequirement().addList(BEARER_AUTH))
        .tags(
            listOf(
                Tag().name(ApiTags.OAUTH).description(
                    "Google·DataGSM 소셜 로그인 API. 브라우저 리다이렉트로 동작하므로 Swagger의 Try it out으로는 끝까지 진행할 수 없다.",
                ),
                Tag().name(ApiTags.EMAIL_AUTH).description("이메일·비밀번호 회원가입/로그인 API. 성공하면 바로 JWT를 발급한다."),
                Tag().name(ApiTags.USER).description("로그인한 사용자 본인 정보 API."),
                Tag().name(ApiTags.PARCEL).description(
                    "택배 등록·조회·도착 처리·구역 배정·수령 API. 상태는 PENDING(도착 예정) → ARRIVED(도착 완료) → CLAIMED(수령 완료) 순서로만 바뀐다.",
                ),
                Tag().name(ApiTags.REPORT).description("택배 분실 신고 API. 진행 상태는 RECEIVED(접수) / IN_PROGRESS(처리 중) / DONE(완료)."),
                Tag().name(ApiTags.NOTIFICATION).description(
                    "택배 알림 조회·읽음 처리와 푸시용 디바이스 토큰 등록 API. 알림은 택배 상태가 바뀔 때 서버가 자동으로 만든다.",
                ),
                Tag().name(ApiTags.CHAT).description(
                    "1:1 채팅 API. 택배기사(ADMIN) ↔ 학생/선생님은 항상 가능하고, 학생 ↔ 선생님은 양쪽 모두 DataGSM 계정일 때만 가능하다.",
                ),
                Tag().name(ApiTags.DRIVER_QR).description("택배기사 QR 발급·스캔 API. 모두 관리자(ADMIN) 전용."),
            ),
        )

    // 컨트롤러의 description을 여러 줄 문자열로 쓰면 들여쓰기가 마크다운 코드 블록으로 보이므로 제거한다.
    @Bean
    fun trimDescriptionCustomizer(): OperationCustomizer = OperationCustomizer { operation, _ ->
        operation.description = operation.description?.trimIndent()
        operation
    }

    // 로그인이 필요한 API는 토큰이 없으면 Spring Security가 /login으로 리다이렉트하므로 모든 해당 API에 302를 명시한다.
    @Bean
    fun loginRedirectResponseCustomizer(): GlobalOpenApiCustomizer = GlobalOpenApiCustomizer { openApi ->
        openApi.paths.values
            .flatMap { it.readOperations() }
            .filter { it.security == null }
            .forEach { operation ->
                operation.responses.addApiResponse(
                    "302",
                    ApiResponse().description("토큰이 없거나 만료·위조됨. 로그인 페이지(`Location: /login`)로 리다이렉트된다."),
                )
            }
    }

    companion object {
        const val BEARER_AUTH = "bearerAuth"

        private val API_DESCRIPTION =
            """
            택배왔숑 서버 API 문서입니다.

            ## 인증
            1. 로그인 API(이메일 로그인 또는 OAuth 로그인 후 `GET /auth/token`)로 JWT를 받습니다.
            2. 오른쪽 위 **Authorize** 버튼에 받은 `token` 값을 넣으면 이후 요청에 `Authorization: Bearer {token}` 헤더가 붙습니다.
            3. 토큰 유효 시간은 1시간입니다.

            자물쇠 아이콘이 없는 API는 토큰 없이 호출할 수 있습니다.

            ## 에러 응답
            에러 응답 본문은 두 가지 형식이 있습니다.

            - **`ErrorResponse`** `{ "message": "..." }`: 택배·분실 신고·알림·채팅·QR API의 비즈니스 에러와 요청 값 검증 실패
            - **`DefaultErrorResponse`** `{ "timestamp", "status", "error", "path" }`: Spring 기본 에러. 권한 부족(403), 이메일 인증 API의 검증 실패, 잘못된 JSON·enum 값 등. 이 형식에는 에러 사유 문구가 담기지 않습니다.

            ## 토큰이 없거나 만료된 경우
            로그인이 필요한 API를 토큰 없이(또는 만료·위조된 토큰으로) 호출하면 401이 아니라 **302 리다이렉트**(`Location: /login`)가 반환됩니다.
            `fetch`·`axios`는 리다이렉트를 자동으로 따라가 로그인 HTML 페이지(200)를 받게 되므로, 응답이 JSON이 아니면 토큰 만료로 보고 다시 로그인시키세요.
            로그인은 했지만 권한이 없는 경우(관리자 전용 API, 남의 데이터 접근)는 **403**입니다.

            ## 날짜 형식
            날짜·시간은 서버 시간 기준 `yyyy-MM-ddTHH:mm:ss` 형식의 문자열이며, 초 아래 소수점이 붙을 수 있습니다. (예: `2026-11-09T14:30:00.123456`)
            """.trimIndent()
    }
}

object ApiTags {
    const val OAUTH = "OAuth"
    const val EMAIL_AUTH = "Email Auth"
    const val USER = "User"
    const val PARCEL = "Parcel"
    const val REPORT = "Report"
    const val NOTIFICATION = "Notification"
    const val CHAT = "Chat"
    const val DRIVER_QR = "Driver QR"
}
