package com.example.penaltykick

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager

class introActivity : AppCompatActivity() {
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

        /*
        실제 구현시에는 자동로그인 되어있을 경우 화면 넘기기
        대여 되어있는 경우에는 백그라운드로 계속 돌리게 할 것이므로 여기서는 따로 설정할 필요 없음
        로그인 안되어있을 경우 로그인 화면으로 넘어가게 할 것
         */
        val handler= Handler()
        handler.postDelayed({
            var intent= Intent(this,loginActivity::class.java)
            startActivity(intent)
        },3000)
    }


    override fun onPause(){
        super.onPause()
        finish()
    }
}