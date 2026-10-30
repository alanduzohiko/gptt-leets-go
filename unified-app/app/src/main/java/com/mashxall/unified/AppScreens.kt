package com.mashxall.unified

import android.content.Context
import android.annotation.SuppressLint
import android.text.Html
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import android.content.ClipData
import android.content.ClipboardManager
import android.app.AlarmManager
import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import android.os.Handler
import android.os.Build
import android.os.CancellationSignal
import android.os.Looper
import android.graphics.Bitmap
import android.graphics.Typeface
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.ParcelFileDescriptor
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import coil.compose.SubcomposeAsyncImage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.outlined.Info
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mashxall.unified.data.*
import com.mashxall.unified.notifications.PrayerAlarmScheduler
import com.mashxall.unified.notifications.prayerAlarmScheduleFailureMessage
import com.mashxall.unified.notifications.prayerAlarmScheduleMessage
import com.mashxall.unified.widgets.PrayerTimesWidgetProvider
import com.caverock.androidsvg.SVG
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.security.MessageDigest
import java.util.Date
import java.util.Calendar
import java.util.Locale
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.roundToInt
import kotlin.math.tan

private val Gold: Color get() = MashxallAppearance.accent
private val PaleGold = Color(0xFFF4E5B4)
private val DeepGreen = Color(0xFF103D32)
private val CardDark = Color(0xFF102B25)
private val MushafNightColorMatrix = ColorMatrix(
    floatArrayOf(
        -1f, 0f, 0f, 0f, 255f,
        0f, -1f, 0f, 0f, 255f,
        0f, 0f, -1f, 0f, 255f,
        0f, 0f, 0f, 1f, 0f
    )
)
private val MushafSepiaColorMatrix = ColorMatrix(
    floatArrayOf(
        0.28106f, 0.55178f, 0.10716f, 0f, 15f,
        0.25714f, 0.50482f, 0.09804f, 0f, 8f,
        0.20631f, 0.40503f, 0.07866f, 0f, 2f,
        0f, 0f, 0f, 1f, 0f
    )
)

/** Keep the list aligned with usable playback coverage instead of saving hidden partial editions as the default. */
internal fun filterRecitersForPlaybackMode(
    reciters: List<Reciter>,
    mode: String,
    query: String
): List<Reciter> = reciters.filter { reciter ->
    val matchesSearch = (reciter.nameArabic + " " + reciter.nameEnglish + " " + reciter.baseUrl)
        .contains(query, ignoreCase = true)
    matchesSearch && if (mode == "full") {
        ReaderPlaybackSettings.supportsAllSurahs(reciter)
    } else {
        reciter.audioType == 0
    }
}

internal object ReaderPlaybackSettings {
    var mode by mutableStateOf("full")

    /** Full-surah playback is the first-use default; keep verse mode when it was explicitly selected. */
    fun normalize(value: String?): String = if (value == "verse") "verse" else "full"

    /** Full-surah mode must not list editions whose source metadata says their coverage is incomplete. */
    fun supportsAllSurahs(reciter: Reciter): Boolean = reciter.surahTotal == 114
}

internal fun resolveReciterStartAyah(mode: String, requestedAyah: Int, ayahCount: Int): Int =
    if (mode == "full") 1 else requestedAyah.coerceIn(1, ayahCount.coerceAtLeast(1))

/** Pick a reciter compatible with the user's chosen mode and actual source coverage. */
internal fun defaultReciterId(mode: String, reciters: List<Reciter>): Int? =
    if (mode == "full") {
        reciters.firstOrNull { it.audioType != 0 && ReaderPlaybackSettings.supportsAllSurahs(it) }?.id
            ?: reciters.firstOrNull { ReaderPlaybackSettings.supportsAllSurahs(it) }?.id
    } else {
        reciters.firstOrNull { it.audioType == 0 }?.id
    }

/** Never retain a hidden partial edition in Full Surah mode or an incompatible source in verse mode. */
internal fun resolveCompatibleReciterId(mode: String, currentId: Int, reciters: List<Reciter>): Int? {
    val current = reciters.firstOrNull { it.id == currentId }
    if (current != null) {
        if (mode == "verse" && current.audioType == 0) return current.id
        if (mode == "full" && ReaderPlaybackSettings.supportsAllSurahs(current)) return current.id
    }
    return defaultReciterId(mode, reciters)
}

@Composable
private fun LoadingOrEmpty(loading: Boolean, language: AppLanguage, foreground: Color, message: String = tr(language, "no_data")) {
    Box(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = loading,
            enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.985f, animationSpec = tween(180)),
            exit = fadeOut(tween(120))
        ) {
            PremiumLoadingSkeleton(accent = Gold)
        }
        AnimatedVisibility(
            visible = !loading,
            enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.985f, animationSpec = tween(180)),
            exit = fadeOut(tween(120))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    Modifier.size(62.dp)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(Gold.copy(alpha = 0.20f), Gold.copy(alpha = 0.055f))))
                        .border(1.dp, Gold.copy(alpha = 0.22f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Info, contentDescription = null, tint = Gold, modifier = Modifier.size(29.dp))
                }
                Text(
                    message,
                    color = foreground.copy(alpha = 0.72f),
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 21.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun SearchBox(value: String, onValue: (String) -> Unit, placeholder: String) {
    PremiumSearchField(
        value = value,
        onValueChange = onValue,
        placeholder = placeholder,
        accent = MaterialTheme.colorScheme.primary,
        foreground = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun ReaderActionIcon(
    icon: ImageVector,
    description: String?,
    enabled: Boolean = true,
    rotationDegrees: Float = 0f,
    hapticFeedback: Boolean = true,
    onClick: () -> Unit
) {
    PremiumIconButton(
        icon = icon,
        contentDescription = description,
        accent = Gold,
        size = 34.dp,
        enabled = enabled,
        rotationDegrees = rotationDegrees,
        circular = true,
        hapticFeedback = hapticFeedback,
        onClick = onClick
    )
}

@Composable
private fun ReaderModeSegment(
    modifier: Modifier = Modifier,
    label: String,
    selected: Boolean,
    icon: ImageVector,
    foreground: Color,
    onClick: () -> Unit
) {
    val fill by animateColorAsState(
        targetValue = if (selected) Gold.copy(alpha = 0.16f) else foreground.copy(alpha = 0.035f),
        animationSpec = tween(220),
        label = "Quran reader mode fill"
    )
    val stroke by animateColorAsState(
        targetValue = if (selected) Gold.copy(alpha = 0.48f) else foreground.copy(alpha = 0.09f),
        animationSpec = tween(220),
        label = "Quran reader mode outline"
    )
    Row(
        modifier.clip(RoundedCornerShape(15.dp)).background(fill)
            .border(1.dp, stroke, RoundedCornerShape(15.dp))
            .premiumClickable(hapticFeedback = true, onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) Gold else foreground.copy(alpha = 0.65f), modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = if (selected) foreground else foreground.copy(alpha = 0.70f), fontSize = 10.sp, maxLines = 1, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
        if (selected) {
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Gold, modifier = Modifier.size(13.dp))
        }
    }
}

@Composable
private fun QuranSurahWelcomeCard(
    language: AppLanguage,
    surahCount: Int,
    onSettings: () -> Unit
) {
    val title = when (language) {
        AppLanguage.EN -> "The Noble Quran"
        AppLanguage.AR -> "القرآن الكريم"
        AppLanguage.CKB -> "قورئانی پیرۆز"
    }
    val subtitle = when (language) {
        AppLanguage.EN -> "Choose a surah, resume reading, or open the page Mushaf."
        AppLanguage.AR -> "اختر سورة، أو تابع قراءتك، أو افتح المصحف المصوّر."
        AppLanguage.CKB -> "سوورەتێک هەڵبژێرە، خوێندنەوە بەردەوام بکە یان مصحەفی پەڕەیی بکەرەوە."
    }
    val countLabel = surahCount.toString() + when (language) {
        AppLanguage.EN -> " surahs available"
        AppLanguage.AR -> " سورة متاحة"
        AppLanguage.CKB -> " سوورەت بەردەستە"
    }
    val mantra = when (language) {
        AppLanguage.EN -> "READ • RECITE • REFLECT"
        AppLanguage.AR -> "اقرأ • رتّل • تدبّر"
        AppLanguage.CKB -> "بخوێنەوە • تلاوەت بکە • بیر بکەوە"
    }
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(25.dp))
            .background(Brush.linearGradient(listOf(DeepGreen, Color(0xFF1A5A45), CardDark)))
            .border(1.dp, Gold.copy(alpha = 0.36f), RoundedCornerShape(25.dp))
    ) {
        Box(
            Modifier.align(Alignment.TopEnd).offset(x = 36.dp, y = (-48).dp).size(132.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Gold.copy(alpha = 0.22f), Color(0xFF4EAB84).copy(alpha = 0.07f), Color.Transparent)))
        )
        Row(
            Modifier.fillMaxWidth().padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(mantra, color = PaleGold, fontSize = 8.sp, letterSpacing = 0.65.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Spacer(Modifier.height(9.dp))
                Text(title, color = Color(0xFFFFF8E7), fontSize = 23.sp, lineHeight = 29.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, color = Color.White.copy(alpha = 0.75f), fontSize = 10.sp, lineHeight = 16.sp)
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.07f))
                        .border(1.dp, Color.White.copy(alpha = 0.13f), CircleShape)
                        .padding(horizontal = 9.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Gold, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(countLabel, color = Color.White.copy(alpha = 0.91f), fontSize = 9.sp, maxLines = 1)
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PremiumIconMedallion(icon = Icons.Outlined.AutoStories, tint = Gold, size = 66.dp, pulse = true)
                Box(
                    Modifier.size(32.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.075f))
                        .border(1.dp, Color.White.copy(alpha = 0.17f), CircleShape)
                        .premiumClickable { onSettings() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Settings, contentDescription = if (language == AppLanguage.EN) "Reader settings" else if (language == AppLanguage.AR) "إعدادات القارئ" else "ڕێکخستنەکانی خوێندنەوە", tint = PaleGold, modifier = Modifier.size(17.dp))
                }
            }
        }
    }
}

@Composable
private fun ReaderSettingsCategoryCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    foreground: Color,
    cardColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().premiumClickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Gold.copy(alpha = 0.20f))
    ) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(43.dp).clip(RoundedCornerShape(14.dp)).background(Gold.copy(alpha = 0.13f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Gold, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = foreground, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(subtitle, color = foreground.copy(alpha = 0.60f), fontSize = 10.sp, lineHeight = 15.sp)
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = Gold)
        }
    }
}


internal data class QuranTextFontOption(
    val id: String,
    val assetPath: String,
    val sizeBytes: Long,
    val sha256: String,
    val labelCkb: String,
    val labelArabic: String,
    val labelEnglish: String
)

/** Font paths, sizes and hashes are taken from the extracted original-source inventory. */
internal val quranTextFontOptions = listOf(
    QuranTextFontOption("hafs_regular", "content/noor_tafsir_1.2.2/base/assets/fonts/hafs_regular.ttf", 242368L, "a0636e68e375af9552470d67773936f54d536e6586ce2608311b2fe7f9cbec3a", "هەفسی قورئان", "حفص للقرآن", "Hafs Regular"),
    QuranTextFontOption("hafs_otf", "content/noor_tafsir_1.2.2/base/res/font/hafs.otf", 232884L, "eaec00f41bdc2437c57c7ba1a69a518ee566ab47e1312ec75314c159db4df917", "هەفسی قورئان (OTF)", "حفص للقرآن (OTF)", "Hafs OTF"),
    QuranTextFontOption("amiri_quran", "content/noor_tafsir_1.2.2/base/assets/fonts/amiri_quran.ttf", 156432L, "0b0d47c50b3b74b798a6a27bca5935fb83161f34fa290a96e60a8a7ab4af80bf", "ئەمری قورئان", "أميري للقرآن", "Amiri Quran"),
    QuranTextFontOption("me_quran", "content/noor_tafsir_1.2.2/base/assets/fonts/me_quran.ttf", 86164L, "e5804830d22eda4ebb00548ef77265b4481bda939c80b280ba6024a967320c06", "ME Quran", "ME Quran", "ME Quran"),
    QuranTextFontOption("al_qalam", "content/noor_tafsir_1.2.2/base/assets/fonts/al_qalam.ttf", 215412L, "2fd8fe6f23b914c9f05642f7dea8d0f37045d3a429a1d3144d579c02bed9bc4b", "القلم", "القلم", "Al Qalam"),
    QuranTextFontOption("uthmanic_hafs", "content/salawati_3.1.3/base/res/font/uthmanic_hafs.otf", 246428L, "59e20b2403687a67854fd199861abed3528068c035d2689898465d5faf39bb31", "ڕەسمی عوسمانی بە حفص", "الرسم العثماني برواية حفص", "Uthmanic Hafs")
)

internal fun quranTextFontOption(id: String): QuranTextFontOption =
    quranTextFontOptions.firstOrNull { it.id == id } ?: quranTextFontOptions.first()

internal fun quranTextFontLabel(option: QuranTextFontOption, language: AppLanguage): String = when (language) {
    AppLanguage.CKB -> option.labelCkb
    AppLanguage.AR -> option.labelArabic
    AppLanguage.EN -> option.labelEnglish
}

private fun quranFontSha256(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { b -> "%02x".format(b.toInt() and 0xff) }

internal fun loadQuranTextTypeface(context: Context, fontId: String): Typeface? = runCatching {
    val option = quranTextFontOption(fontId)
    val extension = option.assetPath.substringAfterLast('.', "ttf")
    val target = File(context.cacheDir, "mashxall-quran-font-${option.id}.$extension")
    val cachedBytes = if (target.isFile && target.length() == option.sizeBytes) target.readBytes() else null
    if (cachedBytes != null && quranFontSha256(cachedBytes) == option.sha256) {
        return@runCatching Typeface.createFromFile(target)
    }
    val sourceBytes = context.assets.open(option.assetPath).use { it.readBytes() }
    check(sourceBytes.size.toLong() == option.sizeBytes && quranFontSha256(sourceBytes) == option.sha256) {
        "The chosen Quran font does not match the verified original-source inventory."
    }
    target.writeBytes(sourceBytes)
    Typeface.createFromFile(target)
}.getOrNull()

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun QuranScreen(language: AppLanguage, foreground: Color, cardColor: Color, initialSurah: Int, initialAyah: Int, onSurah: (Int) -> Unit, onTafsir: (Int, Int) -> Unit, onBookmark: (Int, Int) -> Unit, onOpenVerse: (Int, Int) -> Unit = { _, _ -> }, onPlayAudio: (Int, Int) -> Unit = { _, _ -> }, onBrowseReciters: (Int, Int) -> Unit = { _, _ -> }, onImmersiveMushaf: (Boolean) -> Unit = {}, resumeLastMushaf: Boolean = false, onResumeMushafHandled: () -> Unit = {}, openSettingsOnEntry: Boolean = false, onSettingsRequestConsumed: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var surahs by remember { mutableStateOf<List<Surah>>(emptyList()) }
    var selected by rememberSaveable { mutableIntStateOf(initialSurah) }
    var verses by remember { mutableStateOf<List<Ayah>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var query by rememberSaveable { mutableStateOf("") }
    val prefs = remember { context.getSharedPreferences("mashxall_preferences", Context.MODE_PRIVATE) }
    var translationKey by rememberSaveable {
        mutableStateOf(prefs.getString("quran_translation", null) ?: when (language) {
            AppLanguage.EN -> "sahih"
            AppLanguage.AR -> "none"
            AppLanguage.CKB -> "puxta"
        })
    }
    var chooseTranslation by remember { mutableStateOf(false) }
    var chooseSurah by remember { mutableStateOf(false) }
    var mushafMode by rememberSaveable { mutableStateOf(false) }
    var openingMushaf by remember { mutableStateOf(false) }
    var pendingMushafStartSurah by remember { mutableIntStateOf(0) }
    var showSurahList by rememberSaveable { mutableStateOf(true) }
    var showUnvowelled by rememberSaveable { mutableStateOf(false) }
    var coloredTajweed by rememberSaveable {
        mutableStateOf(prefs.getBoolean("quran_colored_tajweed", false))
    }
    var tajweedAyahs by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var tajweedLoadedSurah by remember { mutableIntStateOf(0) }
    var tajweedLoading by remember { mutableStateOf(false) }
    var tajweedMessage by rememberSaveable { mutableStateOf("") }
    var showReaderSettings by remember { mutableStateOf(false) }
    var readerSettingsSection by rememberSaveable { mutableStateOf("overview") }
    LaunchedEffect(openSettingsOnEntry) {
        if (openSettingsOnEntry) {
            readerSettingsSection = "overview"
            showReaderSettings = true
            onSettingsRequestConsumed()
        }
    }
    var mushafDarkMode by rememberSaveable { mutableStateOf(prefs.getBoolean("mushaf_dark_mode", false)) }
    var mushafSepiaMode by rememberSaveable { mutableStateOf(prefs.getBoolean("mushaf_sepia_mode", false)) }
    var verseTextScale by rememberSaveable { mutableFloatStateOf(prefs.getFloat("quran_verse_text_scale", 1.0f)) }
    var quranTextFontId by rememberSaveable {
        mutableStateOf(quranTextFontOption(prefs.getString("quran_text_font", "hafs_regular") ?: "hafs_regular").id)
    }
    var quranTextTypeface by remember { mutableStateOf<Typeface?>(null) }
    val quranTextFontFamily = remember(quranTextTypeface) {
        quranTextTypeface?.let { FontFamily(it) } ?: FontFamily.Default
    }
    var quranFontLoadFailed by remember { mutableStateOf(false) }
    LaunchedEffect(quranTextFontId) {
        val loadedTypeface = withContext(Dispatchers.IO) { loadQuranTextTypeface(context, quranTextFontId) }
        quranTextTypeface = loadedTypeface
        quranFontLoadFailed = loadedTypeface == null
    }
    var selectedTafsirBook by rememberSaveable {
        mutableStateOf(prefs.getString("quran_tafsir_book", null) ?: when (language) {
            AppLanguage.EN -> "sahih"
            AppLanguage.AR -> "mukhtasar"
            AppLanguage.CKB -> "roshn"
        })
    }
    var readerReciters by remember { mutableStateOf<List<Reciter>>(emptyList()) }
    var preferredReaderReciterId by rememberSaveable {
        mutableIntStateOf(prefs.getInt("preferred_reciter_id", -1))
    }
    val readerPlaybackMode = ReaderPlaybackSettings.mode
    LaunchedEffect(Unit) {
        ReaderPlaybackSettings.mode = ReaderPlaybackSettings.normalize(prefs.getString("reciter_playback_mode", "full"))
        readerReciters = ContentRepository.loadReciters(context)
        if (preferredReaderReciterId == -1 || readerReciters.none { it.id == preferredReaderReciterId }) {
            preferredReaderReciterId = defaultReciterId(ReaderPlaybackSettings.mode, readerReciters) ?: -1
            if (preferredReaderReciterId > 0) {
                prefs.edit().putInt("preferred_reciter_id", preferredReaderReciterId).apply()
            }
        }
    }
    LaunchedEffect(readerReciters, readerPlaybackMode, preferredReaderReciterId) {
        val compatible = resolveCompatibleReciterId(readerPlaybackMode, preferredReaderReciterId, readerReciters)
        if (compatible != null && compatible != preferredReaderReciterId) {
            preferredReaderReciterId = compatible
            prefs.edit().putInt("preferred_reciter_id", compatible).apply()
        }
    }
    var selectedAyahForSheet by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var sheetVerse by remember { mutableStateOf<Ayah?>(null) }
    var sheetTafsir by remember { mutableStateOf("") }
    var sheetRelatedHadith by remember { mutableStateOf<List<AyahHadith>>(emptyList()) }
    var sheetLoading by remember { mutableStateOf(false) }
    var showSheetHadith by remember { mutableStateOf(false) }
    var showSheetWordStudy by remember { mutableStateOf(false) }
    var sheetWords by remember { mutableStateOf<List<WordItem>>(emptyList()) }
    var sheetWordLoading by remember { mutableStateOf(false) }
    var sheetAudioPlaying by remember { mutableStateOf(false) }
    LaunchedEffect(mushafMode) { onImmersiveMushaf(mushafMode) }
    DisposableEffect(Unit) {
        onDispose { onImmersiveMushaf(false) }
    }
    androidx.activity.compose.BackHandler(enabled = !showSurahList || openingMushaf) {
        if (selectedAyahForSheet != null) selectedAyahForSheet = null
        else if (openingMushaf) {
            pendingMushafStartSurah = 0
            openingMushaf = false
            mushafMode = false
            showSurahList = true
        } else {
            mushafMode = false
            query = ""
            showSurahList = true
        }
    }
    var mushafPage by rememberSaveable { mutableIntStateOf(prefs.getInt("last_mushaf_page", 1).coerceIn(1, 604)) }
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) { surahs = ContentRepository.loadSurahs(context) }
    LaunchedEffect(initialSurah) { selected = initialSurah }
    LaunchedEffect(pendingMushafStartSurah, language.key, translationKey) {
        val requestedSurah = pendingMushafStartSurah
        if (requestedSurah !in 1..114) return@LaunchedEffect
        val page = runCatching {
            val sourceVerses = ContentRepository.loadAyahs(
                context, requestedSurah, language.key, translationKey = translationKey
            )
            resolveSurahStartPage(sourceVerses, requestedSurah)
        }.getOrNull()
        if (pendingMushafStartSurah != requestedSurah) return@LaunchedEffect
        if (page != null) {
            selected = requestedSurah
            mushafPage = page
            prefs.edit()
                .putString("quran_reader_mode", "mushaf")
                .putInt("last_mushaf_page", page)
                .putInt("last_surah", requestedSurah)
                .putInt("last_ayah_number", 1)
                .apply()
            pendingMushafStartSurah = 0
            openingMushaf = false
            mushafMode = true
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        } else {
            // Preserve the real verse reader as an explicit fallback if source page metadata is absent.
            selected = requestedSurah
            pendingMushafStartSurah = 0
            openingMushaf = false
            mushafMode = false
            showSurahList = false
            prefs.edit()
                .putString("quran_reader_mode", "verse")
                .putInt("last_surah", requestedSurah)
                .putInt("last_ayah_number", 1)
                .apply()
            Toast.makeText(
                context,
                when (language) {
                    AppLanguage.EN -> "The original Mushaf page for this surah is unavailable; opening verse text instead."
                    AppLanguage.AR -> "صفحة المصحف الأصلية لهذه السورة غير متاحة؛ سيُفتح نص الآيات بدلًا منها."
                    AppLanguage.CKB -> "پەڕەی ڕەسەنی مصحەف بۆ ئەم سوورەتە بەردەست نییە؛ دەقی ئایەتەکان دەکرێتەوە."
                },
                Toast.LENGTH_LONG
            ).show()
        }
    }
    LaunchedEffect(resumeLastMushaf) {
        if (resumeLastMushaf) {
            pendingMushafStartSurah = 0
            selected = prefs.getInt("last_surah", initialSurah).coerceIn(1, 114)
            mushafPage = prefs.getInt("last_mushaf_page", 1).coerceIn(1, 604)
            query = ""
            showSurahList = false
            openingMushaf = false
            mushafMode = shouldRestorePageMushaf(
                prefs.getString("quran_reader_mode", null),
                prefs.contains("last_mushaf_page")
            )
            onResumeMushafHandled()
        }
    }
    LaunchedEffect(selected, language, query, initialAyah, translationKey, mushafMode, showSurahList, openingMushaf) {
        if (mushafMode || showSurahList || openingMushaf) {
            loading = false
            return@LaunchedEffect
        }
        loading = true
        val loaded = if (query.isBlank()) {
            ContentRepository.loadAyahs(context, selected, language.key, translationKey = translationKey)
        } else {
            ContentRepository.searchAyahs(context, query.trim(), language.key, translationKey)
        }
        verses = loaded
        if (query.isBlank()) {
            (loaded.firstOrNull { it.number >= initialAyah } ?: loaded.firstOrNull())?.page
                ?.takeIf { it in 1..604 }?.let { mushafPage = it }
        }
        loading = false
    }
    LaunchedEffect(verses, initialAyah, query, showSurahList, mushafMode, openingMushaf) {
        if (!showSurahList && !mushafMode && !openingMushaf && query.isBlank() && verses.isNotEmpty()) {
            val startAt = verses.indexOfFirst { it.number >= initialAyah }
            if (startAt >= 0) listState.scrollToItem(startAt)
        }
    }
    LaunchedEffect(selected, coloredTajweed, mushafMode, showSurahList, openingMushaf, surahs) {
        tajweedAyahs = emptyMap()
        tajweedLoadedSurah = 0
        if (!coloredTajweed || mushafMode || showSurahList || openingMushaf) {
            tajweedLoading = false
            tajweedMessage = ""
            return@LaunchedEffect
        }
        val expectedCount = surahs.firstOrNull { it.number == selected }?.ayahCount ?: 0
        if (expectedCount < 1) {
            tajweedLoading = false
            tajweedMessage = ""
            return@LaunchedEffect
        }
        tajweedLoading = true
        tajweedMessage = ""
        try {
            val loaded = ContentRepository.loadTajweedAyahs(context, selected, expectedCount)
            tajweedAyahs = loaded
            tajweedLoadedSurah = if (loaded.isNotEmpty()) selected else 0
            tajweedMessage = when {
                loaded.isNotEmpty() && language == AppLanguage.EN ->
                    "Colored Tajweed is active. A successful local cache keeps this surah available offline."
                loaded.isNotEmpty() && language == AppLanguage.AR ->
                    "التجويد الملوّن مفعّل؛ يتيح حفظ السورة محليًا استخدامها دون إنترنت عند نجاح التخزين."
                loaded.isNotEmpty() ->
                    "تەجویدی ڕەنگاوڕەنگ چالاکە؛ ئەگەر پاشەکەوتکردن سەرکەوتوو بێت، سوورەتەکە بەبێ ئینتەرنێتیش دەخوێندرێتەوە."
                language == AppLanguage.EN ->
                    "Tajweed text is unavailable right now. Connect to the internet on first use; regular Quran text remains available."
                language == AppLanguage.AR ->
                    "بيانات التجويد غير متاحة الآن. اتصل بالإنترنت عند الاستخدام الأول؛ وسيبقى نص القرآن العادي متاحًا."
                else ->
                    "دەقی تەجوید ئێستا بەردەست نییە. بۆ یەکەم جار پەیوەندی ئینتەرنێت پێویستە؛ دەقی ئاسایی قورئان هەر بەردەستە."
            }
        } finally {
            tajweedLoading = false
        }
    }
    val currentSurah = surahs.firstOrNull { it.number == selected }
    val currentTranslation = ContentRepository.quranTranslations.firstOrNull { it.id == translationKey }
        ?: ContentRepository.quranTranslations.first()
    LaunchedEffect(selectedAyahForSheet, language, translationKey, selectedTafsirBook) {
        val target = selectedAyahForSheet
        if (target == null) {
            sheetVerse = null
            sheetTafsir = ""
            sheetRelatedHadith = emptyList()
            sheetWords = emptyList()
            showSheetHadith = false
            showSheetWordStudy = false
            sheetAudioPlaying = false
            return@LaunchedEffect
        }
        sheetLoading = true
        sheetVerse = null
        showSheetHadith = false
        showSheetWordStudy = false
        try {
            val versesForSurah = ContentRepository.loadAyahs(context, target.first, language.key, translationKey = translationKey)
            sheetVerse = versesForSurah.firstOrNull { it.number == target.second }
            sheetTafsir = ContentRepository.loadTafsir(context, selectedTafsirBook, target.first, target.second)
            sheetRelatedHadith = ContentRepository.loadHadithsForAyah(context, target.first, target.second)
        } catch (_: Exception) {
            sheetVerse = null
            sheetTafsir = ""
            sheetRelatedHadith = emptyList()
        } finally {
            sheetLoading = false
        }
    }
    Column(Modifier.fillMaxSize()) {
        if (!mushafMode && !showSurahList && !openingMushaf) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                shape = RoundedCornerShape(23.dp),
                border = BorderStroke(1.dp, Gold.copy(alpha = 0.22f))
            ) {
            Row(
                Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(cardColor, Gold.copy(alpha = 0.045f)))).padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(tr(language, "quran"), color = foreground, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (query.isBlank()) currentSurah?.arabicName ?: tr(language, "choose_surah")
                        else when (language) {
                            AppLanguage.EN -> "Search across all 114 surahs"
                            AppLanguage.AR -> "بحث في القرآن الكريم كاملًا"
                            AppLanguage.CKB -> "گەڕان لە هەموو ١١٤ سوورەتدا"
                        },
                        color = Gold, fontSize = 12.sp
                    )
                    if (query.isBlank() && currentSurah != null) {
                        val placeLabel = when {
                            currentSurah.place.contains("meccan", ignoreCase = true) ||
                                currentSurah.place.contains("makki", ignoreCase = true) -> when (language) {
                                    AppLanguage.EN -> "Meccan"
                                    AppLanguage.AR -> "مكية"
                                    AppLanguage.CKB -> "مەککی"
                                }
                            currentSurah.place.contains("medinan", ignoreCase = true) ||
                                currentSurah.place.contains("madani", ignoreCase = true) -> when (language) {
                                    AppLanguage.EN -> "Medinan"
                                    AppLanguage.AR -> "مدنية"
                                    AppLanguage.CKB -> "مەدەنی"
                                }
                            else -> currentSurah.place
                        }
                        val metadata = listOf(
                            currentSurah.englishName,
                            currentSurah.ayahCount.toString() + " " + when (language) {
                                AppLanguage.EN -> "verses"
                                AppLanguage.AR -> "آية"
                                AppLanguage.CKB -> "ئایەت"
                            },
                            placeLabel
                        ).filter { it.isNotBlank() }.joinToString(" • ")
                        if (metadata.isNotBlank()) {
                            Text(metadata, color = foreground.copy(alpha = 0.56f), fontSize = 10.sp, maxLines = 1)
                        }
                        val meaning = currentSurah.uniqueTitle.ifBlank { currentSurah.meaningEnglish }
                        if (meaning.isNotBlank()) {
                            Text(meaning, color = foreground.copy(alpha = 0.48f), fontSize = 10.sp, maxLines = 1)
                        }
                    }
                }
                Row(
                    Modifier.clip(RoundedCornerShape(14.dp)).background(Gold.copy(alpha = 0.12f))
                        .border(1.dp, Gold.copy(alpha = 0.28f), RoundedCornerShape(14.dp))
                        .premiumClickable(hapticFeedback = true) { chooseSurah = true }
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.MenuBook, contentDescription = null, tint = Gold, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(tr(language, "choose_surah"), color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
            }
            Spacer(Modifier.height(8.dp))
            SearchBox(query, { query = it }, tr(language, "search"))
            Spacer(Modifier.height(7.dp))
            val currentTranslationLabel = when (language) {
                AppLanguage.CKB -> currentTranslation.ckb
                AppLanguage.AR -> currentTranslation.arabic
                AppLanguage.EN -> currentTranslation.english
            }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp))
                    .background(Brush.horizontalGradient(listOf(Gold.copy(alpha = 0.12f), cardColor)))
                    .border(1.dp, Gold.copy(alpha = 0.22f), RoundedCornerShape(17.dp))
                    .premiumClickable(hapticFeedback = true) { chooseTranslation = true }
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(31.dp).clip(RoundedCornerShape(11.dp)).background(Gold.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Language, contentDescription = null, tint = Gold, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(tr(language, "translation"), color = Gold, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.7.sp)
                    Text(currentTranslationLabel, color = foreground, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
                Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = Gold, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ReaderModeSegment(
                    modifier = Modifier.weight(1f),
                    label = tr(language, "verse_view"),
                    selected = !mushafMode,
                    icon = Icons.Outlined.MenuBook,
                    foreground = foreground
                ) {
                    mushafMode = false
                    prefs.edit().putString("quran_reader_mode", "verse").apply()
                }
                ReaderModeSegment(
                    modifier = Modifier.weight(1f),
                    label = tr(language, "mushaf_view"),
                    selected = mushafMode,
                    icon = Icons.Outlined.AutoStories,
                    foreground = foreground
                ) {
                    query = ""
                    mushafMode = true
                    prefs.edit()
                        .putString("quran_reader_mode", "mushaf")
                        .putInt("last_mushaf_page", mushafPage)
                        .putInt("last_surah", selected)
                        .putInt("last_ayah_number", initialAyah)
                        .apply()
                }
            }
            if (!mushafMode) {
                Spacer(Modifier.height(5.dp))
                Text(
                    if (query.isNotBlank()) {
                        when (language) {
                            AppLanguage.EN -> "${verses.size} matching verses"
                            AppLanguage.AR -> "${verses.size} آية مطابقة"
                            AppLanguage.CKB -> "${verses.size} ئایەتی هاوتا"
                        }
                    } else currentSurah?.let { it.ayahCount.toString() + " • " + tr(language, "verse") } ?: "",
                    color = foreground.copy(alpha = 0.55f), fontSize = 11.sp
                )
                Spacer(Modifier.height(5.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    ReaderModeSegment(
                        modifier = Modifier.weight(1f),
                        label = if (language == AppLanguage.EN) "With diacritics" else if (language == AppLanguage.AR) "بالتشكيل" else "بە حەرەکات",
                        selected = !showUnvowelled,
                        icon = Icons.Outlined.CheckCircle,
                        foreground = foreground
                    ) { showUnvowelled = false }
                    ReaderModeSegment(
                        modifier = Modifier.weight(1f),
                        label = if (language == AppLanguage.EN) "Without diacritics" else if (language == AppLanguage.AR) "بلا تشكيل" else "بێ حەرەکات",
                        selected = showUnvowelled,
                        icon = Icons.Outlined.Info,
                        foreground = foreground
                    ) { showUnvowelled = true }
                }
                if (coloredTajweed) {
                    Spacer(Modifier.height(5.dp))
                    Text(
                        when {
                            tajweedLoading && language == AppLanguage.EN -> "Loading colour-coded Tajweed…"
                            tajweedLoading && language == AppLanguage.AR -> "جارٍ تحميل التجويد الملوّن…"
                            tajweedLoading -> "تەجویدی ڕەنگاوڕەنگ دادەگیرێت…"
                            tajweedMessage.isNotBlank() -> tajweedMessage
                            else -> when (language) {
                                AppLanguage.EN -> "Tajweed colours apply in this verse-text view. Switch to the page Mushaf for the original printed layout."
                                AppLanguage.AR -> "ألوان التجويد تظهر في عرض نص الآيات؛ انتقل إلى المصحف المصوّر لعرض الصفحات الأصلية."
                                AppLanguage.CKB -> "ڕەنگەکانی تەجوید لەم پێشان‌دانی دەقی ئایەتە؛ بۆ ڕووکاری ڕەسەنی پەڕەی مصحەف، مصحەفی پەڕەیی هەڵبژێرە."
                            }
                        },
                        color = if (tajweedAyahs.isNotEmpty()) Gold else foreground.copy(alpha = 0.58f),
                        fontSize = 9.sp, lineHeight = 14.sp
                    )
                }
            }
        }
        }
        if (mushafMode) {
            MushafPageViewer(
                language = language,
                foreground = foreground,
                page = mushafPage,
                onVerseView = {
                    query = ""
                    selectedAyahForSheet = null
                    mushafMode = false
                    showSurahList = false
                    prefs.edit().putString("quran_reader_mode", "verse").apply()
                },
                darkMode = mushafDarkMode,
                sepiaMode = mushafSepiaMode,
                onPageChange = { requested ->
                    mushafPage = requested.coerceIn(1, 604)
                    prefs.edit().putInt("last_mushaf_page", mushafPage).apply()
                },
                onExitFullscreen = {
                    mushafMode = false
                    showSurahList = true
                },
                onReaderSettings = { readerSettingsSection = "overview"; showReaderSettings = true },
                onPlaySurah = { surahNumber, ayahNumber -> onPlayAudio(surahNumber, ayahNumber) },
                onBookmarkVerse = { surahNumber, ayahNumber -> onBookmark(surahNumber, ayahNumber) },
                onVerseClick = { surahNumber, ayahNumber ->
                    selected = surahNumber
                    onSurah(surahNumber)
                    onOpenVerse(surahNumber, ayahNumber)
                    prefs.edit().putInt("last_surah", surahNumber).putInt("last_ayah_number", ayahNumber).apply()
                    query = ""
                    selectedAyahForSheet = surahNumber to ayahNumber
                }
            )
        } else if (showSurahList) {
            Column(Modifier.fillMaxSize()) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    QuranSurahWelcomeCard(
                        language = language,
                        surahCount = surahs.size,
                        onSettings = { readerSettingsSection = "overview"; showReaderSettings = true }
                    )
                    Spacer(Modifier.height(12.dp))
                    SearchBox(query, { query = it }, tr(language, "choose_surah") + " / " + tr(language, "search"))
                }
                val filteredSurahs = surahs.filter { item ->
                    query.isBlank() || item.arabicName.contains(query.trim(), true) ||
                        item.englishName.contains(query.trim(), true) ||
                        item.number.toString() == query.trim()
                }
                if (surahs.isEmpty()) {
                    LoadingOrEmpty(true, language, foreground)
                } else LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 13.dp, vertical = 5.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    itemsIndexed(filteredSurahs, key = { _, item -> item.number }) { index, item ->
                        val placeLabel = when {
                            item.place.contains("meccan", ignoreCase = true) || item.place.contains("makki", ignoreCase = true) ->
                                when (language) { AppLanguage.EN -> "Meccan"; AppLanguage.AR -> "مكية"; AppLanguage.CKB -> "مەککی" }
                            item.place.contains("medinan", ignoreCase = true) || item.place.contains("madani", ignoreCase = true) ->
                                when (language) { AppLanguage.EN -> "Medinan"; AppLanguage.AR -> "مدنية"; AppLanguage.CKB -> "مەدەنی" }
                            else -> item.place
                        }
                        Card(
                            modifier = Modifier.fillMaxWidth()
                                .staggeredEntrance(itemKey = item.number, index = index)
                                .premiumClickable(hapticFeedback = true) {
                                selected = item.number
                                onSurah(item.number)
                                query = ""
                                selectedAyahForSheet = null
                                pendingMushafStartSurah = item.number
                                showSurahList = false
                                openingMushaf = true
                                mushafMode = false
                                prefs.edit()
                                    .putInt("last_surah", item.number)
                                    .putInt("last_ayah_number", 1)
                                    .apply()
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            colors = CardDefaults.cardColors(containerColor = cardColor),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(0.6.dp, Gold.copy(alpha = 0.16f))
                        ) {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(Brush.linearGradient(listOf(Gold.copy(alpha = 0.24f), Gold.copy(alpha = 0.06f)))).border(1.dp, Gold.copy(alpha = 0.30f), RoundedCornerShape(13.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(item.number.toString(), color = Gold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(item.arabicName, color = foreground, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                                    Text(item.englishName, color = foreground.copy(alpha = 0.68f), fontSize = 11.sp)
                                    Text(
                                        listOf(
                                            item.ayahCount.toString() + " " + when (language) { AppLanguage.EN -> "verses"; AppLanguage.AR -> "آية"; AppLanguage.CKB -> "ئایەت" },
                                            placeLabel
                                        ).filter { it.isNotBlank() }.joinToString(" • "),
                                        color = foreground.copy(alpha = 0.5f), fontSize = 10.sp
                                    )
                                }
                                Spacer(Modifier.width(6.dp))
                                Icon(Icons.Outlined.ChevronRight, null, tint = Gold)
                            }
                        }
                    }
                }
            }
        } else if (openingMushaf) {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = Gold)
                Spacer(Modifier.height(14.dp))
                Text(
                    when (language) {
                        AppLanguage.EN -> "Opening the page-based Mushaf…"
                        AppLanguage.AR -> "جارٍ فتح المصحف ذي الصفحات…"
                        AppLanguage.CKB -> "مصحەفی پەڕەبەندی کراو دەکرێتەوە…"
                    }, color = foreground, fontSize = 13.sp, textAlign = TextAlign.Center
                )
            }
        } else if (verses.isEmpty()) {
            LoadingOrEmpty(loading, language, foreground)
        } else LazyColumn(
            state = listState,
            modifier = Modifier.pointerInput(selected, query, loading) {
                if (query.isNotBlank() || loading) return@pointerInput
                var horizontalDrag = 0f
                detectHorizontalDragGestures(
                    onDragStart = { horizontalDrag = 0f },
                    onHorizontalDrag = { _, delta -> horizontalDrag += delta },
                    onDragEnd = {
                        val target = quranSurahSwipeTarget(selected, horizontalDrag)
                        if (target != null && target != selected) {
                            selected = target
                            query = ""
                            onSurah(target)
                            prefs.edit().putInt("last_surah", target).putInt("last_ayah_number", 1).apply()
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            scope.launch { listState.animateScrollToItem(0) }
                        }
                        horizontalDrag = 0f
                    },
                    onDragCancel = { horizontalDrag = 0f }
                )
            },
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(verses, key = { _, it -> it.surah.toString() + ":" + it.number }) { index, verse ->
                val verseIsFocused = selectedAyahForSheet == (verse.surah to verse.number)
                val verseBorder by animateColorAsState(
                    targetValue = if (verseIsFocused) Gold.copy(alpha = 0.72f) else Gold.copy(alpha = 0.18f),
                    animationSpec = tween(260, easing = FastOutSlowInEasing),
                    label = "Quran focused verse outline"
                )
                var noteExpanded by rememberSaveable(verse.surah, verse.number) { mutableStateOf(false) }
                var showRelatedHadith by remember(verse.surah, verse.number) { mutableStateOf(false) }
                var loadingRelatedHadith by remember(verse.surah, verse.number) { mutableStateOf(false) }
                var relatedHadiths by remember(verse.surah, verse.number) { mutableStateOf<List<AyahHadith>>(emptyList()) }
                Card(
                    modifier = Modifier
                        .staggeredEntrance(itemKey = "${verse.surah}:${verse.number}", index = index)
                        .premiumClickable {
                        selected = verse.surah
                        onOpenVerse(verse.surah, verse.number)
                        prefs.edit().putInt("last_surah", verse.surah).putInt("last_ayah_number", verse.number).apply()
                        query = ""
                        selectedAyahForSheet = verse.surah to verse.number
                    },
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, verseBorder)
                ) {
                    Column(
                        Modifier.fillMaxWidth()
                            .background(Brush.linearGradient(listOf(cardColor, Gold.copy(alpha = if (verseIsFocused) 0.055f else 0.018f), cardColor)))
                            .padding(15.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(32.dp).clip(CircleShape).background(Brush.linearGradient(listOf(DeepGreen, Color(0xFF19725A)))).border(1.dp, Gold.copy(alpha = 0.34f), CircleShape), contentAlignment = Alignment.Center) {
                                Text(verse.number.toString(), color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (query.isNotBlank()) {
                                    val name = if (language == AppLanguage.EN) verse.surahNameEnglish else verse.surahNameArabic
                                    "$name • ${tr(language, "ayah")} ${verse.number}"
                                } else tr(language, "ayah") + " " + verse.number,
                                color = foreground.copy(alpha = 0.5f), fontSize = 11.sp, maxLines = 1
                            )
                            Spacer(Modifier.weight(1f))
                            ReaderActionIcon(Icons.Outlined.AutoStories, tr(language, "tafsir")) {
                                onTafsir(verse.surah, verse.number)
                            }
                            ReaderActionIcon(
                                Icons.Outlined.PlayCircle,
                                if (language == AppLanguage.EN) "Play this verse" else if (language == AppLanguage.AR) "استمع إلى هذه الآية" else "گوێگرتن لەم ئایەتە"
                            ) {
                                onPlayAudio(verse.surah, verse.number)
                            }
                            ReaderActionIcon(Icons.Outlined.BookmarkBorder, if (language == AppLanguage.EN) "Bookmark verse" else if (language == AppLanguage.AR) "حفظ الآية" else "ئایەت پاشەکەوت بکە") {
                                onBookmark(verse.surah, verse.number)
                                val message = when (language) {
                                    AppLanguage.CKB -> "ئایەتەکە پاشەکەوت کرا"
                                    AppLanguage.AR -> "تم حفظ الآية في العلامات المرجعية"
                                    AppLanguage.EN -> "Verse saved to bookmarks"
                                }
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }
                            ReaderActionIcon(
                                Icons.Outlined.Share,
                                if (language == AppLanguage.EN) "Share verse" else if (language == AppLanguage.AR) "مشاركة الآية" else "ئایەت هاوبەش بکە"
                            ) {
                                val surahTitle = surahs.firstOrNull { it.number == verse.surah }?.arabicName.orEmpty()
                                val translationName = when (language) {
                                    AppLanguage.CKB -> currentTranslation.ckb
                                    AppLanguage.AR -> currentTranslation.arabic
                                    AppLanguage.EN -> currentTranslation.english
                                }
                                val shareText = buildString {
                                    append("سورة ").append(surahTitle).append(" • ").append(verse.surah).append(":").append(verse.number)
                                    append("\n\n").append(verse.arabic)
                                    if (verse.translation.isNotBlank()) append("\n\n").append(verse.translation)
                                    if (translationName.isNotBlank()) append("\n— ").append(translationName)
                                    append("\n\n").append("مەشخەڵ • Mashxall")
                                }
                                try {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "Qur'an ${verse.surah}:${verse.number}")
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                    }
                                    context.startActivity(Intent.createChooser(
                                        shareIntent,
                                        when (language) {
                                            AppLanguage.EN -> "Share verse"
                                            AppLanguage.AR -> "مشاركة الآية"
                                            AppLanguage.CKB -> "ئایەت هاوبەش بکە"
                                        }
                                    ))
                                } catch (_: Exception) {
                                    Toast.makeText(context, tr(language, "no_data"), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        val taggedAyah = tajweedAyahs[verse.number]
                            .takeIf { coloredTajweed && !showUnvowelled && tajweedLoadedSurah == verse.surah }
                        val displayedAyah = remember(
                            verse.surah, verse.number, taggedAyah, showUnvowelled, verse.arabic, verse.arabicWithoutHarakat, foreground
                        ) {
                            if (!taggedAyah.isNullOrBlank()) {
                                TajweedText.annotate(taggedAyah, darkMode = foreground.red > 0.5f)
                            } else {
                                androidx.compose.ui.text.AnnotatedString(
                                    if (showUnvowelled && verse.arabicWithoutHarakat.isNotBlank()) verse.arabicWithoutHarakat else verse.arabic
                                )
                            }
                        }
                        Text(
                            text = displayedAyah,
                            color = foreground,
                            fontSize = (23f * verseTextScale).sp,
                            lineHeight = (39f * verseTextScale).sp,
                            textAlign = TextAlign.End,
                            fontFamily = quranTextFontFamily,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (verse.hasHadith) {
                            Spacer(Modifier.height(5.dp))
                            TextButton(
                                onClick = {
                                    showRelatedHadith = true
                                    if (relatedHadiths.isEmpty() && !loadingRelatedHadith) {
                                        loadingRelatedHadith = true
                                        scope.launch {
                                            relatedHadiths = ContentRepository.loadHadithsForAyah(context, verse.surah, verse.number)
                                            loadingRelatedHadith = false
                                        }
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    when (language) {
                                        AppLanguage.EN -> "Read related hadith"
                                        AppLanguage.AR -> "عرض الأحاديث المرتبطة بالآية"
                                        AppLanguage.CKB -> "فەرموودە پەیوەندیدارەکان ببینە"
                                    },
                                    color = Gold.copy(alpha = 0.96f), fontSize = 11.sp
                                )
                            }
                        }
                        if (showRelatedHadith) AlertDialog(
                            onDismissRequest = { showRelatedHadith = false },
                            title = {
                                Column {
                                    Text(
                                        when (language) {
                                            AppLanguage.EN -> "Hadith related to this verse"
                                            AppLanguage.AR -> "الأحاديث المرتبطة بهذه الآية"
                                            AppLanguage.CKB -> "فەرموودەکانی پەیوەندیدار بە ئەم ئایەتە"
                                        },
                                        color = foreground, fontWeight = FontWeight.Bold, fontSize = 17.sp
                                    )
                                    Text(
                                        "${tr(language, "surah")} ${verse.surah} • ${tr(language, "ayah")} ${verse.number}",
                                        color = Gold, fontSize = 11.sp
                                    )
                                }
                            },
                            text = {
                                when {
                                    loadingRelatedHadith -> Box(Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(color = Gold)
                                    }
                                    relatedHadiths.isEmpty() -> Text(
                                        when (language) {
                                            AppLanguage.EN -> "No readable related narration was found for this verse."
                                            AppLanguage.AR -> "لم يُعثر على رواية قابلة للعرض مرتبطة بهذه الآية."
                                            AppLanguage.CKB -> "هیچ فەرموودەیەکی خوێندراوە بۆ ئەم ئایەتە نەدۆزرایەوە."
                                        },
                                        color = foreground, fontSize = 13.sp, lineHeight = 20.sp
                                    )
                                    else -> LazyColumn(
                                        modifier = Modifier.heightIn(max = 420.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        items(relatedHadiths, key = { it.id }) { related ->
                                            Column(
                                                Modifier.fillMaxWidth().padding(bottom = 6.dp)
                                            ) {
                                                val collectionLabel = when (language) {
                                                    AppLanguage.EN -> related.collectionEnglish.ifBlank { related.collectionCode }
                                                    AppLanguage.AR -> related.collectionArabic.ifBlank { related.collectionCode }
                                                    AppLanguage.CKB -> related.collectionSorani.ifBlank { related.collectionArabic.ifBlank { related.collectionCode } }
                                                }
                                                if (collectionLabel.isNotBlank()) {
                                                    Text(collectionLabel, color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Text(related.arabic, color = foreground, fontSize = 17.sp, lineHeight = 28.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                                                val translation = when (language) {
                                                    AppLanguage.CKB -> related.kurdish
                                                    AppLanguage.AR -> ""
                                                    AppLanguage.EN -> related.english
                                                }
                                                if (translation.isNotBlank()) {
                                                    Spacer(Modifier.height(7.dp))
                                                    Text(translation, color = foreground.copy(alpha = 0.84f), fontSize = 12.sp, lineHeight = 20.sp)
                                                }
                                                if (language != AppLanguage.EN && related.english.isNotBlank()) {
                                                    Spacer(Modifier.height(5.dp))
                                                    Text(
                                                        when (language) {
                                                            AppLanguage.AR -> "الترجمة الإنجليزية: " + related.english
                                                            AppLanguage.CKB -> "English: " + related.english
                                                            AppLanguage.EN -> related.english
                                                        },
                                                        color = foreground.copy(alpha = 0.65f), fontSize = 11.sp, lineHeight = 18.sp
                                                    )
                                                }
                                                val localizedReference = when (language) {
                                                    AppLanguage.EN -> related.referenceEnglish
                                                    AppLanguage.AR -> related.referenceArabic
                                                    AppLanguage.CKB -> related.reference
                                                }
                                                if (localizedReference.isNotBlank()) {
                                                    Spacer(Modifier.height(6.dp))
                                                    Text(localizedReference, color = foreground.copy(alpha = 0.56f), fontSize = 10.sp, lineHeight = 16.sp)
                                                }
                                                if (related.verseReferences.size > 1) {
                                                    Spacer(Modifier.height(3.dp))
                                                    Text(
                                                        (if (language == AppLanguage.EN) "Also linked to: " else if (language == AppLanguage.AR) "مرتبطة أيضًا بالآيات: " else "پەیوەستە بە ئایەتەکانی: ") +
                                                            related.verseReferences.filterNot { it == (verse.surah to verse.number) }.joinToString { "${it.first}:${it.second}" },
                                                        color = foreground.copy(alpha = 0.5f), fontSize = 9.sp
                                                    )
                                                }
                                                Divider(color = Gold.copy(alpha = 0.2f), modifier = Modifier.padding(top = 8.dp))
                                            }
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showRelatedHadith = false }) {
                                    Text(if (language == AppLanguage.EN) "Close" else if (language == AppLanguage.AR) "إغلاق" else "داخستن")
                                }
                            }
                        )
                        if (verse.note.isNotBlank()) {
                            TextButton(onClick = { noteExpanded = !noteExpanded }) {
                                Text(
                                    if (noteExpanded) {
                                        if (language == AppLanguage.EN) "Hide verse note" else if (language == AppLanguage.AR) "إخفاء ملاحظة الآية" else "تێبینی ئایەت بشارەوە"
                                    } else {
                                        if (language == AppLanguage.EN) "Verse note" else if (language == AppLanguage.AR) "ملاحظة الآية" else "تێبینی ئایەت"
                                    }, fontSize = 10.sp
                                )
                            }
                            if (noteExpanded) Text(verse.note.replace("<br>", "\n").replace("<br/>", "\n"), color = foreground.copy(alpha = 0.72f), fontSize = 12.sp, lineHeight = 19.sp)
                        }
                        if (verse.translation.isNotBlank()) {
                            Spacer(Modifier.height(8.dp)); Divider(color = Gold.copy(alpha = 0.24f)); Spacer(Modifier.height(8.dp))
                            Text(verse.translation, color = foreground.copy(alpha = 0.83f), fontSize = (14f * verseTextScale).sp, lineHeight = (23f * verseTextScale).sp)
                        }
                    }
                }
            }
        }
    }
    if (chooseSurah) AlertDialog(
        onDismissRequest = { chooseSurah = false }, title = { Text(tr(language, "choose_surah")) },
        text = {
            LazyColumn(Modifier.height(420.dp)) {
                items(surahs, key = { it.number }) { item ->
                    Row(
                        Modifier.fillMaxWidth()
                            .premiumClickable { selected = item.number; onSurah(item.number); chooseSurah = false }
                            .padding(vertical = 9.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(item.number.toString(), color = Gold, modifier = Modifier.width(32.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.arabicName, color = foreground, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            val placeLabel = when {
                                item.place.contains("meccan", ignoreCase = true) ||
                                    item.place.contains("makki", ignoreCase = true) -> when (language) {
                                        AppLanguage.EN -> "Meccan"
                                        AppLanguage.AR -> "مكية"
                                        AppLanguage.CKB -> "مەککی"
                                    }
                                item.place.contains("medinan", ignoreCase = true) ||
                                    item.place.contains("madani", ignoreCase = true) -> when (language) {
                                        AppLanguage.EN -> "Medinan"
                                        AppLanguage.AR -> "مدنية"
                                        AppLanguage.CKB -> "مەدەنی"
                                    }
                                else -> item.place
                            }
                            Text(
                                listOf(
                                    item.englishName,
                                    item.ayahCount.toString() + " " + when (language) {
                                        AppLanguage.EN -> "verses"
                                        AppLanguage.AR -> "آية"
                                        AppLanguage.CKB -> "ئایەت"
                                    },
                                    placeLabel
                                ).filter { it.isNotBlank() }.joinToString(" • "),
                                color = foreground.copy(alpha = 0.56f), fontSize = 10.sp, maxLines = 1
                            )
                            val meaning = item.uniqueTitle.ifBlank { item.meaningEnglish }
                            if (meaning.isNotBlank()) {
                                Text(meaning, color = foreground.copy(alpha = 0.46f), fontSize = 9.sp, maxLines = 1)
                            }
                        }
                        if (item.number == selected) Text("✓", color = Gold, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { chooseSurah = false }) { Text(tr(language, "back")) } }
    )
    if (chooseTranslation) AlertDialog(
        onDismissRequest = { chooseTranslation = false },
        title = { Text(tr(language, "translation")) },
        text = {
            LazyColumn(Modifier.height(380.dp)) {
                items(ContentRepository.quranTranslations, key = { it.id }) { choice ->
                    val label = when (language) {
                        AppLanguage.CKB -> choice.ckb
                        AppLanguage.AR -> choice.arabic
                        AppLanguage.EN -> choice.english
                    }
                    Row(
                        Modifier.fillMaxWidth().premiumClickable {
                            translationKey = choice.id
                            prefs.edit().putString("quran_translation", choice.id).apply()
                            chooseTranslation = false
                        }.padding(vertical = 11.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = translationKey == choice.id, onClick = {
                            translationKey = choice.id
                            prefs.edit().putString("quran_translation", choice.id).apply()
                            chooseTranslation = false
                        })
                        Spacer(Modifier.width(6.dp))
                        Text(label, color = foreground, fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { chooseTranslation = false }) { Text(tr(language, "back")) } }
    )


    if (showReaderSettings) Dialog(
        onDismissRequest = { showReaderSettings = false; readerSettingsSection = "overview" }
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.96f).heightIn(min = 430.dp, max = 730.dp),
            shape = RoundedCornerShape(26.dp), color = cardColor,
            border = BorderStroke(1.dp, Gold.copy(alpha = 0.24f)), shadowElevation = 16.dp
        ) {
            Column(Modifier.fillMaxWidth().padding(17.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (readerSettingsSection != "overview") IconButton(onClick = { readerSettingsSection = "overview" }) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = tr(language, "back"), tint = Gold)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            when (language) { AppLanguage.EN -> "Quran reader settings"; AppLanguage.AR -> "إعدادات قارئ القرآن"; AppLanguage.CKB -> "ڕێکخستنەکانی خوێندنەوەی قورئان" },
                            color = foreground, fontSize = 19.sp, fontWeight = FontWeight.Bold
                        )
                        Text(
                            when (readerSettingsSection) {
                                "font" -> if (language == AppLanguage.EN) "Real Arabic script fonts from the recovered source apps" else if (language == AppLanguage.AR) "خطوط عربية أصلية من التطبيقات المستخرجة" else "فۆنتە ڕەسەنەکانی عەرەبی لە ئەپە ڕیکۆڤەرکراوەکان"
                                "translation" -> if (language == AppLanguage.EN) "Choose the default translation" else if (language == AppLanguage.AR) "اختر الترجمة الافتراضية" else "وەرگێڕانی بنەڕەت هەڵبژێرە"
                                "tafsir" -> if (language == AppLanguage.EN) "Choose the default tafsir" else if (language == AppLanguage.AR) "اختر التفسير الافتراضي" else "تەفسیری بنەڕەت هەڵبژێرە"
                                "reciters" -> if (language == AppLanguage.EN) "Choose the reciter and playback mode" else if (language == AppLanguage.AR) "اختر القارئ وطريقة التشغيل" else "قورئان‌خوێن و شێوازی لێدان هەڵبژێرە"
                                "appearance" -> if (language == AppLanguage.EN) "Mushaf appearance and gestures" else if (language == AppLanguage.AR) "مظهر المصحف والإيماءات" else "ڕووکاری مصحەف و جووڵەکان"
                                else -> if (language == AppLanguage.EN) "Options grouped by task" else if (language == AppLanguage.AR) "خيارات مرتبة حسب الوظيفة" else "هەڵبژاردەکان بەپێی ئەرک ڕێکخراون"
                            },
                            color = foreground.copy(alpha = 0.62f), fontSize = 11.sp
                        )
                    }
                    TextButton(onClick = { showReaderSettings = false; readerSettingsSection = "overview" }) {
                        Text("×", color = Gold, fontSize = 25.sp)
                    }
                }
                Spacer(Modifier.height(9.dp))
                Divider(color = Gold.copy(alpha = 0.25f))
                Spacer(Modifier.height(11.dp))
                when (readerSettingsSection) {
                    "overview" -> {
                        Text(
                            when (language) {
                                AppLanguage.EN -> "Open a section to change just those options. Your choices are saved on this device."
                                AppLanguage.AR -> "افتح القسم المطلوب لتعديل خياراته فقط. تُحفظ اختياراتك على هذا الجهاز."
                                AppLanguage.CKB -> "بەشێک بکەرەوە بۆ ڕێکخستنی هەڵبژاردەکانی. هەڵبژاردەکانت لەم مۆبایلەدا دەپارێزرێن."
                            }, color = foreground.copy(alpha = 0.65f), fontSize = 12.sp, lineHeight = 18.sp
                        )
                        Spacer(Modifier.height(10.dp))
                        val categories = listOf(
                            Triple("appearance",
                                (if (language == AppLanguage.EN) "Mushaf & reading appearance" else if (language == AppLanguage.AR) "المصحف ومظهر القراءة" else "مصحەف و ڕووکاری خوێندنەوە") to
                                (if (language == AppLanguage.EN) "Night mode, warm paper, text size and gestures" else if (language == AppLanguage.AR) "الوضع الليلي والورق الدافئ وحجم النص والإيماءات" else "دۆخی شەوانە، کاغەزی گەرم، قەبارەی دەق و جووڵەکان"),
                                Icons.Outlined.AutoStories),
                            Triple("font",
                                (if (language == AppLanguage.EN) "Quran text font" else if (language == AppLanguage.AR) "خط نص القرآن" else "فۆنتی دەقی قورئان") to
                                (if (language == AppLanguage.EN) "Choose one of six bundled original fonts" else if (language == AppLanguage.AR) "اختر واحدًا من ستة خطوط أصلية مضمّنة" else "یەکێک لە شەش فۆنتی ڕەسەنی ناو حەزمەکە هەڵبژێرە"),
                                Icons.Outlined.AutoStories),
                            Triple("translation",
                                (if (language == AppLanguage.EN) "Translations" else if (language == AppLanguage.AR) "الترجمات" else "وەرگێڕانەکان") to
                                (if (language == AppLanguage.EN) "Choose the translation shown with an ayah" else if (language == AppLanguage.AR) "اختر الترجمة التي تظهر مع الآية" else "وەرگێڕانی هاوڕێی ئایەت هەڵبژێرە"),
                                Icons.Outlined.Language),
                            Triple("tafsir",
                                (if (language == AppLanguage.EN) "Tafsir" else if (language == AppLanguage.AR) "التفسير" else "تەفسیر") to
                                (if (language == AppLanguage.EN) "Choose the commentary opened for an ayah" else if (language == AppLanguage.AR) "اختر التفسير الذي يفتح عند اختيار الآية" else "تەفسیری ئایەت هەڵبژێرە"),
                                Icons.Outlined.MenuBook),
                            Triple("reciters",
                                (if (language == AppLanguage.EN) "Reciter & playback mode" else if (language == AppLanguage.AR) "القارئ وطريقة التشغيل" else "قورئان‌خوێن و شێوازی لێدان") to
                                (if (language == AppLanguage.EN) "Verse-by-verse or full-surah recordings" else if (language == AppLanguage.AR) "تسجيل آية بآية أو السورة كاملة" else "تۆماری ئایەت بە ئایەت یان سوورەتی تەواو"),
                                Icons.Outlined.GraphicEq)
                        )
                        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp), contentPadding = PaddingValues(bottom = 4.dp)) {
                            items(categories, key = { it.first }) { category ->
                                ReaderSettingsCategoryCard(category.second.first, category.second.second, category.third, foreground, cardColor) {
                                    readerSettingsSection = category.first
                                }
                            }
                            item {
                                Card(colors = CardDefaults.cardColors(containerColor = cardColor), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Gold.copy(alpha = 0.16f))) {
                                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                                        Icon(Icons.Outlined.Info, null, tint = Gold, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(10.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(if (language == AppLanguage.EN) "Mushaf editions & Tajweed" else if (language == AppLanguage.AR) "طبعات المصحف والتجويد" else "چاپەکانی مصحەف و تەجوید", color = foreground, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                            Text(
                                                if (language == AppLanguage.EN) "The original 604-page graphic Mushaf remains unchanged. Colored Tajweed is available in verse-text view; extra full-page Mushaf editions need complete verified page assets."
                                                else if (language == AppLanguage.AR) "يبقى المصحف المصوّر الأصلي ذو 604 صفحات كما هو. التجويد الملوّن متاح في عرض نص الآيات؛ أما طبعات المصاحف المصوّرة الإضافية فتحتاج ملفات صفحات كاملة وموثوقة."
                                                else "مصحەفی وێنەیی ڕەسەنی ٦٠٤ پەڕە ناگۆڕدرێت. تەجویدی ڕەنگاوڕەنگ لە پێشان‌دانی دەقی ئایەت بەردەستە؛ چاپی وێنەیی زیاتر پێویستی بە فایلە تەواو و پشتڕاستکراوەکانە.",
                                                color = foreground.copy(alpha = 0.62f), fontSize = 10.sp, lineHeight = 15.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "font" -> {
                        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                Text(
                                    when (language) {
                                        AppLanguage.EN -> "These options change the font in verse-text reading. They do not replace the separately rendered 604-page graphic Mushaf."
                                        AppLanguage.AR -> "تغيّر هذه الخيارات الخط في عرض نص الآيات. وهي لا تستبدل المصحف المصوّر المنفصل ذي 604 صفحات."
                                        AppLanguage.CKB -> "ئەم هەڵبژاردانە فۆنتی خوێندنەوەی دەقی ئایەت دەگۆڕن؛ جێگرەوەی مصحەفی وێنەیی ٦٠٤ پەڕە نین."
                                    },
                                    color = foreground.copy(alpha = 0.62f), fontSize = 11.sp, lineHeight = 17.sp
                                )
                                if (quranFontLoadFailed) {
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        when (language) {
                                            AppLanguage.EN -> "The selected source font failed integrity/loading checks; the system font is used."
                                            AppLanguage.AR -> "فشل التحقق من الخط المحدد أو تحميله؛ يُستخدم خط النظام بدلًا منه."
                                            AppLanguage.CKB -> "پشتڕاستکردنەوە یان بارکردنی فۆنتەکە سەرکەوتوو نەبوو؛ فۆنتی سیستەم بەکاردێت."
                                        },
                                        color = Gold, fontSize = 10.sp, lineHeight = 15.sp
                                    )
                                }
                            }
                            items(quranTextFontOptions, key = { it.id }) { option ->
                                val selectedFont = quranTextFontId == option.id
                                Card(
                                    modifier = Modifier.fillMaxWidth().premiumClickable {
                                        quranTextFontId = option.id
                                        prefs.edit().putString("quran_text_font", option.id).apply()
                                    },
                                    colors = CardDefaults.cardColors(containerColor = cardColor),
                                    shape = RoundedCornerShape(15.dp),
                                    border = BorderStroke(1.dp, if (selectedFont) Gold else Gold.copy(alpha = 0.12f))
                                ) {
                                    Row(
                                        Modifier.fillMaxWidth().padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                quranTextFontLabel(option, language),
                                                color = foreground, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                option.assetPath.substringAfterLast('/'),
                                                color = foreground.copy(alpha = 0.56f), fontSize = 9.sp
                                            )
                                            Text(
                                                when (language) {
                                                    AppLanguage.EN -> "Verified original • ${option.sizeBytes / 1024} KB"
                                                    AppLanguage.AR -> "ملف أصلي موثّق • ${option.sizeBytes / 1024} كيلوبايت"
                                                    AppLanguage.CKB -> "فایلی ڕەسەنی پشتڕاستکراو • ${option.sizeBytes / 1024} کیلۆبایت"
                                                },
                                                color = foreground.copy(alpha = 0.50f), fontSize = 9.sp
                                            )
                                        }
                                        if (selectedFont) Icon(
                                            Icons.Outlined.CheckCircle,
                                            contentDescription = if (language == AppLanguage.EN) "Selected" else if (language == AppLanguage.AR) "محدد" else "هەڵبژێردراوە",
                                            tint = Gold
                                        )
                                    }
                                }
                            }
                        }
                    }
                    "translation" -> {
                        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(ContentRepository.quranTranslations, key = { "reader-translation-" + it.id }) { choice ->
                                val label = when (language) { AppLanguage.CKB -> choice.ckb; AppLanguage.AR -> choice.arabic; AppLanguage.EN -> choice.english }
                                Card(
                                    modifier = Modifier.fillMaxWidth().premiumClickable { translationKey = choice.id; prefs.edit().putString("quran_translation", choice.id).apply() },
                                    colors = CardDefaults.cardColors(containerColor = cardColor), shape = RoundedCornerShape(15.dp),
                                    border = BorderStroke(1.dp, if (translationKey == choice.id) Gold else Gold.copy(alpha = 0.12f))
                                ) {
                                    Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(label, color = foreground, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                            if (translationKey == choice.id) Text(if (language == AppLanguage.EN) "Selected" else if (language == AppLanguage.AR) "محددة" else "هەڵبژێردراوە", color = Gold, fontSize = 10.sp)
                                        }
                                        RadioButton(selected = translationKey == choice.id, onClick = { translationKey = choice.id; prefs.edit().putString("quran_translation", choice.id).apply() })
                                    }
                                }
                            }
                        }
                    }
                    "tafsir" -> {
                        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(ContentRepository.tafsirBooks, key = { "reader-tafsir-" + it.id }) { book ->
                                val label = when (language) { AppLanguage.CKB -> book.labelCkb; AppLanguage.AR -> book.labelAr; AppLanguage.EN -> book.labelEn }
                                Card(
                                    modifier = Modifier.fillMaxWidth().premiumClickable { selectedTafsirBook = book.id; prefs.edit().putString("quran_tafsir_book", book.id).apply() },
                                    colors = CardDefaults.cardColors(containerColor = cardColor), shape = RoundedCornerShape(15.dp),
                                    border = BorderStroke(1.dp, if (selectedTafsirBook == book.id) Gold else Gold.copy(alpha = 0.12f))
                                ) {
                                    Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(label, color = foreground, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                            if (selectedTafsirBook == book.id) Text(if (language == AppLanguage.EN) "Selected" else if (language == AppLanguage.AR) "محدد" else "هەڵبژێردراوە", color = Gold, fontSize = 10.sp)
                                        }
                                        RadioButton(selected = selectedTafsirBook == book.id, onClick = { selectedTafsirBook = book.id; prefs.edit().putString("quran_tafsir_book", book.id).apply() })
                                    }
                                }
                            }
                        }
                    }
                    "reciters" -> {
                        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            item {
                                Text(
                                    if (language == AppLanguage.EN) "PLAYBACK MODE"
                                    else if (language == AppLanguage.AR) "طريقة التشغيل"
                                    else "شێوازی لێدان",
                                    color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                                    PremiumChoicePill(
                                        modifier = Modifier.weight(1f),
                                        label = if (language == AppLanguage.EN) "Full surah" else if (language == AppLanguage.AR) "السورة كاملة" else "سوورەتی تەواو",
                                        selected = readerPlaybackMode == "full",
                                        accent = Gold,
                                        foreground = foreground
                                    ) {
                                        ReaderPlaybackSettings.mode = "full"
                                        prefs.edit().putString("reciter_playback_mode", "full").apply()
                                    }
                                    PremiumChoicePill(
                                        modifier = Modifier.weight(1f),
                                        label = if (language == AppLanguage.EN) "From selected ayah" else if (language == AppLanguage.AR) "من الآية المحددة" else "لە ئایەتی هەڵبژێردراوەوە",
                                        selected = readerPlaybackMode == "verse",
                                        accent = Gold,
                                        foreground = foreground
                                    ) {
                                        ReaderPlaybackSettings.mode = "verse"
                                        prefs.edit().putString("reciter_playback_mode", "verse").apply()
                                    }
                                }
                                Text(
                                    when {
                                        readerPlaybackMode == "full" && language == AppLanguage.EN -> "Starts at ayah 1. Ayah-by-ayah recordings play in sequence; full-surah recordings play as one track."
                                        readerPlaybackMode == "full" && language == AppLanguage.AR -> "يبدأ من الآية الأولى؛ تُشغَّل تسجيلات الآيات بالتتابع، أما التسجيل الكامل فيعمل كمقطع واحد."
                                        readerPlaybackMode == "full" -> "لە ئایەتی یەکەمەوە دەست پێدەکات؛ تۆمارەکانی ئایەت بە ڕیز لێدەدرێن، تۆماری تەواوی سوورەتیش وەک یەک دەنگ لێدەدرێت."
                                        language == AppLanguage.EN -> "Starts at the chosen ayah and continues to the end; only reciters with individual ayah files are listed."
                                        language == AppLanguage.AR -> "يبدأ من الآية المختارة حتى نهاية السورة؛ وتُعرض فقط المصادر ذات ملفات الآيات المنفصلة."
                                        else -> "لە ئایەتی هەڵبژێردراوەوە تا کۆتایی سوورەت دەخوێنێتەوە؛ تەنها سەرچاوەکانی فایلە جیاوازەکانی ئایەت پیشان دەدرێن."
                                    },
                                    color = foreground.copy(alpha = 0.60f), fontSize = 10.sp, lineHeight = 15.sp
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    if (language == AppLanguage.EN) "RECITERS WITH INDIVIDUAL AYAH FILES"
                                    else if (language == AppLanguage.AR) "القرّاء ذوو ملفات الآيات المنفصلة"
                                    else "قورئان‌خوێنەکانی فایلە جیاوازەکانی ئایەت",
                                    color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold
                                )
                            }
                            items(readerReciters.filter { it.audioType == 0 }, key = { "verse-reciter-" + it.id }) { reciter ->
                                Card(modifier = Modifier.fillMaxWidth().premiumClickable { preferredReaderReciterId = reciter.id; prefs.edit().putInt("preferred_reciter_id", reciter.id).apply() }, colors = CardDefaults.cardColors(containerColor = cardColor), shape = RoundedCornerShape(15.dp), border = BorderStroke(1.dp, if (preferredReaderReciterId == reciter.id) Gold else Gold.copy(alpha = 0.12f))) {
                                    Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) { Text(reciter.nameArabic, color = foreground, fontSize = 12.sp, fontWeight = FontWeight.SemiBold); Text(reciter.nameEnglish, color = foreground.copy(alpha = 0.58f), fontSize = 10.sp) }
                                        RadioButton(selected = preferredReaderReciterId == reciter.id, onClick = { preferredReaderReciterId = reciter.id; prefs.edit().putInt("preferred_reciter_id", reciter.id).apply() })
                                    }
                                }
                            }
                            if (readerPlaybackMode == "full") {
                                item {
                                    Spacer(Modifier.height(5.dp))
                                    Text(if (language == AppLanguage.EN) "FULL-SURAH RECORDINGS" else if (language == AppLanguage.AR) "تسجيلات السورة كاملة" else "تۆمارەکانی سوورەتی تەواو", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(if (language == AppLanguage.EN) "These recordings always start at the beginning of the surah. Select an individual-ayah reciter if you need to start at a chosen ayah." else if (language == AppLanguage.AR) "تبدأ هذه التسجيلات دائمًا من أول السورة؛ اختر قارئًا بملفات آيات منفصلة إذا أردت البدء من آية محددة." else "ئەم تۆمارانە هەمیشە لە سەرەتای سوورەتەوە دەست پێدەکەن؛ ئەگەر دەتەوێت لە ئایەتێکی دیاریکراوەوە دەست پێبکەیت، قورئان‌خوێنێک هەڵبژێرە کە فایلەکانی ئایەت بە جیاوازی هەبن.", color = foreground.copy(alpha = 0.60f), fontSize = 10.sp, lineHeight = 15.sp)
                                    Spacer(Modifier.height(6.dp))
                                }
                                items(readerReciters.filter { it.audioType != 0 }, key = { "full-reciter-" + it.id }) { reciter ->
                                Card(modifier = Modifier.fillMaxWidth().premiumClickable { preferredReaderReciterId = reciter.id; prefs.edit().putInt("preferred_reciter_id", reciter.id).apply() }, colors = CardDefaults.cardColors(containerColor = cardColor), shape = RoundedCornerShape(15.dp), border = BorderStroke(1.dp, if (preferredReaderReciterId == reciter.id) Gold else Gold.copy(alpha = 0.12f))) {
                                    Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) { Text(reciter.nameArabic, color = foreground, fontSize = 12.sp, fontWeight = FontWeight.SemiBold); Text(reciter.nameEnglish, color = foreground.copy(alpha = 0.58f), fontSize = 10.sp) }
                                        RadioButton(selected = preferredReaderReciterId == reciter.id, onClick = { preferredReaderReciterId = reciter.id; prefs.edit().putInt("preferred_reciter_id", reciter.id).apply() })
                                    }
                                }
                            }
                            }
                            if (readerPlaybackMode == "full" && readerReciters.none { it.audioType != 0 }) {
                                item {
                                    Text(
                                        if (language == AppLanguage.EN) "No full-surah-only sources are available in the supplied catalogue."
                                        else if (language == AppLanguage.AR) "لا توجد مصادر مقتصرة على تسجيل السورة كاملة في الفهرس المرفق."
                                        else "لە فهرستی بەردەستدا سەرچاوەی تایبەت بە تۆماری تەواوی سوورەت نییە.",
                                        color = foreground.copy(alpha = 0.6f), fontSize = 11.sp, lineHeight = 17.sp
                                    )
                                }
                            }
                            if (readerReciters.isEmpty()) item { Text(tr(language, "loading"), color = foreground.copy(alpha = 0.65f), fontSize = 11.sp) }
                        }
                    }
                    "appearance" -> {
                        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                            Text(if (language == AppLanguage.EN) "Included edition: original 604-page Mushaf." else if (language == AppLanguage.AR) "النسخة المضمّنة: المصحف الأصلي ذو 604 صفحات." else "وەشانی بەردەست: مصحەفی ڕەسەنی ٦٠٤ پەڕە.", color = foreground.copy(alpha = 0.68f), fontSize = 11.sp, lineHeight = 17.sp)
                            Spacer(Modifier.height(12.dp))
                            Card(colors = CardDefaults.cardColors(containerColor = cardColor), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Gold.copy(alpha = 0.22f))) {
                                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            if (language == AppLanguage.EN) "Colored Tajweed in verse text"
                                            else if (language == AppLanguage.AR) "التجويد الملوّن في نص الآيات"
                                            else "تەجویدی ڕەنگاوڕەنگ لە دەقی ئایەت",
                                            color = foreground, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(Modifier.height(3.dp))
                                        Text(
                                            if (language == AppLanguage.EN) "Loads the tagged text for the selected surah online on first use and keeps a private cache for offline reading. The printed page image stays unchanged."
                                            else if (language == AppLanguage.AR) "يحمّل نص التجويد للسورة من الإنترنت عند أول استخدام ويحتفظ بنسخة خاصة للقراءة دون إنترنت. لا يغيّر صورة صفحات المصحف."
                                            else "لە یەکەم بەکارهێنان دەقی تەجویدی سوورەتەکە لە ئۆنلاین دادەگیرێت و لە ناو ئەپەکەدا دەپارێزرێت؛ وێنەی پەڕەی مصحەف ناگۆڕێت.",
                                            color = foreground.copy(alpha = 0.58f), fontSize = 10.sp, lineHeight = 15.sp
                                        )
                                    }
                                    Switch(
                                        checked = coloredTajweed,
                                        onCheckedChange = { enabled ->
                                            coloredTajweed = enabled
                                            prefs.edit().putBoolean("quran_colored_tajweed", enabled).apply()
                                            if (!enabled) {
                                                tajweedAyahs = emptyMap()
                                                tajweedLoadedSurah = 0
                                                tajweedMessage = ""
                                            }
                                        }
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Card(colors = CardDefaults.cardColors(containerColor = cardColor), shape = RoundedCornerShape(16.dp)) {
                                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(if (language == AppLanguage.EN) "Night reading mode" else if (language == AppLanguage.AR) "وضع القراءة الليلي" else "دۆخی خوێندنەوەی شەوانە", color = foreground, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        Text(if (language == AppLanguage.EN) "Invert paper and ink colours" else if (language == AppLanguage.AR) "اعكس لون الورق والحبر" else "ڕەنگی کاغەز و مەرەکەب بگۆڕە", color = foreground.copy(alpha = 0.58f), fontSize = 10.sp)
                                    }
                                    Switch(checked = mushafDarkMode, onCheckedChange = { mushafDarkMode = it; if (it) mushafSepiaMode = false; prefs.edit().putBoolean("mushaf_dark_mode", it).putBoolean("mushaf_sepia_mode", mushafSepiaMode).apply() })
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Card(colors = CardDefaults.cardColors(containerColor = cardColor), shape = RoundedCornerShape(16.dp)) {
                                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(if (language == AppLanguage.EN) "Warm paper / sepia" else if (language == AppLanguage.AR) "الورق الدافئ / سيبيا" else "کاغەزی گەرم / سێپیا", color = foreground, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        Text(if (language == AppLanguage.EN) "Soft cream tone for long reading" else if (language == AppLanguage.AR) "لون كريمي هادئ للقراءة الطويلة" else "ڕەنگی کرێمی نەرم بۆ خوێندنەوەی درێژ", color = foreground.copy(alpha = 0.58f), fontSize = 10.sp)
                                    }
                                    Switch(checked = mushafSepiaMode, onCheckedChange = { mushafSepiaMode = it; if (it) mushafDarkMode = false; prefs.edit().putBoolean("mushaf_sepia_mode", it).putBoolean("mushaf_dark_mode", mushafDarkMode).apply() })
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            Text(if (language == AppLanguage.EN) "VERSE PANEL TEXT SIZE" else if (language == AppLanguage.AR) "حجم خط لوحة الآية" else "قەبارەی دەقی ئایەت", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Slider(value = verseTextScale, onValueChange = { verseTextScale = it; prefs.edit().putFloat("quran_verse_text_scale", it).apply() }, valueRange = 0.8f..1.6f, steps = 7)
                            Text(String.format(Locale.US, "%.0f%%", verseTextScale * 100f), color = foreground.copy(alpha = 0.65f), fontSize = 11.sp)
                            Spacer(Modifier.height(14.dp))
                            Divider(color = Gold.copy(alpha = 0.18f))
                            Spacer(Modifier.height(10.dp))
                            Text(if (language == AppLanguage.EN) "Gestures" else if (language == AppLanguage.AR) "الإيماءات" else "جووڵەکان", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(
                                if (language == AppLanguage.EN) "Swipe right-to-left for the next Mushaf page, left-to-right for the previous page. Pinch to zoom, drag when zoomed, and double-tap to zoom in or reset."
                                else if (language == AppLanguage.AR) "اسحب من اليمين إلى اليسار للصفحة التالية، ومن اليسار إلى اليمين للسابقة. قرّب بإصبعين للتكبير، واسحب عند التكبير، واضغط مرتين للتكبير أو إعادة الحجم."
                                else "بۆ پەڕەی دواتر لە ڕاستەوە بۆ چەپ بسڕەوە، بۆ پەڕەی پێشوو لە چەپەوە بۆ ڕاست. بە دوو پەنجە گەورە بکە و دووجار پەنجە لێبدە بۆ گەورەکردن یان گەڕاندنەوە.",
                                color = foreground.copy(alpha = 0.68f), fontSize = 11.sp, lineHeight = 18.sp
                            )
                        }
                    }
                    else -> readerSettingsSection = "overview"
                }
                Spacer(Modifier.height(11.dp))
                Divider(color = Gold.copy(alpha = 0.20f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { showReaderSettings = false; readerSettingsSection = "overview" }) {
                        Text(if (language == AppLanguage.EN) "Done" else if (language == AppLanguage.AR) "تم" else "تەواو")
                    }
                    Spacer(Modifier.weight(1f))
                    if (readerSettingsSection != "overview") OutlinedButton(onClick = { readerSettingsSection = "overview" }) {
                        Text(if (language == AppLanguage.EN) "All settings" else if (language == AppLanguage.AR) "كل الإعدادات" else "هەموو ڕێکخستنەکان")
                    }
                }
            }
        }
    }

    if (selectedAyahForSheet != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedAyahForSheet = null },
            containerColor = cardColor,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 690.dp).verticalScroll(rememberScrollState()).padding(horizontal = 19.dp, vertical = 8.dp)
            ) {
                val target = selectedAyahForSheet!!
                val surah = surahs.firstOrNull { it.number == target.first }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            (surah?.arabicName ?: "") + " • " + target.first + ":" + target.second,
                            color = Gold, fontSize = 18.sp, fontWeight = FontWeight.Bold
                        )
                        Text(
                            when (language) {
                                AppLanguage.EN -> "Verse actions & study"
                                AppLanguage.AR -> "إجراءات الآية ودراستها"
                                AppLanguage.CKB -> "کردار و خوێندنەوەی ئایەت"
                            }, color = foreground.copy(alpha = 0.58f), fontSize = 11.sp
                        )
                    }
                    TextButton(onClick = { readerSettingsSection = "overview"; showReaderSettings = true }) { Text("⚙", color = Gold, fontSize = 22.sp) }
                }
                Spacer(Modifier.height(11.dp))
                when {
                    sheetLoading -> Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Gold)
                    }
                    sheetVerse == null -> Text(
                        when (language) {
                            AppLanguage.EN -> "Verse text is unavailable for this selection."
                            AppLanguage.AR -> "نص الآية غير متاح لهذا الاختيار."
                            AppLanguage.CKB -> "دەقی ئایەت بۆ ئەم هەڵبژاردنە بەردەست نییە."
                        }, color = foreground, fontSize = 13.sp
                    )
                    else -> {
                        Text(
                            text = sheetVerse!!.arabic,
                            color = foreground,
                            fontSize = (27f * verseTextScale).sp,
                            lineHeight = (48f * verseTextScale).sp,
                            textAlign = TextAlign.End,
                            fontFamily = quranTextFontFamily,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (sheetVerse!!.translation.isNotBlank()) {
                            Spacer(Modifier.height(9.dp))
                            Text(sheetVerse!!.translation, color = foreground.copy(alpha = 0.8f), fontSize = (15f * verseTextScale).sp, lineHeight = (25f * verseTextScale).sp)
                            Text(
                                when (language) { AppLanguage.EN -> currentTranslation.english; AppLanguage.AR -> currentTranslation.arabic; AppLanguage.CKB -> currentTranslation.ckb },
                                color = Gold.copy(alpha = 0.9f), fontSize = 10.sp
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            PremiumChoicePill(
                                modifier = Modifier.weight(1f),
                                label = if (language == AppLanguage.EN) "Copy" else if (language == AppLanguage.AR) "نسخ" else "کۆپی",
                                selected = false,
                                accent = Gold,
                                foreground = foreground,
                                leadingIcon = Icons.Outlined.ContentCopy
                            ) {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                clipboard?.setPrimaryClip(ClipData.newPlainText("Quran verse", sheetVerse!!.arabic + "\n\n" + sheetVerse!!.translation))
                                Toast.makeText(context, if (language == AppLanguage.EN) "Verse copied" else if (language == AppLanguage.AR) "تم نسخ الآية" else "ئایەتەکە کۆپی کرا", Toast.LENGTH_SHORT).show()
                            }
                            PremiumChoicePill(
                                modifier = Modifier.weight(1f),
                                label = if (language == AppLanguage.EN) "Save" else if (language == AppLanguage.AR) "حفظ" else "پاشەکەوت",
                                selected = false,
                                accent = Gold,
                                foreground = foreground,
                                leadingIcon = Icons.Outlined.BookmarkBorder
                            ) {
                                onBookmark(target.first, target.second)
                                Toast.makeText(context, if (language == AppLanguage.EN) "Verse saved" else if (language == AppLanguage.AR) "تم حفظ الآية" else "ئایەت پاشەکەوت کرا", Toast.LENGTH_SHORT).show()
                            }
                            PremiumChoicePill(
                                modifier = Modifier.weight(1f),
                                label = if (language == AppLanguage.EN) if (sheetAudioPlaying) "Pause" else "Play"
                                    else if (language == AppLanguage.AR) if (sheetAudioPlaying) "إيقاف" else "تشغيل"
                                    else if (sheetAudioPlaying) "وەستان" else "لێدان",
                                selected = sheetAudioPlaying,
                                accent = Gold,
                                foreground = foreground,
                                leadingIcon = if (sheetAudioPlaying) Icons.Outlined.PauseCircle else Icons.Outlined.PlayCircle
                            ) {
                                if (sheetAudioPlaying) {
                                    AudioController.stop()
                                    sheetAudioPlaying = false
                                } else {
                                    val mode = ReaderPlaybackSettings.normalize(readerPlaybackMode)
                                    val chosenId = resolveCompatibleReciterId(mode, preferredReaderReciterId, readerReciters)
                                    val chosen = readerReciters.firstOrNull { it.id == chosenId }
                                        ?: readerReciters.firstOrNull { it.audioType == 0 }
                                        ?: readerReciters.firstOrNull()
                                    val ayahCount = surahs.firstOrNull { it.number == target.first }?.ayahCount ?: 0
                                    val startAyah = if (mode == "full") 1 else target.second
                                    val sequence = if (chosen != null && ayahCount > 0) reciterAudioSequence(context, chosen, target.first, startAyah, ayahCount) else emptyList()
                                    if (sequence.isEmpty()) {
                                        Toast.makeText(context, tr(language, "no_data"), Toast.LENGTH_SHORT).show()
                                    } else {
                                        sheetAudioPlaying = true
                                        AudioController.playSequence(context, sequence) { isPlaying -> sheetAudioPlaying = isPlaying }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                AudioController.stop()
                                sheetAudioPlaying = false
                                onBrowseReciters(target.first, target.second)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.GraphicEq, null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                when (language) {
                                    AppLanguage.EN -> "Browse all reciters"
                                    AppLanguage.AR -> "عرض جميع القرّاء والتلاوات"
                                    AppLanguage.CKB -> "بینینی هەموو قورئان‌خوێنەکان"
                                },
                                fontSize = 11.sp
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                val shareText = buildList {
                                    add((surah?.arabicName ?: "") + " • " + target.first + ":" + target.second)
                                    add(sheetVerse!!.arabic)
                                    if (sheetVerse!!.translation.isNotBlank()) add(sheetVerse!!.translation)
                                    if (sheetTafsir.isNotBlank()) add(sheetTafsir)
                                }.joinToString("\n\n")
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(
                                    Intent.createChooser(
                                        sendIntent,
                                        when (language) {
                                            AppLanguage.EN -> "Share Quran verse"
                                            AppLanguage.AR -> "مشاركة الآية"
                                            AppLanguage.CKB -> "ئایەت بڵاو بکەرەوە"
                                        }
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.Share, null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                when (language) {
                                    AppLanguage.EN -> "Share verse, translation and selected tafsir"
                                    AppLanguage.AR -> "مشاركة الآية والترجمة والتفسير المختار"
                                    AppLanguage.CKB -> "بڵاوکردنەوەی ئایەت، وەرگێڕان و تەفسیر"
                                }, fontSize = 11.sp
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            item {
                                PremiumChoicePill(
                                    label = if (language == AppLanguage.EN) "Translation / tafsir" else if (language == AppLanguage.AR) "الترجمة / التفسير" else "وەرگێڕان / تەفسیر",
                                    selected = false,
                                    accent = Gold,
                                    foreground = foreground
                                ) { readerSettingsSection = "overview"; showReaderSettings = true }
                            }
                            item {
                                PremiumChoicePill(
                                    label = (if (language == AppLanguage.EN) "Related Hadith (" else if (language == AppLanguage.AR) "الأحاديث المرتبطة (" else "فەرموودەی پەیوەندیدار (") + sheetRelatedHadith.size + ")",
                                    selected = showSheetHadith,
                                    accent = Gold,
                                    foreground = foreground
                                ) { showSheetHadith = !showSheetHadith }
                            }
                            item {
                                PremiumChoicePill(
                                    label = if (language == AppLanguage.EN) "Word study" else if (language == AppLanguage.AR) "تحليل الكلمات" else "شیکردنەوەی وشە",
                                    selected = showSheetWordStudy,
                                    accent = Gold,
                                    foreground = foreground
                                ) {
                                    showSheetWordStudy = !showSheetWordStudy
                                    if (showSheetWordStudy && sheetWords.isEmpty()) {
                                        sheetWordLoading = true
                                        scope.launch {
                                            sheetWords = ContentRepository.loadWordAnalysis(context, target.first, target.second)
                                            sheetWordLoading = false
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(11.dp))
                        Card(colors = CardDefaults.cardColors(containerColor = foreground.copy(alpha = 0.035f)), shape = RoundedCornerShape(16.dp)) {
                            Column(Modifier.padding(13.dp)) {
                                Text(
                                    ContentRepository.tafsirBooks.firstOrNull { it.id == selectedTafsirBook }?.let { book ->
                                        when (language) { AppLanguage.EN -> book.labelEn; AppLanguage.AR -> book.labelAr; AppLanguage.CKB -> book.labelCkb }
                                    }.orEmpty(),
                                    color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(6.dp))
                                if (sheetTafsir.isBlank()) Text(
                                    when (language) { AppLanguage.EN -> "No tafsir text is available for this verse in the selected source."; AppLanguage.AR -> "لا يتوفر تفسير لهذه الآية في المصدر المختار."; AppLanguage.CKB -> "لە سەرچاوەی هەڵبژێردراودا تەفسیر بۆ ئەم ئایەتە نییە." },
                                    color = foreground.copy(alpha = 0.65f), fontSize = 12.sp, lineHeight = 19.sp
                                ) else Text(sheetTafsir.replace("<br>", "\n").replace("<br/>", "\n"), color = foreground, fontSize = 13.sp, lineHeight = 22.sp)
                            }
                        }
                        if (showSheetHadith) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                when (language) { AppLanguage.EN -> "RELATED HADITH"; AppLanguage.AR -> "الأحاديث المرتبطة"; AppLanguage.CKB -> "فەرموودە پەیوەندیدارەکان" },
                                color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(7.dp))
                            if (sheetRelatedHadith.isEmpty()) Text(
                                when (language) { AppLanguage.EN -> "No linked narration is available for this verse."; AppLanguage.AR -> "لا توجد رواية مرتبطة بهذه الآية."; AppLanguage.CKB -> "فەرموودەی پەیوەندیدار بۆ ئەم ئایەتە بەردەست نییە." },
                                color = foreground.copy(alpha = 0.65f), fontSize = 12.sp
                            ) else sheetRelatedHadith.forEach { narration ->
                                Column(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                                    Text(
                                        when (language) { AppLanguage.EN -> narration.collectionEnglish; AppLanguage.AR -> narration.collectionArabic; AppLanguage.CKB -> narration.collectionSorani.ifBlank { narration.collectionArabic } },
                                        color = Gold, fontWeight = FontWeight.Bold, fontSize = 11.sp
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(narration.arabic, color = foreground, fontSize = 18.sp, lineHeight = 30.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                                    val translated = when (language) { AppLanguage.EN -> narration.english; AppLanguage.AR -> ""; AppLanguage.CKB -> narration.kurdish }
                                    if (translated.isNotBlank()) {
                                        Spacer(Modifier.height(5.dp))
                                        Text(translated, color = foreground.copy(alpha = 0.82f), fontSize = 12.sp, lineHeight = 20.sp)
                                    }
                                    if (narration.reference.isNotBlank()) Text(narration.reference, color = foreground.copy(alpha = 0.5f), fontSize = 10.sp)
                                    Divider(color = Gold.copy(alpha = 0.18f), modifier = Modifier.padding(top = 8.dp))
                                }
                            }
                        }
                        if (showSheetWordStudy) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                when (language) { AppLanguage.EN -> "WORD-BY-WORD STUDY"; AppLanguage.AR -> "دراسة كلمة بكلمة"; AppLanguage.CKB -> "خوێندنەوەی وشە بە وشە" },
                                color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(7.dp))
                            when {
                                sheetWordLoading -> CircularProgressIndicator(color = Gold, modifier = Modifier.size(20.dp))
                                sheetWords.isEmpty() -> Text(tr(language, "not_found"), color = foreground.copy(alpha = 0.65f), fontSize = 12.sp)
                                else -> sheetWords.forEach { word ->
                                    Column(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                                        Text(word.parts.joinToString("  ") { it.glyphText }, color = foreground, fontSize = 21.sp, lineHeight = 33.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                                        if (word.root.isNotBlank()) Text(
                                            (if (language == AppLanguage.EN) "Root: " else if (language == AppLanguage.AR) "الجذر: " else "ڕەگ: ") + word.root,
                                            color = Gold, fontSize = 11.sp
                                        )
                                        if (word.grammar.isNotBlank()) Text(word.grammar, color = foreground.copy(alpha = 0.72f), fontSize = 11.sp, lineHeight = 18.sp)
                                    }
                                    Divider(color = Gold.copy(alpha = 0.15f))
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(28.dp))
            }
        }
    }


}

internal fun shouldRestorePageMushaf(savedReaderMode: String?, hasSavedPage: Boolean): Boolean =
    hasSavedPage && !savedReaderMode.equals("verse", ignoreCase = true)

/** In Arabic/Sorani reading order, a physical left swipe advances to the next surah. */
internal fun resolveSurahStartPage(verses: List<Ayah>, selectedSurah: Int): Int? {
    if (selectedSurah !in 1..114) return null
    return verses.firstOrNull { it.surah == selectedSurah && it.number == 1 }
        ?.page
        ?.takeIf { it in 1..604 }
}

internal fun quranSurahSwipeTarget(
    surah: Int,
    horizontalDragPx: Float,
    thresholdPx: Float = 72f
): Int? {
    if (surah !in 1..114 || !horizontalDragPx.isFinite() || !thresholdPx.isFinite() || thresholdPx <= 0f) return null
    return when {
        horizontalDragPx < -thresholdPx && surah < 114 -> surah + 1
        horizontalDragPx > thresholdPx && surah > 1 -> surah - 1
        else -> null
    }
}

internal enum class MushafEdgeTapAction {
    NEXT_PAGE,
    PREVIOUS_PAGE,
    NONE
}

/**
 * Enable edge-tap page turns only when the page is at its normal zoom and no verse
 * geometry was hit. The wide center of the page remains reserved for ayah selection.
 */
internal fun mushafEdgeTapAction(xFraction: Float, page: Int, scale: Float, verseWasHit: Boolean): MushafEdgeTapAction {
    if (verseWasHit || scale > 1.01f || !xFraction.isFinite() || xFraction !in 0f..1f) {
        return MushafEdgeTapAction.NONE
    }
    return when {
        xFraction <= 0.065f && page < 604 -> MushafEdgeTapAction.NEXT_PAGE
        xFraction >= 0.935f && page > 1 -> MushafEdgeTapAction.PREVIOUS_PAGE
        else -> MushafEdgeTapAction.NONE
    }
}

/** Keep a zoomed Mushaf page pannable without allowing it to disappear beyond the viewport. */
internal fun clampMushafPanOffset(
    currentOffset: Float,
    delta: Float,
    scaledContentExtent: Float,
    viewportExtent: Float
): Float {
    if (!currentOffset.isFinite() || !delta.isFinite() ||
        !scaledContentExtent.isFinite() || !viewportExtent.isFinite() || viewportExtent <= 0f
    ) return 0f
    val limit = ((scaledContentExtent - viewportExtent) / 2f).coerceAtLeast(0f)
    return (currentOffset + delta).coerceIn(-limit, limit)
}

/**
 * Physical left swipe advances the right-to-left Mushaf to the next page.
 * Physical right swipe returns to the previous page. Small drags and boundary
 * gestures are ignored so page selection and zooming remain usable.
 */
internal fun mushafSwipeTarget(page: Int, horizontalDragPx: Float, thresholdPx: Float = 72f): Int? {
    if (page !in 1..604 || !horizontalDragPx.isFinite() || !thresholdPx.isFinite() || thresholdPx <= 0f) return null
    return when {
        horizontalDragPx < -thresholdPx && page < 604 -> page + 1
        horizontalDragPx > thresholdPx && page > 1 -> page - 1
        else -> null
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MushafPageViewer(
    language: AppLanguage,
    foreground: Color,
    page: Int,
    onVerseView: () -> Unit,
    darkMode: Boolean,
    sepiaMode: Boolean,
    onPageChange: (Int) -> Unit,
    onExitFullscreen: () -> Unit,
    onReaderSettings: () -> Unit,
    onPlaySurah: (Int, Int) -> Unit,
    onBookmarkVerse: (Int, Int) -> Unit,
    onVerseClick: (Int, Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val motionAllowed = remember(context) { systemAllowsDecorativeMotion(context) }
    val pageReveal = remember { Animatable(1f) }
    val pageTurnOffset = remember { Animatable(0f) }
    val pageTurnRotation = remember { Animatable(0f) }
    var pageTurnPivot by remember { mutableFloatStateOf(0.5f) }
    var swipePreviewX by remember(page) { mutableFloatStateOf(0f) }
    val animatedSwipePreviewX by animateFloatAsState(
        targetValue = swipePreviewX,
        animationSpec = if (motionAllowed) spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium) else snap(),
        label = "Mushaf finger-follow page drag"
    )
    val animatedSwipeRotationY by animateFloatAsState(
        targetValue = (swipePreviewX / 160f).coerceIn(-1f, 1f) * 8.25f,
        animationSpec = if (motionAllowed) spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium) else snap(),
        label = "Mushaf finger-follow page tilt"
    )
    var lastAnimatedPage by remember { mutableIntStateOf(page) }
    var bitmap by remember(page) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(page, bitmap) {
        if (bitmap != null) {
            val previousPage = lastAnimatedPage
            lastAnimatedPage = page
            val entryDirection = when {
                page > previousPage -> 1f
                page < previousPage -> -1f
                else -> 0f
            }
            if (!motionAllowed || entryDirection == 0f) {
                pageTurnOffset.snapTo(0f)
                pageTurnRotation.snapTo(0f)
                pageReveal.snapTo(1f)
            } else {
                pageTurnPivot = if (entryDirection > 0f) 0.08f else 0.92f
                pageTurnOffset.snapTo(entryDirection * 48f)
                pageTurnRotation.snapTo(entryDirection * 12f)
                pageReveal.snapTo(0.92f)
                val slideAnimation = launch {
                    pageTurnOffset.animateTo(0f, animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium))
                }
                val rotationAnimation = launch {
                    pageTurnRotation.animateTo(0f, animationSpec = tween(260, easing = FastOutSlowInEasing))
                }
                pageReveal.animateTo(1f, animationSpec = tween(240, easing = FastOutSlowInEasing))
                slideAnimation.join()
                rotationAnimation.join()
            }
        }
    }
    var pageVerses by remember(page) { mutableStateOf<List<MushafPageVerse>>(emptyList()) }
    var selectedVerse by remember(page) { mutableStateOf<MushafPageVerse?>(null) }
    var verseRects by remember(page) { mutableStateOf<List<MushafPageRect>>(emptyList()) }
    var loading by remember(page) { mutableStateOf(true) }
    var failed by remember(page) { mutableStateOf(false) }
    var pageInput by rememberSaveable { mutableStateOf(page.toString()) }
    var scale by remember(page) { mutableFloatStateOf(1f) }
    var offsetX by remember(page) { mutableFloatStateOf(0f) }
    var offsetY by remember(page) { mutableFloatStateOf(0f) }
    var viewportSize by remember(page) { mutableStateOf(IntSize.Zero) }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val nextScale = (scale * zoomChange).coerceIn(1f, 4f)
        scale = nextScale
        if (nextScale > 1.01f && viewportSize.width > 0 && viewportSize.height > 0) {
            val fit = minOf(viewportSize.width / 1148f, viewportSize.height / 1641f)
            val scaledWidth = 1148f * fit * nextScale
            val scaledHeight = 1641f * fit * nextScale
            offsetX = clampMushafPanOffset(offsetX, panChange.x, scaledWidth, viewportSize.width.toFloat())
            offsetY = clampMushafPanOffset(offsetY, panChange.y, scaledHeight, viewportSize.height.toFloat())
        } else {
            offsetX = 0f
            offsetY = 0f
        }
    }
    LaunchedEffect(page) { pageInput = page.toString() }
    LaunchedEffect(page) {
        loading = true
        failed = false
        bitmap = null
        pageVerses = ContentRepository.loadMushafPageVerses(context, page)
        val source = ContentRepository.loadMushafPageSvg(context, page)
        if (source == null) {
            failed = true
        } else {
            bitmap = withContext(Dispatchers.Default) {
                try {
                    val svg = SVG.getFromString(source)
                    Bitmap.createBitmap(1148, 1641, Bitmap.Config.ARGB_8888).also { image ->
                        image.eraseColor(AndroidColor.WHITE)
                        svg.renderToCanvas(Canvas(image))
                    }
                } catch (error: Exception) {
                    android.util.Log.e("MashxallMushaf", "Could not render SVG page $page", error)
                    null
                }
            }
            failed = bitmap == null
        }
        loading = false
    }
    LaunchedEffect(page, selectedVerse) {
        val selected = selectedVerse
        verseRects = if (selected == null) emptyList() else {
            ContentRepository.loadMushafVerseRects(context, page, selected.surah, selected.ayah)
        }
    }
    Column(Modifier.fillMaxSize().background(if (darkMode) Color(0xFF081C18) else if (sepiaMode) Color(0xFFE4D2A9) else Color(0xFFF1EFE8))) {
        // Native, compact reader toolbar pinned above the page, like a real
        // Mushaf app rather than an ordinary scrollable content page.
        Row(
            Modifier.fillMaxWidth().background(DeepGreen).padding(horizontal = 5.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ReaderActionIcon(Icons.Outlined.ArrowBack, tr(language, "back"), onClick = onExitFullscreen)
            ReaderActionIcon(
                Icons.Outlined.AutoStories,
                when (language) {
                    AppLanguage.EN -> "Open verse-text reader and Tajweed"
                    AppLanguage.AR -> "فتح قارئ نص الآيات والتجويد"
                    AppLanguage.CKB -> "کردنەوەی خوێندنەوەی دەقی ئایەت و تەجوید"
                },
                onClick = onVerseView
            )
            Column(Modifier.weight(1f).padding(start = 3.dp)) {
                Text(tr(language, "mushaf_view"), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                AnimatedContent(
                    targetState = page,
                    transitionSpec = {
                        if (!motionAllowed) {
                            fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                        } else {
                            (fadeIn(tween(130)) + scaleIn(initialScale = 0.92f, animationSpec = tween(170)) +
                                slideInVertically(initialOffsetY = { it / 3 }, animationSpec = tween(170))) togetherWith
                                (fadeOut(tween(100)) + scaleOut(targetScale = 1.04f, animationSpec = tween(100)) +
                                    slideOutVertically(targetOffsetY = { -it / 4 }, animationSpec = tween(100)))
                        }
                    },
                    label = "Mushaf toolbar page number transition"
                ) { visiblePage ->
                    Text(tr(language, "page") + " " + visiblePage + " / 604", color = PaleGold, fontSize = 10.sp)
                }
            }
            val toolbarVerse = selectedVerse ?: pageVerses.firstOrNull()
            TextButton(
                onClick = {
                    toolbarVerse?.let { target ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onVerseClick(target.surah, target.ayah)
                    }
                },
                enabled = !loading && toolbarVerse != null,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Text(
                    toolbarVerse?.let { "${it.surah}:${it.ayah}" } ?: "—",
                    color = if (toolbarVerse != null) PaleGold else Color.Gray,
                    fontSize = 9.sp,
                    maxLines = 1
                )
            }
            ReaderActionIcon(
                Icons.Outlined.PlayCircle,
                if (language == AppLanguage.EN) "Play surah" else if (language == AppLanguage.AR) "تشغيل السورة" else "خوێندنەوەی سوورەت",
                enabled = !loading && pageVerses.isNotEmpty()
            ) {
                pageVerses.firstOrNull()?.let { onPlaySurah(it.surah, 1) }
            }
            ReaderActionIcon(
                Icons.Outlined.BookmarkBorder,
                if (language == AppLanguage.EN) "Bookmark verse" else if (language == AppLanguage.AR) "حفظ علامة للآية" else "نیشانەکردنی ئایەت",
                enabled = !loading && pageVerses.isNotEmpty(),
                hapticFeedback = false
            ) {
                (selectedVerse ?: pageVerses.firstOrNull())?.let { target ->
                    onBookmarkVerse(target.surah, target.ayah)
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    Toast.makeText(
                        context,
                        when (language) {
                            AppLanguage.EN -> "Bookmark saved for " + target.surah + ":" + target.ayah
                            AppLanguage.AR -> "حُفظت علامة للآية " + target.surah + ":" + target.ayah
                            AppLanguage.CKB -> "ئایەت نیشانە کرا " + target.surah + ":" + target.ayah
                        },
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            ReaderActionIcon(Icons.Outlined.Settings, tr(language, "settings"), onClick = onReaderSettings)
        }
        Row(
            Modifier.fillMaxWidth().background(DeepGreen.copy(alpha = 0.97f)).padding(horizontal = 7.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            ReaderActionIcon(
                Icons.Outlined.ChevronRight,
                tr(language, "previous"),
                enabled = page > 1 && !loading,
                onClick = { onPageChange(page - 1) }
            )
            OutlinedTextField(
                value = pageInput,
                onValueChange = { pageInput = it.filter(Char::isDigit).take(3) },
                modifier = Modifier.weight(1f),
                label = { Text(if (language == AppLanguage.EN) "Page" else if (language == AppLanguage.AR) "صفحة" else "پەڕە", fontSize = 10.sp) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            PremiumChoicePill(
                label = if (language == AppLanguage.EN) "Go" else if (language == AppLanguage.AR) "اذهب" else "بڕۆ",
                selected = false,
                accent = Gold,
                foreground = if (darkMode) Color.White else DeepGreen,
                leadingIcon = Icons.Outlined.Search,
                enabled = pageInput.toIntOrNull()?.let { it in 1..604 } == true && pageInput.toIntOrNull() != page
            ) {
                pageInput.toIntOrNull()?.takeIf { it in 1..604 }?.let(onPageChange)
            }
            ReaderActionIcon(
                Icons.Outlined.ChevronRight,
                tr(language, "next"),
                enabled = page < 604 && !loading,
                rotationDegrees = 180f,
                onClick = { onPageChange(page + 1) }
            )
        }
        val pageProgress by animateFloatAsState(
            targetValue = (page.toFloat() / 604f).coerceIn(0f, 1f),
            animationSpec = if (motionAllowed) tween(380, easing = FastOutSlowInEasing) else snap(),
            label = "Mushaf total page progress"
        )
        Column(Modifier.fillMaxWidth().padding(horizontal = 9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    when (language) {
                        AppLanguage.EN -> "READING PROGRESS"
                        AppLanguage.AR -> "تقدم القراءة"
                        AppLanguage.CKB -> "پێشکەوتنی خوێندنەوە"
                    },
                    color = if (darkMode) PaleGold.copy(alpha = 0.72f) else DeepGreen.copy(alpha = 0.66f),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Spacer(Modifier.weight(1f))
                Text(
                    String.format(Locale.US, "%.1f%%", pageProgress * 100f),
                    color = if (darkMode) PaleGold else DeepGreen,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(4.dp))
            Box(
                Modifier.fillMaxWidth().height(4.dp).clip(CircleShape)
                    .background(if (darkMode) Color.White.copy(alpha = 0.10f) else DeepGreen.copy(alpha = 0.10f))
            ) {
                Box(
                    Modifier.fillMaxWidth(pageProgress).fillMaxHeight().clip(CircleShape)
                        .background(Brush.horizontalGradient(listOf(Gold.copy(alpha = 0.72f), PaleGold, Gold)))
                )
            }
        }
        PremiumPageScrubber(
            currentPage = page,
            pageCount = 604,
            pageLabel = if (language == AppLanguage.EN) "Jump to page" else if (language == AppLanguage.AR) "انتقل إلى صفحة" else "بڕۆ بۆ پەڕە",
            foreground = if (darkMode) Color.White.copy(alpha = 0.82f) else DeepGreen,
            accent = Gold,
            modifier = Modifier.padding(horizontal = 9.dp)
        ) { selectedPage -> onPageChange(selectedPage) }
        Spacer(Modifier.height(7.dp))

        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    darkMode -> Color(0xFF141A18)
                    sepiaMode -> Color(0xFFF0E1BE)
                    else -> Color.White
                }
            ),
            shape = RoundedCornerShape(13.dp),
            border = BorderStroke(1.dp, Gold.copy(alpha = 0.25f))
        ) {
            Box(Modifier.fillMaxSize().padding(5.dp), contentAlignment = Alignment.Center) {
                when {
                    loading -> CircularProgressIndicator(color = Gold)
                    bitmap != null -> Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .onSizeChanged { viewportSize = it }
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offsetX
                                translationY = offsetY
                            }
                            .transformable(transformState, canPan = { scale > 1.01f })
                            .pointerInput(page, scale) {
                                var dragTotal = 0f
                                detectHorizontalDragGestures(
                                    onDragStart = {
                                        dragTotal = 0f
                                        swipePreviewX = 0f
                                    },
                                    onHorizontalDrag = { _, amount ->
                                        if (scale <= 1.01f) {
                                            dragTotal += amount
                                            swipePreviewX = (swipePreviewX + amount)
                                                .coerceIn(-size.width * 0.34f, size.width * 0.34f)
                                        }
                                    },
                                    onDragEnd = {
                                        if (scale <= 1.01f) {
                                            val nextPage = mushafSwipeTarget(page, dragTotal)
                                            if (nextPage != null) {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                onPageChange(nextPage)
                                            } else {
                                                // A short gesture returns the paper to its resting position.
                                                swipePreviewX = 0f
                                            }
                                        }
                                        dragTotal = 0f
                                    },
                                    onDragCancel = {
                                        dragTotal = 0f
                                        swipePreviewX = 0f
                                    }
                                )
                            }
                            .pointerInput(page, scale, bitmap) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        if (scale > 1.01f) {
                                            scale = 1f
                                            offsetX = 0f
                                            offsetY = 0f
                                        } else {
                                            scale = 2f
                                        }
                                    },
                                    onTap = { tap ->
                                        val fit = minOf(size.width / 1148f, size.height / 1641f)
                                        if (fit > 0f) {
                                            val left = (size.width - 1148f * fit) / 2f
                                            val top = (size.height - 1641f * fit) / 2f
                                            val x = (tap.x - left) / fit
                                            val y = (tap.y - top) / fit
                                            if (x in 0f..1148f && y in 0f..1641f) {
                                                scope.launch {
                                                    val hit = ContentRepository.findMushafVerseAtPoint(context, page, x, y)
                                                    if (hit != null) {
                                                        selectedVerse = hit
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        onVerseClick(hit.surah, hit.ayah)
                                                    } else {
                                                        val edgeAction = mushafEdgeTapAction(x / 1148f, page, scale, verseWasHit = false)
                                                        when (edgeAction) {
                                                            MushafEdgeTapAction.NEXT_PAGE -> {
                                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                onPageChange((page + 1).coerceAtMost(604))
                                                            }
                                                            MushafEdgeTapAction.PREVIOUS_PAGE -> {
                                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                onPageChange((page - 1).coerceAtLeast(1))
                                                            }
                                                            MushafEdgeTapAction.NONE -> Unit
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap!!.asImageBitmap(),
                            contentDescription = tr(language, "mushaf_view") + " " + page,
                            contentScale = ContentScale.Fit,
                            colorFilter = when {
                                darkMode -> ColorFilter.colorMatrix(MushafNightColorMatrix)
                                sepiaMode -> ColorFilter.colorMatrix(MushafSepiaColorMatrix)
                                else -> null
                            },
                            modifier = Modifier.fillMaxSize().graphicsLayer {
                                alpha = pageReveal.value
                                translationX = pageTurnOffset.value + animatedSwipePreviewX
                                rotationY = pageTurnRotation.value + animatedSwipeRotationY
                                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(pageTurnPivot, 0.5f)
                                cameraDistance = 28.dp.toPx()
                                shadowElevation = (
                                    pageTurnRotation.value * pageTurnRotation.value +
                                        animatedSwipeRotationY * animatedSwipeRotationY
                                    ) * 0.18f
                                scaleX = 0.97f + pageReveal.value * 0.03f
                                scaleY = 0.97f + pageReveal.value * 0.03f
                            }
                        )
                        if (verseRects.isNotEmpty()) {
                            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                                val fit = minOf(size.width / 1148f, size.height / 1641f)
                                val left = (size.width - 1148f * fit) / 2f
                                val top = (size.height - 1641f * fit) / 2f
                                verseRects.forEach { rect ->
                                    val rectLeft = left + rect.minX * fit
                                    val rectTop = top + rect.minY * fit
                                    val rectWidth = (rect.maxX - rect.minX) * fit
                                    val rectHeight = (rect.maxY - rect.minY) * fit
                                    if (rectWidth > 0f && rectHeight > 0f) {
                                        drawRect(
                                            color = Color(0x66F4C542),
                                            topLeft = androidx.compose.ui.geometry.Offset(rectLeft, rectTop),
                                            size = androidx.compose.ui.geometry.Size(rectWidth, rectHeight)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    failed -> Text(tr(language, "mushaf_unavailable"), color = foreground, textAlign = TextAlign.Center, modifier = Modifier.padding(18.dp))
                }
            }
        }
    }
}

@Composable
fun BookmarksScreen(
    language: AppLanguage,
    foreground: Color,
    cardColor: Color,
    onOpen: (Int, Int) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("mashxall_preferences", Context.MODE_PRIVATE) }
    fun readNotes(): Map<String, String> = runCatching {
        val json = org.json.JSONObject(prefs.getString("bookmark_notes", "{}") ?: "{}")
        buildMap {
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = json.optString(key, "").trim()
                if (value.isNotBlank()) put(key, value)
            }
        }
    }.getOrDefault(emptyMap())
    var bookmarks by remember {
        mutableStateOf(
            prefs.getStringSet("bookmarks", emptySet()).orEmpty().mapNotNull { value ->
                val pieces = value.split(":")
                val surah = pieces.getOrNull(0)?.toIntOrNull()
                val ayah = pieces.getOrNull(1)?.toIntOrNull()
                if (surah != null && ayah != null && surah in 1..114 && ayah > 0) surah to ayah else null
            }.distinct().sortedWith(compareBy<Pair<Int, Int>> { it.first }.thenBy { it.second })
        )
    }
    var notes by remember { mutableStateOf(readNotes()) }
    var query by rememberSaveable { mutableStateOf("") }
    var editorKey by remember { mutableStateOf<String?>(null) }
    var editorText by remember { mutableStateOf("") }
    val visibleBookmarks = bookmarks.filter { pair ->
        val key = "${pair.first}:${pair.second}"
        query.isBlank() ||
            pair.first.toString().contains(query.trim()) ||
            pair.second.toString().contains(query.trim()) ||
            notes[key].orEmpty().contains(query.trim(), ignoreCase = true)
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        PremiumSectionHero(
            title = tr(language, "bookmarks"),
            subtitle = when (language) {
                AppLanguage.EN -> "Return to meaningful verses and keep a personal note beside each one."
                AppLanguage.AR -> "عُد إلى آياتك المختارة ودوّن ملاحظاتك بجانبها."
                AppLanguage.CKB -> "بگەڕێوە بۆ ئایەتە هەڵبژێردراوەکانت و تێبینی تایبەتی لە تەنیشتیان بنووسە."
            },
            icon = Icons.Outlined.BookmarkBorder,
            foreground = foreground,
            accent = Gold,
            metric = when (language) {
                AppLanguage.EN -> "${bookmarks.size} saved verses"
                AppLanguage.AR -> "${bookmarks.size} آية محفوظة"
                AppLanguage.CKB -> "${bookmarks.size} ئایەتی پاشەکەوتکراو"
            },
            eyebrow = when (language) { AppLanguage.EN -> "YOUR PERSONAL COLLECTION"; AppLanguage.AR -> "مجموعتك الشخصية"; AppLanguage.CKB -> "کۆمەڵەی تایبەتی تۆ" }
        )
        Spacer(Modifier.height(10.dp))
        SearchBox(query, { query = it }, if (language == AppLanguage.EN) "Search surah, verse or note…" else if (language == AppLanguage.AR) "ابحث في السورة أو الآية أو الملاحظة…" else "لە سوورەت، ئایەت یان تێبینی بگەڕێ…")
        Spacer(Modifier.height(8.dp))
        if (visibleBookmarks.isEmpty()) {
            Text(
                if (bookmarks.isEmpty()) tr(language, "no_bookmarks") else tr(language, "not_found"),
                color = foreground.copy(alpha = 0.6f), fontSize = 13.sp
            )
        } else {
            LazyColumn(contentPadding = PaddingValues(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(visibleBookmarks, key = { _, it -> it.first.toString() + ":" + it.second.toString() }) { itemIndex, saved ->
                    val key = "${saved.first}:${saved.second}"
                    Card(
                        modifier = Modifier.fillMaxWidth()
                            .staggeredEntrance(itemKey = key, index = itemIndex),
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        shape = RoundedCornerShape(17.dp)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.BookmarkBorder, null, tint = Gold)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f).premiumClickable { onOpen(saved.first, saved.second) }) {
                                    Text(tr(language, "surah") + " " + saved.first, color = foreground, fontWeight = FontWeight.SemiBold)
                                    Text(tr(language, "ayah") + " " + saved.second, color = foreground.copy(alpha = 0.55f), fontSize = 12.sp)
                                }
                                PremiumChoicePill(
                                    label = if (notes[key].isNullOrBlank()) {
                                        if (language == AppLanguage.EN) "Note +" else if (language == AppLanguage.AR) "ملاحظة +" else "تێبینی +"
                                    } else {
                                        if (language == AppLanguage.EN) "Edit note" else if (language == AppLanguage.AR) "تعديل" else "دەستکاری"
                                    },
                                    selected = !notes[key].isNullOrBlank(),
                                    accent = Gold,
                                    foreground = foreground
                                ) {
                                    editorKey = key
                                    editorText = notes[key].orEmpty()
                                }
                                PremiumIconButton(
                                    icon = Icons.Outlined.Close,
                                    contentDescription = if (language == AppLanguage.EN) "Remove bookmark" else if (language == AppLanguage.AR) "إزالة العلامة" else "نیشانەکە بسڕەوە",
                                    accent = Color(0xFFE58A82),
                                    size = 32.dp
                                ) {
                                    val remaining = bookmarks.filterNot { it == saved }
                                    bookmarks = remaining
                                    val nextNotes = notes - key
                                    notes = nextNotes
                                    val json = org.json.JSONObject().apply { nextNotes.forEach { (k, v) -> put(k, v) } }
                                    prefs.edit()
                                        .putStringSet("bookmarks", remaining.map { it.first.toString() + ":" + it.second.toString() }.toSet())
                                        .putString("bookmark_notes", json.toString())
                                        .apply()
                                }
                            }
                            notes[key]?.takeIf { it.isNotBlank() }?.let { note ->
                                Spacer(Modifier.height(5.dp))
                                Text(note, color = foreground.copy(alpha = 0.72f), fontSize = 12.sp, lineHeight = 18.sp, maxLines = 3)
                            }
                            PremiumActionButton(
                                label = if (language == AppLanguage.EN) "Open at this verse" else if (language == AppLanguage.AR) "فتح عند هذه الآية" else "لەسەر ئەم ئایەتە بکەرەوە",
                                icon = Icons.Outlined.MenuBook,
                                accent = Gold,
                                foreground = foreground,
                                modifier = Modifier.fillMaxWidth()
                            ) { onOpen(saved.first, saved.second) }
                        }
                    }
                }
            }
        }
    }

    if (editorKey != null) AlertDialog(
        onDismissRequest = { editorKey = null },
        title = {
            Text(
                if (language == AppLanguage.EN) "Personal verse note"
                else if (language == AppLanguage.AR) "ملاحظة شخصية للآية"
                else "تێبینی تایبەتی بۆ ئایەت"
            )
        },
        text = {
            OutlinedTextField(
                value = editorText,
                onValueChange = { editorText = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                maxLines = 8,
                placeholder = {
                    Text(
                        if (language == AppLanguage.EN) "Write what you learned, a reflection, or a study reminder…"
                        else if (language == AppLanguage.AR) "اكتب فائدة أو تدبرًا أو تذكيرًا للمراجعة…"
                        else "سوود، تدببەر یان تێبینی خوێندنەوە بنووسە…"
                    )
                }
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val key = editorKey ?: return@TextButton
                val text = editorText.trim()
                val nextNotes = if (text.isBlank()) notes - key else notes + (key to text)
                notes = nextNotes
                val json = org.json.JSONObject().apply { nextNotes.forEach { (k, v) -> put(k, v) } }
                prefs.edit().putString("bookmark_notes", json.toString()).apply()
                editorKey = null
            }) { Text(if (language == AppLanguage.EN) "Save note" else if (language == AppLanguage.AR) "حفظ الملاحظة" else "تێبینی پاشەکەوت بکە") }
        },
        dismissButton = { TextButton(onClick = { editorKey = null }) { Text(tr(language, "back")) } }
    )
}

@Composable
fun TafsirScreen(language: AppLanguage, foreground: Color, cardColor: Color, surah: Int, ayah: Int, onChangeVerse: (Int, Int) -> Unit) {
    val context = LocalContext.current
    var selectedBook by rememberSaveable { mutableStateOf(if (language == AppLanguage.EN) "sahih" else if (language == AppLanguage.AR) "mukhtasar" else "roshn") }
    var text by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var surahs by remember { mutableStateOf<List<Surah>>(emptyList()) }
    var chooseSurah by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { surahs = ContentRepository.loadSurahs(context) }
    LaunchedEffect(selectedBook, surah, ayah) {
        loading = true
        text = ContentRepository.loadTafsir(context, selectedBook, surah, ayah)
        loading = false
    }
    val currentSurah = surahs.firstOrNull { it.number == surah }
    val previousVerse: Pair<Int, Int> = when {
        ayah > 1 -> surah to (ayah - 1)
        surah > 1 -> {
            val previous = surahs.firstOrNull { it.number == surah - 1 }
            previous?.let { it.number to it.ayahCount } ?: (surah to 1)
        }
        else -> surah to 1
    }
    val nextVerse: Pair<Int, Int> = when {
        currentSurah != null && ayah < currentSurah.ayahCount -> surah to (ayah + 1)
        currentSurah != null && surah < 114 -> (surah + 1) to 1
        else -> surah to ayah
    }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(16.dp)) {
            PremiumSectionHero(
                title = tr(language, "tafsir"),
                subtitle = when (language) {
                    AppLanguage.EN -> "Explore verse explanations and move through the Quran one ayah at a time."
                    AppLanguage.AR -> "تدبّر تفسير الآيات وانتقل بين آيات القرآن بسهولة."
                    AppLanguage.CKB -> "تەفسیری ئایەتەکان بخوێنەوە و بە ئاسانی لە نێوانیاندا بگوازەرەوە."
                },
                icon = Icons.Outlined.AutoStories,
                foreground = foreground,
                accent = Color(0xFFC3A6EA),
                metric = "114 " + when (language) { AppLanguage.EN -> "surahs"; AppLanguage.AR -> "سورة"; AppLanguage.CKB -> "سوورەت" },
                eyebrow = when (language) { AppLanguage.EN -> "QURAN REFLECTION"; AppLanguage.AR -> "تدبّر القرآن"; AppLanguage.CKB -> "تێڕامان لە قورئان" }
            )
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                items(ContentRepository.tafsirBooks) { book ->
                    PremiumChoicePill(
                        label = tafsirLabel(book, language),
                        selected = selectedBook == book.id,
                        accent = Gold,
                        foreground = foreground
                    ) { selectedBook = book.id }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = { onChangeVerse(previousVerse.first, previousVerse.second) },
                    enabled = surah > 1 || ayah > 1
                ) { Text("‹") }
                Column(
                    modifier = Modifier.weight(1f).premiumClickable { chooseSurah = true }.padding(horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        currentSurah?.arabicName ?: (tr(language, "surah") + " " + surah),
                        color = Gold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 1
                    )
                    Text(
                        tr(language, "ayah") + " " + ayah,
                        color = foreground, fontSize = 11.sp, textAlign = TextAlign.Center
                    )
                }
                OutlinedButton(
                    onClick = { onChangeVerse(nextVerse.first, nextVerse.second) },
                    enabled = nextVerse != (surah to ayah)
                ) { Text("›") }
            }
        }
        Card(
            Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 4.dp)
                .staggeredEntrance(itemKey = "tafsir:$selectedBook:$surah:$ayah", index = 0)
                .animateContentSize(),
            colors = CardDefaults.cardColors(containerColor = cardColor),
            shape = RoundedCornerShape(22.dp)
        ) {
            when {
                loading -> LoadingOrEmpty(true, language, foreground)
                text.isBlank() -> Column(Modifier.padding(18.dp)) {
                    Text(tr(language, "no_data"), color = foreground, fontSize = 13.sp, lineHeight = 21.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(tr(language, "data_notice"), color = foreground.copy(alpha = 0.56f), fontSize = 11.sp, lineHeight = 18.sp)
                }
                else -> Column(Modifier.fillMaxSize().padding(18.dp).verticalScroll(rememberScrollState())) {
                    Text(tafsirLabel(ContentRepository.tafsirBooks.firstOrNull { it.id == selectedBook } ?: ContentRepository.tafsirBooks.first(), language), color = Gold, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Text(text.replace("<br>", "\n").replace("<br/>", "\n").replace("&nbsp;", " "), color = foreground, fontSize = 15.sp, lineHeight = 25.sp)
                }
            }
        }
    }
    if (chooseSurah) AlertDialog(
        onDismissRequest = { chooseSurah = false },
        title = { Text(if (language == AppLanguage.EN) "Choose a surah" else if (language == AppLanguage.AR) "اختر السورة" else "سوورەت هەڵبژێرە") },
        text = {
            LazyColumn(Modifier.height(420.dp)) {
                items(surahs, key = { it.number }) { item ->
                    Row(
                        Modifier.fillMaxWidth().premiumClickable {
                            chooseSurah = false
                            onChangeVerse(item.number, 1)
                        }.padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(30.dp).clip(CircleShape).background(DeepGreen), contentAlignment = Alignment.Center) {
                            Text(item.number.toString(), color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.arabicName, color = foreground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                item.englishName + " • " + item.ayahCount + " " + tr(language, "verse"),
                                color = foreground.copy(alpha = 0.55f), fontSize = 10.sp
                            )
                        }
                        if (item.number == surah) Text("✓", color = Gold, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { chooseSurah = false }) { Text(tr(language, "back")) } }
    )

}
private fun tafsirLabel(book: TafsirBook, language: AppLanguage): String = when (language) {
    AppLanguage.CKB -> book.labelCkb
    AppLanguage.AR -> book.labelAr
    AppLanguage.EN -> book.labelEn
}


private fun cleanHadithDisplayText(value: String): String =
    Html.fromHtml(value.replace("<rr>", "").replace("</rr>", ""), Html.FROM_HTML_MODE_LEGACY)
        .toString().replace('\u00a0', ' ').trim()

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun HadithScreen(language: AppLanguage, foreground: Color, cardColor: Color) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var query by rememberSaveable { mutableStateOf("") }
    val favoritePrefs = remember { context.getSharedPreferences("mashxall_hadith_favorites", Context.MODE_PRIVATE) }
    var favoriteHadithIds by remember {
        mutableStateOf(favoritePrefs.getStringSet("hadith_ids", emptySet()).orEmpty().mapNotNull { it.toIntOrNull() }.filter { it > 0 }.toSet())
    }
    var savedOnly by rememberSaveable { mutableStateOf(false) }
    var selectedMode by rememberSaveable { mutableIntStateOf(0) }
    var articles by remember { mutableStateOf<List<HadithArticle>>(emptyList()) }
    var collections by remember { mutableStateOf<List<HadithCollection>>(emptyList()) }
    var selectedCollectionId by rememberSaveable { mutableIntStateOf(0) }
    var hadithItems by remember { mutableStateOf<List<HadithItem>>(emptyList()) }
    var selectedArticleForDetails by remember { mutableStateOf<HadithArticle?>(null) }
    var selectedHadithForDetails by remember { mutableStateOf<HadithItem?>(null) }
    var totalMatches by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var offset by remember { mutableIntStateOf(0) }
    var hasMore by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        collections = ContentRepository.loadHadithCollections(context)
    }
    LaunchedEffect(query, selectedCollectionId, selectedMode, savedOnly, favoriteHadithIds.takeIf { savedOnly }) {
        loading = true
        loadingMore = false
        offset = 0
        hasMore = true
        if (selectedMode == 1) {
            articles = ContentRepository.loadHadithArticles(context, query.trim())
            hadithItems = emptyList()
            totalMatches = articles.size
            offset = articles.size
            hasMore = false
        } else {
            val bookFilter = selectedCollectionId.takeIf { it > 0 }
            val idFilter = if (savedOnly) favoriteHadithIds else null
            totalMatches = ContentRepository.loadHadithCount(context, query.trim(), bookFilter, idFilter)
            hadithItems = ContentRepository.loadHadiths(
                context, query.trim(), offset = 0, limit = 100, bookId = bookFilter, hadithIds = idFilter
            )
            articles = emptyList()
            offset = hadithItems.size
            hasMore = hadithItems.size == 100 && offset < totalMatches
        }
        loading = false
        listState.scrollToItem(0)
    }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            PremiumSectionHero(
                title = tr(language, "hadith"),
                subtitle = when (language) {
                    AppLanguage.EN -> "Explore trusted narrations, collections and study articles."
                    AppLanguage.AR -> "تصفّح الروايات والمجموعات والمقالات العلمية."
                    AppLanguage.CKB -> "فەرموودە، کۆمەڵەکان و وتارەکانی خوێندنەوە بپشکنە."
                },
                icon = Icons.Outlined.AutoStories,
                foreground = foreground,
                accent = Gold,
                metric = when (language) {
                    AppLanguage.EN -> "$totalMatches results • $offset loaded"
                    AppLanguage.AR -> "$totalMatches نتيجة • تم تحميل $offset"
                    AppLanguage.CKB -> "$totalMatches ئەنجام • $offset بارکراوە"
                },
                eyebrow = when (language) { AppLanguage.EN -> "KNOWLEDGE & NARRATION"; AppLanguage.AR -> "العلم والرواية"; AppLanguage.CKB -> "زانست و فەرموودە" }
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                PremiumChoicePill(
                    label = if (language == AppLanguage.EN) "Narrations" else if (language == AppLanguage.AR) "الأحاديث" else "فەرموودە",
                    selected = selectedMode == 0,
                    accent = Gold,
                    foreground = foreground
                ) { selectedMode = 0 }
                PremiumChoicePill(
                    label = if (language == AppLanguage.EN) "Articles (11)" else if (language == AppLanguage.AR) "المقالات (11)" else "وتارەکان (11)",
                    selected = selectedMode == 1,
                    accent = Gold,
                    foreground = foreground
                ) { selectedMode = 1 }
            }
            Spacer(Modifier.height(8.dp))
            SearchBox(query, { query = it }, tr(language, "search_hadith"))
            if (selectedMode == 0) LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                item(key = "saved-hadith-filter") {
                    PremiumChoicePill(
                        label = when (language) {
                            AppLanguage.EN -> "Saved (${favoriteHadithIds.size})"
                            AppLanguage.AR -> "المحفوظات (${favoriteHadithIds.size})"
                            AppLanguage.CKB -> "پاشەکەوتکراوەکان (${favoriteHadithIds.size})"
                        },
                        selected = savedOnly,
                        accent = Gold,
                        foreground = foreground
                    ) { savedOnly = !savedOnly }
                }
                item(key = "all-hadith-collections") {
                    PremiumChoicePill(
                        label = when (language) {
                            AppLanguage.EN -> "All books"
                            AppLanguage.AR -> "كل الكتب"
                            AppLanguage.CKB -> "هەموو کتێبەکان"
                        },
                        selected = selectedCollectionId == 0,
                        accent = Gold,
                        foreground = foreground
                    ) { selectedCollectionId = 0 }
                }
                items(collections, key = { it.id }) { collection ->
                    val label = when (language) {
                        AppLanguage.EN -> collection.nameEnglish.ifBlank { collection.name }
                        AppLanguage.AR -> collection.nameArabic.ifBlank { collection.name }
                        AppLanguage.CKB -> collection.name.ifBlank { collection.nameArabic }
                    }
                    PremiumChoicePill(
                        label = "$label · ${collection.hadithCount}",
                        selected = selectedCollectionId == collection.id,
                        accent = Gold,
                        foreground = foreground
                    ) { selectedCollectionId = collection.id }
                }
            }
        }
        if (selectedMode == 1) {
            if (articles.isEmpty()) LoadingOrEmpty(loading, language, foreground, tr(language, "not_found"))
            else LazyColumn(
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(articles, key = { _, it -> "hadith-article-" + it.id }) { itemIndex, article ->
                    val localizedAuthor = when (language) {
                        AppLanguage.CKB -> article.authorCkb.ifBlank { article.authorArabic }
                        AppLanguage.AR -> article.authorArabic.ifBlank { article.authorCkb }
                        AppLanguage.EN -> article.authorEnglish.ifBlank { article.authorCkb }
                    }
                    val localizedTitle = when (language) {
                        AppLanguage.CKB -> article.titleCkb.ifBlank { article.titleArabic }
                        AppLanguage.AR -> article.titleArabic.ifBlank { article.titleCkb }
                        AppLanguage.EN -> article.titleEnglish.ifBlank { article.titleCkb }
                    }
                    val localizedBody = when (language) {
                        AppLanguage.CKB -> article.bodyCkb.ifBlank { article.bodyArabic }
                        AppLanguage.AR -> article.bodyArabic.ifBlank { article.bodyCkb }
                        AppLanguage.EN -> article.bodyEnglish.ifBlank { article.bodyCkb }
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth()
                                    .staggeredEntrance(itemKey = article.id, index = itemIndex)
                                    .premiumClickable { selectedArticleForDetails = article },
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(0.8.dp, Gold.copy(alpha = 0.22f))
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(DeepGreen), contentAlignment = Alignment.Center) {
                                    Text(article.id.toString(), color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(localizedTitle, color = Gold, fontSize = 15.sp, fontWeight = FontWeight.Bold, lineHeight = 22.sp, modifier = Modifier.weight(1f))
                                Icon(Icons.Outlined.ChevronRight, null, tint = Gold)
                            }
                            if (localizedAuthor.isNotBlank()) {
                                Spacer(Modifier.height(5.dp))
                                Text(
                                    when (language) { AppLanguage.EN -> "Author: " + localizedAuthor; AppLanguage.AR -> "المؤلف: " + localizedAuthor; AppLanguage.CKB -> "نووسەر: " + localizedAuthor },
                                    color = foreground.copy(alpha = 0.55f), fontSize = 10.sp
                                )
                            }
                            Spacer(Modifier.height(9.dp))
                            Text(
                                localizedBody.replace("<rr>", "").replace("</rr>", "").take(190).let { if (localizedBody.length > 190) it.trimEnd() + "…" else it },
                                color = foreground.copy(alpha = 0.78f), fontSize = 12.sp, lineHeight = 20.sp, maxLines = 4
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                when (language) { AppLanguage.EN -> "Read article & translations"; AppLanguage.AR -> "قراءة المقال والترجمات"; AppLanguage.CKB -> "وتار و وەرگێڕانەکان بخوێنەوە" },
                                color = Gold, fontWeight = FontWeight.SemiBold, fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        } else if (hadithItems.isEmpty()) LoadingOrEmpty(loading, language, foreground) else LazyColumn(
            state = listState,
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(hadithItems, key = { _, item -> item.id }) { index, hadith ->
                var showEnglish by rememberSaveable(hadith.id) { mutableStateOf(language == AppLanguage.EN) }
                var showNotes by rememberSaveable(hadith.id) { mutableStateOf(false) }
                val collection = when (language) {
                    AppLanguage.EN -> hadith.bookNameEn.ifBlank { hadith.bookName }
                    AppLanguage.AR -> hadith.bookNameAr.ifBlank { hadith.bookName }
                    AppLanguage.CKB -> hadith.bookName
                }
                val kitab = when (language) {
                    AppLanguage.EN -> hadith.kitabNameEn.ifBlank { hadith.kitabName }
                    AppLanguage.AR -> hadith.kitabNameAr.ifBlank { hadith.kitabName }
                    AppLanguage.CKB -> hadith.kitabName
                }
                val chapter = when (language) {
                    AppLanguage.EN -> hadith.chapterNameEn.ifBlank { hadith.chapterName }
                    AppLanguage.AR -> hadith.chapterNameAr.ifBlank { hadith.chapterName }
                    AppLanguage.CKB -> hadith.chapterName
                }
                val numberInBook = hadith.sortInBook.ifBlank { hadith.sort.toString() }
                val hadithIsFavorite = favoriteHadithIds.contains(hadith.id)
                val hadithFavoriteFill by animateColorAsState(
                    targetValue = if (hadithIsFavorite) Gold.copy(alpha = 0.18f) else foreground.copy(alpha = 0.035f),
                    animationSpec = tween(220),
                    label = "Hadith favorite background"
                )
                val hadithFavoriteOutline by animateColorAsState(
                    targetValue = if (hadithIsFavorite) Gold.copy(alpha = 0.48f) else foreground.copy(alpha = 0.12f),
                    animationSpec = tween(220),
                    label = "Hadith favorite outline"
                )
                val translatedText = when (language) {
                    AppLanguage.CKB -> hadith.sorani
                    AppLanguage.AR -> hadith.arabic
                    AppLanguage.EN -> hadith.english
                }
                Card(
                    modifier = Modifier.fillMaxWidth()
                        .staggeredEntrance(itemKey = hadith.id, index = index)
                        .premiumClickable { selectedHadithForDetails = hadith },
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    shape = RoundedCornerShape(19.dp),
                    border = BorderStroke(0.7.dp, Gold.copy(alpha = 0.18f))
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(chapter.ifBlank { kitab.ifBlank { collection.ifBlank { if (language == AppLanguage.EN) "Hadith topic" else if (language == AppLanguage.AR) "موضوع الحديث" else "بابەتی فەرموودە" } } },
                                    color = Gold, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 20.sp)
                                if (kitab.isNotBlank()) Text(kitab, color = foreground.copy(alpha = 0.72f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    collection + " • " + (if (language == AppLanguage.EN) "No. " else if (language == AppLanguage.AR) "رقم " else "ژمارە ") + numberInBook,
                                    color = foreground.copy(alpha = 0.5f), fontSize = 10.sp
                                )
                            }
                            PremiumIconButton(
                                icon = Icons.Outlined.BookmarkBorder,
                                contentDescription = if (language == AppLanguage.EN) "Save narration" else if (language == AppLanguage.AR) "حفظ الحديث" else "فەرموودە پاشەکەوت بکە",
                                accent = Gold,
                                size = 38.dp,
                                selected = hadithIsFavorite,
                                hapticFeedback = true
                            ) {
                                val nextSaved = if (hadithIsFavorite) favoriteHadithIds - hadith.id else favoriteHadithIds + hadith.id
                                favoriteHadithIds = nextSaved
                                favoritePrefs.edit().putStringSet("hadith_ids", nextSaved.map { it.toString() }.toSet()).apply()
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            cleanHadithDisplayText(hadith.arabic).take(155).let { if (cleanHadithDisplayText(hadith.arabic).length > 155) it.trimEnd() + "…" else it },
                            color = foreground, fontSize = 17.sp, lineHeight = 28.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth()
                        )
                        if (translatedText.isNotBlank() && translatedText != hadith.arabic) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                cleanHadithDisplayText(translatedText).take(115).let { if (cleanHadithDisplayText(translatedText).length > 115) it.trimEnd() + "…" else it },
                                color = foreground.copy(alpha = 0.75f), fontSize = 12.sp, lineHeight = 19.sp, maxLines = 3
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                when (language) { AppLanguage.EN -> "View full narration"; AppLanguage.AR -> "عرض الحديث كاملًا"; AppLanguage.CKB -> "فەرموودەکە بە تەواوی ببینە" },
                                color = Gold, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Outlined.ChevronRight, null, tint = Gold)
                        }
                    }
                }
            }
            if (hasMore) {
                item(key = "load-more-hadith") {
                    PremiumActionButton(
                        label = when (language) {
                            AppLanguage.EN -> if (loadingMore) "Loading narrations…" else "Load more narrations"
                            AppLanguage.AR -> if (loadingMore) "جارٍ تحميل الروايات…" else "تحميل المزيد من الروايات"
                            AppLanguage.CKB -> if (loadingMore) "فەرموودەکان بار دەکرێن…" else "فەرموودەی زیاتر باربکە"
                        },
                        icon = Icons.Outlined.AutoStories,
                        accent = Gold,
                        foreground = foreground,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loadingMore,
                        loading = loadingMore
                    ) {
                        if (!loadingMore) scope.launch {
                            loadingMore = true
                            try {
                                val nextPage = ContentRepository.loadHadiths(
                                    context,
                                    query.trim(),
                                    offset,
                                    100,
                                    selectedCollectionId.takeIf { it > 0 },
                                    if (savedOnly) favoriteHadithIds else null
                                )
                                hadithItems = hadithItems + nextPage
                                offset += nextPage.size
                                hasMore = nextPage.size == 100 && offset < totalMatches
                            } finally {
                                loadingMore = false
                            }
                        }
                    }
                }
            }
        }
    }

    selectedArticleForDetails?.let { article ->
        val localizedTitle = when (language) {
            AppLanguage.CKB -> article.titleCkb.ifBlank { article.titleArabic }
            AppLanguage.AR -> article.titleArabic.ifBlank { article.titleCkb }
            AppLanguage.EN -> article.titleEnglish.ifBlank { article.titleCkb }
        }
        val localizedAuthor = when (language) {
            AppLanguage.CKB -> article.authorCkb.ifBlank { article.authorArabic }
            AppLanguage.AR -> article.authorArabic.ifBlank { article.authorCkb }
            AppLanguage.EN -> article.authorEnglish.ifBlank { article.authorCkb }
        }
        val localizedBody = when (language) {
            AppLanguage.CKB -> article.bodyCkb.ifBlank { article.bodyArabic }
            AppLanguage.AR -> article.bodyArabic.ifBlank { article.bodyCkb }
            AppLanguage.EN -> article.bodyEnglish.ifBlank { article.bodyCkb }
        }
        ModalBottomSheet(
            onDismissRequest = { selectedArticleForDetails = null },
            containerColor = cardColor,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(Modifier.fillMaxWidth().heightIn(max = 690.dp).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text(localizedTitle, color = Gold, fontSize = 21.sp, fontWeight = FontWeight.Bold, lineHeight = 29.sp)
                if (localizedAuthor.isNotBlank()) {
                    Spacer(Modifier.height(7.dp))
                    Text(
                        when (language) { AppLanguage.EN -> "Author: " + localizedAuthor; AppLanguage.AR -> "المؤلف: " + localizedAuthor; AppLanguage.CKB -> "نووسەر: " + localizedAuthor },
                        color = foreground.copy(alpha = 0.6f), fontSize = 11.sp
                    )
                }
                Spacer(Modifier.height(15.dp))
                Text(cleanHadithDisplayText(localizedBody), color = foreground, fontSize = 15.sp, lineHeight = 26.sp)
                if (language != AppLanguage.CKB && article.bodyCkb.isNotBlank()) {
                    Spacer(Modifier.height(16.dp))
                    Text(if (language == AppLanguage.EN) "SORANI" else "السورانية", color = Gold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Spacer(Modifier.height(5.dp))
                    Text(cleanHadithDisplayText(article.bodyCkb), color = foreground.copy(alpha = 0.82f), fontSize = 13.sp, lineHeight = 22.sp)
                }
                if (language != AppLanguage.EN && article.bodyEnglish.isNotBlank()) {
                    Spacer(Modifier.height(16.dp))
                    Text("ENGLISH", color = Gold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Spacer(Modifier.height(5.dp))
                    Text(cleanHadithDisplayText(article.bodyEnglish), color = foreground.copy(alpha = 0.78f), fontSize = 13.sp, lineHeight = 22.sp)
                }
                if (language != AppLanguage.AR && article.bodyArabic.isNotBlank()) {
                    Spacer(Modifier.height(16.dp))
                    Text(if (language == AppLanguage.EN) "ARABIC SOURCE" else "النص العربي", color = Gold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Spacer(Modifier.height(5.dp))
                    Text(cleanHadithDisplayText(article.bodyArabic), color = foreground, fontSize = 18.sp, lineHeight = 31.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                }
                if (article.authorDetail.isNotBlank()) {
                    Spacer(Modifier.height(14.dp))
                    Text(if (language == AppLanguage.EN) "AUTHOR DETAILS" else if (language == AppLanguage.AR) "معلومات المؤلف" else "زانیاری نووسەر", color = Gold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Spacer(Modifier.height(5.dp))
                    Text(cleanHadithDisplayText(article.authorDetail), color = foreground.copy(alpha = 0.72f), fontSize = 12.sp, lineHeight = 20.sp)
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Mashxall Hadith article", localizedTitle + "\n\n" + cleanHadithDisplayText(localizedBody)))
                        Toast.makeText(context, if (language == AppLanguage.EN) "Article copied" else if (language == AppLanguage.AR) "تم نسخ المقالة" else "وتارەکە کۆپی کرا", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.ContentCopy, null)
                    Spacer(Modifier.width(7.dp))
                    Text(if (language == AppLanguage.EN) "Copy article" else if (language == AppLanguage.AR) "نسخ المقالة" else "وتار کۆپی بکە")
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    selectedHadithForDetails?.let { hadith ->
        val collection = when (language) {
            AppLanguage.EN -> hadith.bookNameEn.ifBlank { hadith.bookName }
            AppLanguage.AR -> hadith.bookNameAr.ifBlank { hadith.bookName }
            AppLanguage.CKB -> hadith.bookName
        }
        val kitab = when (language) {
            AppLanguage.EN -> hadith.kitabNameEn.ifBlank { hadith.kitabName }
            AppLanguage.AR -> hadith.kitabNameAr.ifBlank { hadith.kitabName }
            AppLanguage.CKB -> hadith.kitabName
        }
        val chapter = when (language) {
            AppLanguage.EN -> hadith.chapterNameEn.ifBlank { hadith.chapterName }
            AppLanguage.AR -> hadith.chapterNameAr.ifBlank { hadith.chapterName }
            AppLanguage.CKB -> hadith.chapterName
        }
        val numberInBook = hadith.sortInBook.ifBlank { hadith.sort.toString() }
        ModalBottomSheet(
            onDismissRequest = { selectedHadithForDetails = null },
            containerColor = cardColor,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(Modifier.fillMaxWidth().heightIn(max = 690.dp).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text(chapter.ifBlank { kitab.ifBlank { collection } }, color = Gold, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp)
                Spacer(Modifier.height(5.dp))
                Text(collection + " • " + (if (language == AppLanguage.EN) "No. " else if (language == AppLanguage.AR) "رقم " else "ژمارە ") + numberInBook, color = foreground.copy(alpha = 0.6f), fontSize = 11.sp)
                Spacer(Modifier.height(15.dp))
                Text(cleanHadithDisplayText(hadith.arabic), color = foreground, fontSize = 22.sp, lineHeight = 38.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                if (hadith.sorani.isNotBlank()) {
                    Spacer(Modifier.height(16.dp))
                    Text(if (language == AppLanguage.EN) "SORANI TRANSLATION" else if (language == AppLanguage.AR) "الترجمة السورانية" else "وەرگێڕانی سۆرانی", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(cleanHadithDisplayText(hadith.sorani), color = foreground.copy(alpha = 0.85f), fontSize = 14.sp, lineHeight = 24.sp)
                }
                if (hadith.english.isNotBlank()) {
                    Spacer(Modifier.height(16.dp))
                    Text(if (language == AppLanguage.EN) "ENGLISH TRANSLATION" else if (language == AppLanguage.AR) "الترجمة الإنجليزية" else "وەرگێڕانی ئینگلیزی", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(cleanHadithDisplayText(hadith.english), color = foreground.copy(alpha = 0.8f), fontSize = 13.sp, lineHeight = 22.sp)
                }
                if (hadith.note.isNotBlank()) {
                    Spacer(Modifier.height(14.dp))
                    Text(if (language == AppLanguage.EN) "NOTE" else if (language == AppLanguage.AR) "الملاحظة" else "تێبینی", color = Gold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Spacer(Modifier.height(5.dp))
                    Text(cleanHadithDisplayText(hadith.note), color = foreground.copy(alpha = 0.78f), fontSize = 12.sp, lineHeight = 21.sp)
                }
                if (hadith.footnote.isNotBlank()) {
                    Spacer(Modifier.height(14.dp))
                    Text(if (language == AppLanguage.EN) "FOOTNOTE" else if (language == AppLanguage.AR) "الحاشية" else "پەراوێز", color = Gold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Spacer(Modifier.height(5.dp))
                    Text(cleanHadithDisplayText(hadith.footnote), color = foreground.copy(alpha = 0.68f), fontSize = 11.sp, lineHeight = 19.sp)
                }
                Spacer(Modifier.height(15.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    val hadithSaved = favoriteHadithIds.contains(hadith.id)
                    PremiumChoicePill(
                        modifier = Modifier.weight(1f),
                        label = if (language == AppLanguage.EN) if (hadithSaved) "Saved" else "Save"
                            else if (language == AppLanguage.AR) if (hadithSaved) "محفوظ" else "حفظ"
                            else if (hadithSaved) "پاشەکەوتکراو" else "پاشەکەوت",
                        selected = hadithSaved,
                        accent = Gold,
                        foreground = foreground,
                        leadingIcon = Icons.Outlined.BookmarkBorder
                    ) {
                        val nextSaved = if (hadithSaved) favoriteHadithIds - hadith.id else favoriteHadithIds + hadith.id
                        favoriteHadithIds = nextSaved
                        favoritePrefs.edit().putStringSet("hadith_ids", nextSaved.map { it.toString() }.toSet()).apply()
                    }
                    PremiumChoicePill(
                        modifier = Modifier.weight(1f),
                        label = if (language == AppLanguage.EN) "Copy" else if (language == AppLanguage.AR) "نسخ" else "کۆپی",
                        selected = false,
                        accent = Gold,
                        foreground = foreground,
                        leadingIcon = Icons.Outlined.ContentCopy
                    ) {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Mashxall Hadith", collection + "\n" + chapter + "\n\n" + cleanHadithDisplayText(hadith.arabic) + "\n\n" + cleanHadithDisplayText(hadith.sorani) + "\n\n" + cleanHadithDisplayText(hadith.english) + "\n" + numberInBook))
                        Toast.makeText(context, if (language == AppLanguage.EN) "Hadith copied" else if (language == AppLanguage.AR) "تم نسخ الحديث" else "فەرموودەکە کۆپی کرا", Toast.LENGTH_SHORT).show()
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

}

internal fun nextAzkarRepetition(currentCount: Int, targetCount: Int): Int =
    if (targetCount <= 0) currentCount.coerceAtLeast(0)
    else (currentCount.coerceAtLeast(0) + 1).coerceAtMost(targetCount)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AzkarScreen(language: AppLanguage, foreground: Color, cardColor: Color) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val progressPrefs = remember { context.getSharedPreferences("mashxall_azkar_progress", Context.MODE_PRIVATE) }
    val favoritePrefs = remember { context.getSharedPreferences("mashxall_azkar_favorites", Context.MODE_PRIVATE) }
    var query by rememberSaveable { mutableStateOf("") }
    var allItems by remember { mutableStateOf(emptyList<AzkarItem>()) }
    var categories by remember { mutableStateOf(emptyList<AzkarCategory>()) }
    var loading by remember { mutableStateOf(true) }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var selectedCategory by rememberSaveable { mutableIntStateOf(0) }
    var selectedItem by remember { mutableStateOf<AzkarItem?>(null) }
    var favoriteIds by remember {
        mutableStateOf(favoritePrefs.getStringSet("favorite_ids", emptySet()).orEmpty().toSet())
    }

    LaunchedEffect(Unit) { categories = ContentRepository.loadAzkarCategories(context) }
    LaunchedEffect(query, language) {
        loading = true
        allItems = ContentRepository.loadAzkar(context, language.key, query.trim())
        loading = false
    }
    val tabItems = allItems.filter { item ->
        when (selectedTab) {
            1 -> item.titleCkb.contains("بەیانی", true) ||
                item.titleAr.contains("الصباح", true) ||
                item.titleEn.contains("morning", true) ||
                item.categoryCkb.contains("بەیانی", true) ||
                item.categoryArabic.contains("الصباح", true) ||
                item.categoryEnglish.contains("morning", true)
            2 -> item.titleCkb.contains("ئێوار", true) ||
                item.titleAr.contains("المساء", true) ||
                item.titleEn.contains("evening", true) ||
                item.categoryCkb.contains("ئێوار", true) ||
                item.categoryArabic.contains("المساء", true) ||
                item.categoryEnglish.contains("evening", true)
            3 -> favoriteIds.contains(item.id.toString())
            else -> true
        }
    }
    val visibleItems = tabItems.filter { selectedCategory == 0 || it.categoryId == selectedCategory }
    val showCategoryLanding = selectedCategory == 0 && query.isBlank() && selectedTab == 0
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            if (selectedCategory != 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PremiumIconButton(
                        icon = Icons.Outlined.ChevronRight,
                        contentDescription = if (language == AppLanguage.EN) "All remembrance topics" else if (language == AppLanguage.AR) "كل أقسام الأذكار" else "هەموو بابەتەکانی زیکر",
                        accent = Gold,
                        size = 36.dp,
                        rotationDegrees = 180f
                    ) {
                        selectedCategory = 0
                        selectedItem = null
                    }
                    Text(
                        when (language) { AppLanguage.EN -> "All remembrance topics"; AppLanguage.AR -> "كل أقسام الأذكار"; AppLanguage.CKB -> "هەموو بابەتەکانی زیکر" },
                        color = foreground.copy(alpha = 0.65f), fontSize = 11.sp
                    )
                }
                Spacer(Modifier.height(4.dp))
            }
            PremiumSectionHero(
                title = if (selectedCategory == 0) tr(language, "azkar") else categories.firstOrNull { it.id == selectedCategory }?.let {
                    when (language) { AppLanguage.CKB -> it.ckb; AppLanguage.AR -> it.arabic; AppLanguage.EN -> it.english }
                } ?: tr(language, "azkar"),
                subtitle = when (language) {
                    AppLanguage.EN -> if (selectedCategory == 0) "Choose a collection and build a daily rhythm of remembrance." else "Read, repeat and keep your selected remembrance."
                    AppLanguage.AR -> if (selectedCategory == 0) "اختر مجموعة واجعل الذكر جزءًا من يومك." else "اقرأ الذكر وكرّره واحتفظ بالمفضّل."
                    AppLanguage.CKB -> if (selectedCategory == 0) "کۆمەڵەیەک هەڵبژێرە و زیکر بکە بە بەشێک لە ڕۆژەکەت." else "زیکرەکە بخوێنەوە و دووبارەی بکەرەوە."
                },
                icon = Icons.Outlined.Spa,
                foreground = foreground,
                accent = Color(0xFF7AC6A7),
                metric = when (language) { AppLanguage.EN -> "${visibleItems.size} entries"; AppLanguage.AR -> "${visibleItems.size} ذكرًا"; AppLanguage.CKB -> "${visibleItems.size} زیکر" },
                eyebrow = when (language) { AppLanguage.EN -> "DAILY REMEMBRANCE"; AppLanguage.AR -> "الأذكار اليومية"; AppLanguage.CKB -> "زیکری ڕۆژانە" }
            )
            Spacer(Modifier.height(10.dp))
            SearchBox(query, { query = it; if (it.isNotBlank()) selectedCategory = 0 }, tr(language, "search_azkar"))
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                items(listOf(0, 1, 2, 3)) { index ->
                    PremiumChoicePill(
                        label = when (language) {
                            AppLanguage.CKB -> listOf("بەشەکان", "بەیانی", "ئێواران", "دڵخوازەکان")[index]
                            AppLanguage.AR -> listOf("الأقسام", "الصباح", "المساء", "المفضلة")[index]
                            AppLanguage.EN -> listOf("Categories", "Morning", "Evening", "Favourites")[index]
                        },
                        selected = selectedTab == index,
                        accent = Gold,
                        foreground = foreground
                    ) {
                        selectedTab = index
                        selectedCategory = 0
                    }
                }
            }
        }

        if (loading) {
            LoadingOrEmpty(true, language, foreground)
        } else if (showCategoryLanding) {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                itemsIndexed(categories, key = { _, item -> item.id }) { index, category ->
                    val label = when (language) {
                        AppLanguage.CKB -> category.ckb.ifBlank { category.arabic }
                        AppLanguage.AR -> category.arabic.ifBlank { category.ckb }
                        AppLanguage.EN -> category.english.ifBlank { category.ckb }
                    }
                    val count = allItems.count { it.categoryId == category.id }
                    Card(
                        modifier = Modifier.fillMaxWidth()
                            .staggeredEntrance(itemKey = category.id, index = index)
                            .premiumClickable { selectedCategory = category.id },
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(0.7.dp, Gold.copy(alpha = 0.18f))
                    ) {
                        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(Gold.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.Spa, null, tint = Gold, modifier = Modifier.size(25.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(label, color = foreground, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    when (language) {
                                        AppLanguage.EN -> "$count entries • tap to open"
                                        AppLanguage.AR -> "$count ذكرًا • اضغط للفتح"
                                        AppLanguage.CKB -> "$count زیکر • بۆ کردنەوە دەست لێبدە"
                                    }, color = foreground.copy(alpha = 0.57f), fontSize = 10.sp
                                )
                            }
                            Icon(Icons.Outlined.ChevronRight, null, tint = Gold)
                        }
                    }
                }
                if (categories.isEmpty()) {
                    item { LoadingOrEmpty(false, language, foreground, tr(language, "not_found")) }
                }
            }
        } else if (visibleItems.isEmpty()) {
            LoadingOrEmpty(false, language, foreground, tr(language, "not_found"))
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                items(visibleItems, key = { it.id }) { item ->
                    val title = when (language) {
                        AppLanguage.CKB -> item.titleCkb.ifBlank { item.titleAr }
                        AppLanguage.AR -> item.titleAr.ifBlank { item.titleCkb }
                        AppLanguage.EN -> item.titleEn.ifBlank { item.titleCkb }
                    }
                    val categoryLabel = when (language) {
                        AppLanguage.CKB -> item.categoryCkb
                        AppLanguage.AR -> item.categoryArabic
                        AppLanguage.EN -> item.categoryEnglish
                    }
                    val count = item.count.coerceAtLeast(1)
                    val completed = progressPrefs.getInt(item.id.toString(), 0)
                    val repetitionProgress by animateFloatAsState(
                        targetValue = (completed.toFloat() / count.toFloat()).coerceIn(0f, 1f),
                        animationSpec = tween(420, easing = FastOutSlowInEasing),
                        label = "Azkar repetition progress"
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth().premiumClickable { selectedItem = item },
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        shape = RoundedCornerShape(19.dp),
                        border = BorderStroke(0.7.dp, Gold.copy(alpha = 0.18f))
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    if (categoryLabel.isNotBlank()) Text(categoryLabel, color = foreground.copy(alpha = 0.5f), fontSize = 10.sp)
                                    Text(title, color = Gold, fontSize = 14.sp, fontWeight = FontWeight.Bold, lineHeight = 21.sp)
                                }
                                val isFavorite = favoriteIds.contains(item.id.toString())
                                Box(
                                    Modifier.size(36.dp).clip(CircleShape)
                                        .background(Gold.copy(alpha = if (isFavorite) 0.18f else 0.055f))
                                        .border(1.dp, Gold.copy(alpha = if (isFavorite) 0.48f else 0.18f), CircleShape)
                                        .premiumClickable(hapticFeedback = true) {
                                            val id = item.id.toString()
                                            favoriteIds = if (isFavorite) favoriteIds - id else favoriteIds + id
                                            favoritePrefs.edit().putStringSet("favorite_ids", favoriteIds).apply()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    AnimatedContent(
                                        targetState = isFavorite,
                                        transitionSpec = {
                                            (scaleIn(initialScale = 0.45f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)) + fadeIn(tween(140))) togetherWith
                                                (scaleOut(targetScale = 0.55f, animationSpec = tween(100)) + fadeOut(tween(100)))
                                        },
                                        label = "Azkar favourite star pulse"
                                    ) { saved ->
                                        Text(if (saved) "★" else "☆", color = Gold, fontSize = 22.sp)
                                    }
                                }
                            }
                            Spacer(Modifier.height(7.dp))
                            Text(
                                cleanHadithDisplayText(item.arabic).take(155).let { if (item.arabic.length > 155) it.trimEnd() + "…" else it },
                                color = foreground, fontSize = 18.sp, lineHeight = 30.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        if (language == AppLanguage.EN) "Repeat: $completed / $count"
                                        else if (language == AppLanguage.AR) "التكرار: $completed / $count"
                                        else "ژمارە: $completed / $count",
                                        color = foreground.copy(alpha = 0.58f), fontSize = 10.sp
                                    )
                                    Spacer(Modifier.height(5.dp))
                                    Box(
                                        Modifier.fillMaxWidth(0.88f).height(4.dp)
                                            .clip(CircleShape)
                                            .background(foreground.copy(alpha = 0.08f))
                                    ) {
                                        Box(
                                            Modifier.fillMaxWidth(repetitionProgress).height(4.dp)
                                                .clip(CircleShape)
                                                .background(Brush.horizontalGradient(listOf(Gold, PaleGold)))
                                        )
                                    }
                                }
                                Text(
                                    when (language) { AppLanguage.EN -> "Open remembrance"; AppLanguage.AR -> "فتح الذكر"; AppLanguage.CKB -> "زیکرەکە بکەرەوە" },
                                    color = Gold, fontSize = 10.sp, fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    selectedItem?.let { item ->
        val title = when (language) {
            AppLanguage.CKB -> item.titleCkb.ifBlank { item.titleAr }
            AppLanguage.AR -> item.titleAr.ifBlank { item.titleCkb }
            AppLanguage.EN -> item.titleEn.ifBlank { item.titleCkb }
        }
        val translation = when (language) {
            AppLanguage.CKB -> item.ckb
            AppLanguage.AR -> ""
            AppLanguage.EN -> item.english
        }
        var count by remember(item.id) {
            mutableIntStateOf(progressPrefs.getInt(item.id.toString(), 0).coerceIn(0, item.count.coerceAtLeast(1)))
        }
        val targetCount = item.count.coerceAtLeast(1)
        val incrementCount: () -> Unit = {
            val nextCount = nextAzkarRepetition(count, targetCount)
            if (nextCount != count) {
                count = nextCount
                progressPrefs.edit().putInt(item.id.toString(), nextCount).apply()
                haptic.performHapticFeedback(
                    if (nextCount >= targetCount) HapticFeedbackType.LongPress
                    else HapticFeedbackType.TextHandleMove
                )
            }
        }
        val countPulse = remember(item.id) { Animatable(1f) }
        LaunchedEffect(count) {
            countPulse.snapTo(0.94f)
            countPulse.animateTo(1f, animationSpec = tween(160))
        }
        val isFavorite = favoriteIds.contains(item.id.toString())
        ModalBottomSheet(
            onDismissRequest = { selectedItem = null },
            containerColor = cardColor,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(Modifier.fillMaxWidth().heightIn(max = 690.dp).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 10.dp)) {
                Text(title, color = Gold, fontSize = 21.sp, fontWeight = FontWeight.Bold, lineHeight = 29.sp)
                Spacer(Modifier.height(13.dp))
                Text(
                    cleanHadithDisplayText(item.arabic),
                    color = foreground,
                    fontSize = 25.sp,
                    lineHeight = 44.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .premiumClickable(enabled = count < targetCount, hapticFeedback = false, onClick = incrementCount)
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                        .graphicsLayer { scaleX = countPulse.value; scaleY = countPulse.value }
                )
                Text(
                    when (language) {
                        AppLanguage.EN -> "Tap the Arabic text to count"
                        AppLanguage.AR -> "اضغط على نص الذكر للعدّ"
                        AppLanguage.CKB -> "بۆ ژماردن لەسەر دەقی زیکرەکە دەست لێبدە"
                    },
                    color = foreground.copy(alpha = 0.46f),
                    fontSize = 10.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                if (translation.isNotBlank()) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        when (language) { AppLanguage.EN -> "TRANSLATION"; AppLanguage.AR -> "الترجمة"; AppLanguage.CKB -> "وەرگێڕان" },
                        color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(cleanHadithDisplayText(translation), color = foreground.copy(alpha = 0.85f), fontSize = 14.sp, lineHeight = 24.sp)
                }
                if (item.referenceCkb.isNotBlank()) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        when (language) { AppLanguage.EN -> "SOURCE"; AppLanguage.AR -> "المصدر"; AppLanguage.CKB -> "سەرچاوە" },
                        color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(item.referenceCkb, color = foreground.copy(alpha = 0.67f), fontSize = 12.sp, lineHeight = 20.sp)
                }
                Spacer(Modifier.height(19.dp))
                Card(colors = CardDefaults.cardColors(containerColor = foreground.copy(alpha = 0.045f)), shape = RoundedCornerShape(19.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (language == AppLanguage.EN) "Repetitions" else if (language == AppLanguage.AR) "عدد التكرارات" else "ژمارەی دووبارەکردنەوە",
                            color = foreground.copy(alpha = 0.65f), fontSize = 12.sp
                        )
                        Spacer(Modifier.height(5.dp))
                        Text(
                            "$count / $targetCount",
                            color = Gold, fontSize = 30.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.graphicsLayer { scaleX = countPulse.value; scaleY = countPulse.value }
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                count = 0
                                progressPrefs.edit().putInt(item.id.toString(), 0).apply()
                            }) {
                                Text(if (language == AppLanguage.EN) "Reset" else if (language == AppLanguage.AR) "إعادة" else "سفر")
                            }
                            Button(
                                enabled = count < targetCount,
                                onClick = incrementCount
                            ) {
                                Icon(Icons.Outlined.CheckCircle, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    when {
                                        count >= targetCount && language == AppLanguage.EN -> "Completed"
                                        count >= targetCount && language == AppLanguage.AR -> "اكتمل"
                                        count >= targetCount -> "تەواو بوو"
                                        language == AppLanguage.EN -> "Count"
                                        language == AppLanguage.AR -> "تسبيح"
                                        else -> "ژماردن"
                                    }
                                )
                            }
                        }
                        if (count >= targetCount) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                when (language) {
                                    AppLanguage.EN -> "Target reached — reset to start another round."
                                    AppLanguage.AR -> "اكتمل العدد — أعد التصفير لبدء دورة أخرى."
                                    AppLanguage.CKB -> "ژمارەکە تەواو بوو — سفر بکەرەوە بۆ دەستپێکردنی خولێکی نوێ."
                                },
                                color = Gold, fontSize = 11.sp, textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                PremiumChoicePill(
                    modifier = Modifier.fillMaxWidth(),
                    label = when (language) {
                        AppLanguage.EN -> if (isFavorite) "Remove from favourites" else "Add to favourites"
                        AppLanguage.AR -> if (isFavorite) "إزالة من المفضلة" else "أضف إلى المفضلة"
                        AppLanguage.CKB -> if (isFavorite) "لە دڵخوازەکان بیسڕەوە" else "بیخە ناو دڵخوازەکان"
                    },
                    selected = isFavorite,
                    accent = Gold,
                    foreground = foreground,
                    leadingIcon = Icons.Outlined.BookmarkBorder
                ) {
                    val id = item.id.toString()
                    favoriteIds = if (isFavorite) favoriteIds - id else favoriteIds + id
                    favoritePrefs.edit().putStringSet("favorite_ids", favoriteIds).apply()
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun NamesScreen(language: AppLanguage, foreground: Color, cardColor: Color) {
    val context = LocalContext.current
    val favoritePrefs = remember { context.getSharedPreferences("mashxall_name_favorites", Context.MODE_PRIVATE) }
    var names by remember { mutableStateOf<List<AllahName>>(emptyList()) }
    var query by rememberSaveable { mutableStateOf("") }
    var favoritesOnly by rememberSaveable { mutableStateOf(false) }
    var favoriteIds by remember {
        mutableStateOf(favoritePrefs.getStringSet("favorite_ids", emptySet()).orEmpty().toSet())
    }

    LaunchedEffect(Unit) { names = ContentRepository.loadNames(context) }
    val normalizedQuery = query.trim()
    val visibleNames = names.filter { item ->
        val matchesFavorite = !favoritesOnly || favoriteIds.contains(item.number.toString())
        val searchable = listOf(
            item.arabic, item.ckb, item.badini, item.english,
            item.surahAyah, item.highlightSection, item.farsi, item.extraText
        ).joinToString(" ")
        matchesFavorite && (normalizedQuery.isBlank() || searchable.contains(normalizedQuery, ignoreCase = true))
    }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            PremiumSectionHero(
                title = tr(language, "names"),
                subtitle = when (language) {
                    AppLanguage.EN -> "Reflect on the beautiful names and their meanings."
                    AppLanguage.AR -> "تدبّر الأسماء الحسنى ومعانيها ومراجعها."
                    AppLanguage.CKB -> "لە ناوە جوانەکان و ماناکانیان ورد ببەوە."
                },
                icon = Icons.Outlined.Spa,
                foreground = foreground,
                accent = Color(0xFFE6A8C1),
                metric = when (language) {
                    AppLanguage.EN -> "${visibleNames.size} shown • ${favoriteIds.size} saved"
                    AppLanguage.AR -> "${visibleNames.size} معروض • ${favoriteIds.size} محفوظ"
                    AppLanguage.CKB -> "${visibleNames.size} پیشان دەدرێت • ${favoriteIds.size} پاشەکەوتکراو"
                },
                eyebrow = when (language) { AppLanguage.EN -> "THE BEAUTIFUL NAMES"; AppLanguage.AR -> "الأَسْمَاءُ الْحُسْنَى"; AppLanguage.CKB -> "ناوە جوانەکان" }
            )
            Spacer(Modifier.height(10.dp))
            SearchBox(
                query, { query = it },
                when (language) {
                    AppLanguage.EN -> "Search Arabic, Kurdish, English, or references…"
                    AppLanguage.AR -> "ابحث بالعربية أو الكردية أو الإنجليزية أو المراجع…"
                    AppLanguage.CKB -> "بە عەرەبی، کوردی، ئینگلیزی یان سەرچاوە بگەڕێ…"
                }
            )
            Spacer(Modifier.height(7.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                PremiumChoicePill(
                    label = when (language) { AppLanguage.EN -> "All names"; AppLanguage.AR -> "كل الأسماء"; AppLanguage.CKB -> "هەموو ناوەکان" },
                    selected = !favoritesOnly,
                    accent = Gold,
                    foreground = foreground
                ) { favoritesOnly = false }
                PremiumChoicePill(
                    label = when (language) { AppLanguage.EN -> "Favourites"; AppLanguage.AR -> "المفضلة"; AppLanguage.CKB -> "دڵخوازەکان" },
                    selected = favoritesOnly,
                    accent = Gold,
                    foreground = foreground
                ) { favoritesOnly = true }
            }
        }
        if (visibleNames.isEmpty()) {
            LoadingOrEmpty(
                names.isEmpty(), language, foreground,
                if (favoritesOnly && favoriteIds.isEmpty()) {
                    when (language) {
                        AppLanguage.EN -> "No favourite names yet. Tap the star to save a name."
                        AppLanguage.AR -> "لا توجد أسماء مفضلة بعد. اضغط النجمة لحفظ الاسم."
                        AppLanguage.CKB -> "هێشتا ناوی دڵخواز نییە؛ ئەستێرەکە دابگرە بۆ پاشەکەوتکردن."
                    }
                } else tr(language, "not_found")
            )
        } else LazyColumn(
            contentPadding = PaddingValues(vertical = 7.dp, horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            itemsIndexed(visibleNames, key = { _, it -> it.number }) { itemIndex, name ->
                var expanded by rememberSaveable(name.number) { mutableStateOf(false) }
                val expandArrowRotation by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = if (expanded) 90f else 0f,
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                    ),
                    label = "Mashxall name card expand arrow"
                )
                val favorite = favoriteIds.contains(name.number.toString())
                val description = when (language) {
                    AppLanguage.CKB -> name.ckb
                    // The bundled names table has no separate Arabic meaning field.
                    // Do not repeat the Arabic name itself as if it were a meaning.
                    AppLanguage.AR -> ""
                    AppLanguage.EN -> name.english
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                        .staggeredEntrance(itemKey = name.number, index = itemIndex)
                        .premiumClickable { expanded = !expanded }
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(42.dp).clip(CircleShape).background(DeepGreen), contentAlignment = Alignment.Center) {
                                Text(name.number.toString(), color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(name.arabic, color = Gold, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                                if (description.isNotBlank()) {
                                    Text(description, color = foreground, fontSize = 12.sp, lineHeight = 18.sp)
                                }
                            }
                            Box(
                                Modifier.size(38.dp).clip(CircleShape)
                                    .background(Gold.copy(alpha = if (favorite) 0.18f else 0.045f))
                                    .border(1.dp, Gold.copy(alpha = if (favorite) 0.48f else 0.17f), CircleShape)
                                    .premiumClickable(hapticFeedback = true) {
                                        val key = name.number.toString()
                                        favoriteIds = if (favorite) favoriteIds - key else favoriteIds + key
                                        favoritePrefs.edit().putStringSet("favorite_ids", favoriteIds).apply()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                AnimatedContent(
                                    targetState = favorite,
                                    transitionSpec = {
                                        (scaleIn(initialScale = 0.45f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)) + fadeIn(tween(140))) togetherWith
                                            (scaleOut(targetScale = 0.55f, animationSpec = tween(100)) + fadeOut(tween(100)))
                                    },
                                    label = "Divine name favorite star pulse"
                                ) { saved ->
                                    Text(if (saved) "★" else "☆", color = Gold, fontSize = 23.sp)
                                }
                            }
                            Icon(
                                Icons.Outlined.ChevronRight,
                                contentDescription = if (expanded) {
                                    if (language == AppLanguage.EN) "Collapse details"
                                    else if (language == AppLanguage.AR) "طي التفاصيل"
                                    else "وردەکارییەکان بشارەوە"
                                } else {
                                    if (language == AppLanguage.EN) "Expand details"
                                    else if (language == AppLanguage.AR) "عرض التفاصيل"
                                    else "وردەکارییەکان پیشان بدە"
                                },
                                tint = Gold,
                                modifier = Modifier.graphicsLayer { rotationZ = expandArrowRotation }.size(23.dp)
                            )
                        }
                        AnimatedVisibility(
                            visible = expanded,
                            enter = androidx.compose.animation.expandVertically(
                                animationSpec = tween(250)
                            ) + fadeIn(tween(180)) + scaleIn(
                                initialScale = 0.98f,
                                animationSpec = tween(220)
                            ),
                            exit = androidx.compose.animation.shrinkVertically(
                                animationSpec = tween(180)
                            ) + fadeOut(tween(120))
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Spacer(Modifier.height(10.dp))
                            if (name.highlightSection.isNotBlank()) {
                                Text(name.highlightSection, color = foreground, fontSize = 19.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                                Spacer(Modifier.height(6.dp))
                            }
                            if (name.surahAyah.isNotBlank()) {
                                Text(
                                    when (language) {
                                        AppLanguage.EN -> "Qur'an reference: ${name.surahAyah}"
                                        AppLanguage.AR -> "موضع قرآني: ${name.surahAyah}"
                                        AppLanguage.CKB -> "سەرچاوەی قورئان: ${name.surahAyah}"
                                    }, color = Gold, fontSize = 11.sp
                                )
                                Spacer(Modifier.height(6.dp))
                            }
                            if (language != AppLanguage.CKB && name.ckb.isNotBlank()) {
                                Text("کوردی سۆرانی: " + name.ckb, color = foreground.copy(alpha = 0.75f), fontSize = 12.sp, lineHeight = 19.sp)
                                Spacer(Modifier.height(5.dp))
                            }
                            if (language != AppLanguage.EN && name.english.isNotBlank()) {
                                Text("English: " + name.english, color = foreground.copy(alpha = 0.75f), fontSize = 12.sp, lineHeight = 19.sp)
                                Spacer(Modifier.height(5.dp))
                            }
                            if (language == AppLanguage.CKB && name.badini.isNotBlank()) {
                                Text("بادینی: " + name.badini, color = foreground.copy(alpha = 0.72f), fontSize = 12.sp, lineHeight = 19.sp)
                                Spacer(Modifier.height(5.dp))
                            }
                            if (name.farsi.isNotBlank()) {
                                Text(
                                    when (language) {
                                        AppLanguage.EN -> "Persian: " + name.farsi
                                        AppLanguage.AR -> "الفارسية: " + name.farsi
                                        AppLanguage.CKB -> "فارسی: " + name.farsi
                                    }, color = foreground.copy(alpha = 0.62f), fontSize = 11.sp, lineHeight = 17.sp
                                )
                                Spacer(Modifier.height(5.dp))
                            }
                            if (name.extraText.isNotBlank()) {
                                Text(
                                    when (language) {
                                        AppLanguage.EN -> "Additional source note: " + name.extraText
                                        AppLanguage.AR -> "معلومة إضافية من المصدر: " + name.extraText
                                        AppLanguage.CKB -> "زانیاریی زیادەی سەرچاوە: " + name.extraText
                                    }, color = foreground.copy(alpha = 0.62f), fontSize = 11.sp, lineHeight = 17.sp
                                )
                            }
                            }
                        }
                    }
                }
            }
        }
    }
}

internal data class TasbihTapResult(
    val roundCount: Int,
    val totalCount: Int,
    val accepted: Boolean
)

/** Enforce source-defined round target while preserving the all-time total. */
internal fun nextTasbihTap(roundCount: Int, totalCount: Int, targetCount: Int): TasbihTapResult {
    val safeRound = roundCount.coerceAtLeast(0)
    val safeTotal = totalCount.coerceAtLeast(0)
    if ((targetCount > 0 && safeRound >= targetCount) ||
        safeRound == Int.MAX_VALUE ||
        safeTotal == Int.MAX_VALUE
    ) {
        return TasbihTapResult(safeRound, safeTotal, accepted = false)
    }
    return TasbihTapResult(
        roundCount = (safeRound + 1).let { next -> if (targetCount > 0) next.coerceAtMost(targetCount) else next },
        totalCount = safeTotal + 1,
        accepted = true
    )
}

internal fun tasbihProgressFraction(count: Int, target: Int): Float =
    if (target <= 0) 0f else (count.toFloat() / target.toFloat()).coerceIn(0f, 1f)

@Composable
fun TasbihScreen(language: AppLanguage, foreground: Color, cardColor: Color) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("mashxall_preferences", Context.MODE_PRIVATE) }
    var presets by remember { mutableStateOf(emptyList<TasbihPreset>()) }
    var selectedId by rememberSaveable { mutableIntStateOf(prefs.getInt("tasbih_preset_id", 3)) }
    var count by rememberSaveable { mutableIntStateOf(prefs.getInt("tasbih_count_3", 0)) }
    var totalCount by rememberSaveable {
        val initialPreset = prefs.getInt("tasbih_preset_id", 3)
        mutableIntStateOf(prefs.getInt("tasbih_total_$initialPreset", prefs.getInt("tasbih_count_$initialPreset", 0)))
    }
    val haptic = LocalHapticFeedback.current
    val tapPulse = remember { Animatable(1f) }
    LaunchedEffect(count) {
        tapPulse.snapTo(0.95f)
        tapPulse.animateTo(1f, animationSpec = tween(130))
    }
    val selected = presets.firstOrNull { it.id == selectedId }
    val targetCount = selected?.targetCount?.takeIf { it > 0 } ?: 0
    val targetProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = tasbihProgressFraction(count, targetCount),
        animationSpec = tween(220),
        label = "tasbihTargetProgress"
    )

    LaunchedEffect(Unit) {
        presets = ContentRepository.loadTasbihPresets(context)
        if (presets.none { it.id == selectedId } && presets.isNotEmpty()) selectedId = presets.first().id
    }
    LaunchedEffect(selectedId, presets) {
        count = prefs.getInt("tasbih_count_$selectedId", 0)
        totalCount = prefs.getInt("tasbih_total_$selectedId", count)
        prefs.edit().putInt("tasbih_preset_id", selectedId).apply()
    }
    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        PremiumSectionHero(
            title = tr(language, "tasbih"),
            subtitle = when (language) {
                AppLanguage.EN -> "A focused counter with saved rounds and gentle tactile feedback."
                AppLanguage.AR -> "عداد هادئ يحفظ جولاتك ويستجيب للمس."
                AppLanguage.CKB -> "ژمێرەرێکی ئارام کە خولەکانت دەپارێزێت و بە دەست لێدان وەڵام دەداتەوە."
            },
            icon = Icons.Outlined.Spa,
            foreground = foreground,
            accent = Color(0xFF82CDB1),
            metric = when (language) { AppLanguage.EN -> "ALL-TIME • DAILY PRACTICE"; AppLanguage.AR -> "مداومة • ذكر يومي"; AppLanguage.CKB -> "بەردەوامی • زیکری ڕۆژانە" },
            eyebrow = when (language) { AppLanguage.EN -> "MINDFUL WORSHIP"; AppLanguage.AR -> "عِبَادَةٌ بِحُضُور"; AppLanguage.CKB -> "پەرستش بە ئاگایی" }
        )
        Spacer(Modifier.height(11.dp))
        if (presets.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                items(presets, key = { it.id }) { preset ->
                    PremiumChoicePill(
                        label = preset.zikr.replace("\n", " ").take(18),
                        selected = selectedId == preset.id,
                        accent = Gold,
                        foreground = foreground
                    ) {
                        selectedId = preset.id
                        count = prefs.getInt("tasbih_count_${preset.id}", 0)
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            selected?.zikr ?: "سُبْحَانَ اللّٰهِ • اَلْحَمْدُلِلّٰهِ • اَللّٰهُ أَكْبَرُ",
            color = Gold, textAlign = TextAlign.Center, fontSize = 18.sp, lineHeight = 29.sp
        )
        selected?.targetCount?.takeIf { it > 0 }?.let { target ->
            Spacer(Modifier.height(6.dp))
            Text(
                when (language) {
                    AppLanguage.EN -> "Source target: $target repetitions"
                    AppLanguage.AR -> "العدد المستهدف في المصدر: $target تكرارًا"
                    AppLanguage.CKB -> "ژمارەی ئامانجی سەرچاوە: $target جار"
                },
                color = foreground.copy(alpha = 0.65f), fontSize = 11.sp
            )
            AnimatedVisibility(
                visible = count >= target,
                enter = fadeIn(tween(180)) + scaleIn(
                    initialScale = 0.72f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                ),
                exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.92f, animationSpec = tween(120))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PremiumIconMedallion(icon = Icons.Outlined.CheckCircle, tint = Gold, size = 25.dp, pulse = true)
                    Spacer(Modifier.width(7.dp))
                    Text(
                        when (language) {
                            AppLanguage.EN -> "Target reached"
                            AppLanguage.AR -> "تم بلوغ العدد المستهدف"
                            AppLanguage.CKB -> "گەیشتن بە ژمارەی ئامانج"
                        },
                        color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        if (language == AppLanguage.CKB && !selected?.meaningCkb.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(selected?.meaningCkb.orEmpty(), color = foreground.copy(alpha = 0.7f), fontSize = 12.sp, textAlign = TextAlign.Center, lineHeight = 18.sp)
        }
        Spacer(Modifier.height(24.dp))
        Box(
            Modifier.size(238.dp).clip(CircleShape).background(
                androidx.compose.ui.graphics.Brush.radialGradient(listOf(Color(0xFF1E7259), DeepGreen, CardDark))
            ).premiumClickable(hapticFeedback = false) {
                val result = nextTasbihTap(count, totalCount, targetCount)
                if (result.accepted) {
                    count = result.roundCount
                    totalCount = result.totalCount
                    prefs.edit()
                        .putInt("tasbih_count_$selectedId", count)
                        .putInt("tasbih_total_$selectedId", totalCount)
                        .putInt("tasbih_count", count)
                        .apply()
                    haptic.performHapticFeedback(
                        if (targetCount > 0 && count >= targetCount) HapticFeedbackType.LongPress
                        else HapticFeedbackType.TextHandleMove
                    )
                }
            }.graphicsLayer {
                scaleX = tapPulse.value
                scaleY = tapPulse.value
            },
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize().padding(10.dp)) {
                val strokeWidth = 6.dp.toPx()
                drawArc(
                    color = Color.White.copy(alpha = 0.12f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = strokeWidth,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                )
                if (targetCount > 0) {
                    drawArc(
                        color = Gold,
                        startAngle = -90f,
                        sweepAngle = 360f * targetProgress,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = strokeWidth,
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedContent(
                    targetState = count,
                    transitionSpec = {
                        (fadeIn(tween(110)) + scaleIn(
                            initialScale = 0.72f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                        )) togetherWith
                            (fadeOut(tween(90)) + scaleOut(targetScale = 1.20f, animationSpec = tween(100)))
                    },
                    label = "Tasbih rolling counter"
                ) { displayedCount ->
                    Text(displayedCount.toString(), color = Color.White, fontSize = 54.sp, fontWeight = FontWeight.ExtraBold)
                }
                Text(if (language == AppLanguage.EN) "TAP TO COUNT" else if (language == AppLanguage.AR) "اضغط للعدّ" else "بۆ ژماردن لێی بدە", color = Gold, fontSize = 10.sp, letterSpacing = 1.4.sp)
                if (targetCount > 0) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "${(targetProgress * 100f).toInt()}%",
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 10.sp
                    )
                }
            }
        }
        Spacer(Modifier.height(9.dp))
        Text(
            when (language) {
                AppLanguage.EN -> "All-time count: $totalCount"
                AppLanguage.AR -> "إجمالي التسبيحات: $totalCount"
                AppLanguage.CKB -> "کۆی ژمارەکان: $totalCount"
            },
            color = foreground.copy(alpha = 0.63f), fontSize = 11.sp
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
            PremiumActionButton(
                label = tr(language, "reset"),
                icon = Icons.Outlined.Refresh,
                accent = Color(0xFFE58A82),
                foreground = foreground,
                modifier = Modifier.weight(1f)
            ) {
                count = 0
                totalCount = 0
                prefs.edit()
                    .putInt("tasbih_count_$selectedId", 0)
                    .putInt("tasbih_total_$selectedId", 0)
                    .putInt("tasbih_count", 0)
                    .apply()
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            PremiumActionButton(
                label = if (language == AppLanguage.EN) "New round" else if (language == AppLanguage.AR) "دورة جديدة" else "خولێکی نوێ",
                icon = Icons.Outlined.Repeat,
                accent = Gold,
                foreground = foreground,
                modifier = Modifier.weight(1f)
            ) {
                count = 0
                prefs.edit().putInt("tasbih_count_$selectedId", 0).putInt("tasbih_count", 0).apply()
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }
    }
}


private fun reciterOfflineFile(context: Context, reciter: Reciter, surah: Int, ayah: Int = 1): File {
    val reciterDir = File(File(context.filesDir, "reciter-audio"), reciter.id.toString())
    val chapter = surah.coerceIn(1, 114)
    return if (reciter.audioType == 0) {
        File(File(reciterDir, "surah-%03d".format(Locale.US, chapter)), "ayah-%03d.mp3".format(Locale.US, ayah.coerceAtLeast(1)))
    } else {
        File(reciterDir, "surah-%03d.mp3".format(Locale.US, chapter))
    }
}

private fun isReciterSurahOffline(context: Context, reciter: Reciter, surah: Int, ayahCount: Int): Boolean {
    if (surah !in 1..reciter.surahTotal) return false
    if (reciter.audioType != 0) return reciterOfflineFile(context, reciter, surah).let { it.isFile && it.length() > 512L }
    if (ayahCount < 1) return false
    return (1..ayahCount).all { ayah ->
        reciterOfflineFile(context, reciter, surah, ayah).let { it.isFile && it.length() > 512L }
    }
}

private fun reciterAudioSequence(
    context: Context,
    reciter: Reciter,
    surah: Int,
    startingAyah: Int,
    ayahCount: Int
): List<String> {
    if (surah !in 1..reciter.surahTotal) return emptyList()
    if (reciter.audioType != 0) {
        val local = reciterOfflineFile(context, reciter, surah)
        return listOf(if (local.isFile && local.length() > 512L) "local:${local.absolutePath}" else ContentRepository.reciterSampleUrl(reciter, surah, 1))
    }
    if (ayahCount < 1) return emptyList()
    val start = startingAyah.coerceIn(1, ayahCount)
    return (start..ayahCount).map { ayah ->
        val local = reciterOfflineFile(context, reciter, surah, ayah)
        if (local.isFile && local.length() > 512L) "local:${local.absolutePath}"
        else ContentRepository.reciterSampleUrl(reciter, surah, ayah)
    }
}

private suspend fun downloadReciterSurah(
    context: Context,
    reciter: Reciter,
    surah: Int,
    ayahCount: Int,
    onProgress: (Int, Int) -> Unit
) = withContext(Dispatchers.IO) {
    val chapter = surah.coerceIn(1, 114)
    require(chapter in 1..reciter.surahTotal) { "This recitation edition does not contain the selected surah." }
    if (reciter.audioType == 0) {
        require(ayahCount > 0) { "The source did not provide this surah's verse count." }
        for (ayah in 1..ayahCount) {
            val destination = reciterOfflineFile(context, reciter, chapter, ayah)
            if (!destination.isFile || destination.length() <= 512L) {
                downloadReciterFile(ContentRepository.reciterSampleUrl(reciter, chapter, ayah), destination)
            }
            withContext(Dispatchers.Main) { onProgress(ayah, ayahCount) }
        }
    } else {
        val destination = reciterOfflineFile(context, reciter, chapter)
        if (!destination.isFile || destination.length() <= 512L) {
            downloadReciterFile(ContentRepository.reciterSampleUrl(reciter, chapter, 1), destination)
        }
        withContext(Dispatchers.Main) { onProgress(1, 1) }
    }
}

private fun onlineAdhanOfflineFile(context: Context, item: OnlineAdhan): File {
    val safeId = item.id.replace(Regex("[^A-Za-z0-9_-]"), "_")
    return File(File(context.filesDir, "adhan-audio"), "$safeId.mp3")
}

private suspend fun downloadOnlineAdhan(context: Context, item: OnlineAdhan): File = withContext(Dispatchers.IO) {
    val destination = onlineAdhanOfflineFile(context, item)
    if (!destination.isFile || destination.length() <= 512L) {
        downloadReciterFile(item.url, destination)
    }
    require(destination.isFile && destination.length() > 512L) { "The downloaded adhan file is incomplete." }
    destination
}

private fun formatAudioFileSize(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
    bytes >= 1024L -> String.format(Locale.US, "%.0f KB", bytes / 1024.0)
    else -> "$bytes B"
}

private fun downloadReciterFile(url: String, destination: File) {
    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 15_000
        readTimeout = 60_000
        instanceFollowRedirects = true
        setRequestProperty("User-Agent", "Mashxall Android")
    }
    val temporary = File(destination.parentFile, destination.name + ".part")
    try {
        destination.parentFile?.mkdirs()
        if (temporary.exists()) temporary.delete()
        connection.connect()
        val code = connection.responseCode
        if (code !in 200..299) throw java.io.IOException("The audio server returned HTTP $code.")
        if (connection.contentLengthLong > 100L * 1024L * 1024L) {
            throw java.io.IOException("This audio file is larger than the supported download limit.")
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
                        throw java.io.IOException("The audio file exceeded the supported download limit.")
                    }
                    output.write(buffer, 0, count)
                }
            }
        }
        if (total <= 512L || !temporary.isFile) {
            throw java.io.IOException("The audio source returned an empty or incomplete file.")
        }
        if (destination.exists()) destination.delete()
        if (!temporary.renameTo(destination)) {
            temporary.copyTo(destination, overwrite = true)
            temporary.delete()
        }
    } finally {
        if (temporary.exists()) temporary.delete()
        connection.disconnect()
    }
}

internal data class BundledAdhanTrack(val item: AudioItem, val category: String)

/**
 * Identify genuine adhan/call-to-prayer assets in the recovered app bundles.
 * Do not classify generic Quran recitation or unrelated religious audio as adhan.
 */
internal fun bundledAdhanCategory(item: AudioItem): String? {
    val searchable = (item.assetPath + " " + item.title + " " + item.group)
        .lowercase(Locale.ROOT)
        .replace('_', ' ')
        .replace('-', ' ')
        .replace('/', ' ')
    // Source bundles contain Arabic-only filenames as well as English/Latin names.
    // Match whole words in both scripts so original Adhan recordings are not hidden
    // just because their filenames do not contain the English word "adhan".
    val mosqueRecording = Regex(
        """(?:^|[^a-z])(mosque|masjid|haram|makkah|mecca|madinah|madina|nabawi)(?:[^a-z]|$)"""
    ).containsMatchIn(searchable) || Regex(
        """(?:^|[^\p{L}])(المسجد الحرام|مسجد الحرام|المسجد النبوي|مسجد نبوي|المسجد|مسجد|الحرم|مكة المكرمة|مكة|مكه|المدينة المنورة|المدينة)(?:[^\p{L}]|$)"""
    ).containsMatchIn(searchable)
    val adhanRecording = Regex(
        """(?:^|[^a-z])(adhan|athan|azan|muezzin|muazzin|iqama|iqamah|takbir|takbeer|call to prayer|prayer call)(?:[^a-z]|$)"""
    ).containsMatchIn(searchable) || Regex(
        """(?:^|[^\p{L}])(الأذان|الاذان|أذان|اذان|آذان|المؤذن|مؤذن|المؤذنون|مؤذنون|المؤذنين|مؤذنين|الموذن|موذن|الإقامة|الاقامة|إقامة|اقامة|التكبير|تكبير|نداء الصلاة)(?:[^\p{L}]|$)"""
    ).containsMatchIn(searchable)
    return when {
        mosqueRecording -> "mosque"
        adhanRecording -> "muezzin"
        else -> null
    }
}

internal fun filterSelectableAdhanAssets(items: List<AudioItem>): List<AudioItem> =
    items.filter { bundledAdhanCategory(it) != null }.distinctBy { it.assetPath }

/** A selected path is valid only when it matches a source-classified Adhan/mosque catalogue entry. */
internal fun hasSelectableAdhanAsset(assetPath: String, availableAssets: List<AudioItem>): Boolean =
    assetPath.isNotBlank() && availableAssets.any {
        it.assetPath == assetPath && bundledAdhanCategory(it) != null
    }

internal fun adhanSourceRequiredMessage(language: AppLanguage): String = when (language) {
    AppLanguage.EN -> "Choose an actual Adhan or mosque recording first. Open the online library, download a track, then return and select it here."
    AppLanguage.AR -> "اختر تسجيل أذان أو مسجد حقيقيًا أولًا. افتح المكتبة عبر الإنترنت ونزّل تسجيلًا، ثم عُد واختره هنا."
    AppLanguage.CKB -> "سەرەتا تۆمارێکی ڕاستەقینەی بانگ یان مزگەوت هەڵبژێرە؛ کتێبخانەی ئۆنلاین بکەرەوە، دەنگێک دابگرە و بگەڕێوە لێرە هەڵیبژێرە."
}

internal fun matchesAdhanLibraryFilter(filter: String, category: String, downloaded: Boolean): Boolean = when (filter) {
    "muezzin" -> category == "muezzin"
    "mosque" -> category == "mosque"
    "downloaded" -> downloaded
    else -> true
}

@Composable
fun AudioScreen(
    language: AppLanguage,
    foreground: Color,
    cardColor: Color,
    initialSurah: Int = 1,
    initialAyah: Int = 1,
    focusReciters: Boolean = false,
    focusAdhan: Boolean = false,
    autoPlayRequested: Boolean = false
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("mashxall_preferences", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    var audio by remember { mutableStateOf<List<AudioItem>>(emptyList()) }
    var onlineAdhan by remember { mutableStateOf<List<OnlineAdhan>>(emptyList()) }
    var reciters by remember { mutableStateOf<List<Reciter>>(emptyList()) }
    var surahs by remember { mutableStateOf<List<Surah>>(emptyList()) }
    var downloadingTracks by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var downloadingAdhan by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var offlineRevision by remember { mutableIntStateOf(0) }
    var offlineAdhanRevision by remember { mutableIntStateOf(0) }
    var audioNotice by rememberSaveable { mutableStateOf("") }
    var preferredReciterId by rememberSaveable { mutableIntStateOf(prefs.getInt("preferred_reciter_id", -1)) }
    val preferredReciter = reciters.firstOrNull { it.id == preferredReciterId }
    var autoPlayKey by rememberSaveable { mutableStateOf("") }
    var playing by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var selectedTab by rememberSaveable { mutableIntStateOf(if (focusAdhan) 2 else if (focusReciters) 1 else 0) }
    var audioQuery by rememberSaveable { mutableStateOf("") }
    var adhanCategory by rememberSaveable { mutableStateOf("all") }
    var surahText by rememberSaveable { mutableStateOf(initialSurah.coerceIn(1, 114).toString()) }
    var ayahText by rememberSaveable { mutableStateOf(initialAyah.coerceAtLeast(1).toString()) }
    val playbackMode = ReaderPlaybackSettings.mode
    LaunchedEffect(initialSurah, initialAyah, focusReciters, focusAdhan) {
        surahText = initialSurah.coerceIn(1, 114).toString()
        ayahText = initialAyah.coerceAtLeast(1).toString()
        selectedTab = if (focusAdhan) 2 else if (focusReciters) 1 else 0
    }
    LaunchedEffect(Unit) {
        ReaderPlaybackSettings.mode = ReaderPlaybackSettings.normalize(prefs.getString("reciter_playback_mode", "full"))
        audio = ContentRepository.loadAudioItems(context)
        onlineAdhan = ContentRepository.loadOnlineAdhan(context)
        val loadedReciters = ContentRepository.loadReciters(context)
        reciters = loadedReciters
        surahs = ContentRepository.loadSurahs(context)
        // The first-use default is complete-surah playback. Prefer a source with
        // complete-surah files; verse-by-verse remains available as an explicit choice.
        if (preferredReciterId < 0 || loadedReciters.none { it.id == preferredReciterId }) {
            defaultReciterId(ReaderPlaybackSettings.mode, loadedReciters)?.let { firstAvailableId ->
                preferredReciterId = firstAvailableId
                prefs.edit().putInt("preferred_reciter_id", firstAvailableId).apply()
            }
        }
        loading = false
    }
    LaunchedEffect(reciters, playbackMode, preferredReciterId) {
        val compatible = resolveCompatibleReciterId(playbackMode, preferredReciterId, reciters)
        if (compatible != null && compatible != preferredReciterId) {
            preferredReciterId = compatible
            prefs.edit().putInt("preferred_reciter_id", compatible).apply()
        }
    }
    LaunchedEffect(reciters, surahs, focusReciters, autoPlayRequested, initialSurah, initialAyah, preferredReciterId, playbackMode) {
        val preferred = reciters.firstOrNull { it.id == preferredReciterId }
        if (autoPlayRequested && focusReciters && preferred != null && surahs.isNotEmpty()) {
            val surahNumber = initialSurah.coerceIn(1, 114)
            val ayahCount = surahs.firstOrNull { it.number == surahNumber }?.ayahCount ?: 1
            val startAyah = resolveReciterStartAyah(playbackMode, initialAyah, ayahCount)
            val sources = reciterAudioSequence(context, preferred, surahNumber, startAyah, ayahCount)
            val key = "${preferred.id}|${surahNumber}|${startAyah}|${playbackMode}"
            if (autoPlayKey != key && sources.isNotEmpty()) {
                autoPlayKey = key
                playing = key
                AudioController.playSequence(context, sources) { state ->
                    if (!state) playing = ""
                }
            }
        }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        PremiumSectionHero(
            title = tr(language, "audio"),
            subtitle = when (language) {
                AppLanguage.EN -> "Reciters, bundled recordings and Adhan libraries in one place."
                AppLanguage.AR -> "القرّاء والتسجيلات المضمّنة ومكتبات الأذان في مكان واحد."
                AppLanguage.CKB -> "قورئان‌خوێن، تۆمارە ناوخۆییەکان و کتێبخانەی بانگ لە یەک شوێندا."
            },
            icon = Icons.Outlined.GraphicEq,
            foreground = foreground,
            accent = Color(0xFFE9B17B),
            metric = when (language) { AppLanguage.EN -> "OFFLINE + STREAMING"; AppLanguage.AR -> "دون اتصال + بث"; AppLanguage.CKB -> "بەبێ ئینتەرنێت + پەخش" },
            eyebrow = when (language) { AppLanguage.EN -> "SOUND & RECITATION"; AppLanguage.AR -> "الصوت والتلاوة"; AppLanguage.CKB -> "دەنگ و تلاوەت" }
        )
        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            items(listOf(0, 1, 2)) { index ->
                PremiumChoicePill(
                    label = when (index) {
                        0 -> if (language == AppLanguage.EN) "On-device audio" else if (language == AppLanguage.AR) "صوتيات الجهاز" else "دەنگی ناوخۆیی"
                        1 -> if (language == AppLanguage.EN) "Reciters" else if (language == AppLanguage.AR) "القرّاء" else "قورئان‌خوێنەکان"
                        else -> if (language == AppLanguage.EN) "Adhan & mosque audio" else if (language == AppLanguage.AR) "الأذان وأصوات المساجد" else "بانگ و دەنگی مزگەوت"
                    },
                    selected = selectedTab == index,
                    accent = Gold,
                    foreground = foreground
                ) {
                    selectedTab = index
                    AudioController.stop()
                    playing = ""
                }
            }
        }
        Spacer(Modifier.height(7.dp))
        SearchBox(audioQuery, { audioQuery = it }, tr(language, "search"))
        AnimatedVisibility(
            visible = audioNotice.isNotBlank(),
            enter = fadeIn(tween(180)) + slideInVertically(
                initialOffsetY = { height -> height / 4 },
                animationSpec = tween(220)
            ),
            exit = fadeOut(tween(120))
        ) {
            val noticeIsError = audioNotice.contains("failed", ignoreCase = true) ||
                audioNotice.contains("فشل") || audioNotice.contains("سەرکەوتوو نەبوو")
            val noticeColor = if (noticeIsError) Color(0xFFE07870) else Gold
            Row(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(noticeColor.copy(alpha = 0.09f))
                    .border(1.dp, noticeColor.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (noticeIsError) Icons.Outlined.Info else Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = noticeColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(9.dp))
                Text(
                    audioNotice,
                    color = foreground,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        if (selectedTab == 2) {
            val searchTerm = audioQuery.trim()
            val bundledAdhan = remember(audio) {
                audio.mapNotNull { item ->
                    bundledAdhanCategory(item)?.let { category -> BundledAdhanTrack(item, category) }
                }
            }
            val visibleBundledAdhan = remember(bundledAdhan, searchTerm, adhanCategory) {
                bundledAdhan.filter { track ->
                    val matchesSearch = searchTerm.isBlank() ||
                        (track.item.title + " " + track.item.assetPath + " " + track.item.group)
                            .contains(searchTerm, ignoreCase = true)
                    matchesSearch && matchesAdhanLibraryFilter(adhanCategory, track.category, downloaded = true)
                }
            }
            val categoryCounts = remember(onlineAdhan, bundledAdhan, offlineAdhanRevision) {
                mapOf(
                    "all" to onlineAdhan.size + bundledAdhan.size,
                    "muezzin" to onlineAdhan.count { it.category == "muezzin" } + bundledAdhan.count { it.category == "muezzin" },
                    "mosque" to onlineAdhan.count { it.category == "mosque" } + bundledAdhan.count { it.category == "mosque" },
                    "downloaded" to bundledAdhan.size + onlineAdhan.count { item ->
                        val file = onlineAdhanOfflineFile(context, item)
                        file.isFile && file.length() > 512L
                    }
                )
            }
            val visibleAdhan = remember(onlineAdhan, searchTerm, adhanCategory, offlineAdhanRevision) {
                onlineAdhan.filter { item ->
                    val matchesSearch = searchTerm.isBlank() ||
                        (item.titleArabic + " " + item.titleEnglish + " " + item.sourceFilename)
                            .contains(searchTerm, ignoreCase = true)
                    val file = onlineAdhanOfflineFile(context, item)
                    matchesSearch && matchesAdhanLibraryFilter(
                        adhanCategory,
                        item.category,
                        file.isFile && file.length() > 512L
                    )
                }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp), contentPadding = PaddingValues(vertical = 3.dp)) {
                items(listOf("all", "muezzin", "mosque", "downloaded"), key = { it }) { category ->
                    val count = categoryCounts[category] ?: 0
                    PremiumChoicePill(
                        label = when (category) {
                            "muezzin" -> when (language) {
                                AppLanguage.EN -> "Muezzins ($count)"
                                AppLanguage.AR -> "المؤذنون ($count)"
                                AppLanguage.CKB -> "موئەزینەکان ($count)"
                            }
                            "mosque" -> when (language) {
                                AppLanguage.EN -> "Mosques ($count)"
                                AppLanguage.AR -> "المساجد ($count)"
                                AppLanguage.CKB -> "مزگەوتەکان ($count)"
                            }
                            "downloaded" -> when (language) {
                                AppLanguage.EN -> "Downloaded ($count)"
                                AppLanguage.AR -> "المنزّل ($count)"
                                AppLanguage.CKB -> "داگیراوەکان ($count)"
                            }
                            else -> when (language) {
                                AppLanguage.EN -> "All ($count)"
                                AppLanguage.AR -> "الكل ($count)"
                                AppLanguage.CKB -> "هەموو ($count)"
                            }
                        },
                        selected = adhanCategory == category,
                        accent = Gold,
                        foreground = foreground
                    ) { adhanCategory = category }
                }
            }
            Text(
                when (language) {
                    AppLanguage.EN -> "${visibleAdhan.size + visibleBundledAdhan.size} recordings shown • ${categoryCounts["muezzin"]} muezzin tracks • ${categoryCounts["mosque"]} mosque/location recordings"
                    AppLanguage.AR -> "المعروض ${visibleAdhan.size + visibleBundledAdhan.size} تسجيلًا • ${categoryCounts["muezzin"]} للمؤذنين • ${categoryCounts["mosque"]} للمساجد والمواقع"
                    AppLanguage.CKB -> "${visibleAdhan.size + visibleBundledAdhan.size} تۆمار پیشان دەدرێت • ${categoryCounts["muezzin"]} دەنگی موئەزین • ${categoryCounts["mosque"]} دەنگی مزگەوت و شوێن"
                },
                color = foreground.copy(alpha = 0.58f), fontSize = 10.sp, lineHeight = 14.sp
            )
            Text(
                when (language) {
                    AppLanguage.EN -> "${onlineAdhan.size} online source-linked recordings + ${bundledAdhan.size} original bundled tracks • bundled tracks work offline immediately"
                    AppLanguage.AR -> "${onlineAdhan.size} تسجيلًا إلكترونيًا من مصادرها + ${bundledAdhan.size} تسجيلًا أصليًا مضمّنًا • الملفات المضمّنة تعمل دون إنترنت فورًا"
                    AppLanguage.CKB -> "${onlineAdhan.size} تۆماری ئۆنلاین لە سەرچاوەی ڕەسەن + ${bundledAdhan.size} تۆماری ناو حەزمەی ڕەسەن • تۆمارە ناوخۆییەکان دەستبەجێ بەبێ ئینتەرنێت کار دەکەن"
                },
                color = foreground.copy(alpha = 0.64f), fontSize = 10.sp, lineHeight = 15.sp
            )
            Text(
                audioCatalogSourceNotice(language),
                color = foreground.copy(alpha = 0.48f), fontSize = 9.sp, lineHeight = 13.sp
            )
            Spacer(Modifier.height(4.dp))
            if (visibleAdhan.isEmpty() && visibleBundledAdhan.isEmpty()) {
                LoadingOrEmpty(loading, language, foreground, if (onlineAdhan.isEmpty() && bundledAdhan.isEmpty()) tr(language, "no_adhan_audio") else tr(language, "not_found"))
            } else LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (visibleBundledAdhan.isNotEmpty()) {
                    item(key = "bundled-adhan-heading") {
                        Text(
                            when (language) {
                                AppLanguage.EN -> "Included with the original app • ready offline"
                                AppLanguage.AR -> "مضمّن من ملفات التطبيق الأصلية • جاهز دون إنترنت"
                                AppLanguage.CKB -> "لەگەڵ فایلە ڕەسەنەکانی ئەپدا هەیە • ئامادەی بەبێ ئینتەرنێت"
                            },
                            color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                        )
                    }
                    itemsIndexed(visibleBundledAdhan, key = { _, it -> "bundled:${it.item.assetPath}" }) { itemIndex, track ->
                        val item = track.item
                        val playKey = "bundled-adhan:${item.assetPath}"
                        Card(
                            modifier = Modifier.fillMaxWidth()
                                .staggeredEntrance(itemKey = item.assetPath, index = itemIndex),
                            colors = CardDefaults.cardColors(containerColor = cardColor),
                            shape = RoundedCornerShape(17.dp),
                            border = BorderStroke(0.6.dp, Gold.copy(alpha = 0.14f))
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PremiumIconActionSurface(
                                    contentDescription = adhanAudioActionDescription(language, playing == playKey),
                                    accent = Gold,
                                    size = 40.dp,
                                    selected = playing == playKey,
                                    onClick = {
                                        if (playing == playKey || playing == "loading:$playKey") {
                                            AudioController.stop()
                                            playing = ""
                                        } else {
                                            playing = "loading:$playKey"
                                            AudioController.play(context, item.assetPath) { state ->
                                                playing = if (state) playKey else ""
                                            }
                                        }
                                    }
                                ) {
                                    when (playing) {
                                        playKey -> PremiumAudioWaveform(
                                            active = true,
                                            accent = Gold,
                                            contentDescription = adhanAudioActionDescription(language, true)
                                        )
                                        "loading:$playKey" -> CircularProgressIndicator(
                                            color = Gold, strokeWidth = 2.dp, modifier = Modifier.size(22.dp)
                                        )
                                        else -> Icon(
                                            Icons.Outlined.PlayCircle,
                                            contentDescription = adhanAudioActionDescription(language, false),
                                            tint = Gold, modifier = Modifier.size(27.dp)
                                        )
                                    }
                                }
                                Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
                                    Text(
                                        item.title, color = foreground, fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp, maxLines = 2, lineHeight = 16.sp
                                    )
                                    Text(
                                        when (track.category) {
                                            "mosque" -> when (language) {
                                                AppLanguage.EN -> "Bundled mosque/location recording"
                                                AppLanguage.AR -> "تسجيل مسجد/موقع من ملفات المصدر"
                                                AppLanguage.CKB -> "تۆماری مزگەوت/شوێن لە فایلەکانی سەرچاوە"
                                            }
                                            else -> when (language) {
                                                AppLanguage.EN -> "Bundled muezzin/adhan recording"
                                                AppLanguage.AR -> "تسجيل مؤذن/أذان من ملفات المصدر"
                                                AppLanguage.CKB -> "تۆماری موئەزین/بانگ لە فایلەکانی سەرچاوە"
                                            }
                                        },
                                        color = foreground.copy(alpha = 0.56f), fontSize = 9.sp, lineHeight = 13.sp
                                    )
                                    Text(
                                        item.assetPath.substringAfterLast('/'),
                                        color = foreground.copy(alpha = 0.45f), fontSize = 9.sp, maxLines = 1
                                    )
                                    Text(
                                        when (language) {
                                            AppLanguage.EN -> "Part of the original app bundle • plays offline; no download needed"
                                            AppLanguage.AR -> "من حزمة التطبيق الأصلية • يعمل دون إنترنت ولا يحتاج تنزيلًا"
                                            AppLanguage.CKB -> "بەشێکە لە حەزمەی ڕەسەنی ئەپ • بەبێ ئینتەرنێت کار دەکات"
                                        },
                                        color = Gold, fontSize = 9.sp, lineHeight = 13.sp
                                    )
                                }
                                Icon(
                                    Icons.Outlined.CheckCircle,
                                    contentDescription = if (language == AppLanguage.EN) "Available offline" else if (language == AppLanguage.AR) "متاح دون إنترنت" else "بەبێ ئینتەرنێت بەردەستە",
                                    tint = Color(0xFF62C995), modifier = Modifier.size(23.dp)
                                )
                            }
                        }
                    }
                }
                itemsIndexed(visibleAdhan, key = { _, it -> "online:${it.id}" }) { itemIndex, item ->
                    val file = onlineAdhanOfflineFile(context, item)
                    val localSaved = remember(item.id, offlineAdhanRevision) { file.isFile && file.length() > 512L }
                    val downloadProgress = downloadingAdhan[item.id]
                    val playKey = "online-adhan-${item.id}"
                    val title = if (language == AppLanguage.EN) item.titleEnglish else item.titleArabic.ifBlank { item.titleEnglish }
                    val categoryLabel = when (item.category) {
                        "mosque" -> when (language) {
                            AppLanguage.EN -> "Mosque / location adhan"
                            AppLanguage.AR -> "أذان مسجد / موقع"
                            AppLanguage.CKB -> "بانگی مزگەوت / شوێن"
                        }
                        else -> when (language) {
                            AppLanguage.EN -> "Muezzin recording"
                            AppLanguage.AR -> "تسجيل مؤذن"
                            AppLanguage.CKB -> "تۆماری موئەزین"
                        }
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth()
                            .staggeredEntrance(itemKey = item.id, index = itemIndex),
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        shape = RoundedCornerShape(17.dp),
                        border = BorderStroke(0.6.dp, Gold.copy(alpha = 0.14f))
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PremiumIconActionSurface(
                                contentDescription = adhanAudioActionDescription(language, playing == playKey),
                                accent = Gold,
                                size = 40.dp,
                                selected = playing == playKey,
                                onClick = {
                                    if (playing == playKey || playing == "loading:$playKey") {
                                        AudioController.stop()
                                        playing = ""
                                    } else {
                                        val source = if (localSaved) "local:${file.absolutePath}" else item.url
                                        playing = "loading:$playKey"
                                        AudioController.playSequence(
                                            context,
                                            listOf(source),
                                            failure = if (localSaved) AudioPlaybackFailure.LOCAL_ASSET else AudioPlaybackFailure.STREAM
                                        ) { state ->
                                            playing = if (state) playKey else ""
                                        }
                                    }
                                }
                            ) {
                                when (playing) {
                                    playKey -> PremiumAudioWaveform(
                                        active = true,
                                        accent = Gold,
                                        contentDescription = adhanAudioActionDescription(language, true)
                                    )
                                    "loading:$playKey" -> CircularProgressIndicator(
                                        color = Gold, strokeWidth = 2.dp, modifier = Modifier.size(22.dp)
                                    )
                                    else -> Icon(
                                        Icons.Outlined.PlayCircle,
                                        contentDescription = adhanAudioActionDescription(language, false),
                                        tint = Gold, modifier = Modifier.size(27.dp)
                                    )
                                }
                            }
                            Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
                                Text(title, color = foreground, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 2, lineHeight = 16.sp)
                                if (item.titleArabic.isNotBlank() && item.titleEnglish.isNotBlank() && item.titleArabic != item.titleEnglish) {
                                    Text(
                                        if (language == AppLanguage.EN) item.titleArabic else item.titleEnglish,
                                        color = foreground.copy(alpha = 0.58f), fontSize = 9.sp, maxLines = 2, lineHeight = 12.sp
                                    )
                                }
                                Text(
                                    "$categoryLabel • ${formatAudioFileSize(item.sizeBytes)}",
                                    color = foreground.copy(alpha = 0.55f), fontSize = 9.sp, maxLines = 2
                                )
                                Text(
                                    when {
                                        downloadProgress != null -> if (language == AppLanguage.EN) "Downloading for offline use…" else if (language == AppLanguage.AR) "جارٍ التنزيل للاستماع دون إنترنت…" else "بەبێ ئینتەرنێت دادەگیرێت…"
                                        localSaved -> if (language == AppLanguage.EN) "Saved on this device • offline ready" else if (language == AppLanguage.AR) "محفوظ على الجهاز • جاهز دون إنترنت" else "لە مۆبایل پارێزراوە • ئامادەی بەبێ ئینتەرنێت"
                                        else -> if (language == AppLanguage.EN) "Stream online or download for offline playback" else if (language == AppLanguage.AR) "استمع عبر الإنترنت أو نزّله للاستماع دون اتصال" else "بە ئۆنلاین گوێی لێ بگرە یان بۆ بەبێ ئینتەرنێت دایبگرە"
                                    },
                                    color = if (downloadProgress != null || localSaved) Gold else foreground.copy(alpha = 0.56f),
                                    fontSize = 9.sp, lineHeight = 13.sp, maxLines = 2
                                )
                            }
                            when {
                                downloadProgress != null -> CircularProgressIndicator(
                                    modifier = Modifier.padding(horizontal = 8.dp).size(21.dp),
                                    color = Gold, strokeWidth = 2.dp
                                )
                                localSaved -> IconButton(onClick = {
                                    if (playing == playKey) {
                                        AudioController.stop()
                                        playing = ""
                                    }
                                    file.delete()
                                    offlineAdhanRevision++
                                    audioNotice = if (language == AppLanguage.EN) "Offline copy removed; the online recording is still available."
                                        else if (language == AppLanguage.AR) "أزيلت النسخة المحفوظة؛ ما زال التسجيل متاحًا عبر الإنترنت."
                                        else "وەشانی بەبێ ئینتەرنێت لابرا؛ تۆمارە ئۆنلاینەکە هێشتا بەردەستە."
                                }) {
                                    Icon(Icons.Outlined.CheckCircle, contentDescription = if (language == AppLanguage.EN) "Remove offline copy" else if (language == AppLanguage.AR) "حذف النسخة المحفوظة" else "لابردنی وەشانی پارێزراو", tint = Color(0xFF62C995), modifier = Modifier.size(24.dp))
                                }
                                else -> IconButton(onClick = {
                                    downloadingAdhan = downloadingAdhan + (item.id to "downloading")
                                    audioNotice = ""
                                    scope.launch {
                                        try {
                                            downloadOnlineAdhan(context, item)
                                            offlineAdhanRevision++
                                            audioNotice = if (language == AppLanguage.EN) "Saved ${item.titleEnglish} for offline playback."
                                                else if (language == AppLanguage.AR) "تم حفظ ${item.titleArabic} للتشغيل دون إنترنت."
                                                else "تۆماری ${item.titleArabic} بۆ بەکارهێنانی بەبێ ئینتەرنێت پارێزرا."
                                        } catch (error: Exception) {
                                            audioNotice = when (language) {
                                                AppLanguage.EN -> "Download failed. Check the connection and try again."
                                                AppLanguage.AR -> "فشل التنزيل. تحقّق من الاتصال ثم حاول مجددًا."
                                                AppLanguage.CKB -> "داگرتن سەرکەوتوو نەبوو؛ پەیوەندی بپشکنە و دووبارە هەوڵ بدەوە."
                                            }
                                        } finally {
                                            downloadingAdhan = downloadingAdhan - item.id
                                        }
                                    }
                                }) {
                                    Icon(Icons.Outlined.Download, contentDescription = if (language == AppLanguage.EN) "Download adhan" else if (language == AppLanguage.AR) "تنزيل الأذان" else "داگرتنی بانگ", tint = Gold, modifier = Modifier.size(23.dp))
                                }
                            }
                        }
                    }
                }
            }
        } else if (selectedTab == 0) {
            val visibleAudio = audio.filter { item ->
                val name = (item.title + " " + item.assetPath + " " + item.group).lowercase(Locale.ROOT)
                audioQuery.isBlank() || name.contains(audioQuery.trim().lowercase(Locale.ROOT))
            }
            if (visibleAudio.isEmpty()) LoadingOrEmpty(loading, language, foreground, tr(language, "no_data"))
            else LazyColumn(contentPadding = PaddingValues(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                itemsIndexed(visibleAudio, key = { _, it -> it.assetPath }) { itemIndex, item ->
                    val trackIsPlaying = playing == item.assetPath
                    val trackBorder by animateColorAsState(
                        targetValue = if (trackIsPlaying) Gold.copy(alpha = 0.72f) else foreground.copy(alpha = 0.09f),
                        animationSpec = tween(230, easing = FastOutSlowInEasing),
                        label = "Local audio active track border"
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth()
                            .staggeredEntrance(itemKey = item.assetPath, index = itemIndex),
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, trackBorder)
                    ) {
                        Row(Modifier.fillMaxWidth()
                            .background(Brush.horizontalGradient(listOf(Gold.copy(alpha = if (trackIsPlaying) 0.10f else 0.025f), cardColor)))
                            .premiumClickable {
                            if (playing == item.assetPath) {
                                AudioController.stop()
                                playing = ""
                            } else {
                                playing = item.assetPath
                                AudioController.play(context, item.assetPath) { state -> if (!state) playing = "" }
                            }
                        }.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                            AnimatedContent(
                                targetState = trackIsPlaying,
                                transitionSpec = {
                                    (scaleIn(initialScale = 0.72f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)) + fadeIn(tween(130))) togetherWith
                                        (scaleOut(targetScale = 0.65f, animationSpec = tween(100)) + fadeOut(tween(100)))
                                },
                                label = "Audio play pause medallion"
                            ) { active ->
                                PremiumIconMedallion(
                                    icon = if (active) Icons.Outlined.PauseCircle else Icons.Outlined.PlayCircle,
                                    tint = Gold,
                                    size = 40.dp,
                                    pulse = active
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(item.title, color = foreground, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(item.group, color = foreground.copy(alpha = 0.48f), fontSize = 10.sp)
                            }
                            PremiumAudioWaveform(
                                active = trackIsPlaying,
                                accent = if (trackIsPlaying) Gold else foreground.copy(alpha = 0.52f)
                            )
                        }
                    }
                }
            }
        } else {
            val visibleReciters = filterRecitersForPlaybackMode(reciters, playbackMode, audioQuery)
            Text(
                if (language == AppLanguage.EN) {
                    if (preferredReciter != null) "Your saved reciter will be used when you open audio playback from a Quran verse."
                    else "Streaming needs internet. Choose a verse-by-verse reciter below; it will be remembered and the Quran play button can use it next time."
                } else if (language == AppLanguage.AR) {
                    if (preferredReciter != null) "سيُستخدم القارئ الذي اخترته عند فتح التشغيل الصوتي من آية في القرآن."
                    else "البث يحتاج إلى الإنترنت. اختر قارئًا يدعم تشغيل الآيات أدناه؛ سيُحفظ اختيارك لزر تشغيل الآية لاحقًا."
                } else {
                    if (preferredReciter != null) "کاتێک لە ئایەتێکی قورئانەوە پەخش دەست پێدەکەیت، ئەو قورئان‌خوێنەی هەڵتبژاردووە بەکاردێت."
                    else "بۆ پەخش ئینتەرنێت پێویستە. قورئان‌خوێنێک هەڵبژێرە کە خوێندنەوەی ئایەت بە ئایەت پشتیوانی بکات؛ دواتر دوگمەی گوێگرتن ئەوە بەکاردهێنێت."
                },
                color = foreground.copy(alpha = 0.62f), fontSize = 11.sp, lineHeight = 17.sp
            )
            Spacer(Modifier.height(7.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = surahText,
                    onValueChange = { surahText = it.filter(Char::isDigit).take(3) },
                    modifier = Modifier.weight(1f),
                    label = { Text(tr(language, "surah")) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = ayahText,
                    onValueChange = { ayahText = it.filter(Char::isDigit).take(3) },
                    modifier = Modifier.weight(1f),
                    label = { Text(tr(language, "ayah")) },
                    singleLine = true
                )
            }
            val allSurahEditionCount = reciters.count { ReaderPlaybackSettings.supportsAllSurahs(it) }
            val partialOnlineEditionCount = reciters.count {
                it.sourceId.isNotBlank() && !ReaderPlaybackSettings.supportsAllSurahs(it)
            }
            Text(
                when (language) {
                    AppLanguage.EN -> "${reciters.size} editions • ${allSurahEditionCount} listed with all 114 surahs • ${partialOnlineEditionCount} partial online editions are kept out of Full surah mode"
                    AppLanguage.AR -> "${reciters.size} إصدارًا • ${allSurahEditionCount} بتغطية 114 سورة • تُستبعد ${partialOnlineEditionCount} إصدارات جزئية من وضع السورة كاملة"
                    AppLanguage.CKB -> "${reciters.size} وەشان • ${allSurahEditionCount} بە تەواوی ١١٤ سوورەت • ${partialOnlineEditionCount} وەشانی بەشەکی لە شێوازی سوورەتی تەواودا پیشان نادرێن"
                },
                color = foreground.copy(alpha = 0.5f), fontSize = 9.sp, lineHeight = 13.sp
            )
            Spacer(Modifier.height(7.dp))
            Text(
                if (language == AppLanguage.EN) "PLAYBACK MODE" else if (language == AppLanguage.AR) "طريقة التشغيل" else "شێوازی لێدان",
                color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                PremiumChoicePill(
                    modifier = Modifier.weight(1f),
                    label = if (language == AppLanguage.EN) "Full surah" else if (language == AppLanguage.AR) "السورة كاملة" else "سوورەتی تەواو",
                    selected = playbackMode == "full",
                    accent = Gold,
                    foreground = foreground
                ) {
                    ReaderPlaybackSettings.mode = "full"
                    prefs.edit().putString("reciter_playback_mode", "full").apply()
                    AudioController.stop()
                    playing = ""
                    autoPlayKey = ""
                }
                PremiumChoicePill(
                    modifier = Modifier.weight(1f),
                    label = if (language == AppLanguage.EN) "From selected ayah" else if (language == AppLanguage.AR) "من الآية المحددة" else "لە ئایەتی هەڵبژێردراوەوە",
                    selected = playbackMode == "verse",
                    accent = Gold,
                    foreground = foreground
                ) {
                    ReaderPlaybackSettings.mode = "verse"
                    prefs.edit().putString("reciter_playback_mode", "verse").apply()
                    AudioController.stop()
                    playing = ""
                    autoPlayKey = ""
                }
            }
            Text(
                when {
                    playbackMode == "full" && language == AppLanguage.EN -> "Starts at the beginning; verse-by-verse recordings are played in sequence."
                    playbackMode == "full" && language == AppLanguage.AR -> "يبدأ من أول السورة؛ وتُشغَّل التسجيلات المنفصلة للآيات بالتتابع."
                    playbackMode == "full" -> "لە سەرەتای سوورەتەوە دەست پێدەکات؛ تۆمارەکانی ئایەتەکان بە ڕیز لێدەدرێن."
                    language == AppLanguage.EN -> "Starts at the chosen ayah and continues to the end; only recordings with separate ayah files are shown."
                    language == AppLanguage.AR -> "يبدأ من الآية المختارة حتى نهاية السورة؛ وتُعرض التسجيلات ذات الملفات المنفصلة للآيات فقط."
                    else -> "لە ئایەتی هەڵبژێردراوەوە تا کۆتایی سوورەت دەڕوات؛ تەنها سەرچاوەکانی فایلە جیاوازەکانی ئایەت پیشان دەدرێن."
                },
                color = foreground.copy(alpha = 0.58f), fontSize = 10.sp, lineHeight = 15.sp
            )
            Spacer(Modifier.height(7.dp))
            if (reciters.isEmpty()) LoadingOrEmpty(loading, language, foreground)
            else if (visibleReciters.isEmpty()) {
                val emptyMessage = when {
                    audioQuery.isNotBlank() -> tr(language, "not_found")
                    playbackMode == "full" && language == AppLanguage.EN ->
                        "No complete 114-surah edition matches the current catalogue. Clear search or switch to From selected ayah."
                    playbackMode == "full" && language == AppLanguage.AR ->
                        "لا يوجد إصدار كامل لـ114 سورة ضمن النتائج الحالية. امسح البحث أو بدّل إلى التشغيل من الآية المحددة."
                    playbackMode == "full" ->
                        "لە کاتەلۆگی ئێستادا وەشانی تەواوی ١١٤ سوورەت نەدۆزرایەوە؛ گەڕان پاک بکەرەوە یان بگۆڕە بۆ لێدان لە ئایەتی هەڵبژێردراوەوە."
                    language == AppLanguage.EN -> "No verse-by-verse reciter is available in this catalogue."
                    language == AppLanguage.AR -> "لا يوجد قارئ بملفات آيات منفصلة في هذا الكتالوج."
                    else -> "لەو کاتەلۆگەدا قورئان‌خوێنی فایلە جیاوازەکانی ئایەت بەردەست نییە."
                }
                LoadingOrEmpty(false, language, foreground, emptyMessage)
            } else LazyColumn(contentPadding = PaddingValues(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(visibleReciters, key = { _, it -> it.id }) { itemIndex, reciter ->
                    val surahNumber = surahText.toIntOrNull()?.coerceIn(1, 114) ?: 1
                    val surahAyahCount = surahs.firstOrNull { it.number == surahNumber }?.ayahCount ?: 0
                    val requestedAyah = ayahText.toIntOrNull() ?: 1
                    val startingAyah = resolveReciterStartAyah(playbackMode, requestedAyah, surahAyahCount)
                    val playKey = "${reciter.id}|${surahNumber}|${startingAyah}|${playbackMode}"
                    val downloadKey = "${reciter.id}|${surahNumber}"
                    val downloadProgress = downloadingTracks[downloadKey]
                    val localComplete = remember(reciter.id, reciter.audioType, surahNumber, surahAyahCount, offlineRevision) {
                        isReciterSurahOffline(context, reciter, surahNumber, surahAyahCount)
                    }
                    val sources = remember(reciter.id, reciter.audioType, surahNumber, startingAyah, surahAyahCount, offlineRevision) {
                        reciterAudioSequence(context, reciter, surahNumber, startingAyah, surahAyahCount)
                    }
                    val playLabel = if (playbackMode == "full") {
                        if (language == AppLanguage.EN) "Play full surah"
                        else if (language == AppLanguage.AR) "تشغيل السورة كاملة"
                        else "خوێندنەوەی تەواوی سوورەت"
                    } else {
                        if (language == AppLanguage.EN) "Play from verse $startingAyah to end of surah"
                        else if (language == AppLanguage.AR) "تشغيل من الآية $startingAyah حتى نهاية السورة"
                        else "لە ئایەتی $startingAyah تا کۆتایی سوورەت بخوێنەوە"
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth()
                            .staggeredEntrance(itemKey = reciter.id, index = itemIndex),
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        shape = RoundedCornerShape(17.dp)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(38.dp).clip(RoundedCornerShape(13.dp)).background(DeepGreen), contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.GraphicEq, null, tint = Gold, modifier = Modifier.size(22.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(reciter.nameArabic, color = foreground, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text(reciter.nameEnglish, color = foreground.copy(alpha = 0.58f), fontSize = 10.sp, maxLines = 1)
                                if (reciter.recitation.isNotBlank()) {
                                    Text(reciter.recitation, color = foreground.copy(alpha = 0.5f), fontSize = 8.sp, maxLines = 2, lineHeight = 11.sp)
                                }
                                Text(
                                    when {
                                        surahNumber > reciter.surahTotal && language == AppLanguage.EN -> "This edition contains only ${reciter.surahTotal} surahs; this one is unavailable."
                                        surahNumber > reciter.surahTotal && language == AppLanguage.AR -> "هذه الطبعة تضم ${reciter.surahTotal} سورة فقط؛ السورة المختارة غير متاحة."
                                        surahNumber > reciter.surahTotal -> "ئەم وەشانە تەنها ${reciter.surahTotal} سوورەتی هەیە؛ ئەم سوورەتە بەردەست نییە."
                                        reciter.sourceId.isNotBlank() && language == AppLanguage.EN -> "MP3Quran.net • ${reciter.surahTotal}/114 surahs"
                                        reciter.sourceId.isNotBlank() && language == AppLanguage.AR -> "MP3Quran.net • ${reciter.surahTotal}/114 سورة"
                                        reciter.sourceId.isNotBlank() -> "MP3Quran.net • ${reciter.surahTotal}/114 سوورەت"
                                        playbackMode == "full" && reciter.audioType == 0 && language == AppLanguage.EN -> "Full surah • sequential ayah playback"
                                        playbackMode == "full" && reciter.audioType == 0 && language == AppLanguage.AR -> "السورة كاملة • تشغيل الآيات بالتتابع"
                                        playbackMode == "full" && reciter.audioType == 0 -> "سوورەتی تەواو • لێدانی یەک لە دوای یەکی ئایەتەکان"
                                        playbackMode == "verse" && language == AppLanguage.EN -> "Verse-by-verse • starts at selected ayah"
                                        playbackMode == "verse" && language == AppLanguage.AR -> "آية بآية • يبدأ من الآية المحددة"
                                        playbackMode == "verse" -> "ئایەت بە ئایەت • دەستپێک لە ئایەتی هەڵبژێردراو"
                                        language == AppLanguage.EN -> "Full-surah recording"
                                        language == AppLanguage.AR -> "تسجيل السورة كاملة"
                                        else -> "تۆماری تەواوی سوورەت"
                                    },
                                    color = Gold.copy(alpha = 0.85f), fontSize = 9.sp
                                )
                                TextButton(
                                    onClick = {
                                        preferredReciterId = reciter.id
                                        prefs.edit().putInt("preferred_reciter_id", reciter.id).apply()
                                    },
                                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp),
                                    modifier = Modifier.height(25.dp)
                                ) {
                                    Text(
                                        if (preferredReciterId == reciter.id) {
                                            if (language == AppLanguage.EN) "★ Preferred reciter" else if (language == AppLanguage.AR) "★ القارئ المفضّل" else "★ قورئان‌خوێنی دڵخواز"
                                        } else {
                                            if (language == AppLanguage.EN) "☆ Set as preferred" else if (language == AppLanguage.AR) "☆ تعيين كمفضّل" else "☆ دڵخواز دیاری بکە"
                                        },
                                        color = if (preferredReciterId == reciter.id) Gold else foreground.copy(alpha = 0.58f),
                                        fontSize = 9.sp, maxLines = 1
                                    )
                                }
                                Text(
                                    when {
                                        downloadProgress != null -> if (language == AppLanguage.EN) "Downloading audio $downloadProgress" else if (language == AppLanguage.AR) "جارٍ تنزيل الصوت $downloadProgress" else "دەنگ دادەگیرێت $downloadProgress"
                                        localComplete -> if (language == AppLanguage.EN) "Saved offline • full surah" else if (language == AppLanguage.AR) "محفوظ دون اتصال • السورة كاملة" else "بەبێ ئینتەرنێت پارێزراوە • سوورەتی تەواو"
                                        else -> if (language == AppLanguage.EN) "Download for offline playback" else if (language == AppLanguage.AR) "تنزيل للتشغيل دون إنترنت" else "داگرتن بۆ لێدان بەبێ ئینتەرنێت"
                                    },
                                    color = if (downloadProgress != null || localComplete) Gold else foreground.copy(alpha = 0.55f),
                                    fontSize = 9.sp, maxLines = 2
                                )
                            }
                            PremiumIconActionSurface(
                                contentDescription = playLabel,
                                accent = Gold,
                                size = 40.dp,
                                enabled = sources.isNotEmpty() && surahNumber <= reciter.surahTotal,
                                selected = playing == playKey,
                                onClick = {
                                    if (playing == playKey) {
                                        AudioController.stop()
                                        playing = ""
                                    } else {
                                        preferredReciterId = reciter.id
                                        prefs.edit().putInt("preferred_reciter_id", reciter.id).apply()
                                        playing = playKey
                                        autoPlayKey = playKey
                                        AudioController.playSequence(context, sources) { state -> if (!state) playing = "" }
                                    }
                                }
                            ) {
                                AnimatedContent(
                                    targetState = playing == playKey,
                                    transitionSpec = {
                                        (scaleIn(initialScale = 0.72f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)) + fadeIn(tween(130))) togetherWith
                                            (scaleOut(targetScale = 0.65f, animationSpec = tween(100)) + fadeOut(tween(100)))
                                    },
                                    label = "Reciter active audio waveform transition"
                                ) { active ->
                                    if (active) {
                                        PremiumAudioWaveform(active = true, accent = Gold, contentDescription = playLabel)
                                    } else {
                                        Icon(
                                            Icons.Outlined.PlayCircle,
                                            contentDescription = playLabel,
                                            tint = Gold,
                                            modifier = Modifier.size(27.dp)
                                        )
                                    }
                                }
                            }
                            IconButton(
                                enabled = downloadProgress == null && surahAyahCount > 0 && surahNumber <= reciter.surahTotal,
                                onClick = {
                                    if (localComplete) {
                                        audioNotice = if (language == AppLanguage.EN) "This surah is already saved for offline playback."
                                            else if (language == AppLanguage.AR) "هذه السورة محفوظة بالفعل للتشغيل دون إنترنت."
                                            else "ئەم سوورەتە پێشتر بۆ لێدانی بەبێ ئینتەرنێت پارێزراوە."
                                    } else {
                                        audioNotice = ""
                                        downloadingTracks = downloadingTracks + (downloadKey to "0")
                                        scope.launch {
                                            try {
                                                downloadReciterSurah(context, reciter, surahNumber, surahAyahCount) { done, total ->
                                                    downloadingTracks = downloadingTracks + (downloadKey to "$done/$total")
                                                }
                                                offlineRevision++
                                                audioNotice = if (language == AppLanguage.EN) "${reciter.nameEnglish}: surah ${surahNumber} is saved for offline playback."
                                                    else if (language == AppLanguage.AR) "تم حفظ السورة ${surahNumber} من ${reciter.nameArabic} للتشغيل دون إنترنت."
                                                    else "سوورەتی ${surahNumber} بە دەنگی ${reciter.nameArabic} بۆ لێدانی بەبێ ئینتەرنێت پارێزرا."
                                            } catch (_: Exception) {
                                                audioNotice = if (language == AppLanguage.EN) "Download failed or the source does not provide every audio file. Check the connection and try again."
                                                    else if (language == AppLanguage.AR) "تعذّر التنزيل أو أن المصدر لا يوفر كل المقاطع. تحقّق من الاتصال ثم حاول مجددًا."
                                                    else "داگرتن سەرکەوتوو نەبوو یان هەموو فایلەکان لە سەرچاوەکەدا نیین؛ پەیوەندی بپشکنە و دووبارە هەوڵ بدەوە."
                                            } finally {
                                                downloadingTracks = downloadingTracks - downloadKey
                                            }
                                        }
                                    }
                                }
                            ) {
                                when {
                                    downloadProgress != null -> CircularProgressIndicator(modifier = Modifier.size(21.dp), color = Gold, strokeWidth = 2.dp)
                                    localComplete -> Icon(Icons.Outlined.CheckCircle, contentDescription = if (language == AppLanguage.EN) "Saved offline" else if (language == AppLanguage.AR) "محفوظ دون اتصال" else "بەبێ ئینتەرنێت پارێزراوە", tint = Color(0xFF62C995), modifier = Modifier.size(24.dp))
                                    else -> Icon(Icons.Outlined.Download, contentDescription = if (language == AppLanguage.EN) "Download full surah" else if (language == AppLanguage.AR) "تنزيل السورة كاملة" else "داگرتنی تەواوی سوورەت", tint = Gold, modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


internal enum class AudioPlaybackFailure {
    RECITATION_SEGMENT,
    LOCAL_ASSET,
    STREAM
}

internal data class AudioSequenceFailureDecision(
    val nextIndex: Int?,
    val showFailureMessage: Boolean
)

/** Skip an unavailable verse recording so one missing ayah does not stop a full surah. */
internal fun audioSequenceFailureDecision(
    currentIndex: Int,
    totalTracks: Int,
    hasStartedPlayback: Boolean
): AudioSequenceFailureDecision {
    val next = (currentIndex + 1).takeIf { it in 0 until totalTracks }
    return AudioSequenceFailureDecision(
        nextIndex = next,
        showFailureMessage = next == null && !hasStartedPlayback
    )
}

internal fun adhanAudioActionDescription(language: AppLanguage, isPlaying: Boolean): String = when (language) {
    AppLanguage.EN -> if (isPlaying) "Pause adhan" else "Play adhan"
    AppLanguage.AR -> if (isPlaying) "إيقاف الأذان مؤقتًا" else "تشغيل الأذان"
    AppLanguage.CKB -> if (isPlaying) "وەستاندنی کاتی بانگ" else "لێدانی بانگ"
}

internal fun audioCatalogSourceNotice(language: AppLanguage): String = when (language) {
    AppLanguage.EN -> "Source catalog: Kiwifu/adhan-mp3 • recordings stay online until you choose Download."
    AppLanguage.AR -> "كتالوج المصدر: Kiwifu/adhan-mp3 • تبقى التسجيلات عبر الإنترنت حتى تختار تنزيلها."
    AppLanguage.CKB -> "کاتەلۆگی سەرچاوە: Kiwifu/adhan-mp3 • تۆمارەکان ئۆنلاین دەمێننەوە تا داگرتن هەڵدەبژێریت."
}

internal fun audioPlaybackFailureMessage(language: AppLanguage, failure: AudioPlaybackFailure): String = when (language) {
    AppLanguage.EN -> when (failure) {
        AudioPlaybackFailure.RECITATION_SEGMENT -> "A recitation segment could not be played. Check the connection or download the surah."
        AudioPlaybackFailure.LOCAL_ASSET -> "The audio file could not be opened. Try another recording."
        AudioPlaybackFailure.STREAM -> "The audio stream is unavailable. Check the connection and retry."
    }
    AppLanguage.AR -> when (failure) {
        AudioPlaybackFailure.RECITATION_SEGMENT -> "تعذّر تشغيل مقطع من التلاوة؛ تحقّق من الاتصال أو نزّل السورة."
        AudioPlaybackFailure.LOCAL_ASSET -> "تعذّر فتح الملف الصوتي؛ جرّب تسجيلاً آخر."
        AudioPlaybackFailure.STREAM -> "تعذّر بث الملف الصوتي؛ تحقق من الاتصال ثم أعد المحاولة."
    }
    AppLanguage.CKB -> when (failure) {
        AudioPlaybackFailure.RECITATION_SEGMENT -> "نەتوانرا بەشێک لە خوێندنەوەکە پەخش بکرێت؛ پەیوەندی بپشکنە یان سوورەتەکە دابگرە."
        AudioPlaybackFailure.LOCAL_ASSET -> "نەتوانرا فایلە دەنگییەکە بکرێتەوە؛ تۆمارێکی تر تاقی بکەرەوە."
        AudioPlaybackFailure.STREAM -> "پەخشکردنی فایلە دەنگییەکە بەردەست نییە؛ پەیوەندی بپشکنە و دووبارە هەوڵ بدەرەوە."
    }
}

internal fun audioSequencePartialPlaybackMessage(language: AppLanguage, unavailableTrackCount: Int): String {
    val count = unavailableTrackCount.coerceAtLeast(0)
    return when (language) {
        AppLanguage.EN -> "Playback continued, but $count audio segment(s) were unavailable. This recitation may be incomplete; check the connection or download the surah."
        AppLanguage.AR -> "استمر التشغيل، لكن تعذّر تشغيل $count مقطعًا صوتيًا؛ قد تكون التلاوة غير مكتملة. تحقّق من الاتصال أو نزّل السورة."
        AppLanguage.CKB -> "لێدانەکە بەردەوام بوو، بەڵام $count بەشی دەنگی بەردەست نەبوون؛ لەوانەیە خوێندنەوەکە ناتەواو بێت. پەیوەندی بپشکنە یان سوورەتەکە دابگرە."
    }
}

private object AudioController {
    private var player: MediaPlayer? = null
    private var playbackGeneration: Int = 0

    fun stop() {
        playbackGeneration += 1
        try { player?.stop() } catch (_: Exception) {}
        try { player?.release() } catch (_: Exception) {}
        player = null
    }

    fun playSequence(
        context: Context,
        sources: List<String>,
        failure: AudioPlaybackFailure = AudioPlaybackFailure.RECITATION_SEGMENT,
        callback: (Boolean) -> Unit
    ) {
        stop()
        if (sources.isEmpty()) {
            callback(false)
            return
        }
        val generation = playbackGeneration
        var hasStartedPlayback = false
        var unavailableTrackCount = 0
        lateinit var playAt: (Int) -> Unit

        fun notifyPartialSequenceIfNeeded() {
            if (hasStartedPlayback && unavailableTrackCount > 0 && generation == playbackGeneration) {
                Toast.makeText(
                    context,
                    audioSequencePartialPlaybackMessage(
                        AppLanguage.from(languageHint(context)),
                        unavailableTrackCount
                    ),
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        fun handleTrackFailure(index: Int) {
            if (generation != playbackGeneration) return
            unavailableTrackCount += 1
            val decision = audioSequenceFailureDecision(index, sources.size, hasStartedPlayback)
            val nextIndex = decision.nextIndex
            if (nextIndex != null) {
                // Defer the retry to avoid deep synchronous recursion if several files fail quickly.
                Handler(Looper.getMainLooper()).post { playAt(nextIndex) }
                return
            }
            if (decision.showFailureMessage) {
                Toast.makeText(
                    context,
                    audioPlaybackFailureMessage(AppLanguage.from(languageHint(context)), failure),
                    Toast.LENGTH_LONG
                ).show()
            } else {
                notifyPartialSequenceIfNeeded()
            }
            callback(false)
            stop()
        }

        playAt = fun(index: Int) {
            if (generation != playbackGeneration) return
            if (index >= sources.size) {
                notifyPartialSequenceIfNeeded()
                callback(false)
                return
            }
            var currentPlayer: MediaPlayer? = null
            try {
                val source = sources[index]
                val next = MediaPlayer()
                currentPlayer = next
                player = next
                if (source.startsWith("local:")) next.setDataSource(source.removePrefix("local:"))
                else next.setDataSource(source)
                next.setOnPreparedListener { prepared ->
                    if (generation != playbackGeneration) {
                        runCatching { prepared.release() }
                        return@setOnPreparedListener
                    }
                    try {
                        prepared.start()
                        if (!hasStartedPlayback) {
                            hasStartedPlayback = true
                            callback(true)
                        }
                    } catch (_: Exception) {
                        runCatching { prepared.release() }
                        if (player === prepared) player = null
                        handleTrackFailure(index)
                    }
                }
                next.setOnCompletionListener { completed ->
                    if (generation != playbackGeneration) {
                        runCatching { completed.release() }
                        return@setOnCompletionListener
                    }
                    runCatching { completed.release() }
                    if (player === completed) player = null
                    playAt(index + 1)
                }
                next.setOnErrorListener { failed, _, _ ->
                    runCatching { failed.release() }
                    if (generation == playbackGeneration) {
                        if (player === failed) player = null
                        handleTrackFailure(index)
                    }
                    true
                }
                next.prepareAsync()
            } catch (_: Exception) {
                if (generation == playbackGeneration) {
                    runCatching { currentPlayer?.release() }
                    if (player === currentPlayer) player = null
                    handleTrackFailure(index)
                }
            }
        }
        playAt(0)
    }

    fun play(context: Context, assetPath: String, callback: (Boolean) -> Unit) {
        stop()
        try {
            val hash = MessageDigest.getInstance("SHA-256").digest(assetPath.toByteArray()).take(8).joinToString("") { "%02x".format(it) }
            val copy = File(context.cacheDir, "audio-" + hash + "." + assetPath.substringAfterLast('.', "mp3"))
            context.assets.open(assetPath).use { input -> copy.outputStream().use { input.copyTo(it) } }
            player = MediaPlayer().apply {
                setDataSource(copy.absolutePath)
                setOnPreparedListener { it.start(); callback(true) }
                setOnCompletionListener { callback(false); stop() }
                setOnErrorListener { _, _, _ -> callback(false); stop(); true }
                prepareAsync()
            }
        } catch (_: Exception) {
            callback(false)
            Toast.makeText(context, audioPlaybackFailureMessage(AppLanguage.from(languageHint(context)), AudioPlaybackFailure.LOCAL_ASSET), Toast.LENGTH_SHORT).show()
        }
    }

    fun playRemote(context: Context, url: String, callback: (Boolean) -> Unit) {
        stop()
        try {
            player = MediaPlayer().apply {
                setDataSource(url)
                setOnPreparedListener { it.start(); callback(true) }
                setOnCompletionListener { callback(false); stop() }
                setOnErrorListener { _, _, _ ->
                    callback(false)
                    Toast.makeText(context, audioPlaybackFailureMessage(AppLanguage.from(languageHint(context)), AudioPlaybackFailure.STREAM), Toast.LENGTH_LONG).show()
                    stop()
                    true
                }
                prepareAsync()
            }
        } catch (_: Exception) {
            callback(false)
            Toast.makeText(context, audioPlaybackFailureMessage(AppLanguage.from(languageHint(context)), AudioPlaybackFailure.STREAM), Toast.LENGTH_LONG).show()
        }
    }

    private fun languageHint(context: Context): String =
        context.getSharedPreferences("mashxall_preferences", Context.MODE_PRIVATE).getString("language", "ckb") ?: "ckb"
}

private val MASHXALL_BACKUP_PREFS = listOf(
    "mashxall_preferences",
    "mashxall_azkar_progress",
    "mashxall_azkar_favorites",
    "mashxall_farmuda_favorites",
    "mashxall_book_favorites",
    "mashxall_hadith_favorites",
    "mashxall_name_favorites"
)

private fun createMashxallBackup(context: Context): org.json.JSONObject {
    val prefRoot = org.json.JSONObject()
    MASHXALL_BACKUP_PREFS.forEach { name ->
        val values = context.getSharedPreferences(name, Context.MODE_PRIVATE).all
        val json = org.json.JSONObject()
        values.forEach { (key, value) ->
            val entry = org.json.JSONObject()
            when (value) {
                is String -> entry.put("type", "string").put("value", value)
                is Boolean -> entry.put("type", "boolean").put("value", value)
                is Int -> entry.put("type", "int").put("value", value)
                is Long -> entry.put("type", "long").put("value", value)
                is Float -> entry.put("type", "float").put("value", value.toDouble())
                is Set<*> -> {
                    val array = org.json.JSONArray()
                    value.filterIsInstance<String>().forEach { array.put(it) }
                    entry.put("type", "string_set").put("value", array)
                }
            }
            if (entry.has("type")) json.put(key, entry)
        }
        prefRoot.put(name, json)
    }
    return org.json.JSONObject()
        .put("format", "mashxall-backup")
        .put("version", 1)
        .put("created_at", System.currentTimeMillis())
        .put("preferences", prefRoot)
}

private fun restoreMashxallBackup(context: Context, root: org.json.JSONObject) {
    require(root.optString("format") == "mashxall-backup") { "Not a Mashxall backup file" }
    require(root.optInt("version") == 1) { "Unsupported backup version" }
    val prefRoot = root.optJSONObject("preferences") ?: error("Backup file has no preference data")
    MASHXALL_BACKUP_PREFS.forEach { name ->
        val values = prefRoot.optJSONObject(name) ?: return@forEach
        val editor = context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear()
        val keys = values.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val entry = values.optJSONObject(key) ?: continue
            when (entry.optString("type")) {
                "string" -> editor.putString(key, entry.optString("value"))
                "boolean" -> editor.putBoolean(key, entry.optBoolean("value"))
                "int" -> editor.putInt(key, entry.optInt("value"))
                "long" -> editor.putLong(key, entry.optLong("value"))
                "float" -> editor.putFloat(key, entry.optDouble("value").toFloat())
                "string_set" -> {
                    val array = entry.optJSONArray("value") ?: org.json.JSONArray()
                    val set = buildSet {
                        for (index in 0 until array.length()) add(array.optString(index))
                    }
                    editor.putStringSet(key, set)
                }
            }
        }
        editor.apply()
    }
}

@Composable
fun BackupRestoreScreen(language: AppLanguage, foreground: Color, cardColor: Color) {
    val context = LocalContext.current
    var status by rememberSaveable { mutableStateOf("") }
    var showExportPasswordDialog by remember { mutableStateOf(false) }
    var showImportPasswordDialog by remember { mutableStateOf(false) }
    var exportPassword by rememberSaveable { mutableStateOf("") }
    var confirmExportPassword by rememberSaveable { mutableStateOf("") }
    var importPassword by rememberSaveable { mutableStateOf("") }
    var exportError by rememberSaveable { mutableStateOf("") }
    var importError by rememberSaveable { mutableStateOf("") }
    var pendingExportBytes by remember { mutableStateOf<ByteArray?>(null) }
    var pendingImportBytes by remember { mutableStateOf<ByteArray?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        val encrypted = pendingExportBytes
        pendingExportBytes = null
        if (uri == null || encrypted == null) {
            encrypted?.fill(0)
            status = when (language) {
                AppLanguage.EN -> "Backup export cancelled."
                AppLanguage.AR -> "تم إلغاء تصدير النسخة الاحتياطية."
                AppLanguage.CKB -> "هەناردەکردنی پاشەکەوت هەڵوەشایەوە."
            }
        } else {
            try {
                val output = context.contentResolver.openOutputStream(uri) ?: error("Could not open the selected destination")
                output.use { it.write(encrypted) }
                status = when (language) {
                    AppLanguage.EN -> "Encrypted backup saved successfully."
                    AppLanguage.AR -> "تم حفظ النسخة الاحتياطية المشفّرة بنجاح."
                    AppLanguage.CKB -> "پاشەکەوتی نهێنیکراو بە سەرکەوتوویی هەڵگیرا."
                }
                Toast.makeText(context, status, Toast.LENGTH_LONG).show()
            } catch (_: Exception) {
                status = when (language) {
                    AppLanguage.EN -> "Could not save the encrypted backup. Choose another folder."
                    AppLanguage.AR -> "تعذر حفظ النسخة المشفّرة. اختر مجلدًا آخر."
                    AppLanguage.CKB -> "نەتوانرا پاشەکەوتی نهێنیکراو هەڵبگیرێت؛ فۆڵدەرێکی تر هەڵبژێرە."
                }
                Toast.makeText(context, status, Toast.LENGTH_LONG).show()
            } finally {
                encrypted.fill(0)
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            var candidateBytes: ByteArray? = null
            try {
                candidateBytes = context.contentResolver.openInputStream(uri)?.use { input ->
                    val result = input.readBytes()
                    require(result.size in 1..(40 * 1024 * 1024)) { "Backup file is empty or too large" }
                    result
                } ?: error("Could not read the selected file")
                val serialized = String(candidateBytes, Charsets.UTF_8)
                if (serialized.startsWith("format=${BackupCrypto.FORMAT}\n")) {
                    // New encrypted envelope is a portable line format, so test and
                    // production code do not depend on Android's org.json stubs.
                    pendingImportBytes?.fill(0)
                    pendingImportBytes = candidateBytes
                    candidateBytes = null
                    importPassword = ""
                    importError = ""
                    showImportPasswordDialog = true
                    status = ""
                } else {
                    // Backwards compatibility for earlier unencrypted JSON exports.
                    val envelope = org.json.JSONObject(serialized)
                    if (envelope.optString("format") != "mashxall-backup") {
                        error("Not a recognized Mashxall backup file")
                    }
                    restoreMashxallBackup(context, envelope)
                    candidateBytes?.fill(0)
                    candidateBytes = null
                    status = when (language) {
                        AppLanguage.EN -> "Legacy unencrypted backup restored. New exports are encrypted."
                        AppLanguage.AR -> "تمت استعادة نسخة قديمة غير مشفّرة. النسخ الجديدة تُشفّر تلقائيًا."
                        AppLanguage.CKB -> "پاشەکەوتی کۆنی نهێنیکراو نەبوو گەڕێندرایەوە؛ پاشەکەوتی نوێ نهێنی دەکرێت."
                    }
                    Toast.makeText(context, status, Toast.LENGTH_LONG).show()
                    (context as? android.app.Activity)?.recreate()
                }
            } catch (_: Exception) {
                candidateBytes?.fill(0)
                status = when (language) {
                    AppLanguage.EN -> "Import failed. Select a valid Mashxall backup file."
                    AppLanguage.AR -> "فشل الاستيراد. اختر ملف نسخة احتياطية صالحًا من مەشخەڵ."
                    AppLanguage.CKB -> "هاوردەکردن سەرکەوتوو نەبوو؛ فایلی پاشەکەوتی دروستی مەشخەڵ هەڵبژێرە."
                }
                Toast.makeText(context, status, Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        PremiumSectionHero(
            title = when (language) {
                AppLanguage.EN -> "Backup & restore"
                AppLanguage.AR -> "النسخ الاحتياطي والاستعادة"
                AppLanguage.CKB -> "پاشەکەوتکردن و گەڕاندنەوە"
            },
            subtitle = when (language) {
                AppLanguage.EN -> "Protect your personal settings and move them safely to another Android phone."
                AppLanguage.AR -> "احمِ إعداداتك الشخصية وانقلها بأمان إلى هاتف Android آخر."
                AppLanguage.CKB -> "ڕێکخستنە تایبەتییەکانت بپارێزە و بە سەلامەتی بیانگوازەوە بۆ مۆبایلێکی تری Android."
            },
            icon = Icons.Outlined.CheckCircle,
            foreground = foreground,
            accent = Color(0xFF8FC6A8),
            eyebrow = when (language) { AppLanguage.EN -> "YOUR DATA • PROTECTED"; AppLanguage.AR -> "بياناتك • محمية"; AppLanguage.CKB -> "داتاکانت • پارێزراون" }
        )
        Spacer(Modifier.height(14.dp))
        Card(colors = CardDefaults.cardColors(containerColor = cardColor), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    when (language) {
                        AppLanguage.EN -> "Included in the backup"
                        AppLanguage.AR -> "محتويات النسخة الاحتياطية"
                        AppLanguage.CKB -> "ئەو داتایانەی پاشەکەوت دەکرێن"
                    }, color = Gold, fontWeight = FontWeight.Bold, fontSize = 15.sp
                )
                Spacer(Modifier.height(8.dp))
                listOf(
                    when (language) { AppLanguage.EN -> "Saved Quran verses and personal notes"; AppLanguage.AR -> "الآيات المحفوظة وملاحظاتها الشخصية"; AppLanguage.CKB -> "ئایەتە پاشەکەوتکراوەکان و تێبینییەکانیان" },
                    when (language) { AppLanguage.EN -> "Saved Hadith, books, names and Farmuda favourites"; AppLanguage.AR -> "الأحاديث والكتب والأسماء والمأثورات المحفوظة"; AppLanguage.CKB -> "فەرموودە و کتێب و ناو و فەرموودە/دوعا دڵخوازەکان" },
                    when (language) { AppLanguage.EN -> "Language, theme, last reading location and prayer settings"; AppLanguage.AR -> "اللغة والمظهر وآخر موضع قراءة وإعدادات الصلاة"; AppLanguage.CKB -> "زمان، ڕووکار، شوێنی خوێندنەوە و ڕێکخستنەکانی نوێژ" },
                    when (language) { AppLanguage.EN -> "Adhkar favourites and repetition counters"; AppLanguage.AR -> "مفضّلات الأذكار وعدّادات التكرار"; AppLanguage.CKB -> "دڵخوازەکانی زیکر و ژمارەکانی دووبارەکردنەوە" },
                    when (language) { AppLanguage.EN -> "Tasbih counts and adhan preferences"; AppLanguage.AR -> "عدادات التسبيح وتفضيلات الأذان"; AppLanguage.CKB -> "ژمارەی تەسبیح و هەڵبژاردەکانی بانگ" }
                ).forEach { item ->
                    Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Outlined.CheckCircle, null, tint = Gold, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(item, color = foreground, fontSize = 12.sp, lineHeight = 18.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = {
            exportPassword = ""
            confirmExportPassword = ""
            exportError = ""
            showExportPasswordDialog = true
        }, modifier = Modifier.fillMaxWidth()) {
            Text(if (language == AppLanguage.EN) "Save encrypted backup" else if (language == AppLanguage.AR) "حفظ نسخة احتياطية مشفّرة" else "پاشەکەوتی نهێنیکراو هەڵبگرە")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { importLauncher.launch(arrayOf("application/octet-stream", "application/json", "text/json", "*/*")) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (language == AppLanguage.EN) "Restore from a backup file" else if (language == AppLanguage.AR) "استعادة من ملف نسخة احتياطية" else "لە فایلی پاشەکەوتەوە بیگەڕێنەوە")
        }
        Spacer(Modifier.height(12.dp))
        if (status.isNotBlank()) Text(status, color = foreground.copy(alpha = 0.75f), fontSize = 12.sp, lineHeight = 18.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            when (language) {
                AppLanguage.EN -> "The backup is encrypted with your passphrase using authenticated encryption. Keep the passphrase safe; it cannot be recovered. The bundled Quran, tafsir, audio and books remain inside the installed app."
                AppLanguage.AR -> "تُشفّر النسخة بكلمة المرور باستخدام تشفير موثّق. احتفظ بكلمة المرور؛ لا يمكن استرجاعها. المصحف والتفسير والصوتيات والكتب تبقى ضمن التطبيق."
                AppLanguage.CKB -> "پاشەکەوتەکە بە وشەی نهێنیت و بە شێوەی نهێنیکردنی پشتڕاستکراو دەپارێزرێت؛ وشەی نهێنی لەبیر مەکە. قورئان و تەفسیر و دەنگ و کتێبەکان لە ناو ئەپەکەدا دەمێننەوە."
            }, color = foreground.copy(alpha = 0.5f), fontSize = 11.sp, lineHeight = 17.sp
        )
    }

    if (showExportPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showExportPasswordDialog = false
                exportPassword = ""
                confirmExportPassword = ""
                exportError = ""
            },
            title = {
                Text(
                    when (language) { AppLanguage.EN -> "Protect backup"; AppLanguage.AR -> "حماية النسخة الاحتياطية"; AppLanguage.CKB -> "پاراستنی پاشەکەوت" },
                    color = foreground, fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        when (language) {
                            AppLanguage.EN -> "Choose a passphrase with at least 8 characters. You must enter the same passphrase when restoring this file."
                            AppLanguage.AR -> "اختر كلمة مرور من 8 أحرف على الأقل. ستحتاج إلى كلمة المرور نفسها عند الاستعادة."
                            AppLanguage.CKB -> "وشەیەکی نهێنی بە لانیکەم ٨ پیت هەڵبژێرە؛ بۆ گەڕاندنەوە هەمان وشە پێویستە."
                        }, color = foreground.copy(alpha = 0.75f), fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = exportPassword,
                        onValueChange = { exportPassword = it; exportError = "" },
                        label = { Text(if (language == AppLanguage.EN) "Passphrase" else if (language == AppLanguage.AR) "كلمة المرور" else "وشەی نهێنی") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmExportPassword,
                        onValueChange = { confirmExportPassword = it; exportError = "" },
                        label = { Text(if (language == AppLanguage.EN) "Confirm passphrase" else if (language == AppLanguage.AR) "تأكيد كلمة المرور" else "دووبارە وشەی نهێنی") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (exportError.isNotBlank()) Text(exportError, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    when {
                        exportPassword.length < 8 -> exportError = when (language) {
                            AppLanguage.EN -> "Use at least 8 characters."
                            AppLanguage.AR -> "استخدم 8 أحرف على الأقل."
                            AppLanguage.CKB -> "لانیکەم ٨ پیت بەکاربهێنە."
                        }
                        exportPassword != confirmExportPassword -> exportError = when (language) {
                            AppLanguage.EN -> "The passphrases do not match."
                            AppLanguage.AR -> "كلمتا المرور غير متطابقتين."
                            AppLanguage.CKB -> "وشە نهێنییەکان یەک ناگرنەوە."
                        }
                        else -> try {
                            pendingExportBytes?.fill(0)
                            pendingExportBytes = BackupCrypto.encrypt(
                                createMashxallBackup(context).toString(2),
                                exportPassword.toCharArray()
                            )
                            showExportPasswordDialog = false
                            exportPassword = ""
                            confirmExportPassword = ""
                            exportError = ""
                            exportLauncher.launch("Mashxall-Backup.mashxall")
                        } catch (_: Exception) {
                            pendingExportBytes?.fill(0)
                            pendingExportBytes = null
                            exportError = when (language) {
                                AppLanguage.EN -> "Could not encrypt the backup on this device."
                                AppLanguage.AR -> "تعذر تشفير النسخة الاحتياطية على هذا الجهاز."
                                AppLanguage.CKB -> "نەتوانرا پاشەکەوتەکە لەم ئامێرەدا نهێنی بکرێت."
                            }
                            showExportPasswordDialog = true
                        }
                    }
                }) {
                    Text(if (language == AppLanguage.EN) "Encrypt and continue" else if (language == AppLanguage.AR) "تشفير ومتابعة" else "نهێنی بکە و بەردەوام بە")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showExportPasswordDialog = false
                    exportPassword = ""
                    confirmExportPassword = ""
                    exportError = ""
                }) {
                    Text(if (language == AppLanguage.EN) "Cancel" else if (language == AppLanguage.AR) "إلغاء" else "هەڵوەشاندنەوە")
                }
            }
        )
    }

    if (showImportPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showImportPasswordDialog = false
                importPassword = ""
                importError = ""
                pendingImportBytes?.fill(0)
                pendingImportBytes = null
            },
            title = {
                Text(
                    when (language) { AppLanguage.EN -> "Unlock backup"; AppLanguage.AR -> "فتح النسخة الاحتياطية"; AppLanguage.CKB -> "کردنەوەی پاشەکەوت" },
                    color = foreground, fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        when (language) {
                            AppLanguage.EN -> "Enter the passphrase that was used when this backup was created."
                            AppLanguage.AR -> "أدخل كلمة المرور التي استُخدمت عند إنشاء هذه النسخة."
                            AppLanguage.CKB -> "ئەو وشە نهێنییە بنووسە کە کاتی دروستکردنی پاشەکەوتەکە بەکارهاتووە."
                        }, color = foreground.copy(alpha = 0.75f), fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = importPassword,
                        onValueChange = { importPassword = it; importError = "" },
                        label = { Text(if (language == AppLanguage.EN) "Passphrase" else if (language == AppLanguage.AR) "كلمة المرور" else "وشەی نهێنی") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (importError.isNotBlank()) Text(importError, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val encrypted = pendingImportBytes
                    if (encrypted == null) {
                        importError = if (language == AppLanguage.EN) "No backup file is selected." else if (language == AppLanguage.AR) "لم يتم اختيار ملف نسخة احتياطية." else "هیچ فایلی پاشەکەوت هەڵنەبژێردراوە."
                    } else if (importPassword.length < 8) {
                        importError = if (language == AppLanguage.EN) "Enter a passphrase of at least 8 characters." else if (language == AppLanguage.AR) "أدخل كلمة مرور من 8 أحرف على الأقل." else "وشەی نهێنییەک بە لانیکەم ٨ پیت بنووسە."
                    } else {
                        try {
                            val plainText = BackupCrypto.decrypt(encrypted, importPassword.toCharArray())
                            val backup = org.json.JSONObject(plainText)
                            restoreMashxallBackup(context, backup)
                            status = when (language) {
                                AppLanguage.EN -> "Encrypted backup restored successfully."
                                AppLanguage.AR -> "تمت استعادة النسخة الاحتياطية المشفّرة بنجاح."
                                AppLanguage.CKB -> "پاشەکەوتی نهێنیکراو بە سەرکەوتوویی گەڕێندرایەوە."
                            }
                            encrypted.fill(0)
                            pendingImportBytes = null
                            importPassword = ""
                            importError = ""
                            showImportPasswordDialog = false
                            Toast.makeText(context, status, Toast.LENGTH_LONG).show()
                            (context as? android.app.Activity)?.recreate()
                        } catch (_: Exception) {
                            importError = when (language) {
                                AppLanguage.EN -> "Could not unlock this backup. Check the passphrase and try again."
                                AppLanguage.AR -> "تعذر فتح النسخة. تحقق من كلمة المرور وحاول مرة أخرى."
                                AppLanguage.CKB -> "نەتوانرا پاشەکەوتەکە بکرێتەوە؛ وشەی نهێنی بپشکنە و دووبارە هەوڵ بدەوە."
                            }
                        }
                    }
                }) {
                    Text(if (language == AppLanguage.EN) "Decrypt and restore" else if (language == AppLanguage.AR) "فك التشفير والاستعادة" else "نهێنی لاببە و بیگەڕێنەوە")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImportPasswordDialog = false
                    importPassword = ""
                    importError = ""
                    pendingImportBytes?.fill(0)
                    pendingImportBytes = null
                }) {
                    Text(if (language == AppLanguage.EN) "Cancel" else if (language == AppLanguage.AR) "إلغاء" else "هەڵوەشاندنەوە")
                }
            }
        )
    }
}

private fun validDownloadedPdf(file: File): Boolean {
    if (!file.isFile || file.length() < 100L) return false
    return try {
        file.inputStream().buffered().use { input ->
            val header = ByteArray(5)
            input.read(header) == header.size && String(header, Charsets.ISO_8859_1) == "%PDF-"
        }
    } catch (_: Exception) {
        false
    }
}

private fun localBookPdfFile(context: Context, book: BookItem): File {
    val folder = File(context.filesDir, "downloaded-books").apply { mkdirs() }
    val slug = book.uniqueText.trim().ifBlank { book.id.toString() }
        .replace(Regex("[^A-Za-z0-9._-]+"), "_")
        .trim('_')
        .ifBlank { book.id.toString() }
    return File(folder, "${book.id}_$slug.pdf")
}

private suspend fun downloadCatalogueBook(context: Context, book: BookItem): File = withContext(Dispatchers.IO) {
    val target = localBookPdfFile(context, book)
    if (validDownloadedPdf(target)) return@withContext target

    val slug = book.uniqueText.trim()
        .replace(Regex("[^A-Za-z0-9._-]+"), "_")
        .trim('_')
        .ifBlank { book.id.toString() }
    val urls = buildList {
        book.url.trim().takeIf { it.startsWith("https://", true) || it.startsWith("http://", true) }
            ?.let(::add)
        add("https://github.com/H4zh4n/TafseeriNoor/raw/files/books/$slug.pdf")
        add("https://raw.githubusercontent.com/H4zh4n/TafseeriNoor/files/books/$slug.pdf")
    }.distinct()

    var lastFailure: Exception? = null
    for (candidate in urls) {
        val temporary = File(target.parentFile, target.name + ".part")
        val connection = try {
            (URL(candidate).openConnection() as HttpURLConnection).apply {
                connectTimeout = 20_000
                readTimeout = 60_000
                instanceFollowRedirects = true
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mashxall-Android/1.0")
                setRequestProperty("Accept", "application/pdf,application/octet-stream;q=0.9,*/*;q=0.5")
            }
        } catch (error: Exception) {
            lastFailure = error
            continue
        }
        try {
            val status = connection.responseCode
            if (status !in 200..299) throw java.io.IOException("Book server returned HTTP $status")
            connection.inputStream.use { input ->
                temporary.outputStream().buffered().use { output -> input.copyTo(output, 128 * 1024) }
            }
            if (!validDownloadedPdf(temporary)) {
                throw java.io.IOException("The returned file was not a valid PDF")
            }
            if (!temporary.renameTo(target)) {
                temporary.copyTo(target, overwrite = true)
                temporary.delete()
            }
            if (!validDownloadedPdf(target)) throw java.io.IOException("Downloaded PDF failed integrity validation")
            return@withContext target
        } catch (error: Exception) {
            lastFailure = error
            temporary.delete()
        } finally {
            connection.disconnect()
        }
    }
    throw java.io.IOException(
        "This book could not be downloaded. The source may have moved or the server may be unavailable.",
        lastFailure
    )
}

@Composable
fun BooksScreen(language: AppLanguage, foreground: Color, cardColor: Color, onOpenPdf: (String) -> Unit) {
    val context = LocalContext.current
    val favoritePrefs = remember { context.getSharedPreferences("mashxall_book_favorites", Context.MODE_PRIVATE) }
    var query by rememberSaveable { mutableStateOf("") }
    var books by remember { mutableStateOf<List<BookItem>>(emptyList()) }
    var pdfs by remember { mutableStateOf<List<String>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var favoritesOnly by rememberSaveable { mutableStateOf(false) }
    var favoriteIds by remember {
        mutableStateOf(favoritePrefs.getStringSet("book_ids", emptySet()).orEmpty().toSet())
    }
    val bookScope = rememberCoroutineScope()
    var downloadingIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var downloadedPaths by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }

    LaunchedEffect(Unit) { pdfs = ContentRepository.loadPdfPaths(context) }
    LaunchedEffect(books) {
        if (books.isNotEmpty()) {
            downloadedPaths = downloadedPaths + books.mapNotNull { item ->
                val file = localBookPdfFile(context, item)
                if (validDownloadedPdf(file)) item.id to file.absolutePath else null
            }.toMap()
        }
    }
    LaunchedEffect(query) {
        loading = true
        books = ContentRepository.loadBooks(context, query.trim())
        loading = false
    }
    val visiblePdfs = if (favoritesOnly) emptyList() else if (query.isBlank()) pdfs else pdfs.filter { path ->
        path.substringAfterLast('/').contains(query.trim(), ignoreCase = true) ||
            path.substringBeforeLast('/').contains(query.trim(), ignoreCase = true)
    }
    val visibleBooks = books.filter { !favoritesOnly || favoriteIds.contains(it.id.toString()) }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            PremiumSectionHero(
                title = when (language) { AppLanguage.EN -> "Book library"; AppLanguage.AR -> "مكتبة الكتب"; AppLanguage.CKB -> "کتێبخانەی کتێب" },
                subtitle = when (language) {
                    AppLanguage.EN -> "Browse source books, open bundled PDFs and keep favourites."
                    AppLanguage.AR -> "تصفّح الكتب الأصلية وافتح ملفات PDF واحفظ المفضّل."
                    AppLanguage.CKB -> "کتێبە ڕەسەنەکان بپشکنە، PDF بکەرەوە و دڵخوازەکان بپارێزە."
                },
                icon = Icons.Outlined.MenuBook,
                foreground = foreground,
                accent = Color(0xFF9EB9E9),
                metric = when (language) { AppLanguage.EN -> "${books.size} records • ${pdfs.size} PDFs"; AppLanguage.AR -> "${books.size} سجل • ${pdfs.size} PDF"; AppLanguage.CKB -> "${books.size} تۆمار • ${pdfs.size} PDF" },
                eyebrow = when (language) { AppLanguage.EN -> "READING COLLECTION"; AppLanguage.AR -> "مجموعات القراءة"; AppLanguage.CKB -> "کۆمەڵەی خوێندنەوە" }
            )
            Spacer(Modifier.height(10.dp))
            SearchBox(query, { query = it }, tr(language, "search"))
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                PremiumChoicePill(
                    label = if (language == AppLanguage.EN) "All books" else if (language == AppLanguage.AR) "كل الكتب" else "هەموو کتێبەکان",
                    selected = !favoritesOnly,
                    accent = Gold,
                    foreground = foreground
                ) { favoritesOnly = false }
                PremiumChoicePill(
                    label = if (language == AppLanguage.EN) "Favourites" else if (language == AppLanguage.AR) "المفضلة" else "دڵخوازەکان",
                    selected = favoritesOnly,
                    accent = Gold,
                    foreground = foreground
                ) { favoritesOnly = true }
            }
        }
        if (visibleBooks.isEmpty() && visiblePdfs.isEmpty()) {
            LoadingOrEmpty(
                loading, language, foreground,
                if (favoritesOnly && favoriteIds.isEmpty()) {
                    if (language == AppLanguage.EN) "No book favourites yet. Tap the star on a catalogue record to save it."
                    else if (language == AppLanguage.AR) "لا توجد كتب مفضلة بعد. اضغط النجمة لحفظ كتاب."
                    else "هێشتا کتێبی دڵخواز نییە؛ ئەستێرەکە دابگرە بۆ پاشەکەوتکردن."
                } else tr(language, "not_found")
            )
        } else LazyColumn(
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            if (visiblePdfs.isNotEmpty()) {
                item(key = "pdf-section-heading") {
                    Text(
                        when (language) {
                            AppLanguage.EN -> "OFFLINE PDF LIBRARY • ${visiblePdfs.size}"
                            AppLanguage.AR -> "مكتبة PDF دون إنترنت • ${visiblePdfs.size}"
                            AppLanguage.CKB -> "کتێبخانەی PDF بەبێ ئینتەرنێت • ${visiblePdfs.size}"
                        }, color = Gold, fontWeight = FontWeight.Bold, fontSize = 12.sp
                    )
                }
                itemsIndexed(visiblePdfs, key = { _, path -> path }) { pdfIndex, path ->
                    Card(
                        Modifier.fillMaxWidth()
                            .staggeredEntrance(itemKey = "pdf:$path", index = pdfIndex)
                            .animateContentSize()
                            .premiumClickable { onOpenPdf(path) },
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        shape = RoundedCornerShape(15.dp)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.MenuBook, null, tint = Gold, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(path.substringAfterLast('/'), color = foreground, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                Text(path.substringBeforeLast('/'), color = foreground.copy(alpha = 0.5f), fontSize = 9.sp, maxLines = 2)
                            }
                            Icon(Icons.Outlined.ChevronRight, null, tint = foreground.copy(alpha = 0.45f))
                        }
                    }
                }
            }
            if (visibleBooks.isNotEmpty()) {
                item(key = "catalogue-section-heading") {
                    Text(
                        when (language) {
                            AppLanguage.EN -> "BOOK CATALOGUE • ${visibleBooks.size}"
                            AppLanguage.AR -> "فهرس الكتب • ${visibleBooks.size}"
                            AppLanguage.CKB -> "فهرستی کتێبەکان • ${visibleBooks.size}"
                        }, color = Gold, fontWeight = FontWeight.Bold, fontSize = 12.sp
                    )
                }
            }
            itemsIndexed(visibleBooks, key = { _, item -> "book-" + item.id }) { bookIndex, item ->
                val favorite = favoriteIds.contains(item.id.toString())
                Card(
                    modifier = Modifier.fillMaxWidth()
                        .staggeredEntrance(itemKey = "book:${item.id}", index = bookIndex)
                        .animateContentSize(),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(0.7.dp, Gold.copy(alpha = 0.17f))
                ) {
                    Column(Modifier.padding(13.dp)) {
                        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (item.coverUrl.startsWith("http", ignoreCase = true)) {
                                SubcomposeAsyncImage(
                                    model = item.coverUrl,
                                    contentDescription = item.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(width = 78.dp, height = 108.dp).clip(RoundedCornerShape(10.dp)),
                                    loading = {
                                        Box(Modifier.fillMaxSize().background(DeepGreen.copy(alpha = 0.35f)), contentAlignment = Alignment.Center) {
                                            CircularProgressIndicator(color = Gold, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                        }
                                    },
                                    error = {
                                        Box(Modifier.fillMaxSize().background(DeepGreen.copy(alpha = 0.35f)), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Outlined.MenuBook, null, tint = Gold, modifier = Modifier.size(34.dp))
                                        }
                                    }
                                )
                            } else {
                                Box(
                                    Modifier.size(width = 78.dp, height = 108.dp).clip(RoundedCornerShape(10.dp)).background(DeepGreen),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Outlined.MenuBook, null, tint = Gold, modifier = Modifier.size(36.dp))
                                }
                            }
                            Column(Modifier.weight(1f)) {
                                Text(item.title, color = foreground, fontSize = 14.sp, fontWeight = FontWeight.Bold, lineHeight = 20.sp)
                                if (item.uniqueText.isNotBlank()) {
                                    Spacer(Modifier.height(3.dp))
                                    Text(item.uniqueText, color = foreground.copy(alpha = 0.58f), fontSize = 10.sp, lineHeight = 15.sp)
                                }
                                if (item.writer.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(item.writer, color = Gold, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                    Text(
                                        item.pages.toString() + " • " + when (language) {
                                            AppLanguage.EN -> "pages"
                                            AppLanguage.AR -> "صفحة"
                                            AppLanguage.CKB -> "پەڕە"
                                        },
                                        color = foreground.copy(alpha = 0.52f), fontSize = 10.sp
                                    )
                                    if (item.branch.isNotBlank() && item.branch != "0") {
                                        Text("•", color = foreground.copy(alpha = 0.35f), fontSize = 10.sp)
                                        Text(item.branch, color = foreground.copy(alpha = 0.52f), fontSize = 10.sp, maxLines = 1)
                                    }
                                    Spacer(Modifier.weight(1f))
                                    PremiumIconButton(
                                        icon = Icons.Outlined.BookmarkBorder,
                                        contentDescription = if (language == AppLanguage.EN) "Save book" else if (language == AppLanguage.AR) "حفظ الكتاب" else "کتێب پاشەکەوت بکە",
                                        accent = Gold,
                                        size = 32.dp,
                                        selected = favorite
                                    ) {
                                        val key = item.id.toString()
                                        favoriteIds = if (favorite) favoriteIds - key else favoriteIds + key
                                        favoritePrefs.edit().putStringSet("book_ids", favoriteIds).apply()
                                    }
                                }
                            }
                        }
                        if (item.description.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Text(item.description, color = foreground.copy(alpha = 0.73f), fontSize = 11.sp, lineHeight = 17.sp, maxLines = 7)
                        }
                        if (item.extraText.isNotBlank()) {
                            Spacer(Modifier.height(7.dp))
                            Text(item.extraText, color = foreground.copy(alpha = 0.58f), fontSize = 10.sp, lineHeight = 15.sp, maxLines = 3)
                        }
                        if (item.extraNumber > 0) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                when (language) {
                                    AppLanguage.EN -> "Source catalogue code: ${item.extraNumber}"
                                    AppLanguage.AR -> "رقم إضافي في الفهرس: ${item.extraNumber}"
                                    AppLanguage.CKB -> "ژمارەی زیادەی فهرست: ${item.extraNumber}"
                                }, color = foreground.copy(alpha = 0.43f), fontSize = 9.sp
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        val localPdfPath = downloadedPaths[item.id]
                        PremiumActionButton(
                            label = when (language) {
                                AppLanguage.EN -> when {
                                    item.id in downloadingIds -> "Downloading PDF…"
                                    localPdfPath != null -> "Open offline PDF"
                                    else -> "Download PDF and open"
                                }
                                AppLanguage.AR -> when {
                                    item.id in downloadingIds -> "جارٍ تنزيل PDF…"
                                    localPdfPath != null -> "فتح PDF دون إنترنت"
                                    else -> "تنزيل PDF وفتحه"
                                }
                                AppLanguage.CKB -> when {
                                    item.id in downloadingIds -> "PDF دادەگیرێت…"
                                    localPdfPath != null -> "PDF بکەرەوە (بێ ئینتەرنێت)"
                                    else -> "PDF دابگرە و بیکەرەوە"
                                }
                            },
                            icon = if (localPdfPath != null) Icons.Outlined.MenuBook else Icons.Outlined.ChevronRight,
                            accent = Color(0xFF9EB9E9),
                            foreground = foreground,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = item.id !in downloadingIds,
                            loading = item.id in downloadingIds
                        ) {
                            if (localPdfPath != null && validDownloadedPdf(File(localPdfPath))) {
                                onOpenPdf(localPdfPath)
                            } else {
                                downloadingIds = downloadingIds + item.id
                                bookScope.launch {
                                    try {
                                        val downloaded = downloadCatalogueBook(context, item)
                                        downloadedPaths = downloadedPaths + (item.id to downloaded.absolutePath)
                                        Toast.makeText(
                                            context,
                                            when (language) {
                                                AppLanguage.EN -> "Book downloaded and saved offline"
                                                AppLanguage.AR -> "تم تنزيل الكتاب وحفظه للقراءة دون إنترنت"
                                                AppLanguage.CKB -> "کتێبەکە داگیرا و بۆ خوێندنەوەی بێ ئینتەرنێت پاشەکەوت کرا"
                                            }, Toast.LENGTH_LONG
                                        ).show()
                                        onOpenPdf(downloaded.absolutePath)
                                    } catch (_: Exception) {
                                        Toast.makeText(
                                            context,
                                            when (language) {
                                                AppLanguage.EN -> "Download failed: check internet or try again later."
                                                AppLanguage.AR -> "فشل التنزيل؛ تحقق من الإنترنت أو حاول لاحقًا."
                                                AppLanguage.CKB -> "داگرتن سەرکەوتوو نەبوو؛ ئینتەرنێت بپشکنە و دووبارە هەوڵ بدە."
                                            }, Toast.LENGTH_LONG
                                        ).show()
                                    } finally {
                                        downloadingIds = downloadingIds - item.id
                                    }
                                }
                            }
                        }
                        if (item.url.startsWith("http", ignoreCase = true)) {
                            TextButton(onClick = {
                                try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.url))) }
                                catch (_: Exception) { Toast.makeText(context, tr(language, "no_data"), Toast.LENGTH_SHORT).show() }
                            }) {
                                Text(
                                    if (language == AppLanguage.EN) "Open source book link"
                                    else if (language == AppLanguage.AR) "فتح رابط الكتاب"
                                    else "بەستەری کتێب بکەرەوە",
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PdfReaderScreen(language: AppLanguage, foreground: Color, assetPath: String) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("mashxall_preferences", Context.MODE_PRIVATE) }
    val pageProgressKey = remember(assetPath) {
        "book_pdf_page_" + MessageDigest.getInstance("SHA-256")
            .digest(assetPath.toByteArray(Charsets.UTF_8))
            .take(8).joinToString("") { "%02x".format(it) }
    }
    var pageIndex by rememberSaveable(assetPath) {
        mutableIntStateOf(preferences.getInt(pageProgressKey, 0).coerceAtLeast(0))
    }
    var pageCount by remember(assetPath) { mutableIntStateOf(0) }
    var bitmap by remember(assetPath) { mutableStateOf<Bitmap?>(null) }
    var loading by remember { mutableStateOf(true) }
    var loadFailed by remember { mutableStateOf(false) }
    val pdfMotionAllowed = remember(context) { systemAllowsDecorativeMotion(context) }
    var pdfDragPreview by remember(pageIndex) { mutableFloatStateOf(0f) }
    val animatedPdfDragX by androidx.compose.animation.core.animateFloatAsState(
        targetValue = pdfDragPreview,
        animationSpec = if (pdfMotionAllowed) androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
        ) else androidx.compose.animation.core.snap(),
        label = "PDF finger-follow page drag"
    )
    val animatedPdfTurnY by androidx.compose.animation.core.animateFloatAsState(
        targetValue = (pdfDragPreview / 170f).coerceIn(-1f, 1f) * 8f,
        animationSpec = if (pdfMotionAllowed) androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
        ) else androidx.compose.animation.core.snap(),
        label = "PDF page tilt"
    )
    val readingProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pageCount > 0) ((pageIndex + 1).toFloat() / pageCount.toFloat()).coerceIn(0f, 1f) else 0f,
        animationSpec = androidx.compose.animation.core.tween(260, easing = FastOutSlowInEasing),
        label = "PDF reading progress"
    )

    LaunchedEffect(pageIndex, pageProgressKey) {
        preferences.edit().putInt(pageProgressKey, pageIndex.coerceAtLeast(0)).apply()
    }

    LaunchedEffect(assetPath, pageIndex) {
        if (assetPath.isBlank()) {
            loading = false
            loadFailed = true
        } else {
            loading = true
            loadFailed = false
            try {
                val rendered = withContext(Dispatchers.IO) {
                    val digest = MessageDigest.getInstance("SHA-256")
                        .digest(assetPath.toByteArray(Charsets.UTF_8))
                        .take(10).joinToString("") { "%02x".format(it) }
                    val requestedFile = File(assetPath)
                    val localPdf = if (requestedFile.isAbsolute && validDownloadedPdf(requestedFile)) {
                        requestedFile
                    } else {
                        File(context.cacheDir, "mashxall-$digest.pdf").also { cached ->
                            if (!cached.exists() || cached.length() == 0L) {
                                context.assets.open(assetPath).use { input ->
                                    cached.outputStream().use { output -> input.copyTo(output, 128 * 1024) }
                                }
                            }
                        }
                    }
                    ParcelFileDescriptor.open(localPdf, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                        PdfRenderer(descriptor).use { renderer ->
                            val total = renderer.pageCount
                            if (total <= 0) Pair(total, null as Bitmap?) else {
                                val safeIndex = pageIndex.coerceIn(0, total - 1)
                                renderer.openPage(safeIndex).use { page ->
                                    val width = page.width.coerceAtMost(1400)
                                    val scale = width.toFloat() / page.width.toFloat()
                                    val height = (page.height * scale).toInt().coerceAtLeast(1)
                                    val image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                                    image.eraseColor(AndroidColor.WHITE)
                                    page.render(image, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                    Pair(total, image)
                                }
                            }
                        }
                    }
                }
                pageCount = rendered.first
                bitmap = rendered.second
                pageIndex = pageIndex.coerceIn(0, (pageCount - 1).coerceAtLeast(0))
                loadFailed = pageCount <= 0 || bitmap == null
            } catch (_: Exception) {
                bitmap = null
                pageCount = 0
                loadFailed = true
            } finally {
                loading = false
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp))
                .background(Brush.horizontalGradient(listOf(Gold.copy(alpha = 0.11f), Color(0xFF18382F))))
                .border(1.dp, Gold.copy(alpha = 0.17f), RoundedCornerShape(17.dp))
                .padding(horizontal = 11.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PremiumIconMedallion(icon = Icons.Outlined.MenuBook, tint = Gold, size = 36.dp)
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(assetPath.substringAfterLast('/'), color = foreground, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                Text(
                    when (language) { AppLanguage.EN -> "CONTINUE READING"; AppLanguage.AR -> "متابعة القراءة"; AppLanguage.CKB -> "بەردەوام بە لە خوێندنەوە" },
                    color = Gold.copy(alpha = 0.88f), fontSize = 8.sp, letterSpacing = 0.8.sp, fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(7.dp))
        Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(foreground.copy(alpha = 0.08f))) {
            Box(Modifier.fillMaxWidth(readingProgress).fillMaxHeight().clip(CircleShape)
                .background(Brush.horizontalGradient(listOf(Gold, Color(0xFF7BC6A7)))))
        }
        PremiumPageScrubber(
            currentPage = pageIndex + 1,
            pageCount = pageCount,
            pageLabel = if (language == AppLanguage.EN) "Jump to page" else if (language == AppLanguage.AR) "انتقل إلى صفحة" else "بڕۆ بۆ پەڕە",
            foreground = foreground,
            accent = Gold,
            modifier = Modifier.padding(top = 3.dp)
        ) { selectedPage ->
            pageIndex = (selectedPage - 1).coerceIn(0, (pageCount - 1).coerceAtLeast(0))
        }
        Spacer(Modifier.height(9.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
            PremiumChoicePill(
                modifier = Modifier.weight(1f),
                label = tr(language, "previous"),
                selected = false,
                accent = Gold,
                foreground = foreground,
                leadingIcon = Icons.Outlined.ArrowBack,
                enabled = pageIndex > 0 && !loading
            ) { pageIndex = (pageIndex - 1).coerceAtLeast(0) }
            AnimatedContent(
                targetState = if (pageCount > 0) pageIndex + 1 else 0,
                transitionSpec = {
                    (fadeIn(tween(120)) + slideInVertically(initialOffsetY = { it / 3 }, animationSpec = tween(150))) togetherWith
                        (fadeOut(tween(90)) + slideOutVertically(targetOffsetY = { -it / 3 }, animationSpec = tween(90)))
                },
                label = "PDF page number rollover"
            ) { visiblePage ->
                Text(
                    if (visiblePage > 0) tr(language, "page") + " " + visiblePage + " / " + pageCount else tr(language, "open_pdf"),
                    color = Gold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            PremiumChoicePill(
                modifier = Modifier.weight(1f),
                label = tr(language, "next"),
                selected = false,
                accent = Gold,
                foreground = foreground,
                leadingIcon = Icons.Outlined.ChevronRight,
                enabled = pageCount > 0 && pageIndex < pageCount - 1 && !loading
            ) { pageIndex = (pageIndex + 1).coerceAtMost((pageCount - 1).coerceAtLeast(0)) }
        }
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier.fillMaxSize().weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF1B2925))
                .pointerInput(pageIndex, pageCount, loading, bitmap) {
                    if (loading || bitmap == null || pageCount <= 1) return@pointerInput
                    var horizontalDrag = 0f
                    detectHorizontalDragGestures(
                        onDragStart = {
                            horizontalDrag = 0f
                            pdfDragPreview = 0f
                        },
                        onHorizontalDrag = { _, delta ->
                            horizontalDrag += delta
                            pdfDragPreview = (pdfDragPreview + delta).coerceIn(-size.width * 0.32f, size.width * 0.32f)
                        },
                        onDragEnd = {
                            val target = pdfSwipeTarget(pageIndex, pageCount, horizontalDrag)
                            if (target != null) {
                                pageIndex = target
                            } else {
                                pdfDragPreview = 0f
                            }
                        },
                        onDragCancel = {
                            horizontalDrag = 0f
                            pdfDragPreview = 0f
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            when {
                loading -> CircularProgressIndicator(color = Gold)
                bitmap != null -> AnimatedContent(
                    targetState = bitmap,
                    transitionSpec = {
                        (fadeIn(tween(160)) + scaleIn(initialScale = 0.975f, animationSpec = tween(210))) togetherWith
                            (fadeOut(tween(110)) + scaleOut(targetScale = 1.015f, animationSpec = tween(110)))
                    },
                    label = "PDF reader page transition"
                ) { visibleBitmap ->
                    if (visibleBitmap != null) {
                        Image(
                            visibleBitmap.asImageBitmap(),
                            contentDescription = assetPath.substringAfterLast('/'),
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().graphicsLayer {
                                translationX = animatedPdfDragX
                                rotationY = animatedPdfTurnY
                                cameraDistance = 26.dp.toPx()
                                scaleX = 1f - (kotlin.math.abs(animatedPdfTurnY) / 8f) * 0.015f
                                scaleY = 1f - (kotlin.math.abs(animatedPdfTurnY) / 8f) * 0.015f
                                shadowElevation = kotlin.math.abs(animatedPdfTurnY) * 0.35f
                            }
                        )
                    }
                }
                loadFailed -> Text(tr(language, "data_notice"), color = foreground, textAlign = TextAlign.Center, modifier = Modifier.padding(18.dp))
            }
        }
    }
}

@Composable
fun LexiconScreen(language: AppLanguage, foreground: Color, cardColor: Color) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("") }
    var entries by remember { mutableStateOf<List<LexiconItem>>(emptyList()) }
    var totalMatches by remember { mutableIntStateOf(0) }
    var offset by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(true) }

    LaunchedEffect(query) {
        loading = true
        loadingMore = false
        offset = 0
        totalMatches = ContentRepository.loadLexiconCount(context, query.trim())
        entries = ContentRepository.searchLexicon(context, query.trim(), offset = 0, limit = 100)
        offset = entries.size
        hasMore = offset < totalMatches && entries.size == 100
        loading = false
    }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            PremiumSectionHero(
                title = tr(language, "lexicon"),
                subtitle = when (language) {
                    AppLanguage.EN -> "Explore words, roots, explanations and source references."
                    AppLanguage.AR -> "استكشف المفردات والجذور والشروح والمراجع."
                    AppLanguage.CKB -> "وشە، ڕەگ، ڕوونکردنەوە و سەرچاوەکان بپشکنە."
                },
                icon = Icons.Outlined.Search,
                foreground = foreground,
                accent = Color(0xFFB9A7F1),
                metric = when (language) { AppLanguage.EN -> "$totalMatches results • $offset loaded"; AppLanguage.AR -> "$totalMatches نتيجة • $offset محمّل"; AppLanguage.CKB -> "$totalMatches ئەنجام • $offset بارکراوە" },
                eyebrow = when (language) { AppLanguage.EN -> "ARABIC & KURDISH WORDS"; AppLanguage.AR -> "المفردات العربية والكردية"; AppLanguage.CKB -> "وشە عەرەبی و کوردییەکان" }
            )
            Spacer(Modifier.height(10.dp))
            SearchBox(query, { query = it }, tr(language, "search_words"))
        }
        if (entries.isEmpty()) {
            LoadingOrEmpty(loading, language, foreground)
        } else {
            LazyColumn(
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(entries, key = { index, item -> "${item.title}:${item.root}:$index" }) { itemIndex, item ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                            .staggeredEntrance(itemKey = "${item.title}:${item.root}:$itemIndex", index = itemIndex)
                            .animateContentSize(),
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(item.title, color = Gold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            if (item.root.isNotBlank()) {
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    (if (language == AppLanguage.EN) "Root: " else if (language == AppLanguage.AR) "الجذر: " else "ڕەگ: ") + item.root,
                                    color = foreground.copy(alpha = 0.65f), fontSize = 10.sp
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(item.content, color = foreground, fontSize = 12.sp, lineHeight = 19.sp)
                            if (item.reference.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    (if (language == AppLanguage.EN) "Reference: " else if (language == AppLanguage.AR) "المصدر: " else "سەرچاوە: ") + item.reference,
                                    color = foreground.copy(alpha = 0.5f), fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
                if (hasMore) {
                    item(key = "load-more-lexicon") {
                        PremiumActionButton(
                            label = when (language) {
                                AppLanguage.EN -> if (loadingMore) "Loading more entries…" else "Load more entries"
                                AppLanguage.AR -> if (loadingMore) "جارٍ تحميل المزيد…" else "تحميل المزيد من النتائج"
                                AppLanguage.CKB -> if (loadingMore) "زیاتر بار دەکرێت…" else "دەروازەی زیاتر باربکە"
                            },
                            icon = Icons.Outlined.Search,
                            accent = Color(0xFFB9A7F1),
                            foreground = foreground,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !loadingMore
                        ) {
                            if (!loadingMore) scope.launch {
                                loadingMore = true
                                try {
                                    val next = ContentRepository.searchLexicon(context, query.trim(), offset, 100)
                                    entries = entries + next
                                    offset += next.size
                                    hasMore = next.size == 100 && offset < totalMatches
                                } finally {
                                    loadingMore = false
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

internal fun isRecentLocationFix(
    locationTimeMillis: Long,
    nowMillis: Long,
    maxAgeMillis: Long = 30L * 60L * 1000L
): Boolean {
    if (maxAgeMillis < 0L) return false
    val ageMillis = nowMillis - locationTimeMillis
    return ageMillis >= 0L && ageMillis <= maxAgeMillis
}

internal fun hasEnabledLocationProvider(context: Context): Boolean {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
    return listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).any { provider ->
        runCatching { manager.isProviderEnabled(provider) }.getOrDefault(false)
    }
}

private fun mostRecentUsableCachedLocation(
    context: Context,
    manager: LocationManager,
    nowMillis: Long
): Location? {
    val hasFinePermission = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val hasCoarsePermission = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    if (!hasFinePermission && !hasCoarsePermission) return null

    return listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        .mapNotNull { provider ->
            // Even after the explicit permission check, Android can revoke access between calls.
            runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
        }
        .filter { location ->
            isRecentLocationFix(location.time, nowMillis) &&
                location.latitude.isFinite() && location.longitude.isFinite() &&
                location.latitude in -90.0..90.0 && location.longitude in -180.0..180.0 &&
                (!location.hasAccuracy() || location.accuracy <= 50_000f)
        }
        .maxByOrNull { it.time }
        ?.let(::Location)
}


/** Accept only plausible, recent location results; provider callbacks can occasionally be stale. */
internal fun isUsableIncomingLocation(
    locationTimeMillis: Long,
    latitude: Double,
    longitude: Double,
    nowMillis: Long,
    accuracyMeters: Float? = null,
    maxAgeMillis: Long = 60_000L
): Boolean {
    if (!isRecentLocationFix(locationTimeMillis, nowMillis, maxAgeMillis)) return false
    if (!latitude.isFinite() || latitude !in -90.0..90.0) return false
    if (!longitude.isFinite() || longitude !in -180.0..180.0) return false
    if (accuracyMeters != null && (!accuracyMeters.isFinite() || accuracyMeters < 0f || accuracyMeters > 50_000f)) return false
    return true
}

@SuppressLint("MissingPermission")
internal fun requestCurrentLocation(
    context: Context,
    onLocation: (Location) -> Unit,
    onError: (String) -> Unit
) {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    if (manager == null) {
        onError("LOCATION_PERMISSION_OR_PROVIDER_UNAVAILABLE")
        return
    }
    val hasFinePermission = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val hasCoarsePermission = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    if (!hasFinePermission && !hasCoarsePermission) {
        onError("LOCATION_PERMISSION_DENIED")
        return
    }

    val now = System.currentTimeMillis()
    val cachedLocation = mostRecentUsableCachedLocation(context, manager, now)
    val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        .filter { provider -> try { manager.isProviderEnabled(provider) } catch (_: Exception) { false } }

    if (cachedLocation != null) {
        // Show a recent cached point immediately; keep searching for a live fix.
        // Caller labels it as cached based on Location.time, never as fresh GPS.
        onLocation(Location(cachedLocation))
    }
    if (providers.isEmpty()) {
        if (cachedLocation != null) {
            onError("LOCATION_LAST_KNOWN_ONLY")
        } else {
            val openedSettings = runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }.isSuccess
            onError(if (openedSettings) "LOCATION_SETTINGS_OPENED" else "LOCATION_SERVICES_OFF")
        }
        return
    }

    val mainHandler = Handler(Looper.getMainLooper())
    val listeners = mutableListOf<LocationListener>()
    val cancellationSignals = mutableListOf<CancellationSignal>()
    var bestLocation: Location? = null
    var finished = false
    var deferredFinish: Runnable? = null
    var timeout: Runnable? = null
    var registeredProviders = 0
    lateinit var finishBest: Runnable

    fun cleanup() {
        timeout?.let(mainHandler::removeCallbacks)
        deferredFinish?.let(mainHandler::removeCallbacks)
        listeners.forEach { listener ->
            try { manager.removeUpdates(listener) } catch (_: Exception) {}
        }
        cancellationSignals.forEach { signal -> runCatching { signal.cancel() } }
        cancellationSignals.clear()
    }

    fun considerLocation(location: Location?) {
        if (finished || location == null) return
        val accuracy = if (location.hasAccuracy()) location.accuracy else null
        if (!isUsableIncomingLocation(
                location.time, location.latitude, location.longitude,
                System.currentTimeMillis(), accuracy, maxAgeMillis = 60_000L
            )
        ) return

        // A fresh sensor fix supersedes the cached point even if accuracy is marginally worse:
        // the user may have moved since the cache was recorded.
        val prior = bestLocation
        val newIsBetter = prior == null ||
            (location.hasAccuracy() && (!prior.hasAccuracy() || location.accuracy < prior.accuracy))
        if (newIsBetter) bestLocation = Location(location)

        if (bestLocation?.hasAccuracy() == true && bestLocation!!.accuracy <= 15f) {
            finishBest.run()
        } else {
            deferredFinish?.let(mainHandler::removeCallbacks)
            deferredFinish = Runnable { finishBest.run() }
            mainHandler.postDelayed(deferredFinish!!, 3500L)
        }
    }

    finishBest = Runnable {
        if (!finished) {
            finished = true
            cleanup()
            val best = bestLocation
            if (best != null) onLocation(best)
            else if (cachedLocation != null) onError("LOCATION_LAST_KNOWN_ONLY")
            else onError("No fresh location fix. Turn on GPS and try again outdoors.")
        }
    }
    timeout = Runnable { finishBest.run() }
    mainHandler.postDelayed(timeout!!, 20_000L)

    providers.forEach { provider ->
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) = considerLocation(location)
            @Deprecated("Deprecated by Android; retained for API 24 compatibility")
            override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) = Unit
            override fun onProviderEnabled(provider: String) = Unit
            override fun onProviderDisabled(provider: String) = Unit
        }
        listeners += listener
        var signal: CancellationSignal? = null
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+: use the current-location request API instead of the deprecated single-update path.
                signal = CancellationSignal()
                cancellationSignals += signal
                manager.getCurrentLocation(provider, signal, context.mainExecutor) { location ->
                    considerLocation(location)
                }
            } else {
                manager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
            }
            registeredProviders += 1
        } catch (_: SecurityException) {
            signal?.let {
                runCatching { it.cancel() }
                cancellationSignals.remove(it)
            }
            try { manager.removeUpdates(listener) } catch (_: Exception) {}
            listeners.remove(listener)
        } catch (_: IllegalArgumentException) {
            signal?.let {
                runCatching { it.cancel() }
                cancellationSignals.remove(it)
            }
            try { manager.removeUpdates(listener) } catch (_: Exception) {}
            listeners.remove(listener)
        }
    }
    if (registeredProviders == 0 && !finished) {
        finished = true
        cleanup()
        if (cachedLocation != null) onError("LOCATION_LAST_KNOWN_ONLY")
        else onError("LOCATION_PERMISSION_OR_PROVIDER_UNAVAILABLE")
    }
}

private data class PrayerCountdownTarget(
    val prayerKey: String,
    val timeText: String,
    val targetMillis: Long
)

private fun prayerTimestampForDate(date: Date, clockText: String): Long? {
    val parts = clockText.trim().split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: return null
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return Calendar.getInstance().apply {
        time = date
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun formatPrayerCountdown(remainingMillis: Long): String {
    val totalSeconds = remainingMillis.coerceAtLeast(0L) / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
}

/**
 * Present the verified timetable source as human-readable UI copy. Raw source
 * keys and database routing labels should never leak into the prayer screen.
 */
internal fun prayerSourceDescription(source: String, language: AppLanguage): String {
    val normalized = source.substringBefore(" • ").trim().lowercase(Locale.ROOT)
    val isSalawati = normalized.startsWith("salawati")
    if (!isSalawati) {
        return when (language) {
            AppLanguage.EN -> "Prayer timetable source unavailable"
            AppLanguage.AR -> "مصدر جدول الصلاة غير متاح"
            AppLanguage.CKB -> "سەرچاوەی خشتەی نوێژ بەردەست نییە"
        }
    }

    val linkedLocality = normalized.startsWith("salawati-dependent")
    val distanceKm = Regex("""(\d+(?:\.\d+)?)\s*km\b""", RegexOption.IGNORE_CASE)
        .find(source)?.groupValues?.getOrNull(1)?.toDoubleOrNull()

    val timetableLabel = when (language) {
        AppLanguage.EN -> if (linkedLocality) "Salawati original timetable (linked locality)" else "Salawati original timetable"
        AppLanguage.AR -> if (linkedLocality) "جدول صلواتي الأصلي (مدينة مرتبطة)" else "جدول صلواتي الأصلي"
        AppLanguage.CKB -> if (linkedLocality) "خشتەی ڕەسەنی صەڵەواتی (شاری بەستراو)" else "خشتەی ڕەسەنی صەڵەواتی"
    }
    val distanceLabel = distanceKm?.let { distance ->
        val formatted = String.format(Locale.US, "%.1f", distance)
        when (language) {
            AppLanguage.EN -> " • nearest source schedule $formatted km away"
            AppLanguage.AR -> " • أقرب جدول مصدر يبعد $formatted كم"
            AppLanguage.CKB -> " • نزیکترین خشتەی سەرچاوە $formatted کیلۆمەتر دوورە"
        }
    }.orEmpty()
    return when (language) {
        AppLanguage.EN -> "Source: $timetableLabel$distanceLabel. Times shown in local phone time."
        AppLanguage.AR -> "المصدر: $timetableLabel$distanceLabel. الأوقات بتوقيت الهاتف المحلي."
        AppLanguage.CKB -> "سەرچاوە: $timetableLabel$distanceLabel. کاتەکان بە کاتی مۆبایلەکەتن."
    }
}

@OptIn(ExperimentalMaterial3Api::class)
internal fun prayerJourneyProgressFraction(nowMillis: Long, fajrMillis: Long, ishaMillis: Long): Float {
    if (ishaMillis <= fajrMillis) return 0f
    return ((nowMillis - fajrMillis).toDouble() / (ishaMillis - fajrMillis).toDouble())
        .toFloat()
        .coerceIn(0f, 1f)
}

private data class PrayerJourneyMarker(
    val key: String,
    val label: String,
    val fraction: Float
)

@Composable
private fun PrayerJourneyTimeline(
    language: AppLanguage,
    foreground: Color,
    cardColor: Color,
    markers: List<PrayerJourneyMarker>,
    progress: Float,
    nextPrayerKey: String?
) {
    val context = LocalContext.current
    val motionAllowed = remember(context) { systemAllowsDecorativeMotion(context) }
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = if (motionAllowed) tween(720, easing = FastOutSlowInEasing) else snap(),
        label = "Prayer day timeline progress"
    )
    val cursorPulse = if (motionAllowed) {
        val transition = rememberInfiniteTransition(label = "Prayer timeline live marker")
        val scale by transition.animateFloat(
            initialValue = 0.88f,
            targetValue = 1.16f,
            animationSpec = infiniteRepeatable(
                animation = tween(1050, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "Prayer timeline cursor pulse"
        )
        scale
    } else 1f
    val percent = (progress * 100f).roundToInt().coerceIn(0, 100)
    val title = when (language) {
        AppLanguage.EN -> "Your prayer journey"
        AppLanguage.AR -> "مسار صلوات يومك"
        AppLanguage.CKB -> "ڕێڕەوی نوێژەکانی ڕۆژت"
    }
    val subtitle = when (language) {
        AppLanguage.EN -> "From Fajr to Isha"
        AppLanguage.AR -> "من الفجر إلى العشاء"
        AppLanguage.CKB -> "لە بەیانەوە تا عیشا"
    }
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, Gold.copy(alpha = 0.18f))
    ) {
        Column(
            Modifier.fillMaxWidth()
                .background(Brush.linearGradient(listOf(Gold.copy(alpha = 0.055f), cardColor, cardColor)))
                .padding(horizontal = 14.dp, vertical = 13.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, color = foreground, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text(subtitle, color = foreground.copy(alpha = 0.52f), fontSize = 9.sp)
                }
                AnimatedContent(
                    targetState = percent,
                    transitionSpec = {
                        (fadeIn(tween(120)) + scaleIn(initialScale = 0.88f, animationSpec = tween(160))) togetherWith
                            (fadeOut(tween(90)) + scaleOut(targetScale = 1.06f, animationSpec = tween(90)))
                    },
                    label = "Prayer day percentage"
                ) { value ->
                    Text("$value%", color = Gold, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
            Spacer(Modifier.height(15.dp))
            BoxWithConstraints(Modifier.fillMaxWidth().height(26.dp)) {
                val trackWidth = maxWidth
                Box(
                    Modifier.align(Alignment.CenterStart).fillMaxWidth().height(4.dp)
                        .clip(CircleShape).background(foreground.copy(alpha = 0.10f))
                )
                Box(
                    Modifier.align(Alignment.CenterStart).fillMaxWidth(animatedProgress).height(4.dp)
                        .clip(CircleShape)
                        .background(Brush.horizontalGradient(listOf(Color(0xFF69B994), Gold)))
                )
                markers.forEach { marker ->
                    val x = (trackWidth.value * marker.fraction.coerceIn(0f, 1f) - 4.5f).dp
                    val completed = marker.fraction <= animatedProgress
                    val selected = marker.key == nextPrayerKey
                    Box(
                        Modifier.align(Alignment.CenterStart).offset(x = x)
                            .size(if (selected) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(if (selected) Gold else if (completed) Color(0xFF75C9A2) else foreground.copy(alpha = 0.38f))
                            .border(1.dp, cardColor, CircleShape)
                    )
                }
                if (markers.isNotEmpty()) {
                    val cursorX = (trackWidth.value * animatedProgress - 6.dp.value).dp
                    Box(
                        Modifier.align(Alignment.CenterStart).offset(x = cursorX)
                            .size(12.dp).graphicsLayer { scaleX = cursorPulse; scaleY = cursorPulse }
                            .clip(CircleShape)
                            .background(Color(0xFFFFE4A0).copy(alpha = 0.22f))
                            .border(1.5.dp, Gold, CircleShape)
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                markers.forEach { marker ->
                    val selected = marker.key == nextPrayerKey
                    Text(
                        marker.label,
                        color = if (selected) Gold else foreground.copy(alpha = 0.56f),
                        fontSize = 8.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun PrayerScreen(language: AppLanguage, foreground: Color, cardColor: Color, selectedCity: String, use24Hour: Boolean = false, onCity: (City) -> Unit, onQibla: () -> Unit, onBrowseAdhanLibrary: () -> Unit = {}, isActive: Boolean = true) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("mashxall_preferences", Context.MODE_PRIVATE) }
    var cities by remember { mutableStateOf<List<City>>(emptyList()) }
    var times by remember { mutableStateOf<PrayerRow?>(null) }
    var tomorrowTimes by remember { mutableStateOf<PrayerRow?>(null) }
    var clockNowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var chooseCity by remember { mutableStateOf(false) }
    var citySearch by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var locating by remember { mutableStateOf(false) }
    var locationMessage by remember { mutableStateOf("") }
    var retryLocationWhenResumed by remember { mutableStateOf(false) }
    var locationMode by rememberSaveable {
        mutableStateOf(
            resolvePrayerLocationMode(
                prefs.getString("prayer_location_mode", null),
                prefs.getString("gps_latitude", null)?.toDoubleOrNull() != null &&
                    prefs.getString("gps_longitude", null)?.toDoubleOrNull() != null,
                prefs.getString("manual_city_latitude", null)?.toDoubleOrNull() != null &&
                    prefs.getString("manual_city_longitude", null)?.toDoubleOrNull() != null,
                hasUsableCitySelection = selectedCity.isNotBlank()
            )
        )
    }
    val usableSavedGps = remember {
        val lat = prefs.getString("gps_latitude", null)?.toDoubleOrNull()
        val lon = prefs.getString("gps_longitude", null)?.toDoubleOrNull()
        if (locationMode == "gps" && isUsableSavedGpsFix(
                lat, lon, prefs.getLong("gps_location_timestamp", 0L), System.currentTimeMillis()
            )
        ) lat to lon else null
    }
    var gpsLatitude by remember { mutableStateOf(usableSavedGps?.first) }
    var gpsLongitude by remember { mutableStateOf(usableSavedGps?.second) }
    var gpsAccuracy by remember {
        mutableStateOf(if (usableSavedGps != null) prefs.getString("gps_accuracy", null)?.toFloatOrNull() else null)
    }
    var nearestKnownCity by remember { mutableStateOf<NearbySalawatiCity?>(null) }
    var manualLatitude by remember { mutableStateOf(prefs.getString("manual_city_latitude", null)?.toDoubleOrNull()) }
    var manualLongitude by remember { mutableStateOf(prefs.getString("manual_city_longitude", null)?.toDoubleOrNull()) }
    val manualCountry = prefs.getString("selected_city_country", "") ?: ""
    var adhanEnabled by rememberSaveable { mutableStateOf(prefs.getBoolean("adhan_enabled", false)) }
    var adhanAssets by remember { mutableStateOf(emptyList<AudioItem>()) }
    var adhanAssetsLoaded by remember { mutableStateOf(false) }
    var onlineAdhanCount by remember { mutableIntStateOf(0) }
    var selectedAdhanAsset by rememberSaveable {
        mutableStateOf(prefs.getString("adhan_audio_asset", "") ?: "")
    }
    var adhanSoundQuery by rememberSaveable { mutableStateOf("") }
    var previewingAdhan by remember { mutableStateOf("") }
    var chooseAdhanSound by remember { mutableStateOf(false) }
    var adhanMessage by remember { mutableStateOf("") }
    val visibleCities = remember(cities, citySearch) {
        if (citySearch.isBlank()) cities
        else cities.filter { city ->
            citySearchMatchesQuery(citySearch.trim(), city.name, city.country)
        }
    }

    val onFix: (Location) -> Unit = { fix ->
        // A cached point can immediately populate the timetable, but it is not reported
        // as live until Android supplies a recent Location.time.
        val receivedAt = System.currentTimeMillis()
        val isFreshFix = isRecentLocationFix(fix.time, receivedAt, 60_000L)
        locating = !isFreshFix
        gpsLatitude = fix.latitude
        gpsLongitude = fix.longitude
        gpsAccuracy = if (fix.hasAccuracy()) fix.accuracy else null
        locationMode = "gps"
        manualLatitude = null
        manualLongitude = null
        prefs.edit()
            .putString("prayer_location_mode", "gps")
            .putString("gps_latitude", fix.latitude.toString())
            .putString("gps_longitude", fix.longitude.toString())
            .putLong("gps_location_timestamp", fix.time)
            .putString("gps_accuracy", if (fix.hasAccuracy()) fix.accuracy.toString() else "")
            .remove("manual_city_latitude")
            .remove("manual_city_longitude")
            .apply()
        locationMessage = if (!isFreshFix) {
            val ageMinutes = ((receivedAt - fix.time).coerceAtLeast(0L) / 60_000L)
            when (language) {
                AppLanguage.EN -> "Using last known location (~$ageMinutes min old) while refreshing GPS/network…"
                AppLanguage.AR -> "يُستخدم آخر موقع محفوظ (عمره نحو $ageMinutes دقيقة) أثناء تحديث GPS/الشبكة…"
                AppLanguage.CKB -> "شوێنی پێشوو بەکاردێت (نزیکەی $ageMinutes خولەک) تا GPS/تۆڕ نوێ دەبێتەوە…"
            }
        } else if (fix.hasAccuracy()) {
            val acc = fix.accuracy.toInt().toString()
            when (language) {
                AppLanguage.EN -> "Live GPS/network fix; reported accuracy about ±$acc m"
                AppLanguage.AR -> "تم تحديث الموقع مباشرة؛ الدقة التقريبية ±$acc متر"
                AppLanguage.CKB -> "شوێنی ڕاستەوخۆ نوێکرایەوە؛ وردبینی نزیکەی ±$acc مەتر"
            }
        } else {
            when (language) {
                AppLanguage.EN -> "Live coordinates received; Android did not report accuracy."
                AppLanguage.AR -> "تم تحديث الإحداثيات مباشرة، لكن الهاتف لم يذكر دقة القياس."
                AppLanguage.CKB -> "کۆئۆردیناتی ڕاستەوخۆ وەرگیرا، بەڵام وردبینی نەدراوە."
            }
        }
    }
    val onLocationError: (String) -> Unit = { error ->
        locating = false
        if (error == "LOCATION_SETTINGS_OPENED") retryLocationWhenResumed = true
        locationMessage = when (error) {
            "LOCATION_LAST_KNOWN_ONLY" -> when (language) {
                AppLanguage.EN -> "Showing the recent saved location, but Android could not refresh it. Turn on Location and retry."
                AppLanguage.AR -> "يُعرض الموقع المحفوظ حديثًا، لكن Android لم يستطع تحديثه. فعّل الموقع ثم أعد المحاولة."
                AppLanguage.CKB -> "شوێنی پاشەکەوتکراوی نزیک پیشان دەدرێت، بەڵام ئەندرۆید نوێی نەکردەوە؛ شوێن چالاک بکە و دووبارە هەوڵ بدەوە."
            }
            "LOCATION_SETTINGS_OPENED" -> when (language) {
                AppLanguage.EN -> "Location settings opened. Turn on Location, return here, then retry."
                AppLanguage.AR -> "فُتحت إعدادات الموقع. فعّل الموقع، ثم ارجع إلى التطبيق وأعد المحاولة."
                AppLanguage.CKB -> "ڕێکخستنەکانی شوێن کرانەوە؛ شوێن چالاک بکە، بگەڕێوە و دووبارە هەوڵ بدەوە."
            }
            "LOCATION_SERVICES_OFF" -> when (language) {
                AppLanguage.EN -> "Location services are off. Turn them on in Android settings, then retry."
                AppLanguage.AR -> "خدمات الموقع متوقفة. فعّلها من إعدادات Android ثم أعد المحاولة."
                AppLanguage.CKB -> "خزمەتگوزاری شوێن ناچالاکە؛ لە ڕێکخستنەکانی Android چالاکی بکە و دووبارە هەوڵ بدەوە."
            }
            "LOCATION_PERMISSION_DENIED" -> when (language) {
                AppLanguage.EN -> "Location permission is not currently granted. Allow it in Android App settings, then retry."
                AppLanguage.AR -> "إذن الموقع غير ممنوح حاليًا. اسمح به من إعدادات التطبيق في Android ثم أعد المحاولة."
                AppLanguage.CKB -> "مۆڵەتی شوێن لەم کاتەدا نەدراوە؛ لە ڕێکخستنەکانی ئەپی Android مۆڵەت بدە و دووبارە هەوڵ بدەوە."
            }
            "LOCATION_PERMISSION_DENIED" -> when (language) {
                AppLanguage.EN -> "Location permission is not currently granted. Allow it in Android App settings, then retry."
                AppLanguage.AR -> "إذن الموقع غير ممنوح حاليًا. اسمح به من إعدادات التطبيق في Android ثم أعد المحاولة."
                AppLanguage.CKB -> "مۆڵەتی شوێن لەم کاتەدا نەدراوە؛ لە ڕێکخستنەکانی ئەپی Android مۆڵەت بدە و دووبارە هەوڵ بدەوە."
            }
            "LOCATION_PERMISSION_OR_PROVIDER_UNAVAILABLE" -> when (language) {
                AppLanguage.EN -> "Android could not start a location provider. Check Location permissions and turn on GPS/network location."
                AppLanguage.AR -> "تعذّر تشغيل مزوّد الموقع في Android. تحقّق من الأذونات وفعّل GPS/موقع الشبكة."
                AppLanguage.CKB -> "ئەندرۆید نەیتوانی دابینکەری شوێن دەستپێبکات؛ مۆڵەتەکان بپشکنە و GPS/شوێنی تۆڕ چالاک بکە."
            }
            else -> when (language) {
                AppLanguage.EN -> "Could not obtain a fresh location. Check Android Location settings and retry."
                AppLanguage.AR -> "تعذّر الحصول على موقع حديث. تحقّق من إعدادات الموقع في Android ثم أعد المحاولة."
                AppLanguage.CKB -> "نەتوانرا شوێنێکی نوێ وەربگیرێت؛ ڕێکخستنەکانی شوێنی Android بپشکنە و دووبارە هەوڵ بدەوە."
            }
        }
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            adhanEnabled = true
            prefs.edit().putBoolean("adhan_enabled", true).putString("adhan_audio_asset", selectedAdhanAsset).apply()
            adhanMessage = ""
        } else {
            adhanEnabled = false
            prefs.edit().putBoolean("adhan_enabled", false).apply()
            PrayerAlarmScheduler.cancelAll(context)
            adhanMessage = when (language) {
                AppLanguage.EN -> "Notification permission was denied; prayer alerts remain off."
                AppLanguage.AR -> "رُفض إذن الإشعارات؛ بقيت تنبيهات الصلاة متوقفة."
                AppLanguage.CKB -> "مۆڵەتی ئاگادارکردنەوە ڕەتکرایەوە؛ ئاگادارکردنەوەکان ناچالاکن."
            }
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val allowed = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (allowed) {
            locationMode = "gps"
            prefs.edit().putString("prayer_location_mode", "gps").apply()
            locating = true
            locationMessage = ""
            requestCurrentLocation(context, onFix, onLocationError)
        } else {
            locating = false
            locationMessage = when (language) {
                AppLanguage.EN -> "Location permission was not granted. Choose a city instead."
                AppLanguage.AR -> "لم تمنح إذن الموقع. يمكنك اختيار مدينة يدويًا."
                AppLanguage.CKB -> "مۆڵەتی شوێن نەدرا؛ دەتوانیت شارێک هەڵبژێریت."
            }
        }
    }
    val locateNow = {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (fine || coarse) {
            locationMode = "gps"
            prefs.edit().putString("prayer_location_mode", "gps").apply()
            locating = true
            locationMessage = ""
            requestCurrentLocation(context, onFix, onLocationError)
        } else {
            locationMode = "gps"
            prefs.edit().putString("prayer_location_mode", "gps").apply()
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    val prayerLifecycleOwner = context as? LifecycleOwner
    DisposableEffect(prayerLifecycleOwner, retryLocationWhenResumed) {
        if (prayerLifecycleOwner == null || !retryLocationWhenResumed) {
            onDispose { }
        } else {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME && retryLocationWhenResumed) {
                    val permissionGranted =
                        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    val retry = shouldRetryLocationOnResume(retryLocationWhenResumed, true, permissionGranted)
                    retryLocationWhenResumed = false
                    when {
                        !permissionGranted -> onLocationError("LOCATION_PERMISSION_DENIED")
                        !hasEnabledLocationProvider(context) -> onLocationError("LOCATION_SERVICES_OFF")
                        retry -> locateNow()
                    }
                }
            }
            prayerLifecycleOwner.lifecycle.addObserver(observer)
            onDispose { prayerLifecycleOwner.lifecycle.removeObserver(observer) }
        }
    }

    LaunchedEffect(locationMode, gpsLatitude, gpsLongitude) {
        val latitude = gpsLatitude
        val longitude = gpsLongitude
        nearestKnownCity = null
        if (locationMode == "gps" && latitude != null && longitude != null) {
            nearestKnownCity = try {
                ContentRepository.findNearestSalawatiCityAtLocation(context, latitude, longitude)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
        }
    }

    LaunchedEffect(isActive, selectedCity) {
        if (!isActive) return@LaunchedEffect

        // Sync the saved mode and coordinates whenever the Prayer screen is re-entered.
        // Otherwise a restored composition can keep the old city/GPS mode in memory.
        val savedGpsLat = prefs.getString("gps_latitude", null)?.toDoubleOrNull()
        val savedGpsLon = prefs.getString("gps_longitude", null)?.toDoubleOrNull()
        val savedGpsTime = prefs.getLong("gps_location_timestamp", 0L)
        val savedManualLat = prefs.getString("manual_city_latitude", null)?.toDoubleOrNull()
        val savedManualLon = prefs.getString("manual_city_longitude", null)?.toDoubleOrNull()
        val hasManualCoordinates = savedManualLat != null && savedManualLon != null
        val savedMode = resolvePrayerLocationMode(
            prefs.getString("prayer_location_mode", null),
            savedGpsLat != null && savedGpsLon != null,
            hasManualCoordinates,
            hasUsableCitySelection = selectedCity.isNotBlank()
        )
        locationMode = savedMode
        manualLatitude = savedManualLat
        manualLongitude = savedManualLon
        if (savedMode == "gps") {
            val savedFixIsFresh = isUsableSavedGpsFix(
                savedGpsLat, savedGpsLon, savedGpsTime, System.currentTimeMillis()
            )
            gpsLatitude = savedGpsLat.takeIf { savedFixIsFresh }
            gpsLongitude = savedGpsLon.takeIf { savedFixIsFresh }
            gpsAccuracy = if (savedFixIsFresh) prefs.getString("gps_accuracy", null)?.toFloatOrNull() else null
        } else {
            gpsLatitude = null
            gpsLongitude = null
            gpsAccuracy = null
        }

        val alreadyAsked = prefs.getBoolean("prayer_location_prompted", false)
        val missingGpsFix = gpsLatitude == null || gpsLongitude == null
        if (savedMode == "gps") {
            val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            when {
                fine || coarse -> {
                    // Refresh current location on entry; schedule lookup remains tied to Salawati source rows.
                    locateNow()
                }
                !alreadyAsked -> {
                    prefs.edit().putBoolean("prayer_location_prompted", true).apply()
                    locateNow()
                }
                missingGpsFix -> locationMessage = when (language) {
                    AppLanguage.EN -> "Allow location permission to show your current location, or choose a city."
                    AppLanguage.AR -> "اسمح بإذن الموقع لعرض موقعك الحالي، أو اختر مدينة."
                    AppLanguage.CKB -> "بۆ پیشاندانی شوێنی ئێستات مۆڵەتی شوێن بدە یان شارێک هەڵبژێرە."
                }
            }
        } else if (!alreadyAsked && missingGpsFix && savedManualLat == null && savedManualLon == null) {
            prefs.edit().putBoolean("prayer_location_prompted", true).apply()
            locateNow()
        }
    }

    LaunchedEffect(citySearch) {
        delay(250L)
        cities = ContentRepository.searchCities(context, citySearch.trim(), limit = 220)
    }
    LaunchedEffect(Unit) {
        while (true) {
            clockNowMillis = System.currentTimeMillis()
            // A 15-second refresh is sufficient for a prayer countdown and
            // avoids forcing expensive prayer-screen recomposition every second.
            delay(15_000L)
        }
    }
    val currentLocalDateKey = remember(clockNowMillis) {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(clockNowMillis))
    }
    val visibleAdhanAssets = remember(adhanAssets, adhanSoundQuery) {
        if (adhanSoundQuery.isBlank()) adhanAssets else adhanAssets.filter { sound ->
            (sound.title + " " + sound.group + " " + sound.assetPath)
                .contains(adhanSoundQuery.trim(), ignoreCase = true)
        }
    }
    LaunchedEffect(isActive, language) {
        if (!isActive) return@LaunchedEffect
        adhanAssetsLoaded = false

        // Only actual Adhan/mosque recordings are selectable for scheduled alerts.
        // Generic Quran recitation and unrelated source audio must never become an adhan by fallback.
        val bundledAssets = filterSelectableAdhanAssets(ContentRepository.loadAudioItems(context))
        val onlineCatalog = ContentRepository.loadOnlineAdhan(context)
        onlineAdhanCount = onlineCatalog.size
        val downloadedOnlineAssets = onlineCatalog.mapNotNull { item ->
            val file = onlineAdhanOfflineFile(context, item)
            if (file.isFile && file.length() > 512L) {
                AudioItem(
                    assetPath = "local:${file.absolutePath}",
                    title = item.titleArabic.ifBlank { item.titleEnglish },
                    group = if (language == AppLanguage.EN) "Downloaded online adhan" else if (language == AppLanguage.AR) "أذان منزّل للاستماع دون إنترنت" else "بانگی داگیراو بۆ بەبێ ئینتەرنێت"
                )
            } else null
        }
        val availableAssets = filterSelectableAdhanAssets(bundledAssets + downloadedOnlineAssets)
        adhanAssets = availableAssets
        adhanAssetsLoaded = true
        if (availableAssets.none { it.assetPath == selectedAdhanAsset }) {
            val recommended = availableAssets.firstOrNull()
            selectedAdhanAsset = recommended?.assetPath.orEmpty()
            prefs.edit().putString("adhan_audio_asset", selectedAdhanAsset).apply()
            if (recommended == null && adhanEnabled) {
                adhanEnabled = false
                prefs.edit().putBoolean("adhan_enabled", false).apply()
                PrayerAlarmScheduler.cancelAll(context)
                adhanMessage = adhanSourceRequiredMessage(language)
            }
        }
    }
    LaunchedEffect(adhanEnabled, selectedAdhanAsset, adhanAssets, adhanAssetsLoaded, locationMode, gpsLatitude, gpsLongitude, manualLatitude, manualLongitude, manualCountry, selectedCity, language) {
        if (adhanEnabled) {
            if (!adhanAssetsLoaded) return@LaunchedEffect
            val selectedSourceIsValid = hasSelectableAdhanAsset(selectedAdhanAsset, adhanAssets)
            if (!selectedSourceIsValid) {
                adhanEnabled = false
                prefs.edit().putBoolean("adhan_enabled", false).putString("adhan_audio_asset", "").apply()
                PrayerAlarmScheduler.cancelAll(context)
                adhanMessage = adhanSourceRequiredMessage(language)
            } else {
                prefs.edit().putString("adhan_audio_asset", selectedAdhanAsset).apply()
                val result = try {
                    PrayerAlarmScheduler.scheduleUpcomingFromPreferences(context)
                } catch (_: Exception) {
                    null
                }
                adhanMessage = if (result != null) {
                    prayerAlarmScheduleMessage(result, language)
                } else {
                    prayerAlarmScheduleFailureMessage(language)
                }
            }
        } else {
            PrayerAlarmScheduler.cancelAll(context)
            // Preserve a useful error when no genuine Adhan source is selected.
            if (adhanAssetsLoaded && hasSelectableAdhanAsset(selectedAdhanAsset, adhanAssets)) adhanMessage = ""
        }
    }
    LaunchedEffect(selectedCity, locationMode, gpsLatitude, gpsLongitude, manualLatitude, manualLongitude) {
        PrayerTimesWidgetProvider.requestRefresh(context)
    }
    LaunchedEffect(selectedCity, locationMode, gpsLatitude, gpsLongitude, manualLatitude, manualLongitude, manualCountry, currentLocalDateKey) {
        loading = true
        times = null
        tomorrowTimes = null
        val targetDate = Date(clockNowMillis)
        times = try {
            when {
                locationMode == "gps" && gpsLatitude != null && gpsLongitude != null -> ContentRepository.loadPrayerTimesAtLocation(
                    context, gpsLatitude!!, gpsLongitude!!, targetDate
                )
                locationMode == "gps" -> null
                manualLatitude != null && manualLongitude != null -> {
                    ContentRepository.loadPrayerTimes(context, selectedCity, targetDate, manualCountry)
                        ?: ContentRepository.loadPrayerTimesAtLocation(
                            context, manualLatitude!!, manualLongitude!!, targetDate
                        )
                }
                else -> {
                    ContentRepository.loadPrayerTimes(context, selectedCity, targetDate, manualCountry)
                        ?: ContentRepository.findCityByName(context, selectedCity, manualCountry)?.let { city ->
                            ContentRepository.loadPrayerTimesAtLocation(context, city.latitude, city.longitude, targetDate)
                        }
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
        loading = false
    }
    LaunchedEffect(
        selectedCity, locationMode, gpsLatitude, gpsLongitude, manualLatitude, manualLongitude,
        manualCountry, currentLocalDateKey
    ) {
        tomorrowTimes = null
        val tomorrow = Calendar.getInstance().apply {
            timeInMillis = clockNowMillis
            add(Calendar.DAY_OF_YEAR, 1)
        }.time
        tomorrowTimes = try {
            when {
                locationMode == "gps" && gpsLatitude != null && gpsLongitude != null ->
                    ContentRepository.loadPrayerTimesAtLocation(
                        context, gpsLatitude!!, gpsLongitude!!, tomorrow
                    )
                locationMode == "gps" -> null
                manualLatitude != null && manualLongitude != null -> {
                    ContentRepository.loadPrayerTimes(context, selectedCity, tomorrow, manualCountry)
                        ?: ContentRepository.loadPrayerTimesAtLocation(
                            context, manualLatitude!!, manualLongitude!!, tomorrow
                        )
                }
                else -> {
                    ContentRepository.loadPrayerTimes(context, selectedCity, tomorrow, manualCountry)
                        ?: ContentRepository.findCityByName(context, selectedCity, manualCountry)?.let { city ->
                            ContentRepository.loadPrayerTimesAtLocation(context, city.latitude, city.longitude, tomorrow)
                        }
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
    }
    val nearestKnownCityLabel = nearestKnownCity?.city?.name?.let { cityName ->
        localizedKurdistanCityName(cityName, language.key).ifBlank { cityName }
    }
    val nearestKnownCityCountry = nearestKnownCity?.city?.country.orEmpty()
    val nearestKnownDistance = nearestKnownCity?.distanceKm?.let { "%.1f".format(Locale.US, it) }.orEmpty()
    val nextPrayerTarget = remember(times, tomorrowTimes, clockNowMillis) {
        val currentTimes = times
        if (currentTimes == null) {
            null
        } else {
            val now = Date(clockNowMillis)
            val todaysPrayers = listOf(
                "fajr" to currentTimes.fajr,
                "dhuhr" to currentTimes.dhuhr,
                "asr" to currentTimes.asr,
                "maghrib" to currentTimes.maghrib,
                "isha" to currentTimes.isha
            )
            val nextToday = todaysPrayers.firstNotNullOfOrNull { (key, text) ->
                val target = prayerTimestampForDate(now, text)
                if (target != null && target > clockNowMillis) PrayerCountdownTarget(key, text, target) else null
            }
            nextToday ?: tomorrowTimes?.let { nextDay ->
                val nextDate = Calendar.getInstance().apply {
                    timeInMillis = clockNowMillis
                    add(Calendar.DAY_OF_YEAR, 1)
                }.time
                prayerTimestampForDate(nextDate, nextDay.fajr)?.let { target ->
                    PrayerCountdownTarget("fajr", nextDay.fajr, target)
                }
            }
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        PremiumSectionHero(
            title = tr(language, "prayer"),
            subtitle = when (language) {
                AppLanguage.EN -> "Accurate local prayer times, location-aware timetable and Adhan controls."
                AppLanguage.AR -> "مواقيت محلية دقيقة، وجدول مرتبط بموقعك، وتحكم بتنبيهات الأذان."
                AppLanguage.CKB -> "کاتی وردی نوێژ، خشتەی پەیوەست بە شوێن و کۆنترۆڵی ئاگادارکردنەوەی بانگ."
            },
            icon = Icons.Outlined.Schedule,
            foreground = foreground,
            accent = Color(0xFF84B9EA),
            metric = when (language) {
                AppLanguage.EN -> if (locationMode == "gps" && gpsLatitude != null) "GPS LOCATION ACTIVE" else "SALAWATI SOURCE"
                AppLanguage.AR -> if (locationMode == "gps" && gpsLatitude != null) "الموقع عبر GPS مفعّل" else "مصدر صلواتي"
                AppLanguage.CKB -> if (locationMode == "gps" && gpsLatitude != null) "شوێنی GPS چالاکە" else "سەرچاوەی صەڵەواتی"
            },
            eyebrow = when (language) { AppLanguage.EN -> "YOUR DAILY PRAYER RHYTHM"; AppLanguage.AR -> "مواقيت عبادتك اليومية"; AppLanguage.CKB -> "کاتی نوێژی ڕۆژانەت" }
        )
        Spacer(Modifier.height(10.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = cardColor),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(38.dp).clip(RoundedCornerShape(13.dp)).background(Gold.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Outlined.Schedule, null, tint = Gold, modifier = Modifier.size(22.dp)) }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        when (language) { AppLanguage.EN -> "Today's prayer times"; AppLanguage.AR -> "مواقيت صلاة اليوم"; AppLanguage.CKB -> "کاتەکانی نوێژی ئەمڕۆ" },
                        color = foreground, fontWeight = FontWeight.Bold, fontSize = 13.sp
                    )
                    Text(
                        SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).format(Date(clockNowMillis)),
                        color = foreground.copy(alpha = 0.55f), fontSize = 10.sp
                    )
                }
                Text(
                    when (language) { AppLanguage.EN -> "LIVE"; AppLanguage.AR -> "مباشر"; AppLanguage.CKB -> "زیندوو" },
                    color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Card(colors = CardDefaults.cardColors(containerColor = DeepGreen), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Explore, null, tint = Gold, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(tr(language, "city"), color = Color(0xFFF4E5B4), fontSize = 11.sp)
                        val nearestScheduleCity = times?.city
                            ?.takeIf { gpsLatitude != null && gpsLongitude != null }
                            ?.let { cityName -> localizedKurdistanCityName(cityName, language.key).ifBlank { cityName } }
                        Text(
                            if (locationMode == "gps") {
                                if (gpsLatitude != null && gpsLongitude != null) {
                                    when {
                                        nearestScheduleCity != null -> when (language) {
                                            AppLanguage.EN -> "Your GPS position • nearest source schedule: $nearestScheduleCity"
                                            AppLanguage.AR -> "موقعك عبر GPS • أقرب جدول مصدر: $nearestScheduleCity"
                                            AppLanguage.CKB -> "شوێنی GPSی تۆ • نزیکترین خشتەی سەرچاوە: $nearestScheduleCity"
                                        }
                                        nearestKnownCityLabel != null -> when (language) {
                                            AppLanguage.EN -> "GPS found • nearest Salawati locality: $nearestKnownCityLabel, $nearestKnownCityCountry ($nearestKnownDistance km)"
                                            AppLanguage.AR -> "تم تحديد GPS • أقرب بلدة في صلواتي: $nearestKnownCityLabel، $nearestKnownCityCountry ($nearestKnownDistance كم)"
                                            AppLanguage.CKB -> "GPS دیاری کرا • نزیکترین شوێنی صەڵەواتی: $nearestKnownCityLabel، $nearestKnownCityCountry ($nearestKnownDistance کم)"
                                        }
                                        else -> when (language) {
                                            AppLanguage.EN -> "Current GPS location"
                                            AppLanguage.AR -> "الموقع الحالي عبر GPS"
                                            AppLanguage.CKB -> "شوێنی ئێستا بە GPS"
                                        }
                                    }
                                } else when (language) {
                                    AppLanguage.EN -> if (locating) "Finding your current location…" else "Current location unavailable"
                                    AppLanguage.AR -> if (locating) "جارٍ تحديد موقعك الحالي…" else "الموقع الحالي غير متاح"
                                    AppLanguage.CKB -> if (locating) "شوێنی ئێستات دیاری دەکرێت…" else "شوێنی ئێستا بەردەست نییە"
                                }
                            } else localizedKurdistanCityName(selectedCity, language.key),
                            color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold
                        )
                        if (locationMode == "gps" && gpsLatitude != null && gpsLongitude != null) {
                            Text(String.format(Locale.US, "%.5f, %.5f", gpsLatitude, gpsLongitude), color = Color(0xFFB9CDC5), fontSize = 11.sp)
                        } else if (locationMode == "city") {
                            Text(tr(language, "select_city"), color = Color(0xFFB9CDC5), fontSize = 10.sp)
                        }
                    }
                    PremiumIconButton(
                        icon = Icons.Outlined.GpsFixed,
                        contentDescription = when (language) {
                            AppLanguage.EN -> "Refresh location"
                            AppLanguage.AR -> "تحديث الموقع"
                            AppLanguage.CKB -> "نوێکردنەوەی شوێن"
                        },
                        accent = Gold,
                        size = 38.dp,
                        selected = locating,
                        onClick = locateNow
                    )
                    PremiumIconButton(
                        icon = Icons.Outlined.Search,
                        contentDescription = when (language) {
                            AppLanguage.EN -> "Search for a city"
                            AppLanguage.AR -> "البحث عن مدينة"
                            AppLanguage.CKB -> "گەڕان بەدوای شارێکدا"
                        },
                        accent = Gold,
                        size = 38.dp
                    ) {
                        citySearch = ""
                        chooseCity = true
                    }
                }
                Spacer(Modifier.height(7.dp))
                Text(
                    if (gpsLatitude != null) {
                        when (language) {
                            AppLanguage.EN -> "Only verified original Salawati timetables are shown. If no supported timetable is nearby, Mashxall reports that honestly instead of guessing."
                            AppLanguage.AR -> "يعرض التطبيق جداول صلواتي الأصلية الموثوقة فقط. وإذا لم يوجد جدول مدعوم قريب، فسيُظهر ذلك بدل تخمين المواقيت."
                            AppLanguage.CKB -> "تەنها خشتە ڕەسەن و پشتڕاستکراوەکانی صەڵەواتی پیشان دەدرێن؛ ئەگەر خشتەیەکی پشتگیریکراوی نزیک نەبێت، ئەوە ڕوون دەکرێتەوە و کات ناخرێتەوە."
                        }
                    } else {
                        when (language) {
                            AppLanguage.EN -> "Tap the location icon to get a new GPS/network fix, or choose a city."
                            AppLanguage.AR -> "اضغط رمز الموقع للحصول على إحداثيات GPS حديثة، أو اختر مدينة."
                            AppLanguage.CKB -> "نیشانەی شوێن دابگرە بۆ وەرگرتنی کۆئۆردیناتی نوێ، یان شارێک هەڵبژێرە."
                        }
                    }, color = Color(0xFFB9CDC5), fontSize = 11.sp, lineHeight = 17.sp
                )
            }
        }
        if (locating) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = Gold, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text(if (language == AppLanguage.EN) "Finding your location…" else if (language == AppLanguage.AR) "جارٍ تحديد موقعك…" else "شوێنت دیاری دەکرێت…", color = foreground, fontSize = 12.sp)
            }
        }
        if (locationMessage.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(locationMessage, color = foreground.copy(alpha = 0.65f), fontSize = 11.sp, lineHeight = 16.sp)
            if (!locating) {
                TextButton(onClick = locateNow) {
                    Text(
                        when (language) {
                            AppLanguage.EN -> "Try location again"
                            AppLanguage.AR -> "إعادة محاولة تحديد الموقع"
                            AppLanguage.CKB -> "دووبارە شوێن دیاری بکە"
                        },
                        color = Gold, fontSize = 11.sp
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        if (!loading && times == null && locationMode == "gps" && nearestKnownCityLabel != null) {
            Text(
                when (language) {
                    AppLanguage.EN -> "This identifies the closest locality recorded in Salawati; it does not mean a valid timetable exists for it. Choose a supported city or retry GPS for the prayer source."
                    AppLanguage.AR -> "هذا اسم أقرب بلدة مسجلة في صلواتي فقط، ولا يعني وجود جدول مواقيت صالح لها. اختر مدينة مدعومة أو حدّث GPS لمصدر المواقيت."
                    AppLanguage.CKB -> "ئەمە تەنها ناوی نزیکترین شوێنی تۆمارکراوە لە صەڵەواتییە؛ واتای ئەوە نییە خشتەی کاتی نوێژی دروستی هەیە. شارێکی پشتگیریکراو هەڵبژێرە یان GPS نوێ بکەرەوە."
                },
                color = Gold, fontSize = 11.sp, lineHeight = 17.sp
            )
            Spacer(Modifier.height(6.dp))
        }
        if (times == null) LoadingOrEmpty(loading, language, foreground) else {
            val timeItems = listOf(
                Triple("fajr", tr(language, "fajr"), formatPrayerTime(times!!.fajr, language, use24Hour)),
                Triple("sunrise", tr(language, "sunrise"), formatPrayerTime(times!!.sunrise, language, use24Hour)),
                Triple("dhuhr", tr(language, "dhuhr"), formatPrayerTime(times!!.dhuhr, language, use24Hour)),
                Triple("asr", tr(language, "asr"), formatPrayerTime(times!!.asr, language, use24Hour)),
                Triple("maghrib", tr(language, "maghrib"), formatPrayerTime(times!!.maghrib, language, use24Hour)),
                Triple("isha", tr(language, "isha"), formatPrayerTime(times!!.isha, language, use24Hour))
            )
            timeItems.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    pair.forEach { (key, label, time) ->
                        val isUpcoming = nextPrayerTarget?.prayerKey == key
                        val tileBorder by animateColorAsState(
                            targetValue = if (isUpcoming) Gold.copy(alpha = 0.74f) else foreground.copy(alpha = 0.08f),
                            animationSpec = tween(250, easing = FastOutSlowInEasing),
                            label = "Upcoming prayer tile outline"
                        )
                        val tileFill by animateColorAsState(
                            targetValue = if (isUpcoming) Gold.copy(alpha = 0.13f) else Color.Transparent,
                            animationSpec = tween(250, easing = FastOutSlowInEasing),
                            label = "Upcoming prayer tile surface"
                        )
                        Card(
                            Modifier.weight(1f).padding(bottom = 10.dp).animateContentSize(),
                            colors = CardDefaults.cardColors(containerColor = cardColor),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, tileBorder)
                        ) {
                            Row(
                                Modifier.fillMaxWidth()
                                    .background(Brush.horizontalGradient(listOf(tileFill, cardColor)))
                                    .padding(horizontal = 11.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PremiumIconMedallion(
                                    icon = when (key) {
                                        "fajr" -> Icons.Outlined.NightsStay
                                        "sunrise" -> Icons.Outlined.WbSunny
                                        "dhuhr" -> Icons.Outlined.LightMode
                                        "asr" -> Icons.Outlined.Schedule
                                        "maghrib" -> Icons.Outlined.WbTwilight
                                        else -> Icons.Outlined.NightsStay
                                    },
                                    tint = if (isUpcoming) Gold else foreground.copy(alpha = 0.55f),
                                    size = 34.dp,
                                    pulse = isUpcoming
                                )
                                Spacer(Modifier.width(9.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        label,
                                        color = if (isUpcoming) Gold else foreground.copy(alpha = 0.64f),
                                        fontSize = 11.sp,
                                        fontWeight = if (isUpcoming) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    AnimatedContent(
                                        targetState = time,
                                        transitionSpec = {
                                            (fadeIn(tween(150)) + scaleIn(initialScale = 0.94f, animationSpec = tween(180))) togetherWith
                                                (fadeOut(tween(100)) + scaleOut(targetScale = 1.04f, animationSpec = tween(100)))
                                        },
                                        label = "Prayer tile time update"
                                    ) { displayedTime ->
                                        Text(
                                            displayedTime,
                                            color = if (isUpcoming) Gold else foreground,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            maxLines = 1
                                        )
                                    }
                                    if (isUpcoming) {
                                        Spacer(Modifier.height(3.dp))
                                        Text(
                                            when (language) { AppLanguage.EN -> "UP NEXT"; AppLanguage.AR -> "التالية"; AppLanguage.CKB -> "داهاتوو" },
                                            color = Gold.copy(alpha = 0.84f),
                                            fontSize = 7.sp,
                                            letterSpacing = 0.7.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            val journeyDate = Date(clockNowMillis)
            val journeyStart = prayerTimestampForDate(journeyDate, times!!.fajr)
            val journeyEnd = prayerTimestampForDate(journeyDate, times!!.isha)
            val journeySchedule = listOf(
                "fajr" to times!!.fajr,
                "dhuhr" to times!!.dhuhr,
                "asr" to times!!.asr,
                "maghrib" to times!!.maghrib,
                "isha" to times!!.isha
            )
            val journeyMarkers = if (journeyStart != null && journeyEnd != null && journeyEnd > journeyStart) {
                journeySchedule.mapNotNull { (key, timeText) ->
                    val instant = prayerTimestampForDate(journeyDate, timeText)
                    instant?.let {
                        PrayerJourneyMarker(
                            key = key,
                            label = tr(language, key),
                            fraction = ((it - journeyStart).toDouble() / (journeyEnd - journeyStart).toDouble())
                                .toFloat().coerceIn(0f, 1f)
                        )
                    }
                }
            } else emptyList()
            if (journeyStart != null && journeyEnd != null && journeyMarkers.size == 5) {
                Spacer(Modifier.height(4.dp))
                PrayerJourneyTimeline(
                    language = language,
                    foreground = foreground,
                    cardColor = cardColor,
                    markers = journeyMarkers,
                    progress = prayerJourneyProgressFraction(clockNowMillis, journeyStart, journeyEnd),
                    nextPrayerKey = nextPrayerTarget?.prayerKey
                )
                Spacer(Modifier.height(12.dp))
            }
            Text(
                prayerSourceDescription(times!!.source, language),
                color = foreground.copy(alpha = 0.48f), fontSize = 10.sp, lineHeight = 15.sp
            )
        }

        Spacer(Modifier.height(14.dp))
        Spacer(Modifier.height(10.dp))
        if (nextPrayerTarget != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DeepGreen),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                border = BorderStroke(0.8.dp, Gold.copy(alpha = 0.28f))
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            when (language) {
                                AppLanguage.EN -> "NEXT PRAYER"
                                AppLanguage.AR -> "الصلاة القادمة"
                                AppLanguage.CKB -> "نوێژی داهاتوو"
                            }, color = Color(0xFFC7D8CD), fontSize = 10.sp, fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(5.dp))
                        AnimatedContent(
                            targetState = nextPrayerTarget.prayerKey,
                            transitionSpec = {
                                (fadeIn(tween(180)) + scaleIn(initialScale = 0.9f, animationSpec = tween(200))) togetherWith
                                    (fadeOut(tween(100)) + scaleOut(targetScale = 1.04f, animationSpec = tween(100)))
                            },
                            label = "Next prayer name transition"
                        ) { prayerKey ->
                            Text(
                                tr(language, prayerKey),
                                color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(formatPrayerTime(nextPrayerTarget.timeText, language, use24Hour), color = Color(0xFFF1D78D), fontSize = 14.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        AnimatedContent(
                            targetState = formatPrayerCountdown(nextPrayerTarget.targetMillis - clockNowMillis),
                            transitionSpec = {
                                (fadeIn(tween(140)) + scaleIn(initialScale = 0.88f, animationSpec = tween(180))) togetherWith
                                    (fadeOut(tween(90)) + scaleOut(targetScale = 1.06f, animationSpec = tween(90)))
                            },
                            label = "Prayer countdown rolling digits"
                        ) { countdown ->
                            Text(
                                countdown,
                                color = Gold, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Text(
                            when (language) {
                                AppLanguage.EN -> "remaining"
                                AppLanguage.AR -> "الوقت المتبقي"
                                AppLanguage.CKB -> "ماوە"
                            }, color = Color(0xFFC7D8CD), fontSize = 10.sp
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Card(colors = CardDefaults.cardColors(containerColor = cardColor), shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(15.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.GraphicEq, null, tint = Gold, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            when (language) {
                                AppLanguage.EN -> "Adhan notifications"
                                AppLanguage.AR -> "تنبيهات الأذان"
                                AppLanguage.CKB -> "ئاگادارکردنەوەی بانگ"
                            }, color = foreground, fontWeight = FontWeight.Bold, fontSize = 15.sp
                        )
                        Text(
                            when (language) {
                                AppLanguage.EN -> "Play your selected adhan at prayer time, including tracks downloaded for offline use"
                                AppLanguage.AR -> "شغّل صوت الأذان الذي اخترته عند الصلاة، بما في ذلك التسجيلات المنزّلة دون إنترنت"
                                AppLanguage.CKB -> "لە کاتی نوێژدا ئەو بانگە لێبدە کە هەڵتبژاردووە، لەوانەش ئەو تۆمارانەی بۆ بەبێ ئینتەرنێت داتگرتوون"
                            }, color = foreground.copy(alpha = 0.6f), fontSize = 11.sp, lineHeight = 17.sp
                        )
                    }
                    if (adhanEnabled) {
                        PremiumAudioWaveform(
                            active = true,
                            accent = Gold,
                            modifier = Modifier.padding(end = 5.dp),
                            contentDescription = when (language) {
                                AppLanguage.EN -> "Adhan notifications active"
                                AppLanguage.AR -> "تنبيهات الأذان مفعّلة"
                                AppLanguage.CKB -> "ئاگادارکردنەوەی بانگ چالاکە"
                            }
                        )
                    }
                    Switch(checked = adhanEnabled, onCheckedChange = { enable ->
                        if (!enable) {
                            adhanEnabled = false
                            prefs.edit().putBoolean("adhan_enabled", false).apply()
                            PrayerAlarmScheduler.cancelAll(context)
                            adhanMessage = ""
                        } else if (!adhanAssetsLoaded || !hasSelectableAdhanAsset(selectedAdhanAsset, adhanAssets)) {
                            adhanMessage = adhanSourceRequiredMessage(language)
                        } else if (Build.VERSION.SDK_INT >= 33 &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            adhanEnabled = true
                            prefs.edit().putBoolean("adhan_enabled", true).apply()
                        }
                    })
                }
                PremiumActionButton(
                    label = when (language) {
                        AppLanguage.EN -> "Browse full adhan & mosque library"
                        AppLanguage.AR -> "فتح مكتبة الأذان وأصوات المساجد الكاملة"
                        AppLanguage.CKB -> "کردنەوەی کتێبخانەی تەواوی بانگ و دەنگی مزگەوت"
                    },
                    icon = Icons.Outlined.GraphicEq,
                    accent = Gold,
                    foreground = foreground,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onBrowseAdhanLibrary
                )
                Text(
                    when (language) {
                        AppLanguage.EN -> "Choose a source recording, download it in the library, then return here to select it for scheduled prayer alerts."
                        AppLanguage.AR -> "اختر تسجيلًا من المكتبة ونزّله، ثم عُد إلى هنا لاختياره لتنبيهات الصلاة المجدولة."
                        AppLanguage.CKB -> "تۆمارێک لە کتێبخانەکە هەڵبژێرە و دایبگرە؛ پاشان بگەڕێوە بۆ ئەم شوێنە و بۆ ئاگادارکردنەوەی نوێژ هەڵیبژێرە."
                    },
                    color = foreground.copy(alpha = 0.55f), fontSize = 10.sp, lineHeight = 15.sp
                )
                if (adhanEnabled) {
                    Spacer(Modifier.height(10.dp))
                    val soundLabel = adhanAssets.firstOrNull { it.assetPath == selectedAdhanAsset }?.title
                        ?: selectedAdhanAsset.substringAfterLast('/')
                    PremiumActionButton(
                        label = when (language) {
                            AppLanguage.EN -> "Adhan sound: $soundLabel"
                            AppLanguage.AR -> "صوت الأذان: $soundLabel"
                            AppLanguage.CKB -> "دەنگی بانگ: $soundLabel"
                        },
                        icon = Icons.Outlined.VolumeUp,
                        accent = Gold,
                        foreground = foreground,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        adhanSoundQuery = ""
                        chooseAdhanSound = true
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                        if (!alarmManager.canScheduleExactAlarms()) {
                            Text(
                                when (language) {
                                    AppLanguage.EN -> "Exact alarms are not allowed. Android may delay prayer alerts."
                                    AppLanguage.AR -> "المنبهات الدقيقة غير مسموحة؛ قد يؤخر Android تنبيهات الصلاة."
                                    AppLanguage.CKB -> "ئاگادارکردنەوەی ورد ڕێگەپێدراو نییە؛ ئەندرۆید لەوانەیە دوا بخات."
                                }, color = foreground.copy(alpha = 0.6f), fontSize = 10.sp, lineHeight = 15.sp
                            )
                            TextButton(onClick = {
                                try {
                                    context.startActivity(
                                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                } catch (_: Exception) {
                                    adhanMessage = when (language) {
                                        AppLanguage.EN -> "Open exact-alarm permissions in Android Settings."
                                        AppLanguage.AR -> "افتح أذونات المنبه الدقيق من إعدادات Android."
                                        AppLanguage.CKB -> "مۆڵەتی کاتژمێری ورد لە ڕێکخستنەکانی ئەندرۆید بکەرەوە."
                                    }
                                }
                            }) {
                                Text(if (language == AppLanguage.EN) "Allow exact alarms" else if (language == AppLanguage.AR) "السماح بالمنبهات الدقيقة" else "ڕێگەدان بە ئاگادارکردنەوەی ورد")
                            }
                        }
                    }
                }
                if (adhanMessage.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(adhanMessage, color = foreground.copy(alpha = 0.65f), fontSize = 11.sp)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Card(Modifier.fillMaxWidth().premiumClickable { onQibla() }, colors = CardDefaults.cardColors(containerColor = cardColor), shape = RoundedCornerShape(20.dp)) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Explore, null, tint = Gold, modifier = Modifier.size(30.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(tr(language, "qibla"), color = foreground, fontWeight = FontWeight.Bold)
                    Text(tr(language, "qibla_sub"), color = foreground.copy(alpha = 0.55f), fontSize = 11.sp)
                }
                Icon(Icons.Outlined.Explore, null, tint = Gold)
            }
        }
        Spacer(Modifier.height(10.dp))
    }
    if (chooseCity) AlertDialog(
        onDismissRequest = { chooseCity = false },
        title = { Text(tr(language, "select_city")) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                SearchBox(
                    citySearch,
                    { citySearch = it },
                    when (language) {
                        AppLanguage.EN -> "Search city or country…"
                        AppLanguage.AR -> "ابحث عن مدينة أو دولة…"
                        AppLanguage.CKB -> "گەڕان بە ناوی شار یان وڵات…"
                    }
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    when (language) {
                        AppLanguage.EN -> "${visibleCities.size} matching locations"
                        AppLanguage.AR -> "${visibleCities.size} موقعًا مطابقًا"
                        AppLanguage.CKB -> "${visibleCities.size} شوێنی هاوتا"
                    }, color = foreground.copy(alpha = 0.55f), fontSize = 10.sp
                )
                LazyColumn(Modifier.height(355.dp)) {
                    itemsIndexed(visibleCities, key = { _, it -> "${it.name}|${it.country}|${it.latitude}|${it.longitude}" }) { cityIndex, city ->
                        Column(
                            Modifier.fillMaxWidth()
                                .staggeredEntrance(itemKey = "${city.name}|${city.country}|${city.latitude}|${city.longitude}", index = cityIndex)
                                .premiumClickable {
                                // Manual city mode is explicit and shared with Home, Qibla and scheduled alerts.
                                onCity(city)
                                locationMode = "city"
                                gpsLatitude = null
                                gpsLongitude = null
                                gpsAccuracy = null
                                manualLatitude = city.latitude
                                manualLongitude = city.longitude
                                times = null
                                tomorrowTimes = null
                                loading = true
                                prefs.edit()
                                    .putString("prayer_location_mode", "city")
                                    .remove("gps_latitude").remove("gps_longitude")
                                    .remove("gps_location_timestamp").remove("gps_accuracy")
                                    .putString("selected_city", city.name)
                                    .putString("selected_city_country", city.country)
                                    .putString("manual_city_latitude", city.latitude.toString())
                                    .putString("manual_city_longitude", city.longitude.toString())
                                    .apply()
                                chooseCity = false
                            }.padding(vertical = 9.dp, horizontal = 4.dp)
                        ) {
                            val localizedCityName = localizedKurdistanCityName(city.name, language.key)
                            Text(localizedCityName, color = foreground, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            val cityDetails = listOf(city.name.takeIf { it != localizedCityName }, city.country)
                                .filter { !it.isNullOrBlank() }.joinToString(" • ")
                            if (cityDetails.isNotBlank()) Text(cityDetails, color = foreground.copy(alpha = 0.52f), fontSize = 10.sp)
                        }
                        Divider(color = foreground.copy(alpha = 0.08f))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { chooseCity = false }) { Text(tr(language, "back")) } }
    )
    if (chooseAdhanSound) AlertDialog(
        onDismissRequest = {
            AudioController.stop()
            previewingAdhan = ""
            chooseAdhanSound = false
        },
        title = { Text(if (language == AppLanguage.EN) "Choose adhan sound" else if (language == AppLanguage.AR) "اختر صوت الأذان" else "دەنگی بانگ هەڵبژێرە") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                SearchBox(
                    adhanSoundQuery,
                    { adhanSoundQuery = it },
                    when (language) {
                        AppLanguage.EN -> "Search every bundled audio file…"
                        AppLanguage.AR -> "ابحث في كل الملفات الصوتية المرفقة…"
                        AppLanguage.CKB -> "گەڕان لە هەموو فایلە دەنگییە ناوخۆییەکان…"
                    }
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    when (language) {
                        AppLanguage.EN -> "${visibleAdhanAssets.size} of ${adhanAssets.size} audio files on this device"
                        AppLanguage.AR -> "${visibleAdhanAssets.size} من ${adhanAssets.size} ملفًا صوتيًا على الجهاز"
                        AppLanguage.CKB -> "${visibleAdhanAssets.size} لە ${adhanAssets.size} فایل دەنگییەکانی مۆبایل"
                    }, color = foreground.copy(alpha = 0.55f), fontSize = 10.sp
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DeepGreen),
                    shape = RoundedCornerShape(13.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(10.dp)) {
                        Text(
                            when (language) {
                                AppLanguage.EN -> "Full online Adhan & mosque library"
                                AppLanguage.AR -> "مكتبة الأذان والمساجد الكاملة عبر الإنترنت"
                                AppLanguage.CKB -> "کتێبخانەی تەواوی بانگ و مزگەوت بە ئۆنلاین"
                            },
                            color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            when (language) {
                                AppLanguage.EN -> onlineAdhanCount.toString() + " source-linked recordings • 175 muezzins and 56 mosque/location recordings. Download a track in the online library, return here, and select its saved copy for alerts."
                                AppLanguage.AR -> onlineAdhanCount.toString() + " تسجيلًا من المصدر الأصلي • 175 مؤذنًا و56 تسجيل مسجد/موقع. نزّل التسجيل من المكتبة ثم ارجع واختر النسخة المحفوظة للتنبيهات."
                                AppLanguage.CKB -> onlineAdhanCount.toString() + " تۆماری بە سەرچاوەوە بەستراو • ١٧٥ موئەزین و٥٦ تۆماری مزگەوت/شوێن. لە کتێبخانەکە دایبگرە، بگەڕێوە و وەشانی پارێزراو بۆ ئاگادارکردنەوە هەڵبژێرە."
                            },
                            color = Color.White.copy(alpha = 0.75f), fontSize = 10.sp, lineHeight = 15.sp
                        )
                        Spacer(Modifier.height(7.dp))
                        PremiumActionButton(
                            label = when (language) {
                                AppLanguage.EN -> "Browse all " + onlineAdhanCount + " online recordings"
                                AppLanguage.AR -> "تصفح جميع التسجيلات (" + onlineAdhanCount + ")"
                                AppLanguage.CKB -> "بینینی هەموو تۆمارەکان (" + onlineAdhanCount + ")"
                            },
                            icon = Icons.Outlined.GraphicEq,
                            accent = Gold,
                            foreground = foreground,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AudioController.stop()
                            previewingAdhan = ""
                            chooseAdhanSound = false
                            onBrowseAdhanLibrary()
                        }
                    }
                }
                Spacer(Modifier.height(5.dp))
                LazyColumn(Modifier.height(260.dp)) {
                if (visibleAdhanAssets.isEmpty()) {
                    item {
                        Text(
                            when (language) {
                                AppLanguage.EN -> "No bundled or downloaded Adhan recording is available yet. Use the online library above, download a track, then return here."
                                AppLanguage.AR -> "لا يوجد تسجيل أذان مرفق أو منزّل بعد. افتح المكتبة عبر الإنترنت أعلاه، ونزّل تسجيلًا ثم عُد إلى هنا."
                                AppLanguage.CKB -> "هێشتا هیچ تۆماری بانگی ناوخۆیی یان داگیراو بەردەست نییە؛ کتێبخانەی ئۆنلاین لە سەرەوە بکەرەوە، دەنگێک دابگرە و بگەڕێوە."
                            },
                            color = foreground.copy(alpha = 0.65f), fontSize = 11.sp, lineHeight = 17.sp,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp)
                        )
                    }
                }
                items(visibleAdhanAssets, key = { it.assetPath }) { sound ->
                    Row(
                        Modifier.fillMaxWidth().premiumClickable {
                            AudioController.stop()
                            previewingAdhan = ""
                            selectedAdhanAsset = sound.assetPath
                            prefs.edit().putString("adhan_audio_asset", sound.assetPath).apply()
                            adhanMessage = ""
                            chooseAdhanSound = false
                        }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedAdhanAsset == sound.assetPath, onClick = {
                            AudioController.stop()
                            previewingAdhan = ""
                            selectedAdhanAsset = sound.assetPath
                            prefs.edit().putString("adhan_audio_asset", sound.assetPath).apply()
                            adhanMessage = ""
                            chooseAdhanSound = false
                        })
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(sound.title, color = foreground, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(sound.assetPath.substringAfterLast('/'), color = foreground.copy(alpha = 0.5f), fontSize = 10.sp)
                        }
                        PremiumIconActionSurface(
                            contentDescription = if (language == AppLanguage.EN) "Preview audio" else if (language == AppLanguage.AR) "معاينة الصوت" else "تاقیکردنەوەی دەنگ",
                            accent = Gold,
                            size = 38.dp,
                            selected = previewingAdhan == sound.assetPath,
                            onClick = {
                                if (previewingAdhan == sound.assetPath) {
                                    AudioController.stop()
                                    previewingAdhan = ""
                                } else {
                                    AudioController.stop()
                                    previewingAdhan = sound.assetPath
                                    if (sound.assetPath.startsWith("local:")) {
                                        AudioController.playSequence(context, listOf(sound.assetPath)) { active ->
                                            if (!active) previewingAdhan = ""
                                        }
                                    } else {
                                        AudioController.play(context, sound.assetPath) { active ->
                                            if (!active) previewingAdhan = ""
                                        }
                                    }
                                }
                            }
                        ) {
                            if (previewingAdhan == sound.assetPath) {
                                PremiumAudioWaveform(active = true, accent = Gold)
                            } else {
                                Icon(Icons.Outlined.PlayCircle, contentDescription = null, tint = Gold, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                AudioController.stop()
                previewingAdhan = ""
                chooseAdhanSound = false
            }) { Text(tr(language, "back")) }
        }
    )

}

@Composable
fun QiblaScreen(language: AppLanguage, foreground: Color, cardColor: Color, selectedCity: String) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val prefs = remember { context.getSharedPreferences("mashxall_preferences", Context.MODE_PRIVATE) }
    var locationMode by rememberSaveable {
        mutableStateOf(
            resolvePrayerLocationMode(
                prefs.getString("prayer_location_mode", null),
                prefs.getString("gps_latitude", null)?.toDoubleOrNull() != null &&
                    prefs.getString("gps_longitude", null)?.toDoubleOrNull() != null,
                prefs.getString("manual_city_latitude", null)?.toDoubleOrNull() != null &&
                    prefs.getString("manual_city_longitude", null)?.toDoubleOrNull() != null,
                hasUsableCitySelection = selectedCity.isNotBlank()
            )
        )
    }
    val usableSavedGps = remember {
        val lat = prefs.getString("gps_latitude", null)?.toDoubleOrNull()
        val lon = prefs.getString("gps_longitude", null)?.toDoubleOrNull()
        if (locationMode == "gps" && isUsableSavedGpsFix(
                lat, lon, prefs.getLong("gps_location_timestamp", 0L), System.currentTimeMillis()
            )
        ) lat to lon else null
    }
    var gpsLatitude by remember { mutableStateOf(usableSavedGps?.first) }
    var gpsLongitude by remember { mutableStateOf(usableSavedGps?.second) }
    val manualLatitude = prefs.getString("manual_city_latitude", null)?.toDoubleOrNull()
    val manualLongitude = prefs.getString("manual_city_longitude", null)?.toDoubleOrNull()
    val manualCountry = prefs.getString("selected_city_country", "") ?: ""
    var city by remember { mutableStateOf<City?>(null) }
    var azimuth by remember { mutableFloatStateOf(0f) }
    var magneticDeclination by remember { mutableFloatStateOf(0f) }
    var sensorAvailable by remember { mutableStateOf(false) }
    var locating by remember { mutableStateOf(false) }
    var locationMessage by remember { mutableStateOf("") }

    val onFix: (Location) -> Unit = { fix ->
        gpsLatitude = fix.latitude
        gpsLongitude = fix.longitude
        locationMode = "gps"
        prefs.edit()
            .putString("prayer_location_mode", "gps")
            .putString("gps_latitude", fix.latitude.toString())
            .putString("gps_longitude", fix.longitude.toString())
            .putLong("gps_location_timestamp", fix.time)
            .putString("gps_accuracy", if (fix.hasAccuracy()) fix.accuracy.toString() else "")
            .remove("manual_city_latitude")
            .remove("manual_city_longitude")
            .apply()
        locating = false
        locationMessage = when (language) {
            AppLanguage.EN -> if (fix.hasAccuracy()) "Using the phone's current location (±${fix.accuracy.toInt()} m reported accuracy)." else "Using the phone's current location."
            AppLanguage.AR -> if (fix.hasAccuracy()) "يُستخدم موقع الهاتف الحالي (الدقة المعلنة ±${fix.accuracy.toInt()} متر)." else "يُستخدم موقع الهاتف الحالي."
            AppLanguage.CKB -> if (fix.hasAccuracy()) "شوێنی ئێستای مۆبایل بەکاردێت (وردبینی ڕاگەیەندراو ±${fix.accuracy.toInt()} مەتر)." else "شوێنی ئێستای مۆبایل بەکاردێت."
        }
    }
    val onLocationError: (String) -> Unit = { error ->
        locating = false
        locationMessage = when (error) {
            "LOCATION_LAST_KNOWN_ONLY" -> when (language) {
                AppLanguage.EN -> "Showing the recent saved location, but Android could not refresh it. Turn on Location and retry."
                AppLanguage.AR -> "يُعرض الموقع المحفوظ حديثًا، لكن Android لم يستطع تحديثه. فعّل الموقع ثم أعد المحاولة."
                AppLanguage.CKB -> "شوێنی پاشەکەوتکراوی نزیک پیشان دەدرێت، بەڵام ئەندرۆید نوێی نەکردەوە؛ شوێن چالاک بکە و دووبارە هەوڵ بدەوە."
            }
            "LOCATION_SETTINGS_OPENED" -> when (language) {
                AppLanguage.EN -> "Android location settings opened. Turn Location on and return to Mashxall."
                AppLanguage.AR -> "فُتحت إعدادات الموقع في Android. فعّل الموقع ثم عُد إلى مەشخەڵ."
                AppLanguage.CKB -> "ڕێکخستنەکانی شوێنی Android کرانەوە؛ شوێن چالاک بکە و بگەڕێوە."
            }
            "LOCATION_SERVICES_OFF" -> when (language) {
                AppLanguage.EN -> "Location is turned off. The Qibla bearing uses the saved city until location is available."
                AppLanguage.AR -> "خدمة الموقع متوقفة؛ سيُستخدم اتجاه المدينة المحفوظة حتى يتوفر الموقع."
                AppLanguage.CKB -> "خزمەتگوزاری شوێن ناچالاکە؛ تا شوێن بەردەست دەبێت ئاڕاستەی شاری پاشەکەوتکراو بەکاردێت."
            }
            else -> when (language) {
                AppLanguage.EN -> "A fresh location is unavailable. The saved city is being used."
                AppLanguage.AR -> "الموقع الحالي غير متاح؛ يجري استخدام المدينة المحفوظة."
                AppLanguage.CKB -> "شوێنی نوێ بەردەست نییە؛ شاری پاشەکەوتکراو بەکاردێت."
            }
        }
    }

    // Qibla uses the same Android device location as prayer times; it never asks
    // the user to choose a second, conflicting location inside this screen.
    val qiblaPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val allowed = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (allowed) {
            locating = true
            locationMode = "gps"
            prefs.edit().putString("prayer_location_mode", "gps").apply()
            locationMessage = ""
            requestCurrentLocation(context, onFix, onLocationError)
        } else {
            locating = false
            locationMessage = when (language) {
                AppLanguage.EN -> "Location permission is needed to point the compass accurately toward Qibla."
                AppLanguage.AR -> "يلزم إذن الموقع لتوجيه البوصلة نحو القبلة بدقة."
                AppLanguage.CKB -> "بۆ ئاراستەکردنی دروستی کۆمپاوس بەرەو قیبلە، مۆڵەتی شوێن پێویستە."
            }
        }
    }
    LaunchedEffect(Unit) {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (locationMode == "gps" && (fine || coarse)) {
            locating = true
            locationMessage = ""
            requestCurrentLocation(context, onFix, onLocationError)
        } else if (locationMode == "gps") {
            gpsLatitude = null
            gpsLongitude = null
            prefs.edit().remove("gps_latitude").remove("gps_longitude")
                .remove("gps_location_timestamp").remove("gps_accuracy").apply()
            locationMessage = when (language) {
                AppLanguage.EN -> "Current location permission is off. Allow it or select a city in Prayer settings."
                AppLanguage.AR -> "إذن الموقع الحالي متوقف. اسمح به أو اختر مدينة من إعدادات الصلاة."
                AppLanguage.CKB -> "مۆڵەتی شوێنی ئێستا ناچالاکە؛ چالاکی بکە یان لە ڕێکخستنەکانی نوێژ شارێک هەڵبژێرە."
            }
        } else {
            locationMessage = when (language) {
                AppLanguage.EN -> "Using the selected city for Qibla."
                AppLanguage.AR -> "يُستخدم موضع المدينة المختارة لحساب القبلة."
                AppLanguage.CKB -> "شوێنی شاری هەڵبژێردراو بۆ قیبلە بەکاردێت."
            }
        }
    }

    LaunchedEffect(locationMode, selectedCity, gpsLatitude, gpsLongitude, manualLatitude, manualLongitude, manualCountry) {
        city = when {
            locationMode == "gps" && gpsLatitude != null && gpsLongitude != null ->
                City("GPS", gpsLatitude!!, gpsLongitude!!)
            locationMode == "gps" -> null
            manualLatitude != null && manualLongitude != null ->
                City(selectedCity, manualLatitude, manualLongitude, manualCountry)
            else -> ContentRepository.findCityByName(context, selectedCity, manualCountry)
        }
    }
    LaunchedEffect(city) {
        magneticDeclination = city?.let { location ->
            runCatching {
                android.hardware.GeomagneticField(
                    location.latitude.toFloat(),
                    location.longitude.toFloat(),
                    0f,
                    System.currentTimeMillis()
                ).declination
            }.getOrDefault(0f)
        } ?: 0f
    }

    DisposableEffect(context) {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accelerometer = manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = manager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val gravity = FloatArray(3)
        val magnetic = FloatArray(3)
        var hasGravity = false
        var hasMagnetic = false
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        System.arraycopy(event.values, 0, gravity, 0, 3)
                        hasGravity = true
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        System.arraycopy(event.values, 0, magnetic, 0, 3)
                        hasMagnetic = true
                    }
                }
                if (hasGravity && hasMagnetic) {
                    val matrix = FloatArray(9)
                    val orientation = FloatArray(3)
                    if (SensorManager.getRotationMatrix(matrix, null, gravity, magnetic)) {
                        SensorManager.getOrientation(matrix, orientation)
                        azimuth = ((Math.toDegrees(orientation[0].toDouble()).toFloat() + 360f) % 360f)
                        sensorAvailable = true
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (manager != null && accelerometer != null && magnetometer != null) {
            manager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
            manager.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose { manager?.unregisterListener(listener) }
    }

    val bearing = city?.let { qiblaBearing(it.latitude, it.longitude) }
    val relativeHeading = bearing?.let { qiblaRelativeHeading(it.toFloat(), azimuth, magneticDeclination) } ?: 0f
    val aligned = sensorAvailable && bearing != null && kotlin.math.abs(relativeHeading) <= 5f
    val animatedHeading by androidx.compose.animation.core.animateFloatAsState(
        targetValue = relativeHeading,
        animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessLow),
        label = "qiblaHeading"
    )

    LaunchedEffect(aligned) {
        if (aligned) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        PremiumSectionHero(
            title = tr(language, "qibla"),
            subtitle = when (language) {
                AppLanguage.EN -> "A live, sensor-aware compass guiding you toward the Kaaba."
                AppLanguage.AR -> "بوصلة مباشرة تستخدم مستشعر الهاتف لتوجيهك نحو الكعبة."
                AppLanguage.CKB -> "کۆمپاوسێکی ڕاستەوخۆ بە سێنسەری مۆبایل ئاراستەت بەرەو کەعبە دەکات."
            },
            icon = Icons.Outlined.Explore,
            foreground = foreground,
            accent = Color(0xFF82CDB1),
            metric = if (bearing != null) String.format(Locale.US, "%.1f°", bearing) else when (language) { AppLanguage.EN -> "WAITING FOR LOCATION"; AppLanguage.AR -> "بانتظار الموقع"; AppLanguage.CKB -> "چاوەڕێی شوێن" },
            eyebrow = when (language) { AppLanguage.EN -> "SACRED DIRECTION"; AppLanguage.AR -> "الاتجاه المقدّس"; AppLanguage.CKB -> "ئاڕاستەی پیرۆز" }
        )
        Spacer(Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth().animateContentSize(),
            colors = CardDefaults.cardColors(containerColor = cardColor),
            shape = RoundedCornerShape(30.dp),
            border = BorderStroke(1.dp, (if (aligned) Color(0xFF62C995) else Gold).copy(alpha = 0.48f))
        ) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier.size(264.dp).clip(CircleShape)
                        .background(Brush.radialGradient(listOf(Color(0xFF1A5946), DeepGreen, Color(0xFF071E1A))))
                        .border(2.dp, (if (aligned) Color(0xFF62C995) else Gold).copy(alpha = 0.9f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    PremiumCompassOrbit(
                        tint = if (aligned) Color(0xFF62C995) else Gold,
                        size = 236.dp
                    )
                    Icon(
                        Icons.Outlined.Explore,
                        contentDescription = null,
                        tint = if (aligned) Color(0xFF62C995) else Gold,
                        modifier = Modifier.size(174.dp).rotate(animatedHeading)
                    )
                    Text("N", color = PaleGold, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.TopCenter).padding(top = 17.dp))
                    Text("🕋", fontSize = 26.sp)
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    city?.name?.let { if (it == "GPS") when (language) { AppLanguage.EN -> "Current location"; AppLanguage.AR -> "الموقع الحالي"; AppLanguage.CKB -> "شوێنی ئێستا" } else localizedKurdistanCityName(it, language.key) } ?: when (language) {
                        AppLanguage.EN -> "Location unavailable"
                        AppLanguage.AR -> "الموقع غير متاح"
                        AppLanguage.CKB -> "شوێن بەردەست نییە"
                    },
                    color = Gold, fontSize = 14.sp, fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    bearing?.let { String.format(Locale.US, "%.1f°", it) + " • " + tr(language, "qibla_heading") } ?: when (language) {
                        AppLanguage.EN -> "Allow location to calculate direction"
                        AppLanguage.AR -> "اسمح بالموقع لحساب الاتجاه"
                        AppLanguage.CKB -> "مۆڵەتی شوێن بدە بۆ حسابکردنی ئاراستە"
                    },
                    color = foreground, fontSize = if (bearing != null) 28.sp else 15.sp, fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(15.dp))
                Surface(
                    color = (if (aligned) Color(0xFF1F5A42) else Gold.copy(alpha = 0.10f)),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, (if (aligned) Color(0xFF62C995) else Gold).copy(alpha = 0.42f))
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(if (aligned) "✓" else "↻", color = if (aligned) Color(0xFF77E0A9) else Gold, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(9.dp))
                        Text(
                            when {
                                bearing == null -> when (language) {
                                    AppLanguage.EN -> "Location is required before the compass can point to Qibla."
                                    AppLanguage.AR -> "يجب تحديد موقع الهاتف أولًا كي تشير البوصلة إلى القبلة."
                                    AppLanguage.CKB -> "پێش ئەوەی کۆمپاوس بەرەو قیبلە ئاماژە بکات، دەبێت شوێنی مۆبایل دیاری بێت."
                                }
                                aligned -> when (language) {
                                    AppLanguage.EN -> "Correct direction — you're facing Qibla"
                                    AppLanguage.AR -> "الاتجاه صحيح — أنت تواجه القبلة"
                                    AppLanguage.CKB -> "ئاڕاستەکە ڕاستە — ڕووت لە قیبلەیە"
                                }
                                sensorAvailable -> when (language) {
                                    AppLanguage.EN -> "Turn the phone until the pointer aligns with Qibla"
                                    AppLanguage.AR -> "أدر الهاتف حتى تصطف الإشارة مع القبلة"
                                    AppLanguage.CKB -> "مۆبایلەکە بسووڕێنە تا نیشانەکە لەگەڵ قیبلە یەکبگرێتەوە"
                                }
                                else -> when (language) {
                                    AppLanguage.EN -> "Compass sensor unavailable — showing the geographic bearing"
                                    AppLanguage.AR -> "مستشعر البوصلة غير متاح؛ يُعرض الاتجاه الجغرافي"
                                    AppLanguage.CKB -> "سێنسەری کۆمپاوس بەردەست نییە؛ ئاڕاستەی جوگرافی پیشان دەدرێت"
                                }
                            },
                            color = foreground, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, lineHeight = 19.sp, textAlign = TextAlign.Center
                        )
                    }
                }
                if (locationMessage.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(locationMessage, color = foreground.copy(alpha = 0.59f), fontSize = 10.sp, lineHeight = 15.sp, textAlign = TextAlign.Center)
                } else if (locating) {
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(13.dp), strokeWidth = 2.dp, color = Gold)
                        Spacer(Modifier.width(7.dp))
                        Text(
                            when (language) { AppLanguage.EN -> "Refreshing location…"; AppLanguage.AR -> "يجري تحديث الموقع…"; AppLanguage.CKB -> "شوێن نوێ دەکرێتەوە…" },
                            color = foreground.copy(alpha = 0.6f), fontSize = 10.sp
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            when (language) {
                AppLanguage.EN -> "Keep the phone flat and away from magnets or metal objects for a steadier reading."
                AppLanguage.AR -> "أبقِ الهاتف أفقيًا وبعيدًا عن المغناطيس والأجسام المعدنية لتحسين القراءة."
                AppLanguage.CKB -> "مۆبایلەکە بە ئاسۆیی بگرە و لە ئاسن و مگناتیس دووری بخەرەوە بۆ خوێندنەوەی جێگیرتر."
            }, color = foreground.copy(alpha = 0.5f), fontSize = 10.sp, lineHeight = 16.sp, textAlign = TextAlign.Center
        )
    }
}

internal fun qiblaRelativeHeading(
    trueBearingDegrees: Float,
    magneticAzimuthDegrees: Float,
    declinationDegrees: Float
): Float {
    // Android's orientation sensor reports azimuth relative to magnetic north.
    // A positive geomagnetic declination means magnetic north lies east of true
    // north, so add the declination to obtain a true-north heading.
    val trueHeading = ((magneticAzimuthDegrees + declinationDegrees) % 360f + 360f) % 360f
    return ((trueBearingDegrees - trueHeading + 540f) % 360f) - 180f
}

private fun qiblaBearing(latitude: Double, longitude: Double): Double {
    val kaabaLat = Math.toRadians(21.4225)
    val kaabaLon = Math.toRadians(39.8262)
    val lat = Math.toRadians(latitude)
    val lon = Math.toRadians(longitude)
    val dLon = kaabaLon - lon
    val deg = Math.toDegrees(atan2(sin(dLon), cos(lat) * tan(kaabaLat) - sin(lat) * cos(dLon)))
    return (deg + 360.0) % 360.0
}


@Composable
fun WordAnalysisScreen(
    language: AppLanguage,
    foreground: Color,
    cardColor: Color,
    initialSurah: Int,
    initialAyah: Int,
    onLoad: (Int, Int) -> Unit
) {
    val context = LocalContext.current
    val quranGlyphFamily = remember {
        runCatching {
            FontFamily(Typeface.createFromAsset(context.assets, "content/noor_tafsir_1.2.2/base/assets/fonts/amiri_modded.ttf"))
        }.getOrNull()
    }
    var verseContext by remember { mutableStateOf<Ayah?>(null) }
    var surahText by rememberSaveable { mutableStateOf(initialSurah.toString()) }
    var ayahText by rememberSaveable { mutableStateOf(initialAyah.toString()) }
    var items by remember { mutableStateOf<List<WordItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(false) }
    var selectedPart by remember { mutableStateOf<WordPart?>(null) }
    var selectedWord by remember { mutableStateOf<WordItem?>(null) }
    val scope = rememberCoroutineScope()
    fun loadAnalysis(surah: Int, ayah: Int) {
        selected = true
        loading = true
        items = emptyList()
        verseContext = null
        selectedPart = null
        selectedWord = null
        onLoad(surah, ayah)
        scope.launch {
            try {
                val loadedItems = ContentRepository.loadWordAnalysis(context, surah, ayah)
                val loadedVerse = ContentRepository.loadAyahs(context, surah, language.key, translationKey = "none")
                    .firstOrNull { it.number == ayah }
                items = loadedItems
                verseContext = loadedVerse
            } catch (_: Exception) {
                items = emptyList()
                verseContext = null
            } finally {
                loading = false
            }
        }
    }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            PremiumSectionHero(
                title = tr(language, "word_by_word"),
                subtitle = when (language) {
                    AppLanguage.EN -> "Study every word, root and grammatical role in its Quranic context."
                    AppLanguage.AR -> "ادرس كل كلمة وجذرها ووظيفتها النحوية في سياق الآية."
                    AppLanguage.CKB -> "هەر وشەیەک، ڕەگ و ڕۆڵی ڕێزمانی لە چوارچێوەی ئایەتدا بخوێنەوە."
                },
                icon = Icons.Outlined.Language,
                foreground = foreground,
                accent = Color(0xFFBCA8F2),
                eyebrow = when (language) { AppLanguage.EN -> "QURAN WORD STUDY"; AppLanguage.AR -> "دراسة مفردات القرآن"; AppLanguage.CKB -> "خوێندنی وشەکانی قورئان" }
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(surahText, { surahText = it.filter(Char::isDigit).take(3) }, modifier = Modifier.weight(1f), label = { Text(tr(language, "surah")) }, singleLine = true)
                OutlinedTextField(ayahText, { ayahText = it.filter(Char::isDigit).take(3) }, modifier = Modifier.weight(1f), label = { Text(tr(language, "ayah")) }, singleLine = true)
                Button(onClick = {
                    val s = surahText.toIntOrNull()?.coerceIn(1, 114) ?: 1
                    val a = ayahText.toIntOrNull()?.coerceAtLeast(1) ?: 1
                    surahText = s.toString()
                    ayahText = a.toString()
                    loadAnalysis(s, a)
                }) { Text("→") }
            }
        }
        if (!selected || items.isEmpty()) LoadingOrEmpty(loading, language, foreground, if (selected) tr(language, "not_found") else tr(language, "choose_surah"))
        else LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text(tr(language, "surah") + " " + surahText + " • " + tr(language, "ayah") + " " + ayahText, color = Gold, fontWeight = FontWeight.Bold)
                verseContext?.let { verse ->
                    Spacer(Modifier.height(10.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = cardColor), shape = RoundedCornerShape(16.dp)) {
                        Text(verse.arabic, modifier = Modifier.fillMaxWidth().padding(14.dp), color = foreground, textAlign = TextAlign.End, fontSize = 20.sp, lineHeight = 34.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    when (language) {
                        AppLanguage.EN -> "Tap a segment below to study its word type, root and grammar."
                        AppLanguage.AR -> "كل مقطع يعرض نوع الكلمة وجذرها وإعرابها."
                        AppLanguage.CKB -> "هەر بەشێک جۆری وشە، ڕەگ و ڕێزمانەکەی پیشان دەدات."
                    }, color = foreground.copy(alpha = 0.57f), fontSize = 11.sp
                )
            }
            itemsIndexed(items, key = { _, word -> word.position }) { wordIndex, word ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                        .staggeredEntrance(itemKey = word.position, index = wordIndex)
                        .animateContentSize(),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    shape = RoundedCornerShape(17.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(13.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Box(Modifier.size(28.dp).clip(CircleShape).background(DeepGreen), contentAlignment = Alignment.Center) {
                                Text(word.position.toString(), color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                word.parts.forEach { part ->
                                    val partColor = remember(part.colorHex) {
                                        runCatching { Color(android.graphics.Color.parseColor(part.colorHex)) }.getOrDefault(foreground)
                                    }
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).premiumClickable {
                                            selectedPart = part
                                            selectedWord = word
                                        }.padding(horizontal = 3.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            part.glyphText,
                                            color = partColor,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Medium,
                                            fontFamily = quranGlyphFamily ?: FontFamily.Default,
                                            textAlign = TextAlign.Center
                                        )
                                        val typeLabel = when (language) {
                                            AppLanguage.CKB -> part.labelCkb.ifBlank { part.tag }
                                            AppLanguage.AR -> part.labelArabic.ifBlank { part.tag }
                                            AppLanguage.EN -> part.labelEnglish.ifBlank { part.tag }
                                        }
                                        if (typeLabel.isNotBlank()) {
                                            Text(typeLabel, color = partColor, fontSize = 9.sp, maxLines = 2, textAlign = TextAlign.Center)
                                        }
                                    }
                                }
                            }
                        }
                        if (word.root.isNotBlank()) {
                            Spacer(Modifier.height(7.dp))
                            Text((if (language == AppLanguage.EN) "Root: " else if (language == AppLanguage.AR) "الجذر: " else "ڕەگ: ") + word.root, color = Gold, fontSize = 12.sp)
                        }
                        if (word.grammar.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(word.grammar, color = foreground.copy(alpha = 0.7f), fontSize = 12.sp, lineHeight = 18.sp)
                        }
                    }
                }
            }
        }
    }

    if (selectedPart != null && selectedWord != null) {
        val part = selectedPart!!
        val word = selectedWord!!
        val typeLabel = when (language) {
            AppLanguage.CKB -> part.labelCkb.ifBlank { part.tag }
            AppLanguage.AR -> part.labelArabic.ifBlank { part.tag }
            AppLanguage.EN -> part.labelEnglish.ifBlank { part.tag }
        }
        AlertDialog(
            onDismissRequest = { selectedPart = null; selectedWord = null },
            title = {
                Column {
                    Text(part.glyphText, color = Gold, fontSize = 25.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                    Text(
                        if (language == AppLanguage.EN) "Quranic word segment"
                        else if (language == AppLanguage.AR) "جزء من كلمة قرآنية"
                        else "بەشی وشەی قورئانی",
                        color = foreground, fontSize = 14.sp
                    )
                }
            },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        (if (language == AppLanguage.EN) "Word position: " else if (language == AppLanguage.AR) "ترتيب الكلمة: " else "ڕیزبەندی وشە: ") + word.position,
                        color = foreground, fontSize = 12.sp
                    )
                    if (typeLabel.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(if (language == AppLanguage.EN) "Word type" else if (language == AppLanguage.AR) "نوع الكلمة" else "جۆری وشە", color = Gold, fontWeight = FontWeight.Bold)
                        Text(typeLabel + if (part.tag.isNotBlank() && typeLabel != part.tag) " (${part.tag})" else "", color = foreground, fontSize = 13.sp)
                    }
                    if (part.glyphText.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(if (language == AppLanguage.EN) "Arabic segment" else if (language == AppLanguage.AR) "المقطع العربي" else "بەشی عەرەبی", color = Gold, fontWeight = FontWeight.Bold)
                        Text(part.glyphText, color = foreground, fontSize = 20.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                    }
                    if (word.root.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(if (language == AppLanguage.EN) "Root" else if (language == AppLanguage.AR) "الجذر" else "ڕەگ", color = Gold, fontWeight = FontWeight.Bold)
                        Text(word.root, color = foreground, fontSize = 13.sp)
                    }
                    if (word.grammar.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(if (language == AppLanguage.EN) "Grammar / I'rab" else if (language == AppLanguage.AR) "الإعراب والقواعد" else "ڕێزمان و ئیعراب", color = Gold, fontWeight = FontWeight.Bold)
                        Text(word.grammar, color = foreground, fontSize = 12.sp, lineHeight = 19.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedPart = null; selectedWord = null }) {
                    Text(if (language == AppLanguage.EN) "Done" else if (language == AppLanguage.AR) "تم" else "تەواو")
                }
            }
        )
    }

}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun FarmudaScreen(language: AppLanguage, foreground: Color, cardColor: Color) {
    val context = LocalContext.current
    val favoritesPrefs = remember { context.getSharedPreferences("mashxall_farmuda_favorites", Context.MODE_PRIVATE) }
    var query by rememberSaveable { mutableStateOf("") }
    var records by remember { mutableStateOf<List<FarmudaItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var favoritesOnly by rememberSaveable { mutableStateOf(false) }
    var favorites by remember {
        mutableStateOf(favoritesPrefs.getStringSet("farmuda_ids", emptySet()).orEmpty().toSet())
    }
    var selectedRecord by remember { mutableStateOf<FarmudaItem?>(null) }

    LaunchedEffect(query) {
        loading = true
        records = ContentRepository.loadFarmuda(context, query.trim())
        loading = false
    }
    val visibleRecords = records.filter { !favoritesOnly || favorites.contains(it.id.toString()) }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            PremiumSectionHero(
                title = tr(language, "farmuda"),
                subtitle = when (language) {
                    AppLanguage.EN -> "Browse curated source entries and save the ones you value."
                    AppLanguage.AR -> "تصفّح السجلات الأصلية واحفظ ما يهمّك منها."
                    AppLanguage.CKB -> "تۆمارە ڕەسەنەکان بپشکنە و ئەوانەی گرنگن بۆت پاشەکەوت بکە."
                },
                icon = Icons.Outlined.Spa,
                foreground = foreground,
                accent = Color(0xFFE6B17F),
                metric = when (language) { AppLanguage.EN -> "${records.size} source entries • ${favorites.size} saved"; AppLanguage.AR -> "${records.size} سجل • ${favorites.size} محفوظ"; AppLanguage.CKB -> "${records.size} تۆمار • ${favorites.size} پاشەکەوتکراو" },
                eyebrow = when (language) { AppLanguage.EN -> "CURATED COLLECTION"; AppLanguage.AR -> "مختارات موثوقة"; AppLanguage.CKB -> "کۆکراوەی هەڵبژێردراو" }
            )
            Spacer(Modifier.height(10.dp))
            SearchBox(query, { query = it }, tr(language, "search"))
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                PremiumChoicePill(
                    label = if (language == AppLanguage.EN) "All records" else if (language == AppLanguage.AR) "كل السجلات" else "هەموو تۆمارەکان",
                    selected = !favoritesOnly,
                    accent = Gold,
                    foreground = foreground
                ) { favoritesOnly = false }
                PremiumChoicePill(
                    label = if (language == AppLanguage.EN) "Favourites" else if (language == AppLanguage.AR) "المفضلة" else "دڵخوازەکان",
                    selected = favoritesOnly,
                    accent = Gold,
                    foreground = foreground
                ) { favoritesOnly = true }
            }
        }
        if (visibleRecords.isEmpty()) {
            LoadingOrEmpty(
                loading, language, foreground,
                if (favoritesOnly && favorites.isEmpty()) {
                    if (language == AppLanguage.EN) "No favourites yet. Tap the star on a record to save it."
                    else if (language == AppLanguage.AR) "لا توجد مفضلات بعد. اضغط النجمة لحفظ السجل."
                    else "هێشتا دڵخواز نییە؛ بۆ پاشەکەوتکردن ئەستێرەکە دابگرە."
                } else tr(language, "not_found")
            )
        } else LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            itemsIndexed(visibleRecords, key = { _, item -> item.id }) { recordIndex, item ->
                val isFavorite = favorites.contains(item.id.toString())
                val title = item.title.ifBlank { item.badiniTitle }
                val preview = when (language) {
                    AppLanguage.CKB -> item.sorani.ifBlank { item.arabic }
                    AppLanguage.AR -> item.arabic.ifBlank { item.sorani }
                    AppLanguage.EN -> item.sorani.ifBlank { item.arabic }
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(0.8.dp, Gold.copy(alpha = 0.18f)),
                    modifier = Modifier.fillMaxWidth()
                        .staggeredEntrance(itemKey = item.id, index = recordIndex)
                        .animateContentSize()
                        .premiumClickable { selectedRecord = item }
                ) {
                    Column(Modifier.padding(15.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(title, color = Gold, fontSize = 14.sp, fontWeight = FontWeight.Bold, lineHeight = 21.sp)
                                Text(
                                    when (language) {
                                        AppLanguage.EN -> "Supplication / transmitted text"
                                        AppLanguage.AR -> "دعاء أو مأثور"
                                        AppLanguage.CKB -> "دوعا یان دەقی مەئسور"
                                    }, color = foreground.copy(alpha = 0.54f), fontSize = 10.sp
                                )
                            }
                            Box(
                                Modifier.size(38.dp).clip(CircleShape)
                                    .background(Gold.copy(alpha = if (isFavorite) 0.18f else 0.045f))
                                    .border(1.dp, Gold.copy(alpha = if (isFavorite) 0.48f else 0.17f), CircleShape)
                                    .premiumClickable(hapticFeedback = true) {
                                        val key = item.id.toString()
                                        favorites = if (isFavorite) favorites - key else favorites + key
                                        favoritesPrefs.edit().putStringSet("farmuda_ids", favorites).apply()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                AnimatedContent(
                                    targetState = isFavorite,
                                    transitionSpec = {
                                        (scaleIn(initialScale = 0.45f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)) + fadeIn(tween(140))) togetherWith
                                            (scaleOut(targetScale = 0.55f, animationSpec = tween(100)) + fadeOut(tween(100)))
                                    },
                                    label = "Farmuda favourite star pulse"
                                ) { saved ->
                                    Text(if (saved) "★" else "☆", color = Gold, fontSize = 22.sp)
                                }
                            }
                        }
                        if (preview.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            val cleanPreview = cleanHadithDisplayText(preview)
                            Text(
                                if (cleanPreview.length > 125) cleanPreview.take(125).trimEnd() + "…" else cleanPreview,
                                color = foreground.copy(alpha = 0.8f),
                                fontSize = if (language == AppLanguage.AR) 16.sp else 12.sp,
                                lineHeight = if (language == AppLanguage.AR) 27.sp else 20.sp,
                                maxLines = 3,
                                textAlign = if (language == AppLanguage.AR) TextAlign.End else TextAlign.Start,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(Modifier.height(9.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                when (language) {
                                    AppLanguage.EN -> "Read full text"
                                    AppLanguage.AR -> "قراءة النص كاملًا"
                                    AppLanguage.CKB -> "دەقەکە بە تەواوی بخوێنەوە"
                                }, color = Gold, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Outlined.ChevronRight, null, tint = Gold)
                        }
                    }
                }
            }
        }
    }

    selectedRecord?.let { item ->
        val title = item.title.ifBlank { item.badiniTitle }
        ModalBottomSheet(
            onDismissRequest = { selectedRecord = null },
            containerColor = cardColor,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(Modifier.fillMaxWidth().heightIn(max = 690.dp).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 10.dp)) {
                Text(title, color = Gold, fontSize = 21.sp, fontWeight = FontWeight.Bold, lineHeight = 29.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    when (language) {
                        AppLanguage.EN -> "Original source text and available Kurdish renderings are kept separate below."
                        AppLanguage.AR -> "النص الأصلي والترجمات الكردية المتاحة مفصولة أدناه."
                        AppLanguage.CKB -> "دەقی ڕەسەن و وەرگێڕانە کوردییە بەردەستەکان لە خوارەوە جیاکراونەتەوە."
                    }, color = foreground.copy(alpha = 0.56f), fontSize = 11.sp, lineHeight = 17.sp
                )
                if (item.arabic.isNotBlank()) {
                    Spacer(Modifier.height(16.dp))
                    Text(if (language == AppLanguage.EN) "ARABIC" else if (language == AppLanguage.AR) "النص العربي" else "دەقی عەرەبی", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(cleanHadithDisplayText(item.arabic), color = foreground, fontSize = 20.sp, lineHeight = 34.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                }
                if (item.sorani.isNotBlank()) {
                    Spacer(Modifier.height(16.dp))
                    Text(if (language == AppLanguage.EN) "SORANI KURDISH" else if (language == AppLanguage.AR) "الكردية السورانية" else "کوردی سۆرانی", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(cleanHadithDisplayText(item.sorani), color = foreground.copy(alpha = 0.86f), fontSize = 14.sp, lineHeight = 25.sp)
                }
                if (item.badini.isNotBlank() && item.badini != item.sorani) {
                    Spacer(Modifier.height(16.dp))
                    Text(if (language == AppLanguage.EN) "BADINI KURDISH" else if (language == AppLanguage.AR) "الكردية البادينية" else "کوردی بادینی", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(cleanHadithDisplayText(item.badini), color = foreground.copy(alpha = 0.86f), fontSize = 14.sp, lineHeight = 25.sp)
                }
                if (item.title.isNotBlank()) {
                    Spacer(Modifier.height(14.dp))
                    Text(if (language == AppLanguage.EN) "SOURCE TITLE" else if (language == AppLanguage.AR) "عنوان المصدر" else "ناونیشانی سەرچاوە", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(5.dp))
                    Text(cleanHadithDisplayText(item.title), color = foreground.copy(alpha = 0.75f), fontSize = 12.sp, lineHeight = 20.sp)
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(modifier = Modifier.weight(1f), onClick = {
                        val key = item.id.toString()
                        favorites = if (favorites.contains(key)) favorites - key else favorites + key
                        favoritesPrefs.edit().putStringSet("farmuda_ids", favorites).apply()
                    }) {
                        AnimatedContent(
                            targetState = favorites.contains(item.id.toString()),
                            transitionSpec = {
                                (scaleIn(initialScale = 0.5f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)) + fadeIn(tween(130))) togetherWith
                                    (scaleOut(targetScale = 0.6f, animationSpec = tween(100)) + fadeOut(tween(100)))
                            },
                            label = "Farmuda sheet favourite pulse"
                        ) { saved -> Text(if (saved) "★" else "☆", color = Gold) }
                        Spacer(Modifier.width(5.dp))
                        Text(if (language == AppLanguage.EN) "Favourite" else if (language == AppLanguage.AR) "مفضلة" else "دڵخواز", fontSize = 11.sp)
                    }
                    OutlinedButton(modifier = Modifier.weight(1f), onClick = {
                        val combined = buildList {
                            add(title)
                            if (item.arabic.isNotBlank()) add(item.arabic)
                            if (item.sorani.isNotBlank()) add(item.sorani)
                            if (item.badini.isNotBlank() && item.badini != item.sorani) add(item.badini)
                        }.joinToString("\n\n")
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Mashxall supplication", combined))
                        Toast.makeText(context, if (language == AppLanguage.EN) "Text copied" else if (language == AppLanguage.AR) "تم نسخ النص" else "دەقەکە کۆپی کرا", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Outlined.ContentCopy, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(if (language == AppLanguage.EN) "Copy" else if (language == AppLanguage.AR) "نسخ" else "کۆپی", fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
