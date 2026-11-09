package com.example.taekbaewatshongserver.domain.parcel.dto.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "택배 등록 요청")
data class ParcelRegisterRequest(
    @field:Schema(description = "택배사 이름. CJ대한통운(CJ), 우체국택배(우체국), 한진택배(한진), 롯데택배(롯데), 로젠택배(로젠), GS25편의점(GS25), CU편의점(CU)을 인식하며 띄어쓰기는 무시한다.", example = "CJ대한통운", requiredMode = Schema.RequiredMode.REQUIRED)
    val deliveryCompany: String,
    @field:Schema(description = "운송장 번호. 전체 택배에서 중복될 수 없다.", example = "123456789012", requiredMode = Schema.RequiredMode.REQUIRED)
    val invoiceNumber: String,
    @field:Schema(description = "택배 별칭 (선택). 비우면 등록한 사용자 이름이 저장된다.", example = "운동화")
    val alias: String? = null,
)
