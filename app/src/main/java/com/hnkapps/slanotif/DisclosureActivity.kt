package com.hnkapps.slanotif

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class DisclosureActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_disclosure)

        findViewById<Button>(R.id.btn_accept).setOnClickListener {
            val prefs = getSharedPreferences("slack_prefs", MODE_PRIVATE)
            prefs.edit().putBoolean("disclosure_accepted", true).apply()
            
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        findViewById<Button>(R.id.btn_exit).setOnClickListener {
            finishAffinity()
        }
    }
}
