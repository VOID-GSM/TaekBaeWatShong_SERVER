package com.example.taekbaewatshongserver.global.security.oauth

import com.example.taekbaewatshongserver.domain.admin.service.AdminInviteCodeService
import com.example.taekbaewatshongserver.domain.user.dto.TokenResponse
import com.example.taekbaewatshongserver.domain.user.entity.Role
import com.example.taekbaewatshongserver.global.config.ApiTags
import com.example.taekbaewatshongserver.global.exception.AdminInviteCodeException
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
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.util.UriComponentsBuilder

@Tag(name = ApiTags.OAUTH)
@SecurityRequirements
@RestController
class AuthController(
    private val oneTimeAuthCodeStore: OneTimeAuthCodeStore,
    private val adminInviteCodeService: AdminInviteCodeService,
    @Value("\${app.oauth2.admin-redirect-uri}") private val adminRedirectUri: String,
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
        request.session.removeAttribute(ADMIN_INVITE_CODE_SESSION_KEY)
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

            **관리자 판정 규칙** (소셜 로그인 콜백에서 위에서부터 처음 맞는 규칙 하나만 적용)
            1. 이미 `ADMIN`인 계정 → 통과. `inviteCode`를 보냈어도 코드는 사용 처리되지 않는다.
            2. 부트스트랩 관리자 이메일(첫 관리자용 고정값)이고 **DB에 관리자가 한 명도 없음** → `ADMIN`으로 승격하고 통과. 관리자가 한 명이라도 있으면 이 규칙은 건너뛰고 3·4번으로 간다.
            3. 유효한 `inviteCode`를 보냄 → 계정을 `ADMIN`으로 만들거나 승격하고 코드를 사용 처리한다.
            4. 그 외 → `?error=관리자 계정이 아닙니다`

            2번이 적용될 때나 3번에서, 같은 이메일로 **이메일·비밀번호 가입(LOCAL)한 계정**이 이미 있으면 승격하지 않고 `?error=이메일로 가입한 계정은 소셜 로그인으로 관리자 등록을 할 수 없습니다. …`로 실패한다. 초대 코드는 사용 처리되지 않는다. 이 경우 `POST /auth/email/admin/signup`에 초대 코드를 넣어 등록한다.

            **초대 코드**
            - 초대 코드는 첫 관리자(부트스트랩 이메일) 계정만 `POST /admin/invite-codes`로 발급할 수 있다.
            - 처음 관리자로 등록할 때만 보낸다. 등록된 뒤에는 코드 없이 로그인한다.
            - 코드 하나는 발급 시 정한 인원(기본 1명, 최대 20명)까지 사용할 수 있고, 24시간 뒤 만료된다.
            - 시작 단계에서 코드 형식이 틀렸거나, 없거나, 사용 인원이 다 찼거나, 만료됐으면 소셜 로그인 화면으로 가지 않고 바로 관리자 앱 주소로 `?error={사유}`가 붙어 이동한다.
            - 소셜 로그인 중에 다른 사람이 남은 자리를 먼저 써 버리면 콜백에서 다시 확인해 `?error=`로 실패한다.

            성공·실패 모두 관리자 앱의 리다이렉트 주소(`ADMIN_OAUTH2_REDIRECT_URI`)로 이동한다.
        """,
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "302",
            description = "소셜 로그인 화면(`/oauth2/authorization/{provider}`)으로 리다이렉트. " +
                "`inviteCode` 확인에 실패하면 관리자 앱 주소(`ADMIN_OAUTH2_REDIRECT_URI`)에 `?error={사유}`를 붙여 리다이렉트",
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
        @Parameter(description = "관리자 초대 코드. 처음 관리자로 등록할 때만 보낸다. 하이픈·공백·대소문자는 무시한다.", example = "K7Q2-M9XA")
        @RequestParam(required = false)
        inviteCode: String?,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ) {
        val normalizedProvider = validateProvider(provider)
        // 지난 시도에서 남은 코드가 이번 로그인에 섞이지 않도록 항상 먼저 지운다.
        request.session.removeAttribute(ADMIN_INVITE_CODE_SESSION_KEY)

        val normalizedInviteCode = if (inviteCode.isNullOrBlank()) {
            null
        } else {
            try {
                adminInviteCodeService.validate(inviteCode)
            } catch (e: AdminInviteCodeException) {
                val targetUrl = UriComponentsBuilder.fromUriString(adminRedirectUri)
                    .queryParam("error", e.message)
                    .build()
                    .encode()
                    .toUriString()
                response.sendRedirect(targetUrl)
                return
            }
        }

        request.session.setAttribute(LOGIN_TYPE_SESSION_KEY, LoginType.ADMIN.name)
        if (normalizedInviteCode != null) {
            request.session.setAttribute(ADMIN_INVITE_CODE_SESSION_KEY, normalizedInviteCode)
        }
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
        const val ADMIN_INVITE_CODE_SESSION_KEY = "admin_invite_code"
        const val PROVIDER_GOOGLE = "google"
        const val PROVIDER_DATAGSM = "datagsm"
    }
}
