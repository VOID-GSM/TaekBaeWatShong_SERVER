package com.example.taekbaewatshongserver.domain.parcel.entity

import com.example.taekbaewatshongserver.domain.user.entity.User
import com.example.taekbaewatshongserver.global.exception.ParcelException
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Entity
@Table(name = "parcels")
class Parcel(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @Column(nullable = false)
    val deliveryCompany: String,

    @Column(nullable = false, unique = true)
    val invoiceNumber: String,

    @Column(nullable = false)
    var alias: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val owner: User,

    @Enumerated(EnumType.STRING)
    var zone: Zone? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: ParcelStatus = ParcelStatus.PENDING,

    var arrivedAt: LocalDateTime? = null,

    var claimedAt: LocalDateTime? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    val unclaimedDays: Int
        get() = if (status == ParcelStatus.ARRIVED && arrivedAt != null) {
            ChronoUnit.DAYS.between(arrivedAt!!.toLocalDate(), LocalDate.now()).toInt()
        } else 0

    fun markAsArrived(assignedZone: Zone? = null) {
        if (this.status != ParcelStatus.PENDING) {
            throw ParcelException.InvalidStatus("도착 대기(PENDING) 상태의 택배만 스캔 완료할 수 있습니다. (현재 상태: ${this.status})")
        }
        this.status = ParcelStatus.ARRIVED
        this.arrivedAt = LocalDateTime.now()
        assignedZone?.let { this.zone = it }
    }

    fun updateZone(newZone: Zone) {
        this.zone = newZone
    }

    fun markAsClaimed() {
        if (this.status != ParcelStatus.ARRIVED) {
            throw ParcelException.InvalidStatus("도착 완료(ARRIVED) 상태의 택배만 수령 처리할 수 있습니다. (현재 상태: ${this.status})")
        }
        this.status = ParcelStatus.CLAIMED
        this.claimedAt = LocalDateTime.now()
    }
}