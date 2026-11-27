package com.example.taekbaewatshongserver.global.security.email

import com.example.taekbaewatshongserver.domain.user.dto.AdminSignUpRequest
import com.example.taekbaewatshongserver.domain.user.dto.EmailLoginRequest
import com.example.taekbaewatshongserver.domain.user.dto.SignUpRequest
import com.example.taekbaewatshongserver.domain.user.dto.TokenResponse
import com.example.taekbaewatshongserver.domain.user.entity.Role
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
            description = "가입되지 않은 이메일 / 비밀번호 불일치 / Google·DataGSM으로 가입한 계정",
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

            - 이미 ADMIN인 LOCAL 계정은 비밀번호가 일치하면 그대로 JWT를 발급한다. 초대 코드는 필요 없고, 보내도 소비되지 않는다.
            - 그 외에는 유효한 관리자 초대 코드(`inviteCode`)가 있어야 한다. 초대 코드는 첫 관리자 계정이 `POST /admin/invite-codes`로 발급한다.
              - 새 이메일이면 ADMIN 계정을 만든다.
              - 이미 가입된 LOCAL 계정이면 기존 비밀번호가 일치할 때 ADMIN으로 승격한다. 비밀번호와 이름은 바뀌지 않는다.
              - 성공하면 코드의 남은 인원이 1명 줄어든다. 발급할 때 정한 인원이 다 차거나 24시간이 지나면 더 쓸 수 없다.
            - 이미 가입된 이메일은 비밀번호·가입 경로(409/401)를 먼저 검사하고, 그다음 관리자 여부(403)를 검사한다.
            - Google·DataGSM 계정은 해당 소셜 로그인 경로를 사용해야 한다.
            - 비밀번호 규칙은 일반 회원가입과 같다.
            - 400 응답은 두 형식이 있다. 이메일·비밀번호·이름 검증 실패는 `DefaultErrorResponse`(사유 문구 없음), 초대 코드 오류는 `ErrorResponse`(`message` 포함)다.
        """,
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "가입 성공. 발급된 JWT를 반환한다."),
        ApiResponse(
            responseCode = "400",
            description = "이메일 형식 오류 / 비밀번호 길이 오류 / 이름이 비어 있음 (`DefaultErrorResponse`) · 초대 코드 형식 오류 / 없거나 만료되었거나 사용 인원이 다 찬 초대 코드 (`ErrorResponse`)",
            content = [
                Content(
                    schema = Schema(oneOf = [ErrorResponse::class, DefaultErrorResponse::class]),
                    examples = [
                        ExampleObject(name = "초대 코드 형식 오류", value = """{"message": "초대 코드 형식이 올바르지 않습니다."}"""),
                        ExampleObject(name = "유효하지 않은 초대 코드", value = """{"message": "유효하지 않은 초대 코드입니다. 만료되었거나 사용 인원이 다 찬 코드일 수 있습니다."}"""),
                    ],
                ),
            ],
        ),
        ApiResponse(
            responseCode = "401",
            description = "기존 LOCAL 계정의 비밀번호가 일치하지 않음",
            content = [Content(schema = Schema(implementation = DefaultErrorResponse::class))],
        ),
        ApiResponse(
            responseCode = "403",
            description = "ADMIN이 아니고 초대 코드도 보내지 않음",
            content = [Content(schema = Schema(implementation = DefaultErrorResponse::class))],
        ),
        ApiResponse(
            responseCode = "409",
            description = "Google·DataGSM으로 가입한 이메일 / 동시에 같은 이메일로 가입 요청이 들어와 이미 가입됨",
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
            관리자 앱용 이메일 로그인. 이메일·비밀번호가 맞고 이미 ADMIN인 LOCAL 계정만 로그인된다.

            - 이 API는 초대 코드를 받지 않는다. 초대 코드로는 가입(`POST /auth/email/admin/signup`)에서만 관리자로 승격된다.
            - Google·DataGSM 계정은 이메일 로그인을 사용할 수 없으며, 해당 소셜 로그인 경로를 사용해야 한다.
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
            description = "ADMIN이 아닌 계정",
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
