package com.example.penaltykick

import android.content.Intent

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log

import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider.getUriForFile
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.zxing.client.android.Intents
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.jvm.Throws

class onRentActivity : AppCompatActivity() {
    lateinit var btnReturn: Button
    lateinit var imageView:ImageView //test
    lateinit var currentPhotoPath:String
    lateinit var activityResultLauncher:ActivityResultLauncher<Uri>

    lateinit var photoUri:Uri
    val serverAddress="http://10.0.2.2:5000" //using localhost in emulator


    @Throws(IOException::class)
    private fun createImageFile():File{
        val timeStamp:String=SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir:File?=getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",".jpg",storageDir).apply {
            currentPhotoPath=absolutePath
        }

    }

    interface retrofit_interface{
        @Multipart
        @POST("/photo")
        fun postImageRequest(@Part imageFile: MultipartBody.Part): Call<String>

    }

    fun testRetrofit(path:String){
        val file=File(path)
        val fileName="testfile"   //can be modified

        var requestBody:RequestBody= RequestBody.create(MediaType.parse("image/*"),file)
        var body:MultipartBody.Part=MultipartBody.Part.createFormData("uploaded_file",fileName,requestBody)


        var gson: Gson =GsonBuilder()
            .setLenient()
            .create()
        var retrofit= Retrofit.Builder()
            .baseUrl(serverAddress)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        var server=retrofit.create(retrofit_interface::class.java)

        server.postImageRequest(body).enqueue(object : Callback<String> {
            override fun onFailure(call: Call<String>, t: Throwable) {
                Log.d("retrofit result1",t.localizedMessage)
            }

            override fun onResponse(call: Call<String>, response: Response<String>) {
                if(response?.isSuccessful){
                    Log.d("retrofit result2",""+response?.body().toString())
                }
                else{
                    Log.d("retrofit result3","Some error occured")
                }

            }
        })



    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_on_rent)

        btnReturn=findViewById<Button>(R.id.ret)

        activityResultLauncher=registerForActivityResult(ActivityResultContracts.TakePicture()){ result->
            if(result){
                //retrofit2
                //send image to server
                testRetrofit(currentPhotoPath)

            }
        }


        btnReturn.setOnClickListener {
            val photoFile:File=createImageFile()
            photoUri= getUriForFile(this,"com.example.penaltykick.provider",photoFile)
            activityResultLauncher.launch(photoUri)

        }

    }


}