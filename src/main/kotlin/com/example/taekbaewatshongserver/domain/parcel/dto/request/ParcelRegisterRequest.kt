package com.example.taekbaewatshongserver.domain.parcel.dto.request

data class ParcelRegisterRequest(
    val deliveryCompany: String,
    val invoiceNumber: String,
    val alias: String? = null
)