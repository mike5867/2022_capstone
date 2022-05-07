package com.example.penaltykick

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit

object retrofitClient {

    val okHttpClient= OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val gson: Gson = GsonBuilder()
        .setLenient()
        .create()

    private val mainConnection by lazy {
        Retrofit.Builder()
            .baseUrl(Constants.MAIN_SERVER_ADDRESS)
            .client(okHttpClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    }

    val mainServer:RetrofitInterface by lazy{
        mainConnection.create(RetrofitInterface::class.java)
    }

    private val deepLearningConnection by lazy{
        Retrofit.Builder()
            .baseUrl(Constants.DEEPLEARNING_SERVER_ADDRESS)
            .client(okHttpClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    val deeplearningServer:RetrofitInterface by lazy{
        deepLearningConnection.create(RetrofitInterface::class.java)
    }
}