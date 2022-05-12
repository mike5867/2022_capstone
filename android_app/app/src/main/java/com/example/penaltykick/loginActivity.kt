package com.example.penaltykick

import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import okhttp3.OkHttpClient
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
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
                login(id.text.toString(),password.text.toString())
            }

        }

        signIn.setOnClickListener{
            val signupActivity=Intent(this@loginActivity,SignupActivity::class.java)
            startActivity(signupActivity)
        }

    }


    private fun login(id: String, pw: String){

        val server=retrofitClient.mainServer
        server.loginRequest(id,pw).enqueue(object: Callback<String> {
            override fun onFailure(call: Call<String>, t: Throwable) {
                Log.d("main server",t.localizedMessage)
                Toast.makeText(applicationContext,"서버 연결에 실패했습니다.",Toast.LENGTH_LONG).show()
            }

            override fun onResponse(call: Call<String>, response: Response<String>) {
                if(response?.isSuccessful){
                    Log.d("main server",response?.body().toString())
                    val result=JSONObject(response.body().toString()).getString("result")

                    if(result=="not exist"){
                        Toast.makeText(applicationContext,"일치하는 계정이 없습니다.",Toast.LENGTH_LONG).show()
                    }
                    else if(result=="exist"){ //result=="exist"

                        Toast.makeText(applicationContext,"로그인 성공",Toast.LENGTH_LONG).show()
                        val onRenting=JSONObject(response.body().toString()).getString("locker id")

                        val preferencesEditor:SharedPreferences.Editor=mPreferences.edit()
                        preferencesEditor.putString("userid",id)
                        preferencesEditor.putInt("lockerid",onRenting.toInt())
                        preferencesEditor.apply()

                        if(onRenting=="0"){ //대여중이 아닌경우
                            val rentIntent=Intent(this@loginActivity,MainActivity::class.java)
                            startActivity(rentIntent)
                            finish()

                        }
                        else{ //대여중인경우
                            val mainIntent= Intent(this@loginActivity,onRentActivity::class.java)
                            startActivity(mainIntent)
                            finish()
                        }
                    }
                    else{
                        Log.d("main server","Some error occured")
                        Toast.makeText(applicationContext,"서버 응답이 잘못 되었습니다.",Toast.LENGTH_LONG).show()
                    }

                }
            }
        })
    }
}