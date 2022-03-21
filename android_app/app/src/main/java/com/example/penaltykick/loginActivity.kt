package com.example.penaltykick

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.LinearLayout
import java.util.logging.Level
import kotlin.math.log

class loginActivity : AppCompatActivity() {

    /*
    로그인 구현 전까지 로그인 버튼 누르면 메인 액티비티로 가고
    구글 로그인 버튼 누르면 대여중 액티비티로 가게 함
     */
    lateinit var googleBtn:LinearLayout
    lateinit var loginBtn:LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        loginBtn=findViewById<LinearLayout>(R.id.login)
        googleBtn=findViewById<LinearLayout>(R.id.google_login)

        loginBtn.setOnClickListener {
            val rentIntent=Intent(this,MainActivity::class.java)
            startActivity(rentIntent)
            finish()

        }

        googleBtn.setOnClickListener{
            val mainIntent= Intent(this,onRentActivity::class.java)
            startActivity(mainIntent)
            finish()
        }



    }
}