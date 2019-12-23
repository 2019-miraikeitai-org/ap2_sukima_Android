package com.example.sukima_android

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.terms_of_service.*


class TermsOfService : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.terms_of_service)

        val CheckBox: CheckBox = findViewById(R.id.checkbox)

        CheckBox.setOnCheckedChangeListener { _, isChecked ->
            val stateMessage = if (isChecked) "同意されました"  else  "選択解除されました"
            Toast.makeText(this, "$stateMessage", Toast.LENGTH_SHORT).show()

            if(isChecked){
                nextButton.visibility = View.VISIBLE
            }else{
                nextButton.visibility = View.INVISIBLE
            }
        }

        nextButton.setOnClickListener {
            val intent = Intent(this, user_registration::class.java)
            startActivity(intent)
        }


    }
}