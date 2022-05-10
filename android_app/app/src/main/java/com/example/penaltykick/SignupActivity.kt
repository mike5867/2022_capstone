package com.example.penaltykick

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SignupActivity : AppCompatActivity() {
    lateinit var signUpBtn:Button
    lateinit var userId:EditText
    lateinit var userPw:EditText
    lateinit var passwordCheck:EditText
    lateinit var duplicateIdBtn:Button
    lateinit var email:EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        userId=findViewById(R.id.id_text)
        userPw=findViewById(R.id.password_text)
        passwordCheck=findViewById(R.id.passwordcheck_text)
        duplicateIdBtn=findViewById(R.id.dupcheck_btn)
        signUpBtn=findViewById(R.id.signup_btn)
        email=findViewById(R.id.email_text)

        duplicateIdBtn.setOnClickListener {
            if(userId.text.isNotEmpty()){
                duplicationCheck(userId.text.toString())
            }
            else{
                userId.error="아이디를 입력하세요"
            }

        }

        signUpBtn.setOnClickListener {
            if(vaildCheck()){
                val user=User(userId.text.toString(),userPw.text.toString(),email.text.toString())
                addnewUser(user)
            }
        }

    }


    private fun duplicationCheck(id:String){
        val server=retrofitClient.mainServer
        server.duplicateCheckRequest(id).enqueue(object: Callback<String> {
            override fun onFailure(call: Call<String>, t: Throwable) {
                Log.d("main server","server error"+t.message.toString())
                Toast.makeText(applicationContext,"서버 연결에 실패했습니다.",Toast.LENGTH_LONG).show()

            }

            override fun onResponse(call: Call<String>, response: Response<String>) {
                if(response.isSuccessful){
                    Log.d("main server",response.body().toString())
                    val resultCode=JSONObject(response.body().toString()).getString("result")

                    if(resultCode=="pass"){
                        duplicateIdBtn.isEnabled=false
                        duplicateIdBtn.text="확인완료"
                    }
                    else if(resultCode=="fail"){
                        Toast.makeText(applicationContext,"중복된 아이디입니다.",Toast.LENGTH_LONG).show()
                    }
                    else{
                        Toast.makeText(applicationContext,"잘못된 응답입니다.",Toast.LENGTH_LONG).show()
                    }
                }

            }
        })

    }

    private fun vaildCheck():Boolean{
        var isOk=true

        if(userId.text.isEmpty()){
            userId.error="아이디를 입력하세요"
            isOk=false
        }

        if(duplicateIdBtn.isEnabled){
            Toast.makeText(applicationContext,"아이디 중복 확인이 필요합니다.",Toast.LENGTH_LONG).show()
            isOk=false
        }

        if(userPw.text.isEmpty()){
            userPw.error="비밀번호를 입력하세요"
            isOk=false
        }

        if(passwordCheck.text.isEmpty()){
            passwordCheck.error="비밀번호를 다시 입력하세요"
            isOk=false
        }

        if(passwordCheck.text.toString()!=userPw.text.toString()){
            passwordCheck.error="비밀번호가 일치하지 않습니다"
            isOk=false
        }

        if(email.text.isEmpty()){
            email.error="이메일을 입력하세요"
            isOk=false
        }

        else{
            val re_email="^[0-9a-zA-Z]([-_.]?[0-9a-zA-Z])*@[0-9a-zA-Z]([-_.]?[0-9a-zA-Z])*\\.[a-zA-Z]{2,3}$".toRegex()
            if(!re_email.containsMatchIn(email.text)){
                email.error="이메일 형식이 아닙니다."
                isOk=false
            }
        }

        return isOk
    }

    private fun addnewUser(user:User){
        val server=retrofitClient.mainServer
        server.addUserRequest(user).enqueue(object:Callback<String>{
            override fun onFailure(call: Call<String>, t: Throwable) {
                Log.d("main server","server error"+t.message.toString())
                Toast.makeText(applicationContext,"서버 연결에 실패했습니다.",Toast.LENGTH_LONG).show()

            }

            override fun onResponse(call: Call<String>, response: Response<String>) {
                if(response.isSuccessful){
                    Log.d("main server",response.body().toString())
                    val resultCode=JSONObject(response.body().toString()).getString("result")

                    if(resultCode=="pass"){
                        Toast.makeText(applicationContext,"회원 가입이 완료되었습니다.",Toast.LENGTH_LONG).show()
                        val intent= Intent(this@SignupActivity,loginActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                    else if(resultCode=="fail"){
                        Toast.makeText(applicationContext,"회원가입에 실패했습니다.",Toast.LENGTH_LONG).show()

                    }
                    else{
                        Toast.makeText(applicationContext,"잘못된 응답입니다.",Toast.LENGTH_LONG).show()
                    }

                }
            }
        })

    }

}