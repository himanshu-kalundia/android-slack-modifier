package com.hnkapps.slanotif

import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    private var selectedSoundUri: String? = null
    private var testPlayer: MediaPlayer? = null

    private val pickAudioLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            try {
                contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                selectedSoundUri = it.toString()
                findViewById<TextView>(R.id.tv_sound_uri).text = "Selected: ${it.lastPathSegment ?: it.toString()}"
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to get permission for this file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val prefs = getSharedPreferences("slack_prefs", MODE_PRIVATE)

        val etChannelName = findViewById<EditText>(R.id.et_channel_name)
        val tvSoundUri = findViewById<TextView>(R.id.tv_sound_uri)
        val btnPickSound = findViewById<Button>(R.id.btn_pick_sound)
        val btnTestSound = findViewById<Button>(R.id.btn_test_sound)
        val btnSave = findViewById<Button>(R.id.btn_save)

        etChannelName.setText(prefs.getString("channel_name", ""))
        selectedSoundUri = prefs.getString("sound_uri", null)
        
        selectedSoundUri?.let {
            tvSoundUri.text = "Selected: $it"
        }

        btnPickSound.setOnClickListener {
            pickAudioLauncher.launch(arrayOf("audio/*"))
        }

        btnTestSound.setOnClickListener {
            playTestSound()
        }

        btnSave.setOnClickListener {
            prefs.edit()
                .putString("channel_name", etChannelName.text.toString())
                .putString("sound_uri", selectedSoundUri)
                .apply()
            finish()
        }
    }

    private fun playTestSound() {
        val uriStr = selectedSoundUri
        if (uriStr == null) {
            Toast.makeText(this, "Please pick a sound first", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            testPlayer?.release()
            testPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@SettingsActivity, Uri.parse(uriStr))
                prepare()
                start()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error playing sound: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        testPlayer?.release()
        super.onDestroy()
    }
}
