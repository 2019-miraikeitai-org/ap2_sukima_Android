package com.example.sukima_android

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.menuelist.*

class MenueList : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.menuelist)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setumei.setOnClickListener {
            val intent = Intent(this, Description::class.java)
            startActivity(intent)
        }

        help.setOnClickListener {
            val intent = Intent(this, Help::class.java)
            startActivity(intent)
        }

        privacy.setOnClickListener {
            val intent = Intent(this, Privacy::class.java)
            startActivity(intent)
        }

        terms.setOnClickListener {
            val intent = Intent(this, Terms::class.java)
            startActivity(intent)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return super.onSupportNavigateUp()
    }
}