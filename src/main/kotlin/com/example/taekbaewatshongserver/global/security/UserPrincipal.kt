package com.example.taekbaewatshongserver.global.security

import com.example.taekbaewatshongserver.domain.user.entity.User
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.user.OAuth2User

class UserPrincipal(
    val user: User,
    private val attributes: Map<String, Any> = emptyMap(),
) : OAuth2User {

    override fun getAttributes(): Map<String, Any> = attributes

    override fun getAuthorities(): Collection<GrantedAuthority> =
        listOf(SimpleGrantedAuthority("ROLE_${user.role}"))

    override fun getName(): String = user.id.toString()
}