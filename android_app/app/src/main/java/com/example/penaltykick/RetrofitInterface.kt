package com.example.penaltykick

import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.*

interface RetrofitInterface {
    @Multipart
    @POST("/photo")
    fun postImageRequest(@Part imageFile: MultipartBody.Part): Call<String>

    @GET("/lock")
    fun lockRequest(@Query("id") locker_id:Int): Call<String>

    @GET("/unlock")
    fun unlockRequest(@Query("id") locker_id:Int): Call<String>

    @GET("/login")
    fun loginRequest(@Query("id")id:String, @Query("pw")pw:String):Call<String>




}