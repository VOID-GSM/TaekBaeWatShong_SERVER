package com.example.taekbaewatshongserver.global.security.oauth

import com.example.taekbaewatshongserver.domain.user.dto.TokenResponse
import com.example.taekbaewatshongserver.domain.user.entity.Role
import com.example.taekbaewatshongserver.global.config.ApiTags
import com.example.taekbaewatshongserver.global.exception.DefaultErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@Tag(name = ApiTags.OAUTH)
@SecurityRequirements
@RestController
class AuthController(
    private val oneTimeAuthCodeStore: OneTimeAuthCodeStore,
) {

    @Operation(
        summary = "소셜 로그인 시작 (학생·선생님 앱)",
        description = """
            앱(웹뷰/브라우저)에서 이 주소를 열면 Google 또는 DataGSM 로그인 화면으로 리다이렉트된다. `fetch`·`axios`로 호출하는 API가 아니다.

            **전체 흐름**
            1. `GET /auth/login?provider=...&role=...` → 302로 소셜 로그인 화면 이동
            2. 로그인 성공 시 서버가 사용자 앱의 리다이렉트 주소(`OAUTH2_REDIRECT_URI`)로 `?code={일회용 코드}`를 붙여 이동시킨다. 관리자 계정이면 관리자 앱 주소로 이동한다.
            3. 앱은 받은 `code`로 `GET /auth/token`을 호출해 JWT를 받는다.

            로그인에 실패하면 같은 리다이렉트 주소로 `?error={사유}`가 붙어 이동한다.

            **파라미터 규칙**
            - `provider=google`이면 `role`(STUDENT 또는 TEACHER)이 필수다. 처음 가입할 때의 역할로 쓰인다.
            - `provider=datagsm`이면 DataGSM 계정 유형으로 역할이 정해지므로 `role`을 생략해도 된다. 보낸다면 계정 유형과 같아야 하며, 다르면 `?error=`로 실패한다.
        """,
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "302",
            description = "소셜 로그인 화면(`/oauth2/authorization/{provider}`)으로 리다이렉트",
            headers = [Header(name = "Location", description = "이동할 주소", schema = Schema(type = "string", example = "/oauth2/authorization/google"))],
            content = [Content()],
        ),
        ApiResponse(
            responseCode = "400",
            description = "`provider`가 google·datagsm이 아님, 또는 Google 로그인인데 `role`이 STUDENT·TEACHER가 아님",
            content = [Content(schema = Schema(implementation = DefaultErrorResponse::class))],
        ),
    )
    @GetMapping("/auth/login")
    fun login(
        @Parameter(description = "가입 시 사용할 역할. Google 로그인이면 필수, DataGSM 로그인이면 무시해도 된다.", example = "STUDENT")
        @RequestParam(required = false)
        role: Role?,
        @Parameter(
            description = "로그인 수단 (대소문자 무시)",
            schema = Schema(type = "string", allowableValues = ["google", "datagsm"], defaultValue = PROVIDER_GOOGLE),
        )
        @RequestParam(defaultValue = PROVIDER_GOOGLE)
        provider: String,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ) {
        val normalizedProvider = validateProvider(provider)
        // datagsm 계정은 objectType(STUDENT/TEACHER)으로 역할을 결정하므로 role 파라미터가 없어도 된다.
        if (normalizedProvider != PROVIDER_DATAGSM && (role != Role.STUDENT && role != Role.TEACHER)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "role은 STUDENT 또는 TEACHER만 선택할 수 있습니다")
        }
        request.session.setAttribute(LOGIN_TYPE_SESSION_KEY, LoginType.CLIENT.name)
        if (role == Role.STUDENT || role == Role.TEACHER) {
            request.session.setAttribute(SIGNUP_ROLE_SESSION_KEY, role.name)
        }
        response.sendRedirect("/oauth2/authorization/$normalizedProvider")
    }

    @Operation(
        summary = "소셜 로그인 시작 (관리자 앱)",
        description = """
            관리자 앱용 소셜 로그인 시작 주소. 흐름은 `GET /auth/login`과 같다.

            - 서버 환경 변수 `ADMIN_EMAILS`에 등록된 이메일 계정만 관리자로 로그인된다.
            - 성공·실패 모두 관리자 앱의 리다이렉트 주소(`ADMIN_OAUTH2_REDIRECT_URI`)로 이동한다.
        """,
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "302",
            description = "소셜 로그인 화면(`/oauth2/authorization/{provider}`)으로 리다이렉트",
            headers = [Header(name = "Location", description = "이동할 주소", schema = Schema(type = "string", example = "/oauth2/authorization/google"))],
            content = [Content()],
        ),
        ApiResponse(
            responseCode = "400",
            description = "`provider`가 google·datagsm이 아님",
            content = [Content(schema = Schema(implementation = DefaultErrorResponse::class))],
        ),
    )
    @GetMapping("/auth/admin/login")
    fun adminLogin(
        @Parameter(
            description = "로그인 수단 (대소문자 무시)",
            schema = Schema(type = "string", allowableValues = ["google", "datagsm"], defaultValue = PROVIDER_GOOGLE),
        )
        @RequestParam(defaultValue = PROVIDER_GOOGLE)
        provider: String,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ) {
        val normalizedProvider = validateProvider(provider)
        request.session.setAttribute(LOGIN_TYPE_SESSION_KEY, LoginType.ADMIN.name)
        response.sendRedirect("/oauth2/authorization/$normalizedProvider")
    }

    private fun validateProvider(provider: String): String {
        val normalized = provider.lowercase()
        if (normalized != PROVIDER_GOOGLE && normalized != PROVIDER_DATAGSM) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "provider는 google 또는 datagsm만 선택할 수 있습니다")
        }
        return normalized
    }

    @Operation(
        summary = "일회용 코드를 JWT로 교환",
        description = """
            소셜 로그인 성공 후 리다이렉트 주소로 받은 `code`를 JWT로 바꾼다.

            - `code`는 발급 후 **30초** 동안만 유효하고 **한 번만** 쓸 수 있다. 같은 코드로 다시 요청하면 401.
        """,
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "교환 성공. 발급된 JWT를 반환한다."),
        ApiResponse(
            responseCode = "400",
            description = "`code` 파라미터 누락",
            content = [Content(schema = Schema(implementation = DefaultErrorResponse::class))],
        ),
        ApiResponse(
            responseCode = "401",
            description = "존재하지 않거나, 만료됐거나, 이미 사용한 코드",
            content = [Content(schema = Schema(implementation = DefaultErrorResponse::class))],
        ),
    )
    @GetMapping("/auth/token")
    fun exchangeToken(
        @Parameter(description = "리다이렉트 주소의 `code` 쿼리 값", example = "3f2b8c1e-5d7a-4e9b-9c0d-1a2b3c4d5e6f")
        @RequestParam
        code: String,
    ): TokenResponse {
        val token = oneTimeAuthCodeStore.consume(code)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않거나 만료된 code입니다")
        return TokenResponse(token)
    }

    enum class LoginType {
        CLIENT,
        ADMIN,
    }

    companion object {
        const val SIGNUP_ROLE_SESSION_KEY = "signup_role"
        const val LOGIN_TYPE_SESSION_KEY = "login_type"
        const val PROVIDER_GOOGLE = "google"
        const val PROVIDER_DATAGSM = "datagsm"
    }
}
