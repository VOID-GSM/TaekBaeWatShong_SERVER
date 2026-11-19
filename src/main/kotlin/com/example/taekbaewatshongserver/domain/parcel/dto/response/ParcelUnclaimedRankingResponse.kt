package com.example.taekbaewatshongserver.domain.parcel.dto.response

import com.example.taekbaewatshongserver.domain.parcel.entity.Parcel
import com.example.taekbaewatshongserver.domain.parcel.entity.Zone
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "방치 택배 순위")
data class ParcelUnclaimedRankingResponse(
    @field:Schema(description = "도착 후 오래 방치된 순서의 택배 목록 (최대 3건, 없으면 빈 배열)")
    val ranks: List<Rank>,
) {
    @Schema(description = "방치 택배 순위 한 건")
    data class Rank(
        @field:Schema(description = "순위 (1부터 시작)", example = "1")
        val rank: Int,
        @field:Schema(description = "택배 주인 이름", example = "박서연")
        val ownerName: String,
        @field:Schema(description = "보관 구역 (구역 미배정이면 null)", example = "C")
        val zone: Zone?,
        @field:Schema(description = "도착 처리 시각. 방치 시간은 이 값으로 계산한다.", example = "2026-11-15T04:48:00")
        val arrivedAt: LocalDateTime,
    )

    companion object {
        fun from(parcels: List<Parcel>): ParcelUnclaimedRankingResponse = ParcelUnclaimedRankingResponse(
            ranks = parcels.mapNotNull { parcel ->
                parcel.arrivedAt?.let { parcel to it }
            }.mapIndexed { index, (parcel, arrivedAt) ->
                Rank(
                    rank = index + 1,
                    ownerName = parcel.owner.name,
                    zone = parcel.zone,
                    arrivedAt = arrivedAt,
                )
            },
        )
    }
}
