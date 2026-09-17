package com.example.taekbaewatshongserver.domain.parcel.service

import com.example.taekbaewatshongserver.domain.parcel.dto.request.*
import com.example.taekbaewatshongserver.domain.parcel.dto.response.*
import com.example.taekbaewatshongserver.domain.parcel.entity.Parcel
import com.example.taekbaewatshongserver.domain.parcel.entity.ParcelStatus
import com.example.taekbaewatshongserver.domain.parcel.entity.Zone
import com.example.taekbaewatshongserver.domain.parcel.repository.ParcelRepository
import com.example.taekbaewatshongserver.domain.user.entity.User
import com.example.taekbaewatshongserver.global.exception.ParcelException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class ParcelService(
    private val parcelRepository: ParcelRepository,
    private val apickTrackingService: ApickTrackingService,
    private val eventPublisher: ApplicationEventPublisher
) {

    @Transactional
    fun registerParcel(user: User, request: ParcelRegisterRequest): ParcelResponse {
        if (parcelRepository.existsByInvoiceNumber(request.invoiceNumber)) {
            throw ParcelException.Conflict("이미 등록된 운송장 번호입니다.")
        }

        if (!apickTrackingService.validateInvoice(request.deliveryCompany, request.invoiceNumber)) {
            throw ParcelException.InvalidInvoice("유효하지 않은 운송장 번호이거나 조회할 수 없는 택배입니다.")
        }

        val alias = request.alias?.takeIf { it.isNotBlank() } ?: user.name

        val parcel = Parcel(
            deliveryCompany = request.deliveryCompany,
            invoiceNumber = request.invoiceNumber,
            alias = alias,
            owner = user
        )

        return try {
            val savedParcel = parcelRepository.save(parcel)
            eventPublisher.publishEvent(ParcelRegisteredEvent(savedParcel.id))
            ParcelResponse.from(savedParcel)
        } catch (e: DataIntegrityViolationException) {
            throw ParcelException.Conflict("이미 등록된 운송장 번호입니다.")
        }
    }

    fun getAllParcels(status: ParcelStatus?): ParcelListResponse {
        val threeDaysAgo = LocalDateTime.now().minusDays(3)

        val parcels = when (status) {
            ParcelStatus.CLAIMED -> parcelRepository.findClaimedParcelsWithinThreeDays(threeDaysAgo = threeDaysAgo)
            else -> {
                val allParcels = parcelRepository.findAllByOrderByCreatedAtDesc()
                if (status != null) {
                    allParcels.filter { it.status == status }
                } else {
                    allParcels.filter { it.status != ParcelStatus.CLAIMED || it.claimedAt?.isAfter(threeDaysAgo) == true }
                }
            }
        }

        return ParcelListResponse(parcels.map { ParcelResponse.from(it) })
    }

    fun getMyParcels(user: User, status: ParcelStatus?): ParcelListResponse {
        val threeDaysAgo = LocalDateTime.now().minusDays(3)

        val parcels = when (status) {
            ParcelStatus.CLAIMED -> parcelRepository.findAllByOwnerAndStatusAndClaimedAtGreaterThanEqualOrderByCreatedAtDesc(
                owner = user,
                status = ParcelStatus.CLAIMED,
                claimedAt = threeDaysAgo
            )
            else -> {
                val userParcels = parcelRepository.findAllByOwnerOrderByCreatedAtDesc(user)
                if (status != null) {
                    userParcels.filter { it.status == status }
                } else {
                    userParcels.filter { it.status != ParcelStatus.CLAIMED || it.claimedAt?.isAfter(threeDaysAgo) == true }
                }
            }
        }

        return ParcelListResponse(parcels.map { ParcelResponse.from(it) })
    }

    @Transactional
    fun completeParcelScan(request: ParcelCompleteRequest): ParcelResponse {
        val parcel = parcelRepository.findByInvoiceNumber(request.invoiceNumber)
            ?: throw ParcelException.NotFound("해당 운송장 번호의 택배를 찾을 수 없습니다. (${request.invoiceNumber})")

        parcel.markAsArrived(request.zone)

        eventPublisher.publishEvent(ParcelArrivedEvent(parcel.id))

        return ParcelResponse.from(parcel)
    }

    fun getParcelsByZone(zoneParam: Zone?): ParcelZoneGroupResponse {
        val targetZones: List<Zone?> = if (zoneParam != null) {
            listOf(zoneParam)
        } else {
            Zone.entries.toList() + listOf(null)
        }

        val arrivedParcels = parcelRepository.findAllByStatusOrderByCreatedAtDesc(ParcelStatus.ARRIVED)

        val zoneGroupDetails = targetZones.map { zone ->
            val zoneParcels = arrivedParcels.filter { it.zone == zone }

            ParcelZoneGroupResponse.ZoneGroupDetail(
                zone = zone,
                count = zoneParcels.size,
                parcels = zoneParcels.map { parcel ->
                    ParcelZoneGroupResponse.ParcelSimpleDetail(
                        id = parcel.id,
                        invoiceNumber = parcel.invoiceNumber,
                        alias = parcel.alias,
                        ownerName = parcel.owner.name,
                        arrivedAt = parcel.arrivedAt,
                        unclaimedDays = parcel.unclaimedDays
                    )
                }
            )
        }

        return ParcelZoneGroupResponse(zoneGroupDetails)
    }

    @Transactional
    fun assignZone(parcelId: Long, request: ParcelZoneAssignRequest): ParcelZoneAssignResponse {
        val parcel = parcelRepository.findById(parcelId)
            .orElseThrow { ParcelException.NotFound("존재하지 않는 택배 ID입니다.") }

        parcel.updateZone(request.zone)

        if (parcel.status == ParcelStatus.ARRIVED) {
            eventPublisher.publishEvent(ParcelZoneAssignedEvent(parcel.id))
        }

        return ParcelZoneAssignResponse.from(parcel)
    }

    @Transactional
    fun claimParcel(user: User, request: ParcelClaimRequest): ParcelClaimResponse {
        val parcel = parcelRepository.findByInvoiceNumber(request.invoiceNumber)
            ?: throw ParcelException.NotFound("해당 운송장 번호의 택배를 찾을 수 없습니다.")

        if (parcel.owner.id != user.id) {
            throw AccessDeniedException("본인의 택배만 회수 처리할 수 있습니다.")
        }

        parcel.markAsClaimed()

        eventPublisher.publishEvent(ParcelClaimedEvent(parcel.id))

        return ParcelClaimResponse.from(parcel)
    }
}