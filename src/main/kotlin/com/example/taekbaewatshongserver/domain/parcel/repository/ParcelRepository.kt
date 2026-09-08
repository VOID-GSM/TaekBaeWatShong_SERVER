package com.example.taekbaewatshongserver.domain.parcel.repository

import com.example.taekbaewatshongserver.domain.parcel.entity.Parcel
import com.example.taekbaewatshongserver.domain.parcel.entity.ParcelStatus
import com.example.taekbaewatshongserver.domain.parcel.entity.Zone
import com.example.taekbaewatshongserver.domain.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface ParcelRepository : JpaRepository<Parcel, Long> {

    fun findByInvoiceNumber(invoiceNumber: String): Parcel?

    fun findAllByOwnerOrderByCreatedAtDesc(owner: User): List<Parcel>

    fun findAllByOwnerAndStatusOrderByCreatedAtDesc(owner: User, status: ParcelStatus): List<Parcel>

    fun findAllByOwnerAndStatusAndClaimedAtGreaterThanEqualOrderByCreatedAtDesc(
        owner: User,
        status: ParcelStatus,
        claimedAt: LocalDateTime
    ): List<Parcel>

    fun findAllByOrderByCreatedAtDesc(): List<Parcel>

    fun findAllByStatusOrderByCreatedAtDesc(status: ParcelStatus): List<Parcel>

    @Query("""
        SELECT p FROM Parcel p 
        WHERE p.status = :status 
          AND p.claimedAt >= :threeDaysAgo 
        ORDER BY p.createdAt DESC
    """)
    fun findClaimedParcelsWithinThreeDays(
        @Param("status") status: ParcelStatus = ParcelStatus.CLAIMED,
        @Param("threeDaysAgo") threeDaysAgo: LocalDateTime
    ): List<Parcel>

    fun findAllByStatusAndZoneIn(status: ParcelStatus, zones: List<Zone>): List<Parcel>
}