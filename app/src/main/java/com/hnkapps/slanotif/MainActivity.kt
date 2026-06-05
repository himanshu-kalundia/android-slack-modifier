package com.hnkapps.slanotif

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btn_settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        findViewById<Button>(R.id.btn_notif_access).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        val isEnabled = isNotificationServiceEnabled()
        val cardStatus = findViewById<MaterialCardView>(R.id.card_status)
        val tvDesc = findViewById<TextView>(R.id.tv_status_desc)
        val btnAccess = findViewById<Button>(R.id.btn_notif_access)

        if (isEnabled) {
            tvDesc.text = "Permission Granted! Slanotif is active."
            cardStatus.setCardBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_green_light))
            btnAccess.text = "Manage Access"
        } else {
            tvDesc.text = "The app needs permission to see notifications."
            cardStatus.setCardBackgroundColor(ContextCompat.getColor(this, R.color.surface))
            btnAccess.text = "Enable Notification Access"
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
