package com.example.penaltykick

import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
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
        signIn=findViewById<TextView>(R.id.sign_in)
        id=findViewById(R.id.id)
        password=findViewById(R.id.password)

        mPreferences=getSharedPreferences("user", MODE_PRIVATE)


        loginBtn.setOnClickListener {
            login(id.text.toString(),password.text.toString())
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