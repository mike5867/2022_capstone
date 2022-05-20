package com.example.penaltykick

import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit

class loginActivity : AppCompatActivity() {


    lateinit var loginBtn:LinearLayout
    lateinit var signIn: TextView
    lateinit var id:EditText
    lateinit var password:EditText
    lateinit var mPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        loginBtn=findViewById<LinearLayout>(R.id.login)
        signIn=findViewById<TextView>(R.id.sign_up)
        id=findViewById(R.id.id)
        password=findViewById(R.id.password)


        mPreferences=getSharedPreferences("user", MODE_PRIVATE)


        loginBtn.setOnClickListener {

            var check=true

            if(id.text.isEmpty()){
                id.error="아이디를 입력하세요"
                check=false
            }

            if(password.text.isEmpty()){
                password.error = "비밀번호를 입력하세요"
                check=false
            }

            if(check){
                val user=User(id.text.toString(),password.text.toString(),"")
                login(user)
            }

        }

        signIn.setOnClickListener{
            val signupActivity=Intent(this@loginActivity,SignupActivity::class.java)
            startActivity(signupActivity)
        }

    }


    private fun login(user:User){

        val server=retrofitClient.mainServer
        server.loginRequest(user).enqueue(object: Callback<String> {
            override fun onFailure(call: Call<String>, t: Throwable) {
                Log.d("main server","server error"+t.message.toString())
                Toast.makeText(applicationContext,"서버 연결에 실패했습니다.",Toast.LENGTH_LONG).show()
            }

            override fun onResponse(call: Call<String>, response: Response<String>) {
                if(response.isSuccessful){
                    Log.d("main server", response.body().toString())
                    val result=JSONObject(response.body().toString()).getString("result")

                    if(result=="not exist" || result=="wrong pw"){
                        Toast.makeText(applicationContext,"아이디나 비밀번호를 확인하세요.",Toast.LENGTH_LONG).show()
                    }
                    else { //result=="exist"

                        if(result=="admin"){
                            startActivity(Intent(this@loginActivity,AdminActivity::class.java))
                        }

                        Toast.makeText(applicationContext,"로그인 성공",Toast.LENGTH_LONG).show()
                        val onRenting=JSONObject(response.body().toString()).getInt("locker id")

                        val preferencesEditor:SharedPreferences.Editor=mPreferences.edit()
                        preferencesEditor.putString("userid",user.id)
                        preferencesEditor.putInt("lockerid",onRenting)
                        preferencesEditor.apply()

                        if(onRenting==0){ //대여중이 아닌경우
                            val mainIntent=Intent(this@loginActivity,MainActivity::class.java)
                            startActivity(mainIntent)
                            finish()

                        }
                        else{ //대여중인경우
                            // 시간 기록
                            val startTime=JSONObject(response.body().toString()).getString("start time")
                            preferencesEditor.putString("time",startTime)
                            preferencesEditor.apply()
                            val rentIntent= Intent(this@loginActivity,onRentActivity::class.java)
                            startActivity(rentIntent)
                            finish()
                        }
                    }

                }
                else{
                    Log.d("main server","Some error occured")
                    Toast.makeText(applicationContext,"서버 응답이 잘못 되었습니다.",Toast.LENGTH_LONG).show()
                }
            }
        })
    }
}