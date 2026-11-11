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
import androidx.core.content.ContextCompat
import com.mashxall.unified.AppLanguage
import java.io.File
import java.security.MessageDigest
import java.net.HttpURLConnection
import java.net.URL

internal data class AdhanNotificationCopy(
    val fallbackPrayerTitle: String,
    val contentText: String,
    val stopAction: String,
    val pauseAction: String,
    val resumeAction: String,
    val channelName: String,
    val channelDescription: String
)

internal fun adhanNotificationCopy(language: AppLanguage): AdhanNotificationCopy = when (language) {
    AppLanguage.EN -> AdhanNotificationCopy("Prayer time", "Adhan · Mashxall", "Stop", "Pause", "Resume", "Prayer adhan", "Shown while the selected adhan audio plays.")
    AppLanguage.AR -> AdhanNotificationCopy("وقت الصلاة", "الأذان · مەشخەڵ", "إيقاف", "إيقاف مؤقت", "استئناف", "أذان الصلاة", "تظهر هذه القناة أثناء تشغيل صوت الأذان المختار.")
    AppLanguage.CKB -> AdhanNotificationCopy("کاتی نوێژ", "بانگ · مەشخەڵ", "وەستاندن", "وەستاندنی کاتی", "بەردەوامبوون", "بانگی نوێژ", "ئەم ئاگادارکردنەوەیە لە کاتی لێدانی دەنگی بانگی هەڵبژێردراو پیشان دەدرێت.")
}

internal fun resolveLocalAdhanFileReference(reference: String): File? {
    if (!reference.startsWith("local:")) return null
    val path = reference.removePrefix("local:").trim()
    if (path.isBlank()) return null
    return runCatching { File(path).takeIf { it.isFile && it.length() > 512L } }.getOrNull()
}

internal enum class AdhanAudioSourceSelection {
    DEFAULT_SYSTEM_ALARM,
    SELECTED_AUDIO,
    MISSING_SELECTED_AUDIO
}

/** Never substitute a generic ringtone for a non-empty user-selected adhan path. */
internal fun classifyAdhanAudioSource(assetPath: String, source: File?): AdhanAudioSourceSelection =
    when {
        assetPath.isBlank() -> AdhanAudioSourceSelection.DEFAULT_SYSTEM_ALARM
        source != null && source.isFile && source.length() > 512L -> AdhanAudioSourceSelection.SELECTED_AUDIO
        else -> AdhanAudioSourceSelection.MISSING_SELECTED_AUDIO
    }

internal fun missingAdhanAudioMessage(language: AppLanguage): String = when (language) {
    AppLanguage.EN -> "The selected adhan audio could not be played. Open Prayer settings and choose an available or downloaded track."
    AppLanguage.AR -> "تعذّر تشغيل ملف الأذان المحدد. افتح إعدادات الصلاة واختر تسجيلًا متاحًا أو منزّلًا."
    AppLanguage.CKB -> "نەتوانرا دەنگی بانگی هەڵبژێردراو لێ بدرێت؛ ڕێکخستنەکانی نوێژ بکەرەوە و دەنگێکی بەردەست یان داگیراو هەڵبژێرە."
}

