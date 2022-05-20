package com.example.penaltykick

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("id")
    var id:String,
    @SerializedName("password")
    var password:String,
    @SerializedName("email")
    var email:String,
)