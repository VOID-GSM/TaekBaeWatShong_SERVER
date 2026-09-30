package com.example.taekbaewatshongserver.domain.driver.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "drivers")
class Driver(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @Column(nullable = false)
    val driverName: String,

    @Column(nullable = false)
    val deliveryCompany: String,

    @Column(nullable = false, unique = true, updatable = false)
    val qrToken: String,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
