package com.example.taekbaewatshongserver.global.security.oauth

import com.example.taekbaewatshongserver.domain.user.entity.AuthProvider
import com.example.taekbaewatshongserver.domain.user.entity.User
import com.example.taekbaewatshongserver.domain.user.repository.UserRepository
import com.example.taekbaewatshongserver.global.security.UserPrincipal
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service

@Service
class CustomOAuth2UserService(
    private val userRepository: UserRepository,
) : DefaultOAuth2UserService() {

    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val oAuth2User = super.loadUser(userRequest)
        val userInfo = GoogleOAuth2UserInfo(oAuth2User.attributes)

        val existing = userRepository.findByEmail(userInfo.email)
        val user = if (existing != null) {
            existing.name = userInfo.name
            userRepository.save(existing)
        } else {
            userRepository.save(
                User(
                    email = userInfo.email,
                    name = userInfo.name,
                    provider = AuthProvider.GOOGLE,
                    providerId = userInfo.id,
                ),
            )
        }

        return UserPrincipal(user, oAuth2User.attributes)
    }
}