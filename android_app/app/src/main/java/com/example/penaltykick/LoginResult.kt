package com.example.penaltykick

import com.google.gson.annotations.SerializedName
import retrofit2.Response

data class LoginResult(
    @SerializedName("id")
    var id: String,
    @SerializedName("renting")
    var onRent:Int,
    @SerializedName("time")
    var time:String
)
