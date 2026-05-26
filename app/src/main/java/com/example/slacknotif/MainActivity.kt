package com.example.slacknotif

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.LinearLayout
import android.provider.Settings

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val btnSettings = Button(this).apply { text = "Settings" }
        val btnNotifAccess = Button(this).apply { text = "Open Notification Access" }

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        btnNotifAccess.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        layout.addView(btnSettings)
        layout.addView(btnNotifAccess)
        setContentView(layout)
    }
}
