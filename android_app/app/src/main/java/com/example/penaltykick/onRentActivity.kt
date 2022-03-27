package com.example.penaltykick

import android.content.Intent
import android.content.pm.PackageManager

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View

import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider.getUriForFile
import com.google.android.material.snackbar.Snackbar
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
    lateinit var lLayout: View

    lateinit var photoUri:Uri
    val serverAddress="http://10.0.2.2:5000" //using localhost in emulator
    val PERMISSION_REQUEST_CODE=300
    val REQUIRED_PERMISSONS=Array<String>(1){android.Manifest.permission.CAMERA}


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
        lLayout=findViewById(R.id.rent_layout)

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

            if(ActivityCompat.checkSelfPermission(this,android.Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED){
                activityResultLauncher.launch(photoUri)
            }
            else{
                CameraPermission()
            }

        }

    }

    private fun CameraPermission(){
        if(ActivityCompat.checkSelfPermission(this,android.Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)
            return
        else{
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,android.Manifest.permission.CAMERA)){
                Snackbar.make(lLayout,"이 앱을 실행하려면 카메라 권한이 필요합니다",
                Snackbar.LENGTH_INDEFINITE).setAction("확인",View.OnClickListener {
                ActivityCompat.requestPermissions(this,REQUIRED_PERMISSONS,PERMISSION_REQUEST_CODE)
                }).show()
            }else{
                ActivityCompat.requestPermissions(this,REQUIRED_PERMISSONS,PERMISSION_REQUEST_CODE)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode==PERMISSION_REQUEST_CODE){
            activityResultLauncher.launch(photoUri)
        }
        else{
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,REQUIRED_PERMISSONS[0])) {
                Snackbar.make(
                    lLayout, "퍼미션이 거부되었습니다. 앱을 다시 실행하여 퍼미션을 허용하세요",
                    Snackbar.LENGTH_INDEFINITE
                ).setAction("확인", View.OnClickListener {
                    finish()
                }).show()
            }else{
                Snackbar.make(lLayout,"퍼미션이 거부되었습니다. 설정(앱 정보)에서 퍼미션을 허용하세요",
                Snackbar.LENGTH_INDEFINITE).setAction("확인",View.OnClickListener {
                    finish()
                }).show()
            }
        }
    }
}