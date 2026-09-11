package com.example.taekbaewatshongserver.domain.parcel.dto.response

data class ApickTrackingResponse(
    val success: Boolean,
    val data: ApickData?,
    val error: ApickError?
) {
    data class ApickData(
        val invoiceNo: String?,
        val item: String?,
        val receiverName: String?
    )

    data class ApickError(
        val code: String?,
        val message: String?
    )
}