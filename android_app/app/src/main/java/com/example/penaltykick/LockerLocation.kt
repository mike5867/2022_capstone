package com.example.penaltykick

import com.google.gson.annotations.SerializedName

data class LockerLocation(
    @SerializedName("id")
    var id:Int,
    @SerializedName("latitude")
    var latitude:Double,
    @SerializedName("longitude")
    var longitude:Double
)