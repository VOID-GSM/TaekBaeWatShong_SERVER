package com.example.taekbaewatshongserver.global.security.oauth

import com.example.taekbaewatshongserver.domain.admin.service.AdminInviteCodeService
import com.example.taekbaewatshongserver.domain.user.entity.AuthProvider
import com.example.taekbaewatshongserver.domain.user.entity.Role
import com.example.taekbaewatshongserver.domain.user.entity.User
import com.example.taekbaewatshongserver.domain.user.repository.UserRepository
import com.example.taekbaewatshongserver.global.exception.AdminInviteCodeException
import com.example.taekbaewatshongserver.global.security.AdminBootstrap
import com.example.taekbaewatshongserver.global.security.UserPrincipal
import jakarta.servlet.http.HttpSession
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Service
class CustomOAuth2UserService(
    private val userRepository: UserRepository,
    private val adminInviteCodeService: AdminInviteCodeService,
    private val adminBootstrap: AdminBootstrap,
) : DefaultOAuth2UserService() {

    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val session = currentSession()
        // 어떤 실패 경로에서도 코드가 세션에 남지 않도록, 예외가 날 수 있는 작업보다 먼저 꺼내고 지운다.
        val inviteCode = session.getAttribute(AuthController.ADMIN_INVITE_CODE_SESSION_KEY) as String?
        session.removeAttribute(AuthController.ADMIN_INVITE_CODE_SESSION_KEY)

        val oAuth2User = super.loadUser(userRequest)
        val registrationId = userRequest.clientRegistration.registrationId
        val userInfo = when (registrationId) {
            AuthController.PROVIDER_DATAGSM -> DataGsmOAuth2UserInfo(oAuth2User.attributes)
            else -> GoogleOAuth2UserInfo(oAuth2User.attributes)
        }
        val provider = when (registrationId) {
            AuthController.PROVIDER_DATAGSM -> AuthProvider.DATAGSM
            else -> AuthProvider.GOOGLE
        }
        requireActiveAccount(userInfo)

        val loginType = session.getAttribute(AuthController.LOGIN_TYPE_SESSION_KEY) as String?
            ?: throw OAuth2AuthenticationException(
                OAuth2Error("login_type_required"),
                "/auth/login?role=STUDENT|TEACHER 또는 /auth/admin/login 으로 로그인해주세요",
            )

        val user = when (AuthController.LoginType.valueOf(loginType)) {
            AuthController.LoginType.ADMIN -> resolveAdminUser(userInfo, provider, inviteCode)
            AuthController.LoginType.CLIENT -> {
                val existing = userRepository.findByEmail(userInfo.email)
                if (existing != null) {
                    applyUserInfo(existing, userInfo)
                    userRepository.save(existing)
                } else {
                    userRepository.save(
                        User(
                            email = userInfo.email,
                            name = userInfo.name,
                            provider = provider,
                            providerId = userInfo.id,
                            role = resolveSignupRole(session, userInfo),
                            studentNumber = (userInfo as? DataGsmOAuth2UserInfo)?.studentNumber,
                        ),
                    )
                }
            }
        }

        session.removeAttribute(AuthController.LOGIN_TYPE_SESSION_KEY)
        return UserPrincipal(user, oAuth2User.attributes)
    }

    internal fun resolveAdminUser(userInfo: OAuth2UserInfo, provider: AuthProvider, inviteCode: String?): User {
        val existing = userRepository.findByEmail(userInfo.email)

        if (existing != null && existing.role == Role.ADMIN) {
            applyUserInfo(existing, userInfo)
            return userRepository.save(existing)
        }

        if (adminBootstrap.isBootstrapEmail(userInfo.email)) {
            rejectLocalAccount(existing)
            if (existing != null) {
                applyUserInfo(existing, userInfo)
                existing.role = Role.ADMIN
                return userRepository.save(existing)
            }
            return userRepository.save(newAdminUser(userInfo, provider))
        }

        if (inviteCode != null) {
            // consume보다 먼저 검사해야 거절된 시도가 코드 인원을 쓰지 않는다.
            rejectLocalAccount(existing)
            val candidate = existing?.also { applyUserInfo(it, userInfo) } ?: newAdminUser(userInfo, provider)
            // AdminInviteCodeException은 AuthenticationException이 아니라서 그대로 던지면 실패 핸들러를 거치지 않고 500이 된다.
            return try {
                adminInviteCodeService.consume(inviteCode, candidate)
            } catch (e: AdminInviteCodeException) {
                throw OAuth2AuthenticationException(OAuth2Error("invalid_invite_code"), e.message)
            }
        }

        throw OAuth2AuthenticationException(OAuth2Error("not_admin"), "관리자 계정이 아닙니다")
    }

    // 이메일 가입은 이메일 소유를 확인하지 않는다. 남이 먼저 만든 LOCAL 계정을 소셜 로그인으로 승격하면 그 비밀번호를 아는 사람이 관리자가 된다.
    private fun rejectLocalAccount(existing: User?) {
        if (existing != null && existing.provider == AuthProvider.LOCAL) {
            throw OAuth2AuthenticationException(
                OAuth2Error("local_account"),
                "이메일로 가입한 계정은 소셜 로그인으로 관리자 등록을 할 수 없습니다. 이메일 관리자 가입에서 초대 코드를 입력해주세요",
            )
        }
    }

    private fun newAdminUser(userInfo: OAuth2UserInfo, provider: AuthProvider) = User(
        email = userInfo.email,
        name = userInfo.name,
        provider = provider,
        providerId = userInfo.id,
        role = Role.ADMIN,
    )

    private fun resolveSignupRole(session: HttpSession, userInfo: OAuth2UserInfo): Role {
        val requestedRoleName = session.getAttribute(AuthController.SIGNUP_ROLE_SESSION_KEY) as String?
        session.removeAttribute(AuthController.SIGNUP_ROLE_SESSION_KEY)

        if (userInfo is DataGsmOAuth2UserInfo) {
            val resolvedRole = when (userInfo.objectType) {
                "STUDENT" -> Role.STUDENT
                "TEACHER" -> Role.TEACHER
                else -> throw OAuth2AuthenticationException(
                    OAuth2Error("unsupported_object_type"),
                    "DataGSM 계정 유형(${userInfo.objectType})을 지원하지 않습니다",
                )
            }
            if (requestedRoleName != null && requestedRoleName != resolvedRole.name) {
                throw OAuth2AuthenticationException(
                    OAuth2Error("role_mismatch"),
                    "요청한 role($requestedRoleName)과 DataGSM 계정 유형(${userInfo.objectType})이 일치하지 않습니다",
                )
            }
            return resolvedRole
        }

        val roleName = requestedRoleName
            ?: throw OAuth2AuthenticationException(
                OAuth2Error("role_required"),
                "회원가입을 위해 /auth/login?role=STUDENT 또는 /auth/login?role=TEACHER 로 로그인해주세요",
            )
        return Role.valueOf(roleName)
    }

    private fun applyUserInfo(user: User, userInfo: OAuth2UserInfo) {
        if (userInfo !is DataGsmOAuth2UserInfo || userInfo.hasKnownName) {
            user.name = userInfo.name
        }
        if (userInfo is DataGsmOAuth2UserInfo) {
            user.studentNumber = userInfo.studentNumber
        }
    }

    internal fun requireActiveAccount(userInfo: OAuth2UserInfo) {
        if (userInfo is DataGsmOAuth2UserInfo && userInfo.status != "ACTIVE") {
            throw OAuth2AuthenticationException(
                OAuth2Error("inactive_account"),
                "DataGSM 계정 상태(${userInfo.status})로는 로그인할 수 없습니다",
            )
        }
    }

    private fun currentSession(): HttpSession = (RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes).request.session
}
