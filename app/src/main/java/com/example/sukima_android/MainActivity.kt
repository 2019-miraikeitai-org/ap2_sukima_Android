package com.example.sukima_android

import android.content.Context
import android.os.Bundle
import android.content.Intent
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*



class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val data = getSharedPreferences("Data", Context.MODE_PRIVATE)
        var dataInt = data.getInt("DataInt", 0)

        Log.d("data", dataInt.toString())

        var test = 1

        if (dataInt != 0){
            test = 0
    }

        if (test == 1) {
            start_Button.setOnClickListener {
                val intent = Intent(this, user_registration::class.java)
                startActivity(intent)
            }
        } else
            start_Button.setOnClickListener {
                val intent = Intent(this, time_layout::class.java)
                startActivity(intent)
            }
    }
}