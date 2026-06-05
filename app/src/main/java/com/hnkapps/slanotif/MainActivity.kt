package com.hnkapps.slanotif

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.app.Activity
import android.media.RingtoneManager
import android.provider.OpenableColumns
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: RulesAdapter
    private var rules = mutableListOf<NotificationRule>()
    private val gson = Gson()
    
    private var tempSoundUri: String? = null
    private var tempSoundName: String? = null
    private var dialogSoundText: TextView? = null

    // Launcher for external file picker
    private val pickAudioLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            try {
                contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                tempSoundUri = it.toString()
                tempSoundName = getSoundNameFromUri(it)
                dialogSoundText?.text = "Selected: $tempSoundName"
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to get permission for this file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Launcher for system ringtone/notification picker
    private val ringtonePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            uri?.let {
                tempSoundUri = it.toString()
                tempSoundName = getSoundNameFromUri(it)
                dialogSoundText?.text = "Selected: $tempSoundName"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // One-time Disclosure Check
        val prefs = getSharedPreferences("slack_prefs", MODE_PRIVATE)
        if (!prefs.getBoolean("disclosure_accepted", false)) {
            startActivity(Intent(this, DisclosureActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        loadRules()

        val rvRules = findViewById<RecyclerView>(R.id.rv_rules)
        rvRules.layoutManager = LinearLayoutManager(this)
        adapter = RulesAdapter(rules, 
            onDelete = { rule -> deleteRule(rule) },
            onEdit = { rule -> showEditDialog(rule) }
        )
        rvRules.adapter = adapter

        findViewById<View>(R.id.fab_add_rule).setOnClickListener {
            showEditDialog(null)
        }

        findViewById<Button>(R.id.btn_notif_access).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        updateEmptyState()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
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
        updateEmptyState()
    }

    private fun updateEmptyState() {
        findViewById<TextView>(R.id.tv_empty_desc).visibility = if (rules.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun deleteRule(rule: NotificationRule) {
        AlertDialog.Builder(this)
            .setTitle("Delete Rule")
            .setMessage("Delete rule for ${rule.channelName}?")
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
        tempSoundName = rule?.soundName
        
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_rule, null)
        val etName = dialogView.findViewById<EditText>(R.id.et_channel_name)
        dialogSoundText = dialogView.findViewById(R.id.tv_selected_sound)
        val btnPick = dialogView.findViewById<Button>(R.id.btn_pick_sound)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tv_dialog_title)

        if (rule != null) {
            tvTitle.text = "Edit Rule"
            etName.setText(rule.channelName)
            dialogSoundText?.text = "Selected: ${rule.soundName ?: "Default"}"
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
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Sound")
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
                if (name.isBlank()) return@setPositiveButton

                if (rule != null) {
                    rule.channelName = name
                    rule.soundUri = tempSoundUri
                    rule.soundName = tempSoundName
                } else {
                    rules.add(NotificationRule(UUID.randomUUID().toString(), name, tempSoundUri, tempSoundName))
                }
                adapter.updateRules(rules)
                saveRules()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun getSoundNameFromUri(uri: Uri): String {
        var name = "Unknown Sound"
        try {
            // Try RingtoneManager first (for system tones)
            val ringtone = RingtoneManager.getRingtone(this, uri)
            if (ringtone != null) {
                name = ringtone.getTitle(this)
            } else {
                // Try ContentResolver (for external files)
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIndex != -1) {
                        name = cursor.getString(nameIndex)
                    }
                }
            }
        } catch (e: Exception) {
            // Fallback to last segment of URI
            name = uri.lastPathSegment ?: "Custom Sound"
        }
        return name
    }

    private fun updateStatus() {
        val isEnabled = isNotificationServiceEnabled()
        val cardStatus = findViewById<MaterialCardView>(R.id.card_status)
        val tvDesc = findViewById<TextView>(R.id.tv_status_desc)
        val btnAccess = findViewById<Button>(R.id.btn_notif_access)

        if (isEnabled) {
            tvDesc.text = "Slanotif is active."
            tvDesc.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            cardStatus.setStrokeColor(ContextCompat.getColorStateList(this, android.R.color.holo_green_light))
            btnAccess.text = "Settings"
        } else {
            tvDesc.text = "Permission required."
            tvDesc.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            cardStatus.setStrokeColor(ContextCompat.getColorStateList(this, android.R.color.holo_red_light))
            btnAccess.text = "Grant"
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val pkgName = packageName
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        if (!TextUtils.isEmpty(flat)) {
            val names = flat.split(":")
            for (name in names) {
                val cn = ComponentName.unflattenFromString(name)
                if (cn != null && TextUtils.equals(pkgName, cn.packageName)) {
                    return true
                }
            }
        }
        return false
    }
}
