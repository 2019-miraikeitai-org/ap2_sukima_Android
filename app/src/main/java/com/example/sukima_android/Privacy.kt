package com.example.sukima_android

import android.os.Bundle
import android.view.View.FOCUS_DOWN
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.privacy.*

class Privacy: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.privacy)

        scrollView2.fullScroll(FOCUS_DOWN);

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return super.onSupportNavigateUp()
    }
}