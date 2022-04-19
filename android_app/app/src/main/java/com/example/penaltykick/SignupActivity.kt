package com.example.penaltykick

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText

class SignupActivity : AppCompatActivity() {
    lateinit var signUpBtn:Button
    lateinit var userId:EditText
    lateinit var userPw:EditText
    lateinit var verifyPw:EditText
    lateinit var duplicateIdBtn:Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

    }

}