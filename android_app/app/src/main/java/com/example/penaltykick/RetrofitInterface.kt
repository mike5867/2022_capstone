package com.example.penaltykick

import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.*

interface RetrofitInterface {
    @Multipart
    @POST("/photo")
    fun postImageRequest(@Part imageFile: MultipartBody.Part): Call<String>

    @GET("/lock")
    fun lockRequest(@Query("id") locker_id:Int, @Query("user") userid:String): Call<String>

    @GET("/unlock")
    fun unlockRequest(@Query("id") locker_id:Int, @Query("user") userid:String): Call<String>

    @POST("/login")
    fun loginRequest(@Body user:User):Call<LoginResult>

    @GET("/location")
    fun lockerLocationRequest(@Query("lat")lat:Double, @Query("long")long:Double):Call<MutableList<LockerLocation>>

    @GET("/idcheck")
    fun duplicateCheckRequest(@Query("id")id:String):Call<String>

    @POST("/adduser")
    fun addUserRequest(@Body user:User):Call<String>



}