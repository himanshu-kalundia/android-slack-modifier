package com.example.slacknotif

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("slack_prefs", MODE_PRIVATE)

        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24,24,24,24) }
        val channelInput = EditText(this).apply { hint = "Channel name to match" }
        val soundInput = EditText(this).apply { hint = "Sound resource name (raw)" }
        val saveBtn = Button(this).apply { text = "Save" }

        channelInput.setText(prefs.getString("channel_name", ""))
        soundInput.setText(prefs.getString("sound_name", "important_tone"))

        saveBtn.setOnClickListener {
            prefs.edit()
                .putString("channel_name", channelInput.text.toString())
                .putString("sound_name", soundInput.text.toString())
                .apply()
            finish()
        }

        layout.addView(channelInput)
        layout.addView(soundInput)
        layout.addView(saveBtn)
        setContentView(layout)
    }
}
