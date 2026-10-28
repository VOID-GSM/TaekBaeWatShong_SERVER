package com.example.taekbaewatshongserver.domain.driver.entity

import com.example.taekbaewatshongserver.domain.user.entity.User
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "driver_scan_logs")
class DriverScanLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    val driver: Driver,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scanned_by", nullable = false)
    val scannedBy: User,

    @Column(nullable = false, updatable = false)
    val scannedAt: LocalDateTime = LocalDateTime.now(),
)
