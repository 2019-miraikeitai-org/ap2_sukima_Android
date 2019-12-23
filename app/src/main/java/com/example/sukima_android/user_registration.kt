package com.example.sukima_android

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.example.sukima_android.model.post_user
import com.facebook.stetho.okhttp3.StethoInterceptor
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.android.synthetic.main.user_registration.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.lang.Integer.parseInt
import kotlin.coroutines.CoroutineContext

class user_registration : AppCompatActivity() , CoroutineScope {


    private val okHttp = OkHttpClient()
        .newBuilder()
        .addNetworkInterceptor(StethoInterceptor())
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val retrofit = Retrofit.Builder()
        .client(okHttp)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .baseUrl("http://160.16.103.99")
        .build()

    private val client by lazy { retrofit.create(SkimattiClient::class.java) }

    private val job = Job()
    override val coroutineContext: CoroutineContext = Dispatchers.Main + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user_registration)



    RegistrationButton.setOnClickListener {



        if(Man.isChecked == true || Women.isChecked == true || Custom.isChecked == true) {
        launch {
            try {


                var age: Int = 0
                var gender: String = ""

                //　年齢が入力されたときの処理
                if (editText.text != null) {
                    var Age = findViewById(R.id.editText) as EditText // 入力した年齢を変数に格納
                    age = parseInt(Age.text.toString())
                }

                // ラジオボタンが押されたときの処理
                if (Man.isChecked == true) {
                    gender = "man"
                } else if (Women.isChecked == true) {
                    gender = "woman"
                } else if (Custom.isChecked == true) {
                    gender = "custom"
                }

                Log.d("resp", age.toString())
                Log.d("resp", gender.toString())

                val postdata = post_user(age,gender)
                val resp = client.postUser(postdata)

                Log.d("resp", resp.toString())

                val data: SharedPreferences = getSharedPreferences("Data", Context.MODE_PRIVATE)
                val editor = data.edit()
                editor.putInt("DataInt", resp.user_id)
                editor.apply()

            } catch (e: Throwable) {
                Log.e("resp", e.toString())
            }
        }

        val intent = Intent(this, time_layout::class.java)
        startActivity(intent)


    }
}
    }
}