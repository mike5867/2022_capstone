package com.example.penaltykick

import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager

class introActivity : AppCompatActivity() {

    lateinit var mPreferences: SharedPreferences
    fun setFullScreen(){
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.R){
            supportActionBar?.hide()

            window.setDecorFitsSystemWindows(false)
            val controller=window.insetsController
            if(controller!=null){
                controller.hide(WindowInsets.Type.statusBars() or
                WindowInsets.Type.navigationBars())

                controller.systemBarsBehavior=WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

            }
        }
        else{
            supportActionBar?.hide()
            window.decorView.systemUiVisibility=(View.SYSTEM_UI_FLAG_IMMERSIVE
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro)

        setFullScreen()

        mPreferences=getSharedPreferences("user", MODE_PRIVATE)

        val idcheck=mPreferences.getString("userid",null)

        //기록된 id가 없는 경우 로그인 액티비티 실행
        if(idcheck==null){
            intent= Intent(this,loginActivity::class.java)
        }else{
            //대여중인 잠금 장치가 없는 경우(locker id = 0)
            val locker=mPreferences.getInt("lockerid",0)
            if(locker==0){
                intent=Intent(this,MainActivity::class.java)
            }
            else{
                intent=Intent(this,onRentActivity::class.java)
            }
        }

        val handler= Handler()
        handler.postDelayed({
            startActivity(intent)
        },3000)
    }


    override fun onPause(){
        super.onPause()
        finish()
    }
}