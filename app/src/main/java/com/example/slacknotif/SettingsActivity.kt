package com.example.slacknotif

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    private var selectedSoundUri: String? = null

    private val pickAudioLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            selectedSoundUri = it.toString()
            findViewById<TextView>(R.id.tv_sound_uri).text = "Selected: ${it.lastPathSegment ?: it.toString()}"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val prefs = getSharedPreferences("slack_prefs", MODE_PRIVATE)

        val etChannelName = findViewById<EditText>(R.id.et_channel_name)
        val tvSoundUri = findViewById<TextView>(R.id.tv_sound_uri)
        val btnPickSound = findViewById<Button>(R.id.btn_pick_sound)
        val btnSave = findViewById<Button>(R.id.btn_save)

        etChannelName.setText(prefs.getString("channel_name", ""))
        selectedSoundUri = prefs.getString("sound_uri", null)
        
        selectedSoundUri?.let {
            tvSoundUri.text = "Selected: $it"
        }

        btnPickSound.setOnClickListener {
            pickAudioLauncher.launch(arrayOf("audio/*"))
        }

        btnSave.setOnClickListener {
            prefs.edit()
                .putString("channel_name", etChannelName.text.toString())
                .putString("sound_uri", selectedSoundUri)
                .apply()
            finish()
        }
    }
}
