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
import android.media.session.PlaybackState
import android.media.session.MediaSession
import android.media.MediaMetadata
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.mashxall.unified.AppLanguage
import java.io.File
import java.security.MessageDigest
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    private var activePrayerCode: String = ""
    private var activeMuezzinName: String = ""
    private var activeNotificationCopy: AdhanNotificationCopy? = null
    private var activeChannelId: String = CHANNEL_ID
    private var mediaSession: MediaSession? = null
    private var playbackRequestGeneration: Int = 0
    private val playbackScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopPlaybackAndService()
            return START_NOT_STICKY
        }
        if (intent?.action == ACTION_TOGGLE) {
            togglePlayback()
            return START_NOT_STICKY
        }
        val prayerLabel = intent?.getStringExtra(EXTRA_PRAYER_LABEL).orEmpty()
        val prayerCode = intent?.getStringExtra(EXTRA_PRAYER_CODE).orEmpty()
        val muezzinName = intent?.getStringExtra(EXTRA_ADHAN_NAME).orEmpty()
        val audioPath = intent?.getStringExtra(EXTRA_AUDIO_PATH).orEmpty()
        val language = AppLanguage.from(
            getSharedPreferences("mashxall_preferences", Context.MODE_PRIVATE).getString("language", "ckb")
        )
        val notificationCopy = adhanNotificationCopy(language)
        val channelId = CHANNEL_ID + "_" + language.key
        createNotificationChannel(channelId, notificationCopy)
        activePrayerLabel = prayerLabel
        activePrayerCode = prayerCode
        activeMuezzinName = muezzinName
        activeNotificationCopy = notificationCopy
        activeChannelId = channelId
        ensureMediaSession()
        updateMediaSessionMetadata()
        updateMediaSessionPlaybackState()
        val notification = buildPlaybackNotification()
        startForeground(NOTIFICATION_ID, notification)
        if (activePrayerCode.isNotBlank()) {
            postPrayerAlarmAlert(activePrayerCode, activePrayerLabel, activeMuezzinName, language)
        }
        stopRequested = false
        playbackRequestGeneration += 1
        playAsset(audioPath, prayerLabel, notificationCopy, language, channelId, playbackRequestGeneration)
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
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, activeChannelId)
        } else {
            Notification.Builder(this)
        }
        return builder
            .setSmallIcon(com.mashxall.unified.R.drawable.ic_mashxall)
            .setContentTitle(activePrayerLabel.ifBlank { copy.fallbackPrayerTitle })
            .setContentText(activeMuezzinName.ifBlank { copy.contentText })
            .setPriority(Notification.PRIORITY_LOW)
            .setCategory(Notification.CATEGORY_TRANSPORT)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setOngoing(true)
            .addAction(
                if (playbackPaused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause,
                if (playbackPaused) copy.resumeAction else copy.pauseAction,
                togglePending
            )
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, copy.stopAction, stopPending)
            .setStyle(
                Notification.MediaStyle()
                    .setMediaSession(mediaSession?.sessionToken)
                    .setShowActionsInCompactView(0, 1)
            )
            .build()
    }

    private fun publishNotification() {
        if (activeNotificationCopy == null) return
        updateMediaSessionPlaybackState()
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
            updateMediaSessionPlaybackState()
            publishNotification()
        } catch (_: Exception) {
            // The notification remains available; the service releases the player on its error/completion path.
        }
    }

    private fun ensureMediaSession() {
        if (mediaSession != null) return
        mediaSession = MediaSession(this, "Mashxall Adhan").apply {
            setFlags(
                MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() {
                    if (playerPrepared && playbackPaused) togglePlayback()
                }

                override fun onPause() {
                    if (playerPrepared && !playbackPaused) togglePlayback()
                }

                override fun onStop() {
                    stopPlaybackAndService()
                }
            })
            isActive = true
        }
    }

    private fun updateMediaSessionMetadata() {
        val copy = activeNotificationCopy ?: return
        mediaSession?.setMetadata(
            MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, activePrayerLabel.ifBlank { copy.fallbackPrayerTitle })
                .putString(MediaMetadata.METADATA_KEY_ARTIST, activeMuezzinName.ifBlank { copy.contentText })
                .build()
        )
    }

    private fun postPrayerAlarmAlert(
        prayerCode: String,
        prayerLabel: String,
        muezzinName: String,
        language: AppLanguage
    ) {
        val alertChannelId = ALERT_CHANNEL_PREFIX + "_" + language.key
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(alertChannelId) == null) {
                manager.createNotificationChannel(
                    NotificationChannel(
                        alertChannelId,
                        when (language) {
                            AppLanguage.EN -> "Prayer time alerts"
                            AppLanguage.AR -> "تنبيهات أوقات الصلاة"
                            AppLanguage.CKB -> "ئاگادارکردنەوەی کاتەکانی نوێژ"
                        },
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = when (language) {
                            AppLanguage.EN -> "Opens the selected Adhan screen when an enabled prayer begins."
                            AppLanguage.AR -> "تعرض شاشة الأذان المختار عند دخول وقت الصلاة المفعّلة."
                            AppLanguage.CKB -> "لە کاتی هاتنی نوێژی چالاککراودا شاشەی بانگی هەڵبژێردراو پیشان دەدات."
                        }
                        // The selected Adhan file supplies the sound; don't overlay a generic chime.
                        setSound(null, null)
                        enableVibration(true)
                        vibrationPattern = longArrayOf(0L, 260L, 150L, 260L)
                        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    }
                )
            }
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        val prayerIndex = PrayerAlarmScheduler.ALERT_CODES.indexOf(prayerCode).coerceAtLeast(0)
        val screenIntent = Intent(this, AdhanAlertActivity::class.java).apply {
            putExtra(EXTRA_PRAYER_CODE, prayerCode)
            putExtra(EXTRA_PRAYER_LABEL, prayerLabel)
            putExtra(EXTRA_ADHAN_NAME, muezzinName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val screenPending = PendingIntent.getActivity(
            this,
            ALERT_PENDING_INTENT_BASE + prayerIndex,
            screenIntent,
            flags
        )
        val detail = if (muezzinName.isBlank()) {
            when (language) {
                AppLanguage.EN -> "The selected Adhan is starting"
                AppLanguage.AR -> "يبدأ الأذان المختار الآن"
                AppLanguage.CKB -> "بانگی هەڵبژێردراو ئێستا دەست پێدەکات"
            }
        } else {
            when (language) {
                AppLanguage.EN -> "Muezzin: " + muezzinName
                AppLanguage.AR -> "المؤذن: " + muezzinName
                AppLanguage.CKB -> "موئەزین: " + muezzinName
            }
        }
        val alert = NotificationCompat.Builder(this, alertChannelId)
            .setSmallIcon(com.mashxall.unified.R.drawable.ic_mashxall)
            .setContentTitle(prayerLabel.ifBlank { adhanNotificationCopy(language).fallbackPrayerTitle })
            .setContentText(detail)
            .setStyle(NotificationCompat.BigTextStyle().bigText(detail))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setContentIntent(screenPending)
            .setFullScreenIntent(screenPending, true)
            .setTimeoutAfter(10L * 60L * 1000L)
            .build()
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(ALARM_NOTIFICATION_ID, alert)
        Log.i("MashxallAdhanAlarm", "Published prayer alert for " + prayerCode + " with muezzin " + muezzinName)
    }

    private fun cancelPrayerAlarmAlert() {
        runCatching {
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .cancel(ALARM_NOTIFICATION_ID)
        }
    }

    private fun updateMediaSessionPlaybackState() {
        mediaSession?.setPlaybackState(
            PlaybackState.Builder()
                .setActions(
                    PlaybackState.ACTION_PLAY or
                        PlaybackState.ACTION_PAUSE or
                        PlaybackState.ACTION_PLAY_PAUSE or
                        PlaybackState.ACTION_STOP
                )
                .setState(
                    when {
                        !playerPrepared -> PlaybackState.STATE_BUFFERING
                        playbackPaused -> PlaybackState.STATE_PAUSED
                        else -> PlaybackState.STATE_PLAYING
                    },
                    -1L,
                    if (playbackPaused || !playerPrepared) 0f else 1f
                )
                .build()
        )
    }

    private fun stopPlaybackAndService() {
        playbackRequestGeneration += 1
        stopRequested = true
        releasePlayer()
        cancelPrayerAlarmAlert()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun downloadRemoteAdhanFile(url: String): File {
        require(url.startsWith("https://")) { "Adhan audio must use a secure HTTPS source." }
        val key = MessageDigest.getInstance("SHA-256").digest(url.toByteArray())
            .take(12).joinToString("") { "%02x".format(it) }
        val target = File(cacheDir, "adhan-stream-$key.audio")
        if (hasPlausibleMp3File(target)) return target
        target.delete()
        val temporary = File(cacheDir, target.name + ".part")
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12_000
            readTimeout = 40_000
            instanceFollowRedirects = true
            setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android " + Build.VERSION.RELEASE + ") AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Mobile Safari/537.36"
            )
            setRequestProperty("Accept", "audio/mpeg,audio/*;q=0.9,*/*;q=0.8")
        }
        try {
            connection.connect()
            val code = connection.responseCode
            if (code !in 200..299) throw java.io.IOException("The audio server returned HTTP $code.")
            if (connection.contentLengthLong > 100L * 1024L * 1024L) {
                throw java.io.IOException("The Adhan recording is larger than the supported download limit.")
            }
            val contentType = connection.contentType.orEmpty()
            if (contentType.startsWith("text/", ignoreCase = true) ||
                contentType.contains("json", ignoreCase = true) ||
                contentType.contains("xml", ignoreCase = true)
            ) {
                throw java.io.IOException("The audio source returned text or metadata instead of audio.")
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
            if (!hasPlausibleMp3File(temporary)) {
                throw java.io.IOException("The audio source did not return a valid MP3 stream.")
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

    private fun hasPlausibleMp3File(file: File): Boolean {
        if (!file.isFile || file.length() <= 512L) return false
        return runCatching {
            file.inputStream().use { input ->
                val bytes = ByteArray(65_536)
                val count = input.read(bytes)
                if (count < 3) false
                else {
                    val prefix = bytes.copyOf(count)
                    val hasId3 = prefix[0] == 0x49.toByte() && prefix[1] == 0x44.toByte() && prefix[2] == 0x33.toByte()
                    hasId3 || (0 until (count - 1).coerceAtMost(65_535)).any { index ->
                        val first = prefix[index].toInt() and 0xFF
                        val second = prefix[index + 1].toInt() and 0xFF
                        first == 0xFF && (second and 0xE0) == 0xE0 && (second and 0x06) != 0
                    }
                }
            }
        }.getOrDefault(false)
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

    private fun configureAdhanPlayer(mp: MediaPlayer) {
        // These attributes must be applied before setDataSource/prepareAsync.
        mp.setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
        mp.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
    }

    private fun prepareAdhanPlayer(
        mp: MediaPlayer,
        prayerLabel: String,
        notificationCopy: AdhanNotificationCopy,
        language: AppLanguage,
        channelId: String,
        requestGeneration: Int,
        selectedAssetPath: String
    ) {
        mp.setOnPreparedListener { ready ->
            if (!stopRequested && requestGeneration == playbackRequestGeneration) {
                playerPrepared = true
                playbackPaused = false
                try {
                    ready.start()
                    updateMediaSessionPlaybackState()
                    publishNotification()
                    Log.i("MashxallAdhanAlarm", "Selected Adhan started for " + prayerLabel)
                } catch (error: Exception) {
                    Log.e("MashxallAdhanAlarm", "Could not start Adhan for " + prayerLabel, error)
                    handleAdhanPlaybackFailure(prayerLabel, notificationCopy, language, channelId, selectedAssetPath)
                }
            }
        }
        mp.setOnCompletionListener {
            releasePlayer()
            cancelPrayerAlarmAlert()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        mp.setOnErrorListener { failed, what, extra ->
            Log.e("MashxallAdhanAlarm", "MediaPlayer failed for " + prayerLabel + ": what=" + what + " extra=" + extra)
            if (player === failed) {
                handleAdhanPlaybackFailure(prayerLabel, notificationCopy, language, channelId, selectedAssetPath)
            } else {
                runCatching { failed.release() }
            }
            true
        }
        mp.prepareAsync()
    }

    private fun handleAdhanPlaybackFailure(
        prayerLabel: String,
        notificationCopy: AdhanNotificationCopy,
        language: AppLanguage,
        channelId: String,
        selectedAssetPath: String
    ) {
        releasePlayer()
        stopForeground(STOP_FOREGROUND_REMOVE)
        if (activePrayerCode.isNotBlank()) {
            cancelPrayerAlarmAlert()
        }
        if (selectedAssetPath.isNotBlank()) {
            postMissingAudioNotification(prayerLabel, notificationCopy, language, channelId)
        }
        stopSelf()
    }

    private fun playRemoteAdhanStream(
        url: String,
        prayerLabel: String,
        notificationCopy: AdhanNotificationCopy,
        language: AppLanguage,
        channelId: String,
        requestGeneration: Int
    ) {
        if (requestGeneration != playbackRequestGeneration || stopRequested) return
        val mp = MediaPlayer()
        player = mp
        try {
            configureAdhanPlayer(mp)
            val headers = mapOf(
                "User-Agent" to ("Mozilla/5.0 (Linux; Android " + Build.VERSION.RELEASE + ") AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Mobile Safari/537.36"),
                "Accept" to "audio/mpeg,audio/*;q=0.9,*/*;q=0.8"
            )
            mp.setDataSource(this, Uri.parse(url), headers)
            prepareAdhanPlayer(mp, prayerLabel, notificationCopy, language, channelId, requestGeneration, url)
        } catch (error: Exception) {
            Log.e("MashxallAdhanAlarm", "Direct Adhan stream failed for " + prayerLabel, error)
            handleAdhanPlaybackFailure(prayerLabel, notificationCopy, language, channelId, url)
        }
    }
    private fun playAsset(
        assetPath: String,
        prayerLabel: String,
        notificationCopy: AdhanNotificationCopy,
        language: AppLanguage,
        channelId: String,
        requestGeneration: Int
    ) {
        releasePlayer()
        if (assetPath.startsWith("https://")) {
            playbackScope.launch {
                val downloaded = try {
                    withContext(Dispatchers.IO) { downloadRemoteAdhanFile(assetPath) }
                } catch (error: Exception) {
                    Log.w("MashxallAdhanAlarm", "Adhan download failed; trying direct stream playback.", error)
                    null
                }
                if (requestGeneration != playbackRequestGeneration || stopRequested) return@launch
                if (downloaded != null && hasPlausibleMp3File(downloaded)) {
                    playAsset(
                        "local:" + downloaded.absolutePath,
                        prayerLabel,
                        notificationCopy,
                        language,
                        channelId,
                        requestGeneration
                    )
                } else {
                    playRemoteAdhanStream(assetPath, prayerLabel, notificationCopy, language, channelId, requestGeneration)
                }
            }
            return
        }
        val source = try {
            when {
                assetPath.startsWith("local:") -> resolveLocalAdhanFileReference(assetPath)
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
            configureAdhanPlayer(mp)
            when (classifyAdhanAudioSource(assetPath, source)) {
                AdhanAudioSourceSelection.SELECTED_AUDIO -> mp.setDataSource(source!!.absolutePath)
                AdhanAudioSourceSelection.DEFAULT_SYSTEM_ALARM -> {
                    val fallback: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        ?: run {
                            releasePlayer()
                            cancelPrayerAlarmAlert()
                            stopForeground(STOP_FOREGROUND_REMOVE)
                            stopSelf()
                            return
                        }
                    mp.setDataSource(this, fallback)
                }
                AdhanAudioSourceSelection.MISSING_SELECTED_AUDIO -> {
                    releasePlayer()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    postMissingAudioNotification(prayerLabel, notificationCopy, language, channelId)
                    stopSelf()
                    return
                }
            }
            prepareAdhanPlayer(mp, prayerLabel, notificationCopy, language, channelId, requestGeneration, assetPath)
        } catch (error: Exception) {
            Log.e("MashxallAdhanAlarm", "Could not prepare selected Adhan for " + prayerLabel, error)
            handleAdhanPlaybackFailure(prayerLabel, notificationCopy, language, channelId, assetPath)
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
        playbackRequestGeneration += 1
        playbackScope.cancel()
        releasePlayer()
        mediaSession?.let { session ->
            runCatching {
                session.isActive = false
                session.release()
            }
        }
        mediaSession = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_PRAYER_CODE = "prayer_code"
        const val EXTRA_PRAYER_LABEL = "prayer_label"
        const val EXTRA_AUDIO_PATH = "audio_path"
        const val EXTRA_ADHAN_NAME = "adhan_name"
        private const val CHANNEL_ID = "mashxall_adhan_playback"
        private const val ALERT_CHANNEL_PREFIX = "mashxall_adhan_alarm_v2"
        private const val NOTIFICATION_ID = 7842
        private const val MISSING_AUDIO_NOTIFICATION_ID = 7844
        private const val ALARM_NOTIFICATION_ID = 7846
        private const val ALERT_PENDING_INTENT_BASE = 7860
        private const val STOP_REQUEST_CODE = 7843
        private const val TOGGLE_REQUEST_CODE = 7845
        const val ACTION_STOP = "com.mashxall.unified.STOP_ADHAN"
        const val ACTION_TOGGLE = "com.mashxall.unified.TOGGLE_ADHAN"

        /** Start an Adhan-library preview in the same foreground service that owns its notification controls. */
        fun startPreview(context: Context, audioPath: String, title: String) {
            val intent = Intent(context.applicationContext, AdhanPlaybackService::class.java)
                .putExtra(EXTRA_PRAYER_LABEL, title)
                .putExtra(EXTRA_ADHAN_NAME, title)
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
