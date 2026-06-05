package com.hnkapps.slanotif

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SlackNotificationListener : NotificationListenerService() {
    private val TAG = "SlanotifService"
    private var mediaPlayer: MediaPlayer? = null
    private var silenceReceiver: BroadcastReceiver? = null
    private var volumeObserver: ContentObserver? = null
    private var audioManager: AudioManager? = null
    private val gson = Gson()

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName ?: return
        if (!pkg.contains("slack", ignoreCase = true)) return

        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        val subText = extras.getCharSequence("android.subText")?.toString() ?: ""

        Log.d(TAG, "New Slack Notification -> Title: '$title', Text: '$text', SubText: '$subText'")

        val prefs = getSharedPreferences("slack_prefs", Context.MODE_PRIVATE)
        val rulesJson = prefs.getString("notification_rules", "[]")
        val type = object : TypeToken<List<NotificationRule>>() {}.type
        val rules: List<NotificationRule> = gson.fromJson(rulesJson, type)

        Log.d(TAG, "Active rules count: ${rules.size}")

        // Find the BEST match (exact match or most specific)
        var matchedRule: NotificationRule? = null

        // Priority 1: Exact match in title (usually the channel/DM name)
        matchedRule = rules.find { it.channelName.equals(title, ignoreCase = true) }
        
        // Priority 2: Contains match in title
        if (matchedRule == null) {
            matchedRule = rules.find { title.contains(it.channelName, ignoreCase = true) }
        }

        // Priority 3: Contains match in text or subText
        if (matchedRule == null) {
            matchedRule = rules.find { 
                text.contains(it.channelName, ignoreCase = true) || 
                subText.contains(it.channelName, ignoreCase = true) 
            }
        }

        if (matchedRule != null) {
            Log.d(TAG, "MATCH FOUND! Rule: '${matchedRule.channelName}', Sound: ${matchedRule.soundUri}")
            val soundUri = matchedRule.soundUri
            if (soundUri != null) {
                playSound(Uri.parse(soundUri))
            } else {
                playDefaultSound()
            }
        } else {
            Log.d(TAG, "No rule matched this notification.")
        }
    }

    private fun playSound(uri: Uri) {
        stopCurrentSound()
        try {
            requestAudioFocus()
            Log.d(TAG, "Attempting to play URI: $uri")
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setLegacyStreamType(AudioManager.STREAM_NOTIFICATION)
                        .build()
                )
                setDataSource(applicationContext, uri)
                prepare()
                start()
                setOnCompletionListener { cleanup() }
            }
            registerSilencers()
        } catch (e: Exception) {
            Log.e(TAG, "Playback failed for URI: $uri", e)
            playDefaultSound()
        }
    }

    private fun playDefaultSound() {
        try {
            stopCurrentSound()
            requestAudioFocus()
            mediaPlayer = MediaPlayer.create(this, android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)
            mediaPlayer?.apply {
                start()
                setOnCompletionListener { cleanup() }
                registerSilencers()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Default sound failed", e)
        }
    }

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build())
                .build()
            audioManager?.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            audioManager?.requestAudioFocus(null, AudioManager.STREAM_NOTIFICATION, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        }
    }

    private fun stopCurrentSound() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.reset()
                it.release()
            }
        } catch (_: Exception) {
        } finally {
            mediaPlayer = null
            unregisterSilencers()
        }
    }

    private fun cleanup() {
        mediaPlayer?.release()
        mediaPlayer = null
        unregisterSilencers()
    }

    private fun registerSilencers() {
        if (silenceReceiver == null) {
            silenceReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    Log.d(TAG, "Silencing via broadcast: ${intent?.action}")
                    stopCurrentSound()
                }
            }
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
                addAction("android.intent.action.CLOSE_SYSTEM_DIALOGS")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(silenceReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                registerReceiver(silenceReceiver, filter)
            }
        }

        if (volumeObserver == null) {
            volumeObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    super.onChange(selfChange)
                    Log.d(TAG, "Volume change detected, silencing...")
                    stopCurrentSound()
                }
            }
            contentResolver.registerContentObserver(
                android.provider.Settings.System.CONTENT_URI,
                true,
                volumeObserver!!
            )
        }
    }

    private fun unregisterSilencers() {
        silenceReceiver?.let {
            try { unregisterReceiver(it) } catch (_: Exception) {}
        }
        silenceReceiver = null

        volumeObserver?.let {
            try { contentResolver.unregisterContentObserver(it) } catch (_: Exception) {}
        }
        volumeObserver = null
    }

    override fun onDestroy() {
        stopCurrentSound()
        super.onDestroy()
    }
}
