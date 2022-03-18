package com.example.penaltykick

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment

import android.widget.Button
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider.getUriForFile
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.jvm.Throws

class MainActivity : AppCompatActivity() {
    lateinit var btnReturn: Button
    lateinit var imageView:ImageView //test
    lateinit var currentPhotoPath:String
    lateinit var activityResultLauncher:ActivityResultLauncher<Uri>



    @Throws(IOException::class)
    private fun createImageFile():File{
        val timeStamp:String=SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir:File?=getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",".jpg",storageDir).apply {
                currentPhotoPath=absolutePath
        }

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnReturn=findViewById<Button>(R.id.ret)
        imageView=findViewById<ImageView>(R.id.resultImage)

        activityResultLauncher=registerForActivityResult(ActivityResultContracts.TakePicture()){ result->
            if(result){
                //send to server
                val f=File(currentPhotoPath)
                imageView.setImageURI(getUriForFile(this,"com.example.penaltykick.provider",f))  //test

            }
        }



        btnReturn.setOnClickListener {
            val photoFile:File=createImageFile()
            val photoURI:Uri= getUriForFile(this,"com.example.penaltykick.provider",photoFile)
            activityResultLauncher.launch(photoURI)

        }


    }


}