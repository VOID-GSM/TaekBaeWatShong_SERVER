package com.example.taekbaewatshongserver.domain.user.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    var email: String,

    var name: String,

    @Enumerated(EnumType.STRING)
    val provider: AuthProvider,

    val providerId: String,

    @Enumerated(EnumType.STRING)
    var role: Role,
)