package com.example.taekbaewatshongserver.domain.parcel.entity

import com.example.taekbaewatshongserver.domain.user.entity.User
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

    @Column(nullable = false)
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
        this.status = ParcelStatus.ARRIVED
        this.arrivedAt = LocalDateTime.now()
        assignedZone?.let { this.zone = it }
    }

    fun updateZone(newZone: Zone) {
        this.zone = newZone
    }

    fun markAsClaimed() {
        this.status = ParcelStatus.CLAIMED
        this.claimedAt = LocalDateTime.now()
    }
}