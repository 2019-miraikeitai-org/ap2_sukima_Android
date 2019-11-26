package com.example.sukima_android

import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.user_registration.*
import android.content.Intent

class user_registration : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user_registration)



        RegistrationButton.setOnClickListener {
            //　年齢が入力されたときの処理
            if(editText.text !=null){
                var age = findViewById(R.id.editText) as EditText // 入力した年齢を変数に格納
            }

            // ラジオボタンが押されたときの処理
            if(Man.isChecked == true){

            }
            else if(Women.isChecked == true){

            }
            else if(Custom.isChecked == true){

            }

            val intent = Intent(this, time_layout::class.java)
            startActivity(intent)
        }

    }
}