package com.example.penaltykick

import android.animation.Animator
import android.animation.AnimatorInflater
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.jvm.Throws
import kotlin.properties.Delegates

class onRentActivity : AppCompatActivity() {
    lateinit var btnReturn: Button
    lateinit var currentPhotoPath:String
    lateinit var activityResultLauncher:ActivityResultLauncher<Uri>
    lateinit var lLayout: View
    lateinit var rentTime:TextView
    lateinit var printLockerID:TextView
    lateinit var circleImage:ImageView
    lateinit var fadeAnimation: Animator
    lateinit var progressDialog:ProgressDialog
    lateinit var photoUri:Uri
    var lockerID by Delegates.notNull<Int>()
    lateinit var startTime:String
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

        server.lockRequest(lockerId, mPreferences.getString("userid","").toString()).enqueue(object:Callback<String>{
            override fun onFailure(call: Call<String>, t: Throwable) {
                Log.d("main server",t.localizedMessage)
                Toast.makeText(applicationContext,"?????? ????????? ??????????????????.",Toast.LENGTH_LONG).show()
                progressDialog.dismiss()
            }

            override fun onResponse(call: Call<String>, response: Response<String>) {
                if(response?.isSuccessful){
                    Log.d("main server",response?.body().toString())
                    val resultCode=JSONObject(response.body().toString()).getString("result")

                    if (resultCode=="success") {
                        val preferencesEditor:SharedPreferences.Editor=mPreferences.edit()
                        preferencesEditor.putInt("lockerid",0)
                        preferencesEditor.putString("type",null)
                        preferencesEditor.apply()

                        Toast.makeText(applicationContext, "????????? ?????????????????????.", Toast.LENGTH_LONG).show()
                        progressDialog.dismiss()
                        val intent= Intent(this@onRentActivity,MainActivity::class.java)
                        startActivity(intent)
                        finish()

                    }
                    else{ //fail
                        Toast.makeText(applicationContext,"????????? ?????????????????????.",Toast.LENGTH_LONG).show()
                        progressDialog.dismiss()
                    }
                }
                else{
                    Log.d("main server","Some error occured")
                    Toast.makeText(applicationContext,"?????? ????????? ?????????????????????.",Toast.LENGTH_LONG).show()
                    progressDialog.dismiss()
                }
            }
        })
    }

    private fun connectDeepLearning(path:String){
        val file=File(path)
        val fileName=photoUri.lastPathSegment.toString()
        val lockerType=RequestBody.create(MediaType.parse("text/plain"),mPreferences.getString("type",null).toString())

        var requestBody:RequestBody= RequestBody.create(MediaType.parse("image/*"),file)
        var body:MultipartBody.Part=MultipartBody.Part.createFormData("uploaded_file",fileName,requestBody)

        val server=retrofitClient.deeplearningServer

        server.postImageRequest(body,lockerType).enqueue(object : Callback<String> {
            override fun onFailure(call: Call<String>, t: Throwable) {
                Log.d("deep learning server","server error"+t.message.toString())
                Toast.makeText(applicationContext,"?????? ????????? ??????????????????.",Toast.LENGTH_LONG).show()
                progressDialog.dismiss()
            }

            override fun onResponse(call: Call<String>, response: Response<String>) {
                if(response.isSuccessful){
                    Log.d("deep learning server", response.body().toString())
                    val resultCode=JSONObject(response.body().toString()).getString("result")

                    if (resultCode=="pass"){
                        Toast.makeText(applicationContext,"?????? ????????? ???????????????.",Toast.LENGTH_LONG).show()
                        //arduino server request

                        connectMainToLock(lockerID)
                    }
                    else if(resultCode=="different"){
                        Toast.makeText(applicationContext,"?????? ???????????????. ?????? ??????????????????.",Toast.LENGTH_LONG).show()
                        progressDialog.dismiss()
                    }
                    else if(resultCode=="fail"){
                        Toast.makeText(applicationContext,"????????? ????????? ?????? ??? ?????? ???????????????.",Toast.LENGTH_LONG).show()
                        progressDialog.dismiss()
                    }
                    else{
                        Toast.makeText(applicationContext,"????????? ????????? ??? ????????????.",Toast.LENGTH_LONG).show()
                        progressDialog.dismiss()
                    }
                }
                else{
                    Log.d("deep learning server","Some error occured")
                    Toast.makeText(applicationContext,"?????? ????????? ?????????????????????.",Toast.LENGTH_LONG).show()
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
        startTime= mPreferences.getString("time",null).toString()

        circleImage=findViewById(R.id.circle)
        fadeAnimation=AnimatorInflater.loadAnimator(this,R.animator.fade)
        fadeAnimation.setTarget(circleImage)
        fadeAnimation.start()

        rentTime=findViewById(R.id.rent_time)
        val showStartTimeString="?????? ??????: $startTime"
        rentTime.text = showStartTimeString

        printLockerID=findViewById(R.id.locker_id)
        val showPrintLockerString= "?????? ?????? ??????: $lockerID"
        printLockerID.text=showPrintLockerString


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

    override fun onPause() {
        super.onPause()
        fadeAnimation.cancel()
    }

    override fun onStop() {
        super.onStop()
        fadeAnimation.cancel()
    }

    override fun onStart(){
        super.onStart()
        fadeAnimation.start()
    }

    private fun CameraPermission(){
        if(ActivityCompat.checkSelfPermission(this,android.Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)
            return
        else{
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,android.Manifest.permission.CAMERA)){
                Snackbar.make(lLayout,"??? ?????? ??????????????? ????????? ????????? ???????????????",
                Snackbar.LENGTH_INDEFINITE).setAction("??????",View.OnClickListener {
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
                    lLayout, "???????????? ?????????????????????. ?????? ?????? ???????????? ???????????? ???????????????",
                    Snackbar.LENGTH_INDEFINITE
                ).setAction("??????", View.OnClickListener {
                    finish()
                }).show()
            }else{
                Snackbar.make(lLayout,"???????????? ?????????????????????. ??????(??? ??????)?????? ???????????? ???????????????",
                Snackbar.LENGTH_INDEFINITE).setAction("??????",View.OnClickListener {
                    finish()
                }).show()
            }
        }
    }
}