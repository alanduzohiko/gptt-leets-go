package com.mashxall.unified.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.io.File
import java.security.MessageDigest

class AdhanPlaybackService : Service() {
    private var player: MediaPlayer? = null
    private var stopRequested = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopRequested = true
            releasePlayer()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        val prayerLabel = intent?.getStringExtra(EXTRA_PRAYER_LABEL).orEmpty()
        val audioPath = intent?.getStringExtra(EXTRA_AUDIO_PATH).orEmpty()
        createNotificationChannel()
        val stopPending = PendingIntent.getService(
            this,
            STOP_REQUEST_CODE,
            Intent(this, AdhanPlaybackService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(com.mashxall.unified.R.drawable.ic_mashxall)
            .setContentTitle(prayerLabel.ifBlank { "Prayer time" })
            .setContentText("Adhan · مەشخەڵ")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopPending)
            .build()
        startForeground(NOTIFICATION_ID, notification)
        stopRequested = false
        playAsset(audioPath)
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(CHANNEL_ID, "Prayer adhan", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Shown while the selected adhan audio plays."
                    setSound(null, null)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    private fun playAsset(assetPath: String) {
        releasePlayer()
        val source = try {
            if (assetPath.isBlank()) null else {
                val hash = MessageDigest.getInstance("SHA-256").digest(assetPath.toByteArray())
                    .take(8).joinToString("") { "%02x".format(it) }
                File(cacheDir, "adhan-$hash.audio").also { target ->
                    if (!target.exists() || target.length() == 0L) {
                        assets.open(assetPath).use { input -> target.outputStream().use { output -> input.copyTo(output, 64 * 1024) } }
                    }
                }
            }
        } catch (_: Exception) { null }
        val mp = MediaPlayer()
        player = mp
        try {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            if (source != null && source.exists() && source.length() > 0L) {
                mp.setDataSource(source.absolutePath)
            } else {
                val fallback: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                mp.setDataSource(this, fallback)
            }
            mp.setOnPreparedListener { ready -> if (!stopRequested) ready.start() }
            mp.setOnCompletionListener {
                releasePlayer()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            mp.setOnErrorListener { _, _, _ ->
                releasePlayer()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                true
            }
            mp.prepareAsync()
        } catch (_: Exception) {
            releasePlayer()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun releasePlayer() {
        try { player?.setOnCompletionListener(null); player?.setOnErrorListener(null); player?.release() } catch (_: Exception) {}
        player = null
    }

    override fun onDestroy() {
        releasePlayer()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_PRAYER_CODE = "prayer_code"
        const val EXTRA_PRAYER_LABEL = "prayer_label"
        const val EXTRA_AUDIO_PATH = "audio_path"
        private const val CHANNEL_ID = "mashxall_adhan_playback"
        private const val NOTIFICATION_ID = 7842
        private const val STOP_REQUEST_CODE = 7843
        const val ACTION_STOP = "com.mashxall.unified.STOP_ADHAN"
    }
}