class AdhanPlaybackService : Service() {
    private var player: MediaPlayer? = null
    private var stopRequested = false
    private var playerPrepared = false
    private var playbackPaused = false
    private var activePrayerLabel: String = ""
    private var activeNotificationCopy: AdhanNotificationCopy? = null
    private var activeChannelId: String = CHANNEL_ID

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopRequested = true
            releasePlayer()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        if (intent?.action == ACTION_TOGGLE) {
            togglePlayback()
            return START_NOT_STICKY
        }
        val prayerLabel = intent?.getStringExtra(EXTRA_PRAYER_LABEL).orEmpty()
        val audioPath = intent?.getStringExtra(EXTRA_AUDIO_PATH).orEmpty()
        val language = AppLanguage.from(
            getSharedPreferences("mashxall_preferences", Context.MODE_PRIVATE).getString("language", "ckb")
        )
        val notificationCopy = adhanNotificationCopy(language)
        val channelId = CHANNEL_ID + "_" + language.key
        createNotificationChannel(channelId, notificationCopy)
        activePrayerLabel = prayerLabel
        activeNotificationCopy = notificationCopy
        activeChannelId = channelId
        val notification = buildPlaybackNotification()
        startForeground(NOTIFICATION_ID, notification)
        stopRequested = false
        playAsset(audioPath, prayerLabel, notificationCopy, language, channelId)
        return START_NOT_STICKY
    }

    private fun buildPlaybackNotification(): Notification {
        val copy = activeNotificationCopy ?: adhanNotificationCopy(
            AppLanguage.from(
                getSharedPreferences("mashxall_preferences", Context.MODE_PRIVATE)
                    .getString("language", "ckb")
            )
        )
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        val togglePending = PendingIntent.getService(
            this,
            TOGGLE_REQUEST_CODE,
            Intent(this, AdhanPlaybackService::class.java).setAction(ACTION_TOGGLE),
            flags
        )
        val stopPending = PendingIntent.getService(
            this,
            STOP_REQUEST_CODE,
            Intent(this, AdhanPlaybackService::class.java).setAction(ACTION_STOP),
            flags
        )
        return NotificationCompat.Builder(this, activeChannelId)
            .setSmallIcon(com.mashxall.unified.R.drawable.ic_mashxall)
            .setContentTitle(activePrayerLabel.ifBlank { copy.fallbackPrayerTitle })
            .setContentText(copy.contentText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .addAction(
                if (playbackPaused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause,
                if (playbackPaused) copy.resumeAction else copy.pauseAction,
                togglePending
            )
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, copy.stopAction, stopPending)
            .build()
    }

    private fun publishNotification() {
        if (activeNotificationCopy == null) return
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIFICATION_ID, buildPlaybackNotification())
    }

    private fun togglePlayback() {
        val active = player ?: return
        if (!playerPrepared) return
        try {
            if (playbackPaused) {
                active.start()
                playbackPaused = false
            } else {
                active.pause()
                playbackPaused = true
            }
            publishNotification()
        } catch (_: Exception) {
            // The notification remains available; the service releases the player on its error/completion path.
        }
    }

    private fun downloadRemoteAdhanFile(url: String): File {
        require(url.startsWith("https://")) { "Adhan audio must use a secure HTTPS source." }
        val key = MessageDigest.getInstance("SHA-256").digest(url.toByteArray())
            .take(12).joinToString("") { "%02x".format(it) }
        val target = File(cacheDir, "adhan-stream-$key.audio")
        if (target.isFile && target.length() > 512L) return target
        val temporary = File(cacheDir, target.name + ".part")
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 60_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Mashxall Android")
        }
        try {
            connection.connect()
            val code = connection.responseCode
            if (code !in 200..299) throw java.io.IOException("The audio server returned HTTP $code.")
            if (connection.contentLengthLong > 100L * 1024L * 1024L) {
                throw java.io.IOException("The Adhan recording is larger than the supported download limit.")
            }
            if (connection.contentType.orEmpty().contains("text/html", ignoreCase = true)) {
                throw java.io.IOException("The audio source returned a web page instead of audio.")
            }
            var total = 0L
            connection.inputStream.use { input ->
                temporary.outputStream().buffered(64 * 1024).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        if (total > 100L * 1024L * 1024L) {
                            throw java.io.IOException("The Adhan recording exceeded the supported download limit.")
                        }
                        output.write(buffer, 0, count)
                    }
                }
            }
            if (total <= 512L || !temporary.isFile) {
                throw java.io.IOException("The audio source returned an empty or incomplete file.")
            }
            if (target.exists()) target.delete()
            if (!temporary.renameTo(target)) {
                temporary.copyTo(target, overwrite = true)
                temporary.delete()
            }
            return target
        } finally {
            if (temporary.exists()) temporary.delete()
            connection.disconnect()
        }
    }

    private fun createNotificationChannel(channelId: String, copy: AdhanNotificationCopy) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(channelId) == null) {
                val channel = NotificationChannel(channelId, copy.channelName, NotificationManager.IMPORTANCE_LOW).apply {
                    description = copy.channelDescription
                    setSound(null, null)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    private fun playAsset(
        assetPath: String,
        prayerLabel: String,
        notificationCopy: AdhanNotificationCopy,
        language: AppLanguage,
        channelId: String
    ) {
        releasePlayer()
        val source = try {
            when {
                assetPath.startsWith("local:") -> resolveLocalAdhanFileReference(assetPath)
                assetPath.startsWith("https://") -> downloadRemoteAdhanFile(assetPath)
                assetPath.isBlank() -> null
                else -> {
                    val hash = MessageDigest.getInstance("SHA-256").digest(assetPath.toByteArray())
                        .take(8).joinToString("") { "%02x".format(it) }
                    File(cacheDir, "adhan-$hash.audio").also { target ->
                        if (!target.isFile || target.length() <= 512L) {
                            target.delete()
                            assets.open(assetPath).use { input -> target.outputStream().use { output -> input.copyTo(output, 64 * 1024) } }
                        }
                    }
                }
            }
        } catch (_: Exception) { null }

        when (classifyAdhanAudioSource(assetPath, source)) {
            AdhanAudioSourceSelection.MISSING_SELECTED_AUDIO -> {
                // Keep the selected adhan truthful. Do not silently play an unrelated system ringtone.
                stopForeground(STOP_FOREGROUND_REMOVE)
                postMissingAudioNotification(prayerLabel, notificationCopy, language, channelId)
                stopSelf()
                return
            }
            else -> Unit
        }

        val mp = MediaPlayer()
        player = mp
        try {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            when (classifyAdhanAudioSource(assetPath, source)) {
                AdhanAudioSourceSelection.SELECTED_AUDIO -> mp.setDataSource(source!!.absolutePath)
                AdhanAudioSourceSelection.DEFAULT_SYSTEM_ALARM -> {
                    val fallback: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        ?: run {
                            releasePlayer()
                            stopForeground(STOP_FOREGROUND_REMOVE)
                            stopSelf()
                            return
                        }
                    mp.setDataSource(this, fallback)
                }
                AdhanAudioSourceSelection.MISSING_SELECTED_AUDIO -> {
                    // Guarded above; this branch exists for exhaustive enum handling.
                    releasePlayer()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    return
                }
            }
            mp.setOnPreparedListener { ready ->
                if (!stopRequested) {
                    playerPrepared = true
                    playbackPaused = false
                    ready.start()
                    publishNotification()
                }
            }
            mp.setOnCompletionListener {
                releasePlayer()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            mp.setOnErrorListener { _, _, _ ->
                releasePlayer()
                stopForeground(STOP_FOREGROUND_REMOVE)
                if (assetPath.isNotBlank()) {
                    postMissingAudioNotification(prayerLabel, notificationCopy, language, channelId)
                }
                stopSelf()
                true
            }
            mp.prepareAsync()
        } catch (_: Exception) {
            releasePlayer()
            stopForeground(STOP_FOREGROUND_REMOVE)
            if (assetPath.isNotBlank()) {
                postMissingAudioNotification(prayerLabel, notificationCopy, language, channelId)
            }
            stopSelf()
        }
    }

    private fun postMissingAudioNotification(
        prayerLabel: String,
        copy: AdhanNotificationCopy,
        language: AppLanguage,
        channelId: String
    ) {
        val notice = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(com.mashxall.unified.R.drawable.ic_mashxall)
            .setContentTitle(prayerLabel.ifBlank { copy.fallbackPrayerTitle })
            .setContentText(missingAdhanAudioMessage(language))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(MISSING_AUDIO_NOTIFICATION_ID, notice)
    }

    private fun releasePlayer() {
        try { player?.setOnCompletionListener(null); player?.setOnErrorListener(null); player?.release() } catch (_: Exception) {}
        player = null
        playerPrepared = false
        playbackPaused = false
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
        private const val MISSING_AUDIO_NOTIFICATION_ID = 7844
        private const val STOP_REQUEST_CODE = 7843
        private const val TOGGLE_REQUEST_CODE = 7845
        const val ACTION_STOP = "com.mashxall.unified.STOP_ADHAN"
        const val ACTION_TOGGLE = "com.mashxall.unified.TOGGLE_ADHAN"

        /** Start an Adhan-library preview in the same foreground service that owns its notification controls. */
        fun startPreview(context: Context, audioPath: String, title: String) {
            val intent = Intent(context.applicationContext, AdhanPlaybackService::class.java)
                .putExtra(EXTRA_PRAYER_LABEL, title)
                .putExtra(EXTRA_AUDIO_PATH, audioPath)
            ContextCompat.startForegroundService(context.applicationContext, intent)
        }

        /** Stop a preview or a currently playing scheduled Adhan. */
        fun stopPlayback(context: Context) {
            val intent = Intent(context.applicationContext, AdhanPlaybackService::class.java)
                .setAction(ACTION_STOP)
            runCatching { context.applicationContext.startService(intent) }
                .onFailure { runCatching { ContextCompat.startForegroundService(context.applicationContext, intent) } }
        }
    }
}
