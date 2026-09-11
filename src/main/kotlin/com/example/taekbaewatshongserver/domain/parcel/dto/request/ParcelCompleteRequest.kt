package com.example.taekbaewatshongserver.domain.parcel.dto.request

import com.example.taekbaewatshongserver.domain.parcel.entity.Zone

data class ParcelCompleteRequest(
    val invoiceNumber: String,
    val zone: Zone? = null
)