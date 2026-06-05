package com.hnkapps.slanotif

import android.app.Activity
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

class SettingsActivity : AppCompatActivity() {
    private lateinit var adapter: RulesAdapter
    private var rules = mutableListOf<NotificationRule>()
    private val gson = Gson()
    
    private var tempSoundUri: String? = null
    private var testPlayer: MediaPlayer? = null
    private var dialogSoundText: TextView? = null

    // Launcher for external file picker
    private val pickAudioLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            try {
                contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                updateSelectedSound(it)
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to get permission for this file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Launcher for system ringtone/notification picker
    private val ringtonePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            uri?.let { updateSelectedSound(it) }
        }
    }

    private fun updateSelectedSound(uri: Uri) {
        tempSoundUri = uri.toString()
        dialogSoundText?.text = "Selected: ${uri.lastPathSegment ?: "Custom Sound"}"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        loadRules()

        val rvRules = findViewById<RecyclerView>(R.id.rv_rules)
        rvRules.layoutManager = LinearLayoutManager(this)
        adapter = RulesAdapter(rules, 
            onDelete = { rule -> deleteRule(rule) },
            onEdit = { rule -> showEditDialog(rule) }
        )
        rvRules.adapter = adapter

        findViewById<Button>(R.id.btn_add_rule).setOnClickListener {
            showEditDialog(null)
        }
    }

    private fun loadRules() {
        val prefs = getSharedPreferences("slack_prefs", MODE_PRIVATE)
        val json = prefs.getString("notification_rules", "[]")
        val type = object : TypeToken<MutableList<NotificationRule>>() {}.type
        rules = gson.fromJson(json, type) ?: mutableListOf()
    }

    private fun saveRules() {
        val prefs = getSharedPreferences("slack_prefs", MODE_PRIVATE)
        val json = gson.toJson(rules)
        prefs.edit().putString("notification_rules", json).apply()
    }

    private fun deleteRule(rule: NotificationRule) {
        AlertDialog.Builder(this)
            .setTitle("Delete Rule")
            .setMessage("Are you sure you want to delete this rule for ${rule.channelName}?")
            .setPositiveButton("Delete") { _, _ ->
                rules.remove(rule)
                adapter.updateRules(rules)
                saveRules()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditDialog(rule: NotificationRule?) {
        tempSoundUri = rule?.soundUri
        
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_rule, null)
        val etName = dialogView.findViewById<EditText>(R.id.et_channel_name)
        dialogSoundText = dialogView.findViewById(R.id.tv_selected_sound)
        val btnPick = dialogView.findViewById<Button>(R.id.btn_pick_sound)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tv_dialog_title)

        if (rule != null) {
            tvTitle.text = "Edit Rule"
            etName.setText(rule.channelName)
            dialogSoundText?.text = if (rule.soundUri != null) "Selected: ${Uri.parse(rule.soundUri).lastPathSegment}" else "No sound selected"
        } else {
            tvTitle.text = "Add New Rule"
        }

        btnPick.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menu.add("System Tones")
            popup.menu.add("External Audio File")
            popup.setOnMenuItemClickListener { item ->
                when (item.title) {
                    "System Tones" -> {
                        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION or RingtoneManager.TYPE_RINGTONE)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Notification Sound")
                            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, tempSoundUri?.let { Uri.parse(it) })
                        }
                        ringtonePickerLauncher.launch(intent)
                    }
                    "External Audio File" -> {
                        pickAudioLauncher.launch(arrayOf("audio/*"))
                    }
                }
                true
            }
            popup.show()
        }

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name = etName.text.toString()
                if (name.isBlank()) {
                    Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (rule != null) {
                    rule.channelName = name
                    rule.soundUri = tempSoundUri
                } else {
                    rules.add(NotificationRule(UUID.randomUUID().toString(), name, tempSoundUri))
                }
                adapter.updateRules(rules)
                saveRules()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroy() {
        testPlayer?.release()
        super.onDestroy()
    }
}
