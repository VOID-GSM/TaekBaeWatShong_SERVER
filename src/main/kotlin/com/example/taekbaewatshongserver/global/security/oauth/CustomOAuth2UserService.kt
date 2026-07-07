package com.example.taekbaewatshongserver.global.security.oauth

import com.example.taekbaewatshongserver.domain.user.entity.AuthProvider
import com.example.taekbaewatshongserver.domain.user.entity.Role
import com.example.taekbaewatshongserver.domain.user.entity.User
import com.example.taekbaewatshongserver.domain.user.repository.UserRepository
import com.example.taekbaewatshongserver.global.security.UserPrincipal
import jakarta.servlet.http.HttpSession
import org.springframework.beans.factory.annotation.Value
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
    @Value("#{'\${app.admin.emails:}'.split(',')}") adminEmails: List<String>,
) : DefaultOAuth2UserService() {

    private val adminEmails: Set<String> = adminEmails.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()

    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val oAuth2User = super.loadUser(userRequest)
        val userInfo = GoogleOAuth2UserInfo(oAuth2User.attributes)
        val isAdminEmail = userInfo.email.lowercase() in adminEmails
        val session = currentSession()

        val loginType = session.getAttribute(AuthController.LOGIN_TYPE_SESSION_KEY) as String?
            ?: throw OAuth2AuthenticationException(
                OAuth2Error("login_type_required"),
                "/auth/login?role=STUDENT|TEACHER 또는 /auth/admin/login 으로 로그인해주세요",
            )

        val existing = userRepository.findByEmail(userInfo.email)

        val user = when (AuthController.LoginType.valueOf(loginType)) {
            AuthController.LoginType.ADMIN -> {
                if (!isAdminEmail) {
                    throw OAuth2AuthenticationException(OAuth2Error("not_admin"), "관리자 계정이 아닙니다")
                }
                if (existing != null) {
                    existing.name = userInfo.name
                    existing.role = Role.ADMIN
                    userRepository.save(existing)
                } else {
                    userRepository.save(
                        User(
                            email = userInfo.email,
                            name = userInfo.name,
                            provider = AuthProvider.GOOGLE,
                            providerId = userInfo.id,
                            role = Role.ADMIN,
                        ),
                    )
                }
            }
            AuthController.LoginType.CLIENT -> {
                if (existing != null) {
                    existing.name = userInfo.name
                    userRepository.save(existing)
                } else {
                    userRepository.save(
                        User(
                            email = userInfo.email,
                            name = userInfo.name,
                            provider = AuthProvider.GOOGLE,
                            providerId = userInfo.id,
                            role = resolveSignupRole(session),
                        ),
                    )
                }
            }
        }

        session.removeAttribute(AuthController.LOGIN_TYPE_SESSION_KEY)
        return UserPrincipal(user, oAuth2User.attributes)
    }

    private fun resolveSignupRole(session: HttpSession): Role {
        val roleName = session.getAttribute(AuthController.SIGNUP_ROLE_SESSION_KEY) as String?
            ?: throw OAuth2AuthenticationException(
                OAuth2Error("role_required"),
                "회원가입을 위해 /auth/login?role=STUDENT 또는 /auth/login?role=TEACHER 로 로그인해주세요",
            )
        session.removeAttribute(AuthController.SIGNUP_ROLE_SESSION_KEY)
        return Role.valueOf(roleName)
    }

    private fun currentSession(): HttpSession =
        (RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes).request.session
}
