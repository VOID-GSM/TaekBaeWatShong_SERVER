package com.example.taekbaewatshongserver.global.security.oauth

import com.example.taekbaewatshongserver.domain.admin.service.AdminInviteCodeService
import com.example.taekbaewatshongserver.domain.user.entity.AuthProvider
import com.example.taekbaewatshongserver.domain.user.entity.Role
import com.example.taekbaewatshongserver.domain.user.entity.User
import com.example.taekbaewatshongserver.domain.user.repository.UserRepository
import com.example.taekbaewatshongserver.global.exception.AdminInviteCodeException
import com.example.taekbaewatshongserver.global.security.UserPrincipal
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.http.ResponseEntity
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.web.client.RestOperations
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CustomOAuth2UserServiceTest {

    // Kotlin의 non-null 파라미터와 Mockito 매처(any())가 충돌하지 않도록, 매처 대신 기본 Answer로 동작을 정한다.
    private val usersByEmail = mutableMapOf<String, User>()
    private val savedUsers = mutableListOf<User>()
    private val userRepository: UserRepository = mock(UserRepository::class.java) { invocation ->
        when (invocation.method.name) {
            "findByEmail" -> usersByEmail[invocation.getArgument(0)]
            "save" -> invocation.getArgument<User>(0).also { savedUsers += it }
            else -> null
        }
    }

    private val consumeCalls = mutableListOf<Pair<String, User>>()
    private var consumeFailure: AdminInviteCodeException? = null
    private val adminInviteCodeService: AdminInviteCodeService = mock(AdminInviteCodeService::class.java) { invocation ->
        when (invocation.method.name) {
            "consume" -> {
                val code = invocation.getArgument<String>(0)
                val user = invocation.getArgument<User>(1)
                consumeCalls += code to user
                consumeFailure?.let { throw it }
                user.role = Role.ADMIN
                user
            }
            else -> null
        }
    }

    private val service = CustomOAuth2UserService(
        userRepository = userRepository,
        adminInviteCodeService = adminInviteCodeService,
        bootstrapEmail = " TeamVoid0107@Gmail.com ",
    )

    @AfterEach
    fun tearDown() {
        RequestContextHolder.resetRequestAttributes()
    }

    @Test
    fun `status가 ACTIVE가 아닌 DataGSM 계정은 로그인이 거부된다`() {
        val userInfo = dataGsmUserInfo(status = "SUSPENDED")

        val exception = assertFailsWith<OAuth2AuthenticationException> {
            service.requireActiveAccount(userInfo)
        }
        assert(exception.message?.contains("SUSPENDED") == true)
    }

    @Test
    fun `status가 ACTIVE인 DataGSM 계정은 통과한다`() {
        assertDoesNotThrow { service.requireActiveAccount(dataGsmUserInfo(status = "ACTIVE")) }
    }

    @Test
    fun `구글 계정은 status 검증 대상이 아니다`() {
        assertDoesNotThrow { service.requireActiveAccount(googleUserInfo("user@gmail.com")) }
    }

    @Test
    fun `이미 ADMIN인 계정은 통과하고 초대 코드를 소비하지 않는다`() {
        val admin = existingUser("admin@gmail.com", Role.ADMIN)

        val result = service.resolveAdminUser(googleUserInfo("admin@gmail.com"), AuthProvider.GOOGLE, "K7Q2M9XA")

        assertSame(admin, result)
        assertEquals(Role.ADMIN, result.role)
        assertTrue(consumeCalls.isEmpty())
    }

    @Test
    fun `부트스트랩 이메일은 대소문자와 공백을 무시하고 새 ADMIN 계정으로 저장된다`() {
        val result = service.resolveAdminUser(googleUserInfo("  TEAMVOID0107@gmail.COM "), AuthProvider.GOOGLE, null)

        assertEquals(Role.ADMIN, result.role)
        assertEquals(listOf(result), savedUsers)
        assertTrue(consumeCalls.isEmpty())
    }

    @Test
    fun `부트스트랩 이메일의 기존 계정은 ADMIN으로 승격되고 초대 코드를 소비하지 않는다`() {
        val student = existingUser("teamvoid0107@gmail.com", Role.STUDENT)

        val result = service.resolveAdminUser(googleUserInfo("teamvoid0107@gmail.com"), AuthProvider.GOOGLE, "K7Q2M9XA")

        assertSame(student, result)
        assertEquals(Role.ADMIN, result.role)
        assertTrue(consumeCalls.isEmpty())
    }

    @Test
    fun `유효한 초대 코드가 있으면 새 계정을 저장하지 않은 채로 consume에 넘기고 반환값을 쓴다`() {
        val result = service.resolveAdminUser(googleUserInfo("new@gmail.com"), AuthProvider.GOOGLE, "K7Q2M9XA")

        assertEquals(1, consumeCalls.size)
        val (code, candidate) = consumeCalls.single()
        assertEquals("K7Q2M9XA", code)
        assertEquals("new@gmail.com", candidate.email)
        assertEquals(AuthProvider.GOOGLE, candidate.provider)
        assertNull(candidate.id)
        assertSame(candidate, result)
        assertEquals(Role.ADMIN, result.role)
        assertTrue(savedUsers.isEmpty(), "새 계정 저장은 consume 트랜잭션 안에서만 일어나야 한다")
    }

    @Test
    fun `유효한 초대 코드가 있으면 기존 STUDENT 계정이 승격된다`() {
        val student = existingUser("student@gmail.com", Role.STUDENT)

        val result = service.resolveAdminUser(googleUserInfo("student@gmail.com"), AuthProvider.GOOGLE, "K7Q2M9XA")

        assertSame(student, consumeCalls.single().second)
        assertSame(student, result)
        assertEquals(Role.ADMIN, result.role)
    }

    @Test
    fun `인원이 다 찼거나 만료된 초대 코드는 invalid_invite_code로 거부된다`() {
        consumeFailure = AdminInviteCodeException.Invalid()

        val exception = assertFailsWith<OAuth2AuthenticationException> {
            service.resolveAdminUser(googleUserInfo("new@gmail.com"), AuthProvider.GOOGLE, "K7Q2M9XA")
        }

        assertEquals("invalid_invite_code", exception.error.errorCode)
        assertEquals(AdminInviteCodeException.Invalid().message, exception.message)
    }

    @Test
    fun `형식이 틀린 초대 코드도 invalid_invite_code로 거부된다`() {
        consumeFailure = AdminInviteCodeException.InvalidFormat()

        val exception = assertFailsWith<OAuth2AuthenticationException> {
            service.resolveAdminUser(googleUserInfo("new@gmail.com"), AuthProvider.GOOGLE, "BAD")
        }

        assertEquals("invalid_invite_code", exception.error.errorCode)
    }

    @Test
    fun `부트스트랩 이메일이라도 기존 LOCAL 계정이면 local_account로 거부되고 저장되지 않는다`() {
        val local = existingUser("teamvoid0107@gmail.com", Role.TEACHER, AuthProvider.LOCAL)

        val exception = assertFailsWith<OAuth2AuthenticationException> {
            service.resolveAdminUser(googleUserInfo("teamvoid0107@gmail.com"), AuthProvider.GOOGLE, null)
        }

        assertEquals("local_account", exception.error.errorCode)
        assertEquals(Role.TEACHER, local.role)
        assertTrue(savedUsers.isEmpty())
        assertTrue(consumeCalls.isEmpty())
    }

    @Test
    fun `초대 코드가 있어도 기존 LOCAL 계정이면 local_account로 거부되고 consume을 호출하지 않는다`() {
        val local = existingUser("invitee@gmail.com", Role.STUDENT, AuthProvider.LOCAL)

        val exception = assertFailsWith<OAuth2AuthenticationException> {
            service.resolveAdminUser(googleUserInfo("invitee@gmail.com"), AuthProvider.GOOGLE, "K7Q2M9XA")
        }

        assertEquals("local_account", exception.error.errorCode)
        assertEquals(Role.STUDENT, local.role)
        assertTrue(consumeCalls.isEmpty())
        assertTrue(savedUsers.isEmpty())
    }

    @Test
    fun `기존 GOOGLE 계정은 DataGSM 관리자 로그인에서 초대 코드로 승격될 수 있다`() {
        val google = existingUser("student@gsm.hs.kr", Role.STUDENT, AuthProvider.GOOGLE)

        val result = service.resolveAdminUser(dataGsmUserInfo(status = "ACTIVE"), AuthProvider.DATAGSM, "K7Q2M9XA")

        assertSame(google, consumeCalls.single().second)
        assertEquals(Role.ADMIN, result.role)
    }

    @Test
    fun `초대 코드가 없는 비관리자는 not_admin으로 거부된다`() {
        existingUser("student@gmail.com", Role.STUDENT)

        val exception = assertFailsWith<OAuth2AuthenticationException> {
            service.resolveAdminUser(googleUserInfo("student@gmail.com"), AuthProvider.GOOGLE, null)
        }

        assertEquals("not_admin", exception.error.errorCode)
        assertTrue(consumeCalls.isEmpty())
        assertTrue(savedUsers.isEmpty())
    }

    @Test
    fun `초대 코드가 없는 새 계정은 not_admin으로 거부되고 저장되지 않는다`() {
        val exception = assertFailsWith<OAuth2AuthenticationException> {
            service.resolveAdminUser(googleUserInfo("new@gmail.com"), AuthProvider.GOOGLE, null)
        }

        assertEquals("not_admin", exception.error.errorCode)
        assertTrue(savedUsers.isEmpty())
    }

    @Test
    fun `비활성 DataGSM 계정으로 실패해도 세션의 초대 코드는 지워지고 소비되지 않는다`() {
        val request = MockHttpServletRequest()
        request.session!!.setAttribute(AuthController.LOGIN_TYPE_SESSION_KEY, AuthController.LoginType.ADMIN.name)
        request.session!!.setAttribute(AuthController.ADMIN_INVITE_CODE_SESSION_KEY, "K7Q2M9XA")
        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))
        service.setRestOperations(userInfoEndpointReturning(dataGsmAttributes(status = "SUSPENDED")))

        val exception = assertFailsWith<OAuth2AuthenticationException> {
            service.loadUser(dataGsmUserRequest())
        }

        assertEquals("inactive_account", exception.error.errorCode)
        assertNull(request.session!!.getAttribute(AuthController.ADMIN_INVITE_CODE_SESSION_KEY))
        assertTrue(consumeCalls.isEmpty())
    }

    @Test
    fun `관리자 로그인 성공 시 세션의 초대 코드로 승격되고 세션 값은 지워진다`() {
        val request = MockHttpServletRequest()
        request.session!!.setAttribute(AuthController.LOGIN_TYPE_SESSION_KEY, AuthController.LoginType.ADMIN.name)
        request.session!!.setAttribute(AuthController.ADMIN_INVITE_CODE_SESSION_KEY, "K7Q2M9XA")
        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))
        service.setRestOperations(userInfoEndpointReturning(dataGsmAttributes(status = "ACTIVE")))

        val principal = service.loadUser(dataGsmUserRequest())

        assertEquals("K7Q2M9XA", consumeCalls.single().first)
        assertEquals(AuthProvider.DATAGSM, consumeCalls.single().second.provider)
        assertEquals(Role.ADMIN, (principal as UserPrincipal).user.role)
        assertNull(request.session!!.getAttribute(AuthController.ADMIN_INVITE_CODE_SESSION_KEY))
        assertNull(request.session!!.getAttribute(AuthController.LOGIN_TYPE_SESSION_KEY))
    }

    @Test
    fun `학생 로그인에서는 세션에 초대 코드가 남아 있어도 무시하고 지운다`() {
        existingUser("student@gsm.hs.kr", Role.STUDENT)
        val request = MockHttpServletRequest()
        request.session!!.setAttribute(AuthController.LOGIN_TYPE_SESSION_KEY, AuthController.LoginType.CLIENT.name)
        request.session!!.setAttribute(AuthController.ADMIN_INVITE_CODE_SESSION_KEY, "K7Q2M9XA")
        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))
        service.setRestOperations(userInfoEndpointReturning(dataGsmAttributes(status = "ACTIVE")))

        val principal = service.loadUser(dataGsmUserRequest())

        assertEquals(Role.STUDENT, (principal as UserPrincipal).user.role)
        assertTrue(consumeCalls.isEmpty())
        assertNull(request.session!!.getAttribute(AuthController.ADMIN_INVITE_CODE_SESSION_KEY))
    }

    private fun existingUser(email: String, role: Role, provider: AuthProvider = AuthProvider.GOOGLE): User = User(
        id = (usersByEmail.size + 1).toLong(),
        email = email,
        name = "기존",
        provider = provider,
        providerId = if (provider == AuthProvider.LOCAL) null else "p-$email",
        password = if (provider == AuthProvider.LOCAL) "{bcrypt}attacker" else null,
        role = role,
    ).also { usersByEmail[email] = it }

    private fun googleUserInfo(email: String) = GoogleOAuth2UserInfo(
        mapOf("sub" to "1", "email" to email, "name" to "홍길동"),
    )

    private fun dataGsmUserInfo(status: String) = DataGsmOAuth2UserInfo(dataGsmAttributes(status))

    private fun dataGsmAttributes(status: String): Map<String, Any> = mapOf(
        "id" to "1",
        "email" to "student@gsm.hs.kr",
        "status" to status,
        "objectType" to "STUDENT",
        "student" to mapOf("name" to "홍길동"),
    )

    private fun userInfoEndpointReturning(attributes: Map<String, Any>): RestOperations = mock(RestOperations::class.java) { invocation ->
        if (invocation.method.name == "exchange") ResponseEntity.ok(attributes) else null
    }

    private fun dataGsmUserRequest(): OAuth2UserRequest {
        val registration = ClientRegistration.withRegistrationId(AuthController.PROVIDER_DATAGSM)
            .clientId("client")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("http://localhost/login/oauth2/code/datagsm")
            .authorizationUri("http://localhost/authorize")
            .tokenUri("http://localhost/token")
            .userInfoUri("http://localhost/userinfo")
            .userNameAttributeName("id")
            .build()
        val accessToken = OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            "token",
            Instant.now(),
            Instant.now().plusSeconds(60),
        )
        return OAuth2UserRequest(registration, accessToken)
    }
}
