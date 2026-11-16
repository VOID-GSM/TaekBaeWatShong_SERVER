package com.example.taekbaewatshongserver.global.security.email

import com.example.taekbaewatshongserver.domain.user.dto.AdminSignUpRequest
import com.example.taekbaewatshongserver.domain.user.dto.EmailLoginRequest
import com.example.taekbaewatshongserver.domain.user.dto.SignUpRequest
import com.example.taekbaewatshongserver.domain.user.dto.TokenResponse
import com.example.taekbaewatshongserver.domain.user.entity.Role
import com.example.taekbaewatshongserver.global.config.ApiTags
import com.example.taekbaewatshongserver.global.exception.DefaultErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
private val STUDENT_NUMBER_REGEX = Regex("^\\d{4}$")

@Tag(name = ApiTags.EMAIL_AUTH)
@SecurityRequirements
@RestController
@RequestMapping("/auth/email")
class EmailAuthController(
    private val emailAuthService: EmailAuthService,
) {

    @Operation(
        summary = "회원가입 (학생·선생님)",
        description = """
            이메일·비밀번호로 학생 또는 선생님 계정을 만들고 바로 JWT를 발급한다.

            - `role`은 STUDENT 또는 TEACHER만 가능하다.
            - STUDENT는 `studentNumber`(4자리 숫자)가 필수다. TEACHER가 보낸 `studentNumber`는 저장되지 않는다.
            - 비밀번호는 8자 이상, UTF-8 기준 72바이트 이하여야 한다.
            - 에러 응답에는 사유 문구가 담기지 않으므로 클라이언트에서 같은 조건을 먼저 검사하는 것을 권장한다.
        """,
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "가입 성공. 발급된 JWT를 반환한다."),
        ApiResponse(
            responseCode = "400",
            description = "이메일 형식 오류 / 비밀번호 길이 오류 / 이름이 비어 있음 / 학번이 4자리 숫자가 아님 / `role`이 STUDENT·TEACHER가 아님",
            content = [Content(schema = Schema(implementation = DefaultErrorResponse::class))],
        ),
        ApiResponse(
            responseCode = "409",
            description = "이미 가입된 이메일 (소셜 로그인으로 가입한 이메일 포함)",
            content = [Content(schema = Schema(implementation = DefaultErrorResponse::class))],
        ),
    )
    @PostMapping("/signup")
    fun signUp(@RequestBody request: SignUpRequest): TokenResponse {
        validateEmail(request.email)
        validatePassword(request.password)
        if (request.name.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "name은 비어있을 수 없습니다")
        }
        if (request.role == Role.STUDENT && !STUDENT_NUMBER_REGEX.matches(request.studentNumber ?: "")) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "학번은 4자리 숫자여야 합니다")
        }
        return emailAuthService.signUp(request)
    }

    @Operation(
        summary = "로그인 (학생·선생님)",
        description = """
            이메일·비밀번호로 로그인하고 JWT를 발급한다.

            - 이메일 회원가입으로 만든 계정만 로그인할 수 있다. Google·DataGSM으로 가입한 계정은 401.
            - 역할을 검사하지 않으므로 관리자 계정도 이 API로 로그인된다.
        """,
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "로그인 성공. 발급된 JWT를 반환한다."),
        ApiResponse(
            responseCode = "400",
            description = "이메일 형식 오류",
            content = [Content(schema = Schema(implementation = DefaultErrorResponse::class))],
        ),
        ApiResponse(
            responseCode = "401",
            description = "가입되지 않은 이메일 / 비밀번호 불일치 / 소셜 로그인으로 가입한 계정",
            content = [Content(schema = Schema(implementation = DefaultErrorResponse::class))],
        ),
    )
    @PostMapping("/login")
    fun login(@RequestBody request: EmailLoginRequest): TokenResponse {
        validateEmail(request.email)
        return emailAuthService.login(request)
    }

    @Operation(
        summary = "관리자 회원가입",
        description = """
            관리자(ADMIN) 계정을 만들고 바로 JWT를 발급한다.

            - 서버 환경 변수 `ADMIN_EMAILS`에 등록된 이메일만 가입할 수 있다. (대소문자 무시)
            - 비밀번호 규칙은 일반 회원가입과 같다.
        """,
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "가입 성공. 발급된 JWT를 반환한다."),
        ApiResponse(
            responseCode = "400",
            description = "이메일 형식 오류 / 비밀번호 길이 오류 / 이름이 비어 있음",
            content = [Content(schema = Schema(implementation = DefaultErrorResponse::class))],
        ),
        ApiResponse(
            responseCode = "403",
            description = "관리자 이메일 목록에 없는 이메일",
            content = [Content(schema = Schema(implementation = DefaultErrorResponse::class))],
        ),
        ApiResponse(
            responseCode = "409",
            description = "이미 가입된 이메일",
            content = [Content(schema = Schema(implementation = DefaultErrorResponse::class))],
        ),
    )
    @PostMapping("/admin/signup")
    fun adminSignUp(@RequestBody request: AdminSignUpRequest): TokenResponse {
        validateEmail(request.email)
        validatePassword(request.password)
        if (request.name.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "name은 비어있을 수 없습니다")
        }
        return emailAuthService.adminSignUp(request)
    }

    @Operation(
        summary = "관리자 로그인",
        description = """
            관리자 앱용 로그인. 이메일·비밀번호가 맞아도 관리자(ADMIN) 계정이 아니면 거절한다.

            - 이메일 회원가입으로 만든 계정만 로그인할 수 있다.
        """,
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "로그인 성공. 발급된 JWT를 반환한다."),
        ApiResponse(
            responseCode = "400",
            description = "이메일 형식 오류",
            content = [Content(schema = Schema(implementation = DefaultErrorResponse::class))],
        ),
        ApiResponse(
            responseCode = "401",
            description = "가입되지 않은 이메일 / 비밀번호 불일치 / 소셜 로그인으로 가입한 계정",
            content = [Content(schema = Schema(implementation = DefaultErrorResponse::class))],
        ),
        ApiResponse(
            responseCode = "403",
            description = "관리자 계정이 아님",
            content = [Content(schema = Schema(implementation = DefaultErrorResponse::class))],
        ),
    )
    @PostMapping("/admin/login")
    fun adminLogin(@RequestBody request: EmailLoginRequest): TokenResponse {
        validateEmail(request.email)
        return emailAuthService.adminLogin(request)
    }

    private fun validateEmail(email: String) {
        if (!EMAIL_REGEX.matches(email)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "올바른 이메일 형식이 아닙니다")
        }
    }

    private fun validatePassword(password: String) {
        if (password.length < 8) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "비밀번호는 8자 이상이어야 합니다")
        }
        if (password.toByteArray(Charsets.UTF_8).size > 72) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "비밀번호는 72바이트를 초과할 수 없습니다")
        }
    }
}
