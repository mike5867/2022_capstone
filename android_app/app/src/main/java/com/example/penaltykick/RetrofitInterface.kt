package com.example.penaltykick

import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.*

interface RetrofitInterface {
    @Multipart
    @POST("/photo")
    fun postImageRequest(@Part imageFile: MultipartBody.Part): Call<String>

    @GET("/lock/{locker_id}")
    fun lockRequest(@Path("locker_id") lockerId:Int): Call<String>

    @GET("/unlock/{locker_id}")
    fun unlockRequest(@Path("locker_id")lockerId: Int): Call<String>

}