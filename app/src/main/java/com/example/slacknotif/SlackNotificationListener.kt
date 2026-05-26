package com.example.slacknotif

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class SlackNotificationListener : NotificationListenerService() {
    private val TAG = "SlackNotifListener"

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName ?: return
        if (!pkg.contains("slack", ignoreCase = true)) return

        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""

        Log.d(TAG, "Notification received from Slack: title=$title, text=$text")

        val prefs = getSharedPreferences("slack_prefs", Context.MODE_PRIVATE)
        val targetChannel = prefs.getString("channel_name", "") ?: ""
        
        if (targetChannel.isBlank()) {
            Log.d(TAG, "No channel name configured in settings.")
            return
        }

        if (title.contains(targetChannel, ignoreCase = true) || text.contains(targetChannel, ignoreCase = true)) {
            Log.d(TAG, "Match found for channel: $targetChannel")
            val soundName = prefs.getString("sound_name", "important_tone") ?: "important_tone"
            val resId = resources.getIdentifier(soundName, "raw", packageName)
            
            if (resId != 0) {
                playSound(Uri.parse("android.resource://$packageName/$resId"))
            } else {
                Log.w(TAG, "Sound resource '$soundName' not found, playing default.")
                playDefaultSound()
            }
        }
    }

    private fun playSound(uri: Uri) {
        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(applicationContext, uri)
                isLooping = false
                prepare()
                start()
            }
            player.setOnCompletionListener { it.release() }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing sound", e)
            playDefaultSound()
        }
    }

    private fun playDefaultSound() {
        try {
            val defaultPlayer = MediaPlayer.create(this, android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)
            defaultPlayer?.start()
            defaultPlayer?.setOnCompletionListener { it.release() }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing default sound", e)
        }
    }
}
