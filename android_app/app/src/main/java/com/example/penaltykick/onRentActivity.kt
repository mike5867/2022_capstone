package com.example.penaltykick

import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.*

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider.getUriForFile
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.*
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.jvm.Throws
import kotlin.properties.Delegates

class onRentActivity : AppCompatActivity() {
    lateinit var btnReturn: Button
    lateinit var currentPhotoPath:String
    lateinit var activityResultLauncher:ActivityResultLauncher<Uri>
    lateinit var lLayout: View
    lateinit var progressDialog:ProgressDialog
    lateinit var photoUri:Uri
    var lockerID by Delegates.notNull<Int>()
    lateinit var mPreferences:SharedPreferences
    private val PERMISSION_REQUEST_CODE=300
    private val REQUIRED_PERMISSONS=Array<String>(1){android.Manifest.permission.CAMERA}

    @Throws(IOException::class)
    private fun createImageFile():File{
        val timeStamp:String=SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir:File?=getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "${timeStamp}_",".jpg",storageDir).apply {
            currentPhotoPath=absolutePath
        }

    }

    private fun connectMainToLock(lockerId:Int){

        val server=retrofitClient.mainServer

        server.lockRequest(lockerId).enqueue(object:Callback<String>{
            override fun onFailure(call: Call<String>, t: Throwable) {
                Log.d("main server",t.localizedMessage)
                Toast.makeText(applicationContext,"서버 연결에 실패했습니다.",Toast.LENGTH_LONG).show()
                progressDialog.dismiss()
            }

            override fun onResponse(call: Call<String>, response: Response<String>) {
                if(response?.isSuccessful){
                    Log.d("main server",response?.body().toString())
                    val resultCode=JSONObject(response.body().toString()).getString("result")

                    if (resultCode=="success") {
                        val preferencesEditor:SharedPreferences.Editor=mPreferences.edit()
                        preferencesEditor.putInt("lockerid",0)
                        preferencesEditor.apply()

                        Toast.makeText(applicationContext, "잠금이 완료되었습니다.", Toast.LENGTH_LONG).show()
                        progressDialog.dismiss()
                        val intent= Intent(this@onRentActivity,MainActivity::class.java)
                        startActivity(intent)
                        finish()

                    }
                    else{ //fail
                        Toast.makeText(applicationContext,"잠금에 실패하였습니다.",Toast.LENGTH_LONG).show()
                        progressDialog.dismiss()
                    }
                }
                else{
                    Log.d("main server","Some error occured")
                    Toast.makeText(applicationContext,"서버 응답이 잘못되었습니다.",Toast.LENGTH_LONG).show()
                    progressDialog.dismiss()
                }
            }
        })
    }

    private fun connectDeepLearning(path:String){
        val file=File(path)
        val fileName=photoUri.lastPathSegment.toString()

        var requestBody:RequestBody= RequestBody.create(MediaType.parse("image/*"),file)
        var body:MultipartBody.Part=MultipartBody.Part.createFormData("uploaded_file",fileName,requestBody)

        val server=retrofitClient.deeplearningServer

        server.postImageRequest(body).enqueue(object : Callback<String> {
            override fun onFailure(call: Call<String>, t: Throwable) {
                Log.d("deep learning server",t.localizedMessage)
                Toast.makeText(applicationContext,"서버 연결에 실패했습니다.",Toast.LENGTH_LONG).show()
                progressDialog.dismiss()
            }

            override fun onResponse(call: Call<String>, response: Response<String>) {
                if(response?.isSuccessful){
                    Log.d("deep learning server",response?.body().toString())
                    val resultCode=JSONObject(response.body().toString()).getString("result")

                    if (resultCode=="pass"){
                        Toast.makeText(applicationContext,"기기 잠금을 요청합니다.",Toast.LENGTH_LONG).show()
                        //arduino server request

                        connectMainToLock(lockerID)
                    }
                    else if(resultCode=="fail"){
                        Toast.makeText(applicationContext,"올바른 장소에 주차 후 다시 반납하세요.",Toast.LENGTH_LONG).show()
                        progressDialog.dismiss()
                    }
                    else{
                        Toast.makeText(applicationContext,"기기를 인식할 수 없습니다.",Toast.LENGTH_LONG).show()
                        progressDialog.dismiss()
                    }
                }
                else{
                    Log.d("deep learning server","Some error occured")
                    Toast.makeText(applicationContext,"서버 응답이 잘못되었습니다.",Toast.LENGTH_LONG).show()
                    progressDialog.dismiss()
                }
            }
        })

    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_on_rent)

        mPreferences=getSharedPreferences("user", MODE_PRIVATE)
        lockerID=mPreferences.getInt("lockerid",0)

        btnReturn=findViewById<Button>(R.id.ret)
        lLayout=findViewById(R.id.rent_layout)

        progressDialog=ProgressDialog(this)

        activityResultLauncher=registerForActivityResult(ActivityResultContracts.TakePicture()){ result->
            if(result){
                //send image to server
                progressDialog.show()
                connectDeepLearning(currentPhotoPath)

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