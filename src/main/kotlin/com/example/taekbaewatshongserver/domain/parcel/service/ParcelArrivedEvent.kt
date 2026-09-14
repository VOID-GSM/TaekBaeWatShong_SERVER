package com.example.taekbaewatshongserver.domain.parcel.service

import com.example.taekbaewatshongserver.domain.parcel.entity.Parcel

data class ParcelArrivedEvent(
    val parcel: Parcel
)