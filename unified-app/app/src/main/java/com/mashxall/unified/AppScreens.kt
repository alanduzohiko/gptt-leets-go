package com.mashxall.unified

import android.content.Context
import android.text.Html
import android.content.ClipData
import android.content.ClipboardManager
import android.app.AlarmManager
import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Handler
import android.os.Build
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
import android.hardware.GeomagneticField
import android.os.ParcelFileDescriptor
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import coil.compose.SubcomposeAsyncImage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Tune
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.mashxall.unified.data.*
import com.mashxall.unified.notifications.PrayerAlarmScheduler
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
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

private val Gold: Color get() = MashxallAppearance.accent
private val DeepGreen = Color(0xFF103D32)
private val MainText = Color(0xFFF6F2E6)
private val CardDark = Color(0xFF102B25)

private data class QuranFontOption(
    val id: String,
    val labelCkb: String,
    val labelAr: String,
    val labelEn: String,
    val assetPath: String
)

private val QuranFontOptions = listOf(
    QuranFontOption(
        "uthmanic_hafs", "عثماني حفص", "عثماني حفص", "Uthmanic Hafs",
        "content/salawati_3.1.3/base/res/font/uthmanic_hafs.otf"
    ),
    QuranFontOption(
        "amiri_quran", "ئەمیری قورئان", "أميري القرآن", "Amiri Quran",
        "content/noor_tafsir_1.2.2/base/assets/fonts/amiri_quran.ttf"
    ),
    QuranFontOption(
        "me_quran", "Me Quran", "Me Quran", "Me Quran",
        "content/noor_tafsir_1.2.2/base/assets/fonts/me_quran.ttf"
    ),
    QuranFontOption(
        "hafs_regular", "حفص", "حفص", "Hafs Regular",
        "content/noor_tafsir_1.2.2/base/assets/fonts/hafs_regular.ttf"
    ),
    QuranFontOption(
        "amiri_modded", "ئەمیری", "أميري", "Amiri Arabic",
        "content/noor_tafsir_1.2.2/base/assets/fonts/amiri_modded.ttf"
    ),
    QuranFontOption(
        "adobe_arabic", "Adobe Arabic", "Adobe Arabic", "Adobe Arabic",
        "content/noor_tafsir_1.2.2/base/assets/fonts/adobe_arabic.otf"
    ),
    QuranFontOption(
        "al_qalam", "القلم", "القلم", "Al Qalam",
        "content/noor_tafsir_1.2.2/base/assets/fonts/al_qalam.ttf"
    ),
    QuranFontOption(
        "arabic_type", "Arabic Type", "Arabic Type", "Arabic Type",
        "content/noor_tafsir_1.2.2/base/assets/fonts/arabic_type.ttf"
    ),
    QuranFontOption(
        "kitab", "كتاب", "كتاب", "Kitab",
        "content/noor_tafsir_1.2.2/base/assets/fonts/kitab.ttf"
    ),
    QuranFontOption(
        "maddina", "مدّينة", "مدّينة", "Maddina",
        "content/noor_tafsir_1.2.2/base/assets/fonts/maddina.ttf"
    ),
    QuranFontOption(
        "noon", "نون", "نون", "Noon",
        "content/noor_tafsir_1.2.2/base/assets/fonts/noon.ttf"
    ),
    QuranFontOption(
        "hafs_otf", "حفص (OTF)", "حفص (OTF)", "Hafs OTF",
        "content/noor_tafsir_1.2.2/base/res/font/hafs.otf"
    ),
    QuranFontOption(
        "sarchia_muslim", "سەرچیا موسڵم", "سەرچیا موسڵم", "Sarchia Muslim",
        "content/noor_tafsir_1.2.2/base/assets/fonts/sarchia_muslim.ttf"
    )
)

private fun loadQuranFontFamily(context: Context, optionId: String): FontFamily {
    val option = QuranFontOptions.firstOrNull { it.id == optionId } ?: QuranFontOptions.first()
    return runCatching {
        FontFamily(Typeface.createFromAsset(context.assets, option.assetPath))
    }.getOrElse { FontFamily.Default }
}

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

internal fun isAdhanAudioAsset(title: String, assetPath: String, group: String): Boolean {
    val name = (title + " " + assetPath + " " + group).lowercase(Locale.ROOT)
    val keywords = listOf(
        "adhan", "athan", "azan", "muezzin", "muadhin", "muazzin", "takbir", "takbeer",
        "madina", "madinah", "mecca", "makkah", "fajr", "hijaz", "mosque", "masjid",
        "abdulbasit", "abdul_basit", "abdulmajid", "majid", "mansur", "mshari",
        "naqshbandi", "nasr", "peshawa", "rahid", "ramazan",
        "أذان", "اذان", "الاذان", "الأذان", "آذان", "مؤذن", "المؤذن", "المؤذنين",
        "مسجد", "المسجد", "صلاة", "الصلاة", "تكبير", "التكبير", "حي على الصلاة",
        "بانگ", "بەنگ", "موەزین", "موذن", "مزگەوت", "مزگوت", "نوێژ", "بانگی بەیانی"
    )
    return keywords.any { keyword -> name.contains(keyword) }
}

@Composable
private fun LoadingOrEmpty(loading: Boolean, language: AppLanguage, foreground: Color, message: String = tr(language, "no_data")) {
    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        if (loading) CircularProgressIndicator(color = Gold, modifier = Modifier.size(30.dp))
        else Text(message, color = foreground.copy(alpha = 0.66f), textAlign = TextAlign.Center, fontSize = 13.sp, lineHeight = 20.sp)
    }
}

@Composable
private fun SearchBox(value: String, onValue: (String) -> Unit, placeholder: String) {
    OutlinedTextField(
        value = value, onValueChange = onValue, singleLine = true, modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, fontSize = 13.sp) },
        leadingIcon = { Icon(Icons.Outlined.Search, null, tint = Gold) },
        shape = RoundedCornerShape(17.dp)
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun QuranScreen(
    language: AppLanguage,
    foreground: Color,
    cardColor: Color,
    initialSurah: Int,
    initialAyah: Int,
    onSurah: (Int) -> Unit,
    onTafsir: (Int, Int) -> Unit,
    onBookmark: (Int, Int) -> Unit,
    onOpenVerse: (Int, Int) -> Unit = { _, _ -> },
    onPlayAudio: (Int, Int) -> Unit = { _, _ -> },
    onPlaySurahAudio: (Int) -> Unit = {},
    onImmersiveMushaf: (Boolean) -> Unit = {},
    resumeLastMushaf: Boolean = false,
    onResumeMushafHandled: () -> Unit = {},
    openSelectedVerse: Boolean = false,
    onSelectedVerseHandled: () -> Unit = {},
    openReaderSettings: Boolean = false,
    onReaderSettingsHandled: () -> Unit = {},
    onOpenSourcePdf: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var surahs by remember { mutableStateOf<List<Surah>>(emptyList()) }
    var selected by rememberSaveable { mutableIntStateOf(initialSurah) }
    var verses by remember { mutableStateOf<List<Ayah>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var query by rememberSaveable { mutableStateOf("") }
    var searchVersesMode by rememberSaveable { mutableStateOf(false) }
    var searchingVerses by remember { mutableStateOf(false) }
    var verseSearchResults by remember { mutableStateOf<List<Ayah>>(emptyList()) }
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
    var showSurahList by rememberSaveable { mutableStateOf(true) }
    var showUnvowelled by rememberSaveable { mutableStateOf(false) }
    var showReaderSettings by remember { mutableStateOf(false) }
    var mushafInternalOverlayOpen by remember { mutableStateOf(false) }
    var mushafDarkMode by rememberSaveable { mutableStateOf(prefs.getBoolean("mushaf_dark_mode", false)) }
    var mushafSepiaMode by rememberSaveable { mutableStateOf(prefs.getBoolean("mushaf_sepia_mode", false)) }
    var verseTextScale by rememberSaveable { mutableFloatStateOf(prefs.getFloat("quran_verse_text_scale", 1.0f)) }
    var selectedQuranFont by rememberSaveable {
        mutableStateOf(prefs.getString("quran_verse_font", "uthmanic_hafs") ?: "uthmanic_hafs")
    }
    val quranFontFamily = remember(context, selectedQuranFont) {
        loadQuranFontFamily(context, selectedQuranFont)
    }
    var selectedTafsirBook by rememberSaveable {
        mutableStateOf(prefs.getString("quran_tafsir_book", null) ?: when (language) {
            AppLanguage.EN -> "sahih"
            AppLanguage.AR -> "mukhtasar"
            AppLanguage.CKB -> "roshn"
        })
    }
    var readerReciters by remember { mutableStateOf<List<Reciter>>(emptyList()) }
    var readerReciterSearch by rememberSaveable { mutableStateOf("") }
    val filteredReaderReciters = remember(readerReciters, readerReciterSearch) {
        readerReciters.filter { reciter ->
            reciter.audioType == 0 && (
                readerReciterSearch.isBlank() ||
                    reciter.nameArabic.contains(readerReciterSearch.trim(), ignoreCase = true) ||
                    reciter.nameEnglish.contains(readerReciterSearch.trim(), ignoreCase = true)
                )
        }
    }
    var preferredReaderReciterId by rememberSaveable {
        mutableIntStateOf(prefs.getInt("preferred_reciter_id", -1))
    }
    LaunchedEffect(Unit) {
        readerReciters = ContentRepository.loadReciters(context)
        if (preferredReaderReciterId == -1) {
            preferredReaderReciterId = readerReciters.firstOrNull { it.audioType == 0 }?.id ?: -1
            if (preferredReaderReciterId > 0) {
                prefs.edit().putInt("preferred_reciter_id", preferredReaderReciterId).apply()
            }
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
    androidx.activity.compose.BackHandler(
        enabled = openingMushaf ||
            (mushafMode && !mushafInternalOverlayOpen &&
                selectedAyahForSheet == null && !showReaderSettings &&
                !chooseTranslation && !chooseSurah)
    ) {
        // Dialogs and bottom sheets keep their own Android Back handling. The
        // page viewer only handles Back when no overlay is open.
        if (openingMushaf) {
            openingMushaf = false
            showSurahList = true
        } else {
            mushafMode = false
            showSurahList = true
        }
    }
    var mushafPage by rememberSaveable { mutableIntStateOf(prefs.getInt("last_mushaf_page", 1).coerceIn(1, 604)) }
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) { surahs = ContentRepository.loadSurahs(context) }
    LaunchedEffect(searchVersesMode, query, language, translationKey) {
        if (!searchVersesMode || query.isBlank()) {
            verseSearchResults = emptyList()
            searchingVerses = false
            return@LaunchedEffect
        }
        searchingVerses = true
        verseSearchResults = runCatching {
            ContentRepository.searchAyahs(context, query.trim(), language.key, translationKey)
        }.getOrDefault(emptyList())
        searchingVerses = false
    }
    LaunchedEffect(initialSurah) { selected = initialSurah }
    LaunchedEffect(resumeLastMushaf) {
        if (resumeLastMushaf) {
            selected = prefs.getInt("last_surah", initialSurah).coerceIn(1, 114)
            mushafPage = prefs.getInt("last_mushaf_page", 1).coerceIn(1, 604)
            query = ""
            showSurahList = false
            openingMushaf = false
            mushafMode = true
            onResumeMushafHandled()
        }
    }

    LaunchedEffect(openReaderSettings) {
        if (openReaderSettings) {
            selectedAyahForSheet = null
            showSurahList = true
            openingMushaf = false
            mushafMode = false
            showReaderSettings = true
            onReaderSettingsHandled()
        }
    }

    LaunchedEffect(openSelectedVerse, initialSurah, initialAyah) {
        if (openSelectedVerse) {
            val targetSurah = initialSurah.coerceIn(1, 114)
            val targetAyah = initialAyah.coerceAtLeast(1)
            selected = targetSurah
            query = ""
            showSurahList = false
            openingMushaf = true
            mushafMode = false
            selectedAyahForSheet = null
            val verse = runCatching {
                ContentRepository.loadAyahs(
                    context,
                    targetSurah,
                    language.key,
                    translationKey = "none"
                ).firstOrNull { it.number == targetAyah }
            }.getOrNull()
            if (verse == null) {
                // If the saved verse cannot be resolved from the source database,
                // gracefully return to the real surah list rather than guessing.
                openingMushaf = false
                showSurahList = true
                onSelectedVerseHandled()
                return@LaunchedEffect
            }
            mushafPage = verse.page.coerceIn(1, 604)
            prefs.edit()
                .putInt("last_mushaf_page", mushafPage)
                .putInt("last_surah", targetSurah)
                .putInt("last_ayah_number", targetAyah)
                .apply()
            openingMushaf = false
            mushafMode = true
            selectedAyahForSheet = targetSurah to targetAyah
            onSelectedVerseHandled()
        }
    }
    // Do not let a restored or legacy state expose the old text-only reader.
    // Quran entry is the surah list; an explicitly selected/resumed surah opens
    // the graphic Mushaf page viewer.
    LaunchedEffect(showSurahList, mushafMode, openingMushaf, resumeLastMushaf) {
        if (!showSurahList && !mushafMode && !openingMushaf && !resumeLastMushaf) {
            showSurahList = true
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
    val currentSurah = surahs.firstOrNull { it.number == selected }
    val surahNamesByNumber = remember(surahs) { surahs.associate { it.number to it.arabicName } }
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
        if (!mushafMode && !showSurahList) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                OutlinedButton(onClick = { chooseSurah = true }) { Text(tr(language, "choose_surah"), fontSize = 11.sp) }
            }
            Spacer(Modifier.height(8.dp))
            SearchBox(query, { query = it }, tr(language, "search"))
            Spacer(Modifier.height(7.dp))
            val currentTranslationLabel = when (language) {
                AppLanguage.CKB -> currentTranslation.ckb
                AppLanguage.AR -> currentTranslation.arabic
                AppLanguage.EN -> currentTranslation.english
            }
            OutlinedButton(onClick = { chooseTranslation = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Language, null, tint = Gold, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(8.dp))
                Text(tr(language, "translation") + ": " + currentTranslationLabel, fontSize = 11.sp, maxLines = 1)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilterChip(
                    selected = !mushafMode, onClick = { mushafMode = false },
                    label = { Text(tr(language, "verse_view"), fontSize = 11.sp, maxLines = 1) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = mushafMode, onClick = { mushafMode = true; query = "" },
                    label = { Text(tr(language, "mushaf_view"), fontSize = 11.sp, maxLines = 1) },
                    modifier = Modifier.weight(1f)
                )
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = !showUnvowelled,
                        onClick = { showUnvowelled = false },
                        label = { Text(if (language == AppLanguage.EN) "With diacritics" else if (language == AppLanguage.AR) "بالتشكيل" else "بە حەرەکات", fontSize = 10.sp) }
                    )
                    Spacer(Modifier.width(6.dp))
                    FilterChip(
                        selected = showUnvowelled,
                        onClick = { showUnvowelled = true },
                        label = { Text(if (language == AppLanguage.EN) "Without diacritics" else if (language == AppLanguage.AR) "بلا تشكيل" else "بێ حەرەکات", fontSize = 10.sp) }
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
                darkMode = mushafDarkMode,
                sepiaMode = mushafSepiaMode,
                surahTitle = currentSurah?.arabicName.orEmpty(),
                surahNames = surahNamesByNumber,
                onPlaySurahAudio = onPlaySurahAudio,
                onInternalOverlayChanged = { mushafInternalOverlayOpen = it },
                onPageChange = { requested ->
                    mushafPage = requested.coerceIn(1, 604)
                    prefs.edit().putInt("last_mushaf_page", mushafPage).apply()
                },
                onExitFullscreen = {
                    mushafMode = false
                    showSurahList = true
                },
                onReaderSettings = { showReaderSettings = true },
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(tr(language, "quran"), color = foreground, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Text(
                                when (language) {
                                    AppLanguage.EN -> "Choose a surah to open the printed Mushaf"
                                    AppLanguage.AR -> "اختر سورة لفتح المصحف المطبوع"
                                    AppLanguage.CKB -> "سوورەتێک هەڵبژێرە بۆ کردنەوەی مصحەف"
                                }, color = foreground.copy(alpha = 0.62f), fontSize = 12.sp
                            )
                        }
                        TextButton(onClick = { showReaderSettings = true }) {
                            Text("⚙", color = Gold, fontSize = 24.sp)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    SearchBox(
                        query,
                        { query = it },
                        if (searchVersesMode) when (language) {
                            AppLanguage.EN -> "Search Quran text…"
                            AppLanguage.AR -> "ابحث في نص القرآن…"
                            AppLanguage.CKB -> "لە دەقی قورئان بگەڕێ…"
                        } else tr(language, "choose_surah") + " / " + tr(language, "search")
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                        FilterChip(
                            selected = !searchVersesMode,
                            onClick = {
                                searchVersesMode = false
                                query = ""
                                verseSearchResults = emptyList()
                            },
                            label = {
                                Text(
                                    when (language) {
                                        AppLanguage.EN -> "Surah list"
                                        AppLanguage.AR -> "قائمة السور"
                                        AppLanguage.CKB -> "لیستی سوورەتەکان"
                                    }, fontSize = 11.sp
                                )
                            }
                        )
                        FilterChip(
                            selected = searchVersesMode,
                            onClick = {
                                searchVersesMode = true
                                query = ""
                            },
                            label = {
                                Text(
                                    when (language) {
                                        AppLanguage.EN -> "Search Quran text"
                                        AppLanguage.AR -> "البحث في الآيات"
                                        AppLanguage.CKB -> "گەڕان لە دەقی ئایەتەکان"
                                    }, fontSize = 11.sp
                                )
                            }
                        )
                    }
                }
                val filteredSurahs = surahs.filter { item ->
                    query.isBlank() || item.arabicName.contains(query.trim(), true) ||
                        item.englishName.contains(query.trim(), true) ||
                        item.number.toString() == query.trim()
                }
                if (searchVersesMode) {
                    when {
                        query.isBlank() -> Box(
                            Modifier.fillMaxSize().padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                when (language) {
                                    AppLanguage.EN -> "Search Arabic verse text and the selected translation."
                                    AppLanguage.AR -> "ابحث في نص الآيات العربية والترجمة المختارة."
                                    AppLanguage.CKB -> "لە دەقی عەرەبیی ئایەت و وەرگێڕانی هەڵبژێردراودا بگەڕێ."
                                }, color = foreground.copy(alpha = 0.6f), fontSize = 13.sp,
                                lineHeight = 21.sp, textAlign = TextAlign.Center
                            )
                        }
                        searchingVerses -> LoadingOrEmpty(true, language, foreground)
                        verseSearchResults.isEmpty() -> LoadingOrEmpty(
                            false, language, foreground,
                            when (language) {
                                AppLanguage.EN -> "No matching Quran verses were found."
                                AppLanguage.AR -> "لم يتم العثور على آيات مطابقة."
                                AppLanguage.CKB -> "هیچ ئایەتێکی هاوتا نەدۆزرایەوە."
                            }
                        )
                        else -> LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 13.dp, vertical = 5.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(verseSearchResults, key = { "quran-search-${it.surah}-${it.number}" }) { verse ->
                                val surahItem = surahs.firstOrNull { it.number == verse.surah }
                                val surahLabel = when (language) {
                                    AppLanguage.CKB -> surahItem?.arabicName ?: verse.surahNameArabic
                                    AppLanguage.AR -> surahItem?.arabicName ?: verse.surahNameArabic
                                    AppLanguage.EN -> verse.surahNameEnglish.ifBlank { surahItem?.englishName.orEmpty() }
                                }
                                Card(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        selected = verse.surah
                                        onSurah(verse.surah)
                                        onOpenVerse(verse.surah, verse.number)
                                        prefs.edit()
                                            .putInt("last_surah", verse.surah)
                                            .putInt("last_ayah_number", verse.number)
                                            .apply()
                                        query = ""
                                        searchVersesMode = false
                                        showSurahList = false
                                        openingMushaf = true
                                        scope.launch {
                                            val page = if (verse.page in 1..604) verse.page else runCatching {
                                                ContentRepository.loadAyahs(
                                                    context, verse.surah, language.key, translationKey = "none"
                                                ).firstOrNull { it.number == verse.number }?.page
                                            }.getOrNull() ?: 1
                                            mushafPage = page.coerceIn(1, 604)
                                            prefs.edit()
                                                .putInt("last_mushaf_page", mushafPage)
                                                .putInt("last_surah", verse.surah)
                                                .putInt("last_ayah_number", verse.number)
                                                .apply()
                                            openingMushaf = false
                                            mushafMode = true
                                        }
                                    },
                                    colors = CardDefaults.cardColors(containerColor = cardColor),
                                    shape = RoundedCornerShape(18.dp),
                                    border = BorderStroke(0.7.dp, Gold.copy(alpha = 0.19f))
                                ) {
                                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Column(Modifier.weight(1f)) {
                                                Text(
                                                    surahLabel + " • " + verse.surah + ":" + verse.number,
                                                    color = Gold, fontWeight = FontWeight.Bold, fontSize = 13.sp
                                                )
                                                Text(
                                                    when (language) {
                                                        AppLanguage.EN -> "Open matching Mushaf page"
                                                        AppLanguage.AR -> "فتح صفحة المصحف المطابقة"
                                                        AppLanguage.CKB -> "پەڕەی هاوتای مصحەف بکەرەوە"
                                                    }, color = foreground.copy(alpha = 0.55f), fontSize = 10.sp
                                                )
                                            }
                                            Icon(Icons.Outlined.ChevronRight, null, tint = Gold)
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            verse.arabic, color = foreground, fontSize = 20.sp,
                                            lineHeight = 33.sp, textAlign = TextAlign.End,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        if (verse.translation.isNotBlank()) {
                                            Spacer(Modifier.height(6.dp))
                                            Text(
                                                verse.translation, color = foreground.copy(alpha = 0.78f),
                                                fontSize = 12.sp, lineHeight = 20.sp, maxLines = 3
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (surahs.isEmpty()) {
                    LoadingOrEmpty(true, language, foreground)
                } else LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 13.dp, vertical = 5.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    items(filteredSurahs, key = { it.number }) { item ->
                        val placeLabel = when {
                            item.place.contains("meccan", ignoreCase = true) || item.place.contains("makki", ignoreCase = true) ->
                                when (language) { AppLanguage.EN -> "Meccan"; AppLanguage.AR -> "مكية"; AppLanguage.CKB -> "مەککی" }
                            item.place.contains("medinan", ignoreCase = true) || item.place.contains("madani", ignoreCase = true) ->
                                when (language) { AppLanguage.EN -> "Medinan"; AppLanguage.AR -> "مدنية"; AppLanguage.CKB -> "مەدەنی" }
                            else -> item.place
                        }
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable {
                                selected = item.number
                                onSurah(item.number)
                                query = ""
                                showSurahList = false
                                openingMushaf = true
                                scope.launch {
                                    val firstVerse = runCatching {
                                        ContentRepository.loadAyahs(context, item.number, language.key, translationKey = "none").firstOrNull()
                                    }.getOrNull()
                                    mushafPage = (firstVerse?.page ?: 1).coerceIn(1, 604)
                                    prefs.edit().putInt("last_mushaf_page", mushafPage).putInt("last_surah", item.number).putInt("last_ayah_number", 1).apply()
                                    openingMushaf = false
                                    mushafMode = true
                                }
                            },
                            colors = CardDefaults.cardColors(containerColor = cardColor),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(0.6.dp, Gold.copy(alpha = 0.16f))
                        ) {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(Gold.copy(alpha = 0.13f)),
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
        } else LazyColumn(state = listState, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(verses, key = { it.surah.toString() + ":" + it.number }) { verse ->
                var noteExpanded by rememberSaveable(verse.surah, verse.number) { mutableStateOf(false) }
                var showRelatedHadith by remember(verse.surah, verse.number) { mutableStateOf(false) }
                var loadingRelatedHadith by remember(verse.surah, verse.number) { mutableStateOf(false) }
                var relatedHadiths by remember(verse.surah, verse.number) { mutableStateOf<List<AyahHadith>>(emptyList()) }
                Card(
                    modifier = if (query.isNotBlank()) Modifier.clickable {
                        selected = verse.surah
                        onOpenVerse(verse.surah, verse.number)
                        prefs.edit().putInt("last_surah", verse.surah).putInt("last_ayah_number", verse.number).apply()
                        query = ""
                    } else Modifier,
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(0.6.dp, Gold.copy(alpha = 0.18f))
                ) {
                    Column(Modifier.padding(15.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(32.dp).clip(CircleShape).background(DeepGreen), contentAlignment = Alignment.Center) {
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
                            IconButton(onClick = { onTafsir(verse.surah, verse.number) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Outlined.AutoStories, tr(language, "tafsir"), tint = Gold, modifier = Modifier.size(19.dp))
                            }
                            IconButton(onClick = { onPlayAudio(verse.surah, verse.number) }, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    Icons.Outlined.PlayCircle,
                                    if (language == AppLanguage.EN) "Play this verse" else if (language == AppLanguage.AR) "استمع إلى هذه الآية" else "گوێگرتن لەم ئایەتە",
                                    tint = Gold,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            IconButton(onClick = {
                                onBookmark(verse.surah, verse.number)
                                val message = when (language) {
                                    AppLanguage.CKB -> "ئایەتەکە پاشەکەوت کرا"
                                    AppLanguage.AR -> "تم حفظ الآية في العلامات المرجعية"
                                    AppLanguage.EN -> "Verse saved to bookmarks"
                                }
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Outlined.BookmarkBorder, null, tint = foreground.copy(alpha = 0.6f), modifier = Modifier.size(19.dp))
                            }
                            IconButton(onClick = {
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
                            }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Outlined.Share, null, tint = Gold, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (showUnvowelled && verse.arabicWithoutHarakat.isNotBlank()) verse.arabicWithoutHarakat else verse.arabic,
                            color = foreground, fontSize = (23f * verseTextScale).sp, lineHeight = (39f * verseTextScale).sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth()
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
                            .clickable { selected = item.number; onSurah(item.number); chooseSurah = false }
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
                        Modifier.fillMaxWidth().clickable {
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


    if (showReaderSettings) AlertDialog(
        onDismissRequest = { showReaderSettings = false },
        title = {
            Text(
                when (language) {
                    AppLanguage.EN -> "Quran reader settings"
                    AppLanguage.AR -> "إعدادات قارئ القرآن"
                    AppLanguage.CKB -> "ڕێکخستنەکانی خوێندنەوەی قورئان"
                }, color = foreground, fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(Modifier.heightIn(max = 470.dp)) {
                item {
                    Text(
                        when (language) {
                            AppLanguage.EN -> "RECITER LIBRARY"
                            AppLanguage.AR -> "مكتبة القراء"
                            AppLanguage.CKB -> "کتێبخانەی قورئان‌خوێنەکان"
                        }, color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = readerReciterSearch,
                        onValueChange = { readerReciterSearch = it },
                        modifier = Modifier.fillMaxWidth().padding(top = 5.dp, bottom = 3.dp),
                        singleLine = true,
                        label = {
                            Text(
                                when (language) {
                                    AppLanguage.EN -> "Search reciters by name"
                                    AppLanguage.AR -> "ابحث عن قارئ بالاسم"
                                    AppLanguage.CKB -> "بە ناوی قورئان‌خوێن بگەڕێ"
                                }
                            )
                        },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) }
                    )
                    Text(
                        when (language) {
                            AppLanguage.EN -> "${filteredReaderReciters.size} of ${readerReciters.count { it.audioType == 0 }} reciters match"
                            AppLanguage.AR -> "${filteredReaderReciters.size} من ${readerReciters.count { it.audioType == 0 }} قارئًا"
                            AppLanguage.CKB -> "${filteredReaderReciters.size} لە ${readerReciters.count { it.audioType == 0 }} قورئان‌خوێن"
                        }, color = foreground.copy(alpha = 0.58f), fontSize = 10.sp
                    )
                    if (filteredReaderReciters.isEmpty()) {
                        Text(
                            when (language) {
                                AppLanguage.EN -> "No reciter matches this search."
                                AppLanguage.AR -> "لا يوجد قارئ يطابق البحث."
                                AppLanguage.CKB -> "هیچ قورئان‌خوێنێک لەگەڵ ئەم گەڕانە ناگونجێت."
                            }, color = foreground.copy(alpha = 0.58f), fontSize = 11.sp
                        )
                    }
                }
                item {
                    Text(
                        when (language) { AppLanguage.EN -> "DEFAULT TRANSLATION"; AppLanguage.AR -> "الترجمة الافتراضية"; AppLanguage.CKB -> "وەرگێڕانی بنەڕەت" },
                        color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold
                    )
                }
                items(ContentRepository.quranTranslations, key = { "reader-translation-" + it.id }) { choice ->
                    val label = when (language) {
                        AppLanguage.CKB -> choice.ckb
                        AppLanguage.AR -> choice.arabic
                        AppLanguage.EN -> choice.english
                    }
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            translationKey = choice.id
                            prefs.edit().putString("quran_translation", choice.id).apply()
                        }.padding(vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = translationKey == choice.id, onClick = {
                            translationKey = choice.id
                            prefs.edit().putString("quran_translation", choice.id).apply()
                        })
                        Spacer(Modifier.width(5.dp))
                        Text(label, color = foreground, fontSize = 12.sp)
                    }
                }
                item {
                    Spacer(Modifier.height(8.dp))
                    Divider(color = Gold.copy(alpha = 0.25f))
                    Spacer(Modifier.height(9.dp))
                    Text(
                        when (language) { AppLanguage.EN -> "DEFAULT TAFSIR"; AppLanguage.AR -> "التفسير الافتراضي"; AppLanguage.CKB -> "تەفسیرى بنەڕەت" },
                        color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold
                    )
                }
                items(ContentRepository.tafsirBooks, key = { "reader-tafsir-" + it.id }) { book ->
                    val label = when (language) {
                        AppLanguage.CKB -> book.labelCkb
                        AppLanguage.AR -> book.labelAr
                        AppLanguage.EN -> book.labelEn
                    }
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            selectedTafsirBook = book.id
                            prefs.edit().putString("quran_tafsir_book", book.id).apply()
                        }.padding(vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedTafsirBook == book.id, onClick = {
                            selectedTafsirBook = book.id
                            prefs.edit().putString("quran_tafsir_book", book.id).apply()
                        })
                        Spacer(Modifier.width(5.dp))
                        Text(label, color = foreground, fontSize = 12.sp)
                    }
                }
                item {
                    Spacer(Modifier.height(8.dp))
                    Divider(color = Gold.copy(alpha = 0.25f))
                    Spacer(Modifier.height(9.dp))
                    Text(
                        when (language) {
                            AppLanguage.EN -> "DEFAULT VERSE-BY-VERSE RECITER"
                            AppLanguage.AR -> "القارئ الافتراضي آيةً بآية"
                            AppLanguage.CKB -> "قورئان‌خوێنی بنەڕەت بۆ ئایەت بە ئایەت"
                        }, color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold
                    )
                    if (readerReciters.isEmpty()) {
                        Text(tr(language, "loading"), color = foreground.copy(alpha = 0.65f), fontSize = 11.sp)
                    }
                }
                item {
                    Text(
                        when (language) {
                            AppLanguage.EN -> "${filteredReaderReciters.size} matching reciters are listed below"
                            AppLanguage.AR -> "القراء المطابقون للبحث أدناه: ${filteredReaderReciters.size}"
                            AppLanguage.CKB -> "قورئان‌خوێنە گونجاوەکان لە خوارەوەن: ${filteredReaderReciters.size}"
                        }, color = foreground.copy(alpha = 0.58f), fontSize = 10.sp
                    )
                }
                items(
                    filteredReaderReciters,
                    key = { "reader-reciter-" + it.id }
                ) { reciter ->
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            preferredReaderReciterId = reciter.id
                            prefs.edit().putInt("preferred_reciter_id", reciter.id).apply()
                        }.padding(vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = preferredReaderReciterId == reciter.id,
                            onClick = {
                                preferredReaderReciterId = reciter.id
                                prefs.edit().putInt("preferred_reciter_id", reciter.id).apply()
                            }
                        )
                        Spacer(Modifier.width(5.dp))
                        Column(Modifier.weight(1f)) {
                            Text(reciter.nameArabic, color = foreground, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text(reciter.nameEnglish, color = foreground.copy(alpha = 0.58f), fontSize = 10.sp)
                        }
                    }
                }
                item {
                    Spacer(Modifier.height(9.dp))
                    Divider(color = Gold.copy(alpha = 0.25f))
                    Spacer(Modifier.height(9.dp))
                    Text(
                        when (language) {
                            AppLanguage.EN -> "QURAN STUDY GUIDES"
                            AppLanguage.AR -> "أدلة دراسة القرآن"
                            AppLanguage.CKB -> "ڕێنمای خوێندنەوە و تەجویدی قورئان"
                        }, color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold
                    )
                    Text(
                        when (language) {
                            AppLanguage.EN -> "Open the original included PDFs without leaving the Quran section permanently."
                            AppLanguage.AR -> "افتح ملفات PDF الأصلية المرفقة دون فصلها عن سياق القرآن."
                            AppLanguage.CKB -> "PDFە ڕەسەنە بەردەستەکان بکەرەوە بەبێ جیاکردنەوەیان لە بەشی قورئان."
                        }, color = foreground.copy(alpha = 0.58f), fontSize = 10.sp, lineHeight = 15.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = { onOpenSourcePdf("content/noor_tafsir_1.2.2/base/assets/tajwid.pdf") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.MenuBook, null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(
                            when (language) {
                                AppLanguage.EN -> "Open original Tajweed guide (PDF)"
                                AppLanguage.AR -> "فتح دليل التجويد الأصلي (PDF)"
                                AppLanguage.CKB -> "ڕێنمای ڕەسەنی تەجوید بکەرەوە (PDF)"
                            }, fontSize = 11.sp
                        )
                    }
                    OutlinedButton(
                        onClick = { onOpenSourcePdf("content/noor_tafsir_1.2.2/base/assets/kurdish_transliteration.pdf") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.Language, null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(
                            when (language) {
                                AppLanguage.EN -> "Kurdish transliteration guide (PDF)"
                                AppLanguage.AR -> "دليل النقل الصوتي الكردي (PDF)"
                                AppLanguage.CKB -> "ڕێنمای گواستنەوەی دەنگی کوردی (PDF)"
                            }, fontSize = 11.sp
                        )
                    }
                }
                item {
                    Spacer(Modifier.height(9.dp))
                    Divider(color = Gold.copy(alpha = 0.25f))
                    Spacer(Modifier.height(9.dp))
                    Text(
                        when (language) {
                            AppLanguage.EN -> "ARABIC VERSE FONT"
                            AppLanguage.AR -> "خط نص الآية العربية"
                            AppLanguage.CKB -> "فۆنتی دەقی ئایەتی عەرەبی"
                        }, color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        when (language) {
                            AppLanguage.EN -> "Changes Arabic text in the verse panel. The printed page artwork remains unchanged."
                            AppLanguage.AR -> "يغيّر خط النص العربي في لوحة الآية، ولا يغيّر رسم صفحة المصحف."
                            AppLanguage.CKB -> "فۆنتی دەقی عەرەبی لە پانێڵی ئایەتدا دەگۆڕێت؛ نەخشەی پەڕەی مصحەف ناگۆڕێت."
                        }, color = foreground.copy(alpha = 0.58f), fontSize = 10.sp, lineHeight = 15.sp
                    )
                }
                items(QuranFontOptions, key = { "quran-font-" + it.id }) { option ->
                        val label = when (language) {
                            AppLanguage.CKB -> option.labelCkb
                            AppLanguage.AR -> option.labelAr
                            AppLanguage.EN -> option.labelEn
                        }
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                selectedQuranFont = option.id
                                prefs.edit().putString("quran_verse_font", option.id).apply()
                            }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedQuranFont == option.id,
                                onClick = {
                                    selectedQuranFont = option.id
                                    prefs.edit().putString("quran_verse_font", option.id).apply()
                                }
                            )
                            Spacer(Modifier.width(5.dp))
                            Column(Modifier.weight(1f)) {
                                Text(label, color = foreground, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                                    color = foreground.copy(alpha = 0.78f),
                                    fontSize = 17.sp,
                                    lineHeight = 27.sp,
                                    fontFamily = remember(option.id) { loadQuranFontFamily(context, option.id) },
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                item {
                    Spacer(Modifier.height(9.dp))
                    Divider(color = Gold.copy(alpha = 0.25f))
                    Spacer(Modifier.height(9.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                when (language) {
                                    AppLanguage.EN -> "Night reading mode"
                                    AppLanguage.AR -> "وضع القراءة الليلي"
                                    AppLanguage.CKB -> "دۆخی خوێندنەوەی شەوانە"
                                }, color = foreground, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                when (language) {
                                    AppLanguage.EN -> "Invert paper and ink on the page-based Mushaf"
                                    AppLanguage.AR -> "عكس لون الورق والحبر داخل المصحف الرسومي"
                                    AppLanguage.CKB -> "ڕەنگی کاغەز و مەرەکەب لە مصحەفی گرافیکیدا بگۆڕە"
                                }, color = foreground.copy(alpha = 0.58f), fontSize = 10.sp, lineHeight = 15.sp
                            )
                        }
                        Switch(
                            checked = mushafDarkMode,
                            onCheckedChange = {
                                mushafDarkMode = it
                                if (it) mushafSepiaMode = false
                                prefs.edit()
                                    .putBoolean("mushaf_dark_mode", it)
                                    .putBoolean("mushaf_sepia_mode", mushafSepiaMode)
                                    .apply()
                            }
                        )
                    }
                    Spacer(Modifier.height(9.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                when (language) {
                                    AppLanguage.EN -> "Warm paper / sepia"
                                    AppLanguage.AR -> "الورق الدافئ / سيبيا"
                                    AppLanguage.CKB -> "کاغەزی گەرم / سێپیا"
                                }, color = foreground, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                when (language) {
                                    AppLanguage.EN -> "A soft cream page tone for long reading"
                                    AppLanguage.AR -> "لون كريمي هادئ للقراءة الطويلة"
                                    AppLanguage.CKB -> "ڕەنگێکی کرێمی نەرم بۆ خوێندنەوەی درێژ"
                                }, color = foreground.copy(alpha = 0.58f), fontSize = 10.sp, lineHeight = 15.sp
                            )
                        }
                        Switch(
                            checked = mushafSepiaMode,
                            onCheckedChange = {
                                mushafSepiaMode = it
                                if (it) mushafDarkMode = false
                                prefs.edit()
                                    .putBoolean("mushaf_sepia_mode", it)
                                    .putBoolean("mushaf_dark_mode", mushafDarkMode)
                                    .apply()
                            }
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        when (language) { AppLanguage.EN -> "VERSE TEXT SIZE"; AppLanguage.AR -> "حجم خط الآية"; AppLanguage.CKB -> "قەبارەی دەقی ئایەت" },
                        color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold
                    )
                    Slider(
                        value = verseTextScale,
                        onValueChange = {
                            verseTextScale = it
                            prefs.edit().putFloat("quran_verse_text_scale", it).apply()
                        },
                        valueRange = 0.8f..1.6f,
                        steps = 7
                    )
                    Text(
                        String.format(Locale.US, "%.0f%%", verseTextScale * 100f),
                        color = foreground.copy(alpha = 0.65f), fontSize = 11.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { showReaderSettings = false }) {
                Text(if (language == AppLanguage.EN) "Done" else if (language == AppLanguage.AR) "تم" else "تەواو")
            }
        }
    )

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
                    TextButton(onClick = { showReaderSettings = true }) { Text("⚙", color = Gold, fontSize = 22.sp) }
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
                            sheetVerse!!.arabic,
                            color = foreground,
                            fontSize = (27f * verseTextScale).sp,
                            lineHeight = (48f * verseTextScale).sp,
                            fontFamily = quranFontFamily,
                            textAlign = TextAlign.End,
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
                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    clipboard?.setPrimaryClip(ClipData.newPlainText("Quran verse", sheetVerse!!.arabic + "\n\n" + sheetVerse!!.translation))
                                    Toast.makeText(context, if (language == AppLanguage.EN) "Verse copied" else if (language == AppLanguage.AR) "تم نسخ الآية" else "ئایەتەکە کۆپی کرا", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Outlined.ContentCopy, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(3.dp))
                                Text(if (language == AppLanguage.EN) "Copy" else if (language == AppLanguage.AR) "نسخ" else "کۆپی", fontSize = 10.sp)
                            }
                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    onBookmark(target.first, target.second)
                                    Toast.makeText(context, if (language == AppLanguage.EN) "Verse saved" else if (language == AppLanguage.AR) "تم حفظ الآية" else "ئایەت پاشەکەوت کرا", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Outlined.BookmarkBorder, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(3.dp))
                                Text(if (language == AppLanguage.EN) "Save" else if (language == AppLanguage.AR) "حفظ" else "پاشەکەوت", fontSize = 10.sp)
                            }
                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    if (sheetAudioPlaying) {
                                        AudioController.stop()
                                        sheetAudioPlaying = false
                                    } else {
                                        scope.launch {
                                            val reciters = ContentRepository.loadReciters(context)
                                            val savedId = prefs.getInt("preferred_reciter_id", -1)
                                            val chosen = reciters.firstOrNull { it.id == savedId && it.audioType == 0 }
                                                ?: reciters.firstOrNull { it.audioType == 0 }
                                            if (chosen == null) {
                                                Toast.makeText(context, tr(language, "no_data"), Toast.LENGTH_SHORT).show()
                                            } else {
                                                val url = ContentRepository.reciterSampleUrl(chosen, target.first, target.second)
                                                sheetAudioPlaying = true
                                                AudioController.playRemote(context, url) { isPlaying -> sheetAudioPlaying = isPlaying }
                                            }
                                        }
                                    }
                                }
                            ) {
                                Icon(if (sheetAudioPlaying) Icons.Outlined.PauseCircle else Icons.Outlined.PlayCircle, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(3.dp))
                                Text(if (language == AppLanguage.EN) "Play" else if (language == AppLanguage.AR) "تشغيل" else "لێدان", fontSize = 10.sp)
                            }
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
                                FilterChip(
                                    selected = false, onClick = { showReaderSettings = true },
                                    label = { Text(if (language == AppLanguage.EN) "Translation / tafsir" else if (language == AppLanguage.AR) "الترجمة / التفسير" else "وەرگێڕان / تەفسیر", fontSize = 10.sp) }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = showSheetHadith, onClick = { showSheetHadith = !showSheetHadith },
                                    label = { Text(
                                        (if (language == AppLanguage.EN) "Related Hadith (" else if (language == AppLanguage.AR) "الأحاديث المرتبطة (" else "فەرموودەی پەیوەندیدار (") + sheetRelatedHadith.size + ")",
                                        fontSize = 10.sp
                                    ) }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = showSheetWordStudy, onClick = {
                                        showSheetWordStudy = !showSheetWordStudy
                                        if (showSheetWordStudy && sheetWords.isEmpty()) {
                                            sheetWordLoading = true
                                            scope.launch {
                                                sheetWords = ContentRepository.loadWordAnalysis(context, target.first, target.second)
                                                sheetWordLoading = false
                                            }
                                        }
                                    },
                                    label = { Text(if (language == AppLanguage.EN) "Word study" else if (language == AppLanguage.AR) "تحليل الكلمات" else "شیکردنەوەی وشە", fontSize = 10.sp) }
                                )
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

@Composable
private fun MushafPageViewer(
    language: AppLanguage,
    foreground: Color,
    page: Int,
    darkMode: Boolean,
    sepiaMode: Boolean,
    surahTitle: String,
    surahNames: Map<Int, String>,
    onPlaySurahAudio: (Int) -> Unit,
    onInternalOverlayChanged: (Boolean) -> Unit,
    onPageChange: (Int) -> Unit,
    onExitFullscreen: () -> Unit,
    onReaderSettings: () -> Unit,
    onVerseClick: (Int, Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    var bitmap by remember(page) { mutableStateOf<Bitmap?>(null) }
    var pageVerses by remember(page) { mutableStateOf<List<MushafPageVerse>>(emptyList()) }
    var selectedVerse by remember(page) { mutableStateOf<MushafPageVerse?>(null) }
    var verseRects by remember(page) { mutableStateOf<List<MushafPageRect>>(emptyList()) }
    var loading by remember(page) { mutableStateOf(true) }
    var failed by remember(page) { mutableStateOf(false) }
    var pageInput by rememberSaveable { mutableStateOf(page.toString()) }
    var showPagePicker by remember { mutableStateOf(false) }
    var showReaderMenu by remember { mutableStateOf(false) }
    var savedPageKeys by remember {
        mutableStateOf(context.getSharedPreferences("mashxall_preferences", Context.MODE_PRIVATE)
            .getStringSet("mushaf_page_bookmarks", emptySet()).orEmpty().toSet())
    }
    var swipeDistance by remember(page) { mutableFloatStateOf(0f) }
    val visibleSurahTitle = pageVerses.firstOrNull()?.surah?.let { surahNames[it] }.orEmpty().ifBlank { surahTitle }
    var scale by remember(page) { mutableFloatStateOf(1f) }
    var offsetX by remember(page) { mutableFloatStateOf(0f) }
    var offsetY by remember(page) { mutableFloatStateOf(0f) }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val nextScale = (scale * zoomChange).coerceIn(1f, 4f)
        if (scale <= 1.01f && nextScale <= 1.01f &&
            kotlin.math.abs(panChange.x) > kotlin.math.abs(panChange.y) * 1.2f
        ) {
            swipeDistance += panChange.x
            if (swipeDistance < -120f && page < 604) {
                swipeDistance = 0f
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onPageChange(page + 1)
            } else if (swipeDistance > 120f && page > 1) {
                swipeDistance = 0f
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onPageChange(page - 1)
            }
        } else {
            swipeDistance = 0f
            scale = nextScale
            if (scale > 1.01f) {
                offsetX += panChange.x
                offsetY += panChange.y
            } else {
                offsetX = 0f
                offsetY = 0f
            }
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
    Column(Modifier.fillMaxSize().background(if (darkMode) Color(0xFF0C1110) else if (sepiaMode) Color(0xFFF0E1BE) else Color(0xFFF6F3EB))) {
        Row(
            Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onExitFullscreen, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Outlined.ChevronRight,
                    contentDescription = when (language) {
                        AppLanguage.EN -> "Exit Mushaf"
                        AppLanguage.AR -> "الخروج من المصحف"
                        AppLanguage.CKB -> "دەرچوون لە مصحەف"
                    },
                    tint = Gold,
                    modifier = Modifier.rotate(if (language == AppLanguage.EN) 180f else 0f)
                )
            }
            Column(Modifier.weight(1f).padding(start = 2.dp, end = 3.dp)) {
                Text(
                    visibleSurahTitle.ifBlank { tr(language, "mushaf_view") },
                    color = foreground, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                    maxLines = 1
                )
                Text(
                    when (language) {
                        AppLanguage.EN -> "Mushaf • 604 pages"
                        AppLanguage.AR -> "المصحف • ٦٠٤ صفحات"
                        AppLanguage.CKB -> "مصحەف • ٦٠٤ پەڕە"
                    },
                    color = foreground.copy(alpha = 0.58f), fontSize = 9.sp, maxLines = 1
                )
            }
            IconButton(
                onClick = { pageVerses.firstOrNull()?.surah?.let(onPlaySurahAudio) },
                enabled = pageVerses.isNotEmpty() && !loading,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Outlined.PlayCircle,
                    contentDescription = when (language) {
                        AppLanguage.EN -> "Play current surah"
                        AppLanguage.AR -> "تشغيل السورة الحالية"
                        AppLanguage.CKB -> "لێدانی ئەم سوورەتە"
                    }, tint = if (pageVerses.isNotEmpty() && !loading) Gold else foreground.copy(alpha = 0.3f),
                    modifier = Modifier.size(24.dp)
                )
            }
            IconButton(
                onClick = {
                    val key = page.toString()
                    savedPageKeys = if (savedPageKeys.contains(key)) savedPageKeys - key else savedPageKeys + key
                    context.getSharedPreferences("mashxall_preferences", Context.MODE_PRIVATE)
                        .edit().putStringSet("mushaf_page_bookmarks", savedPageKeys).apply()
                    Toast.makeText(context, when {
                        savedPageKeys.contains(key) && language == AppLanguage.EN -> "Page bookmarked"
                        savedPageKeys.contains(key) && language == AppLanguage.AR -> "تم حفظ الصفحة"
                        savedPageKeys.contains(key) -> "پەڕەکە نیشان کرا"
                        language == AppLanguage.EN -> "Page bookmark removed"
                        language == AppLanguage.AR -> "تمت إزالة علامة الصفحة"
                        else -> "نیشانەی پەڕەکە سڕایەوە"
                    }, Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Outlined.BookmarkBorder,
                    contentDescription = if (savedPageKeys.contains(page.toString())) {
                        if (language == AppLanguage.EN) "Remove page bookmark" else if (language == AppLanguage.AR) "إزالة علامة الصفحة" else "سڕینەوەی نیشانەی پەڕە"
                    } else {
                        if (language == AppLanguage.EN) "Bookmark page" else if (language == AppLanguage.AR) "حفظ علامة الصفحة" else "نیشانکردنی پەڕە"
                    },
                    tint = if (savedPageKeys.contains(page.toString())) Gold else foreground.copy(alpha = 0.75f)
                )
            }
            IconButton(onClick = onReaderSettings, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Outlined.Tune,
                    contentDescription = when (language) {
                        AppLanguage.EN -> "Reader settings"
                        AppLanguage.AR -> "إعدادات المصحف"
                        AppLanguage.CKB -> "ڕێکخستنەکانی مصحەف"
                    }, tint = Gold
                )
            }
            Box {
                IconButton(
                    onClick = { showReaderMenu = true; onInternalOverlayChanged(true) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Text("⋮", color = Gold, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                }
                DropdownMenu(expanded = showReaderMenu, onDismissRequest = {
                    showReaderMenu = false
                    onInternalOverlayChanged(false)
                }) {
                    DropdownMenuItem(
                        text = { Text(if (language == AppLanguage.EN) "Go to page" else if (language == AppLanguage.AR) "الانتقال إلى صفحة" else "بڕۆ بۆ پەڕە") },
                        onClick = {
                            showReaderMenu = false
                            pageInput = page.toString()
                            showPagePicker = true
                            onInternalOverlayChanged(true)
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                when (language) {
                                    AppLanguage.EN -> "Choose another surah"
                                    AppLanguage.AR -> "اختيار سورة أخرى"
                                    AppLanguage.CKB -> "سوورەتێکی تر هەڵبژێرە"
                                }
                            )
                        },
                        onClick = {
                            showReaderMenu = false
                            onInternalOverlayChanged(false)
                            onExitFullscreen()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (language == AppLanguage.EN) "Reader appearance" else if (language == AppLanguage.AR) "مظهر المصحف" else "ڕووکاری مصحەف") },
                        onClick = {
                            showReaderMenu = false
                            onInternalOverlayChanged(false)
                            onReaderSettings()
                        }
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(42.dp).padding(horizontal = 15.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onPageChange(page - 1)
                },
                enabled = page > 1 && !loading,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    Icons.Outlined.ChevronRight,
                    contentDescription = when (language) {
                        AppLanguage.EN -> "Previous page"
                        AppLanguage.AR -> "الصفحة السابقة"
                        AppLanguage.CKB -> "پەڕەی پێشوو"
                    },
                    tint = if (page > 1) Gold else foreground.copy(alpha = 0.24f),
                    modifier = Modifier.rotate(if (language == AppLanguage.EN) 180f else 0f)
                )
            }
            Surface(
                color = Gold.copy(alpha = 0.13f),
                contentColor = Gold,
                shape = RoundedCornerShape(11.dp),
                border = BorderStroke(1.dp, Gold.copy(alpha = 0.3f)),
                modifier = Modifier
                    .height(32.dp)
                    .clickable {
                        pageInput = page.toString()
                        showPagePicker = true
                        onInternalOverlayChanged(true)
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Outlined.MenuBook, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(
                        when (language) {
                            AppLanguage.EN -> "Page $page of 604"
                            AppLanguage.AR -> "الصفحة $page من ٦٠٤"
                            AppLanguage.CKB -> "پەڕە $page لە ٦٠٤"
                        }, fontSize = 11.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
            IconButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onPageChange(page + 1)
                },
                enabled = page < 604 && !loading,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    Icons.Outlined.ChevronRight,
                    contentDescription = when (language) {
                        AppLanguage.EN -> "Next page"
                        AppLanguage.AR -> "الصفحة التالية"
                        AppLanguage.CKB -> "پەڕەی دواتر"
                    },
                    tint = if (page < 604) Gold else foreground.copy(alpha = 0.24f),
                    modifier = Modifier.rotate(if (language == AppLanguage.EN) 0f else 180f)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
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
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offsetX
                                translationY = offsetY
                            }
                            .transformable(transformState)
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
                                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        selectedVerse = hit
                                                        onVerseClick(hit.surah, hit.ayah)
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
                            modifier = Modifier.fillMaxSize()
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

    if (showPagePicker) AlertDialog(
        onDismissRequest = {
            showPagePicker = false
            onInternalOverlayChanged(false)
        },
        title = {
            Text(
                when (language) {
                    AppLanguage.EN -> "Go to Mushaf page"
                    AppLanguage.AR -> "الانتقال إلى صفحة المصحف"
                    AppLanguage.CKB -> "بڕۆ بۆ پەڕەی مصحەف"
                }, color = foreground, fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    when (language) {
                        AppLanguage.EN -> "Enter a page number from 1 to 604."
                        AppLanguage.AR -> "أدخل رقم صفحة من 1 إلى 604."
                        AppLanguage.CKB -> "ژمارەی پەڕەیەک لە ١ تا ٦٠٤ بنووسە."
                    }, color = foreground.copy(alpha = 0.65f), fontSize = 12.sp
                )
                OutlinedTextField(
                    value = pageInput,
                    onValueChange = { pageInput = it.filter(Char::isDigit).take(3) },
                    label = { Text(if (language == AppLanguage.EN) "Page number" else if (language == AppLanguage.AR) "رقم الصفحة" else "ژمارەی پەڕە") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                pageInput.toIntOrNull()?.takeIf { it in 1..604 }?.let(onPageChange)
                showPagePicker = false
                onInternalOverlayChanged(false)
            }) {
                Text(if (language == AppLanguage.EN) "Open page" else if (language == AppLanguage.AR) "فتح الصفحة" else "پەڕە بکەرەوە")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                showPagePicker = false
                onInternalOverlayChanged(false)
            }) { Text(tr(language, "back")) }
        }
    )
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
        Text(tr(language, "bookmarks"), color = foreground, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(5.dp))
        Text(
            when (language) {
                AppLanguage.EN -> "${bookmarks.size} saved verses • add personal notes to remember why each verse matters"
                AppLanguage.AR -> "${bookmarks.size} آية محفوظة • أضف ملاحظاتك الشخصية لكل آية"
                AppLanguage.CKB -> "${bookmarks.size} ئایەت پاشەکەوت کراوە • تێبینی تایبەتی بۆ هەر ئایەتێک زیاد بکە"
            }, color = foreground.copy(alpha = 0.56f), fontSize = 11.sp, lineHeight = 17.sp
        )
        Spacer(Modifier.height(8.dp))
        SearchBox(query, { query = it }, if (language == AppLanguage.EN) "Search surah, verse or note…" else if (language == AppLanguage.AR) "ابحث في السورة أو الآية أو الملاحظة…" else "لە سوورەت، ئایەت یان تێبینی بگەڕێ…")
        Spacer(Modifier.height(8.dp))
        if (visibleBookmarks.isEmpty()) {
            Text(
                if (bookmarks.isEmpty()) tr(language, "no_bookmarks") else tr(language, "not_found"),
                color = foreground.copy(alpha = 0.6f), fontSize = 13.sp
            )
        } else {
            LazyColumn(contentPadding = PaddingValues(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(visibleBookmarks, key = { it.first.toString() + ":" + it.second.toString() }) { saved ->
                    val key = "${saved.first}:${saved.second}"
                    Card(colors = CardDefaults.cardColors(containerColor = cardColor), shape = RoundedCornerShape(17.dp)) {
                        Column(Modifier.fillMaxWidth().padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.BookmarkBorder, null, tint = Gold)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f).clickable { onOpen(saved.first, saved.second) }) {
                                    Text(tr(language, "surah") + " " + saved.first, color = foreground, fontWeight = FontWeight.SemiBold)
                                    Text(tr(language, "ayah") + " " + saved.second, color = foreground.copy(alpha = 0.55f), fontSize = 12.sp)
                                }
                                TextButton(onClick = {
                                    editorKey = key
                                    editorText = notes[key].orEmpty()
                                }) {
                                    Text(if (notes[key].isNullOrBlank()) {
                                        if (language == AppLanguage.EN) "Note +" else if (language == AppLanguage.AR) "ملاحظة +" else "تێبینی +"
                                    } else {
                                        if (language == AppLanguage.EN) "Edit note" else if (language == AppLanguage.AR) "تعديل" else "دەستکاری"
                                    }, color = Gold, fontSize = 10.sp)
                                }
                                TextButton(onClick = {
                                    val remaining = bookmarks.filterNot { it == saved }
                                    bookmarks = remaining
                                    val nextNotes = notes - key
                                    notes = nextNotes
                                    val json = org.json.JSONObject().apply { nextNotes.forEach { (k, v) -> put(k, v) } }
                                    prefs.edit()
                                        .putStringSet("bookmarks", remaining.map { it.first.toString() + ":" + it.second.toString() }.toSet())
                                        .putString("bookmark_notes", json.toString())
                                        .apply()
                                }) { Text("×", color = Gold, fontSize = 20.sp) }
                            }
                            notes[key]?.takeIf { it.isNotBlank() }?.let { note ->
                                Spacer(Modifier.height(5.dp))
                                Text(note, color = foreground.copy(alpha = 0.72f), fontSize = 12.sp, lineHeight = 18.sp, maxLines = 3)
                            }
                            TextButton(onClick = { onOpen(saved.first, saved.second) }) {
                                Text(if (language == AppLanguage.EN) "Open at this verse →" else if (language == AppLanguage.AR) "فتح عند هذه الآية ←" else "لەسەر ئەم ئایەتە بکەرەوە ←", fontSize = 10.sp)
                            }
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
            Text(tr(language, "tafsir"), color = foreground, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                items(ContentRepository.tafsirBooks) { book ->
                    FilterChip(selected = selectedBook == book.id, onClick = { selectedBook = book.id }, label = { Text(tafsirLabel(book, language), fontSize = 11.sp, maxLines = 1) })
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = { onChangeVerse(previousVerse.first, previousVerse.second) },
                    enabled = surah > 1 || ayah > 1
                ) { Text("‹") }
                Column(
                    modifier = Modifier.weight(1f).clickable { chooseSurah = true }.padding(horizontal = 4.dp),
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
        Card(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = cardColor), shape = RoundedCornerShape(22.dp)) {
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
                        Modifier.fillMaxWidth().clickable {
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
            Text(tr(language, "hadith"), color = foreground, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(3.dp))
            Text(
                when (language) {
                    AppLanguage.EN -> if (selectedMode == 1) "$totalMatches articles" else "$totalMatches narrations found • $offset loaded"
                    AppLanguage.AR -> if (selectedMode == 1) "$totalMatches مقالة" else "عُثر على $totalMatches رواية • تم تحميل $offset"
                    AppLanguage.CKB -> if (selectedMode == 1) "$totalMatches وتار" else "$totalMatches فەرموودە دۆزرایەوە • $offset بارکراوە"
                }, color = foreground.copy(alpha = 0.54f), fontSize = 11.sp
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                FilterChip(
                    selected = selectedMode == 0,
                    onClick = { selectedMode = 0 },
                    label = { Text(if (language == AppLanguage.EN) "Narrations" else if (language == AppLanguage.AR) "الأحاديث" else "فەرموودە", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = selectedMode == 1,
                    onClick = { selectedMode = 1 },
                    label = { Text(if (language == AppLanguage.EN) "Articles (11)" else if (language == AppLanguage.AR) "المقالات (11)" else "وتارەکان (11)", fontSize = 11.sp) }
                )
            }
            Spacer(Modifier.height(8.dp))
            SearchBox(query, { query = it }, tr(language, "search_hadith"))
            if (selectedMode == 0) LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                item(key = "saved-hadith-filter") {
                    FilterChip(
                        selected = savedOnly,
                        onClick = { savedOnly = !savedOnly },
                        label = {
                            Text(
                                when (language) {
                                    AppLanguage.EN -> "Saved (${favoriteHadithIds.size})"
                                    AppLanguage.AR -> "المحفوظات (${favoriteHadithIds.size})"
                                    AppLanguage.CKB -> "پاشەکەوتکراوەکان (${favoriteHadithIds.size})"
                                }, fontSize = 10.sp
                            )
                        }
                    )
                }
                item(key = "all-hadith-collections") {
                    FilterChip(
                        selected = selectedCollectionId == 0,
                        onClick = { selectedCollectionId = 0 },
                        label = {
                            Text(
                                when (language) {
                                    AppLanguage.EN -> "All books"
                                    AppLanguage.AR -> "كل الكتب"
                                    AppLanguage.CKB -> "هەموو کتێبەکان"
                                }, fontSize = 10.sp
                            )
                        }
                    )
                }
                items(collections, key = { it.id }) { collection ->
                    val label = when (language) {
                        AppLanguage.EN -> collection.nameEnglish.ifBlank { collection.name }
                        AppLanguage.AR -> collection.nameArabic.ifBlank { collection.name }
                        AppLanguage.CKB -> collection.name.ifBlank { collection.nameArabic }
                    }
                    FilterChip(
                        selected = selectedCollectionId == collection.id,
                        onClick = { selectedCollectionId = collection.id },
                        label = { Text("$label · ${collection.hadithCount}", fontSize = 10.sp, maxLines = 1) }
                    )
                }
            }
        }
        if (selectedMode == 1) {
            if (articles.isEmpty()) LoadingOrEmpty(loading, language, foreground, tr(language, "not_found"))
            else LazyColumn(
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(articles, key = { "hadith-article-" + it.id }) { article ->
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
                        modifier = Modifier.fillMaxWidth().clickable { selectedArticleForDetails = article },
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
            items(hadithItems, key = { it.id }) { hadith ->
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
                val translatedText = when (language) {
                    AppLanguage.CKB -> hadith.sorani
                    AppLanguage.AR -> hadith.arabic
                    AppLanguage.EN -> hadith.english
                }
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { selectedHadithForDetails = hadith },
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
                            TextButton(
                                onClick = {
                                    val nextSaved = if (favoriteHadithIds.contains(hadith.id)) favoriteHadithIds - hadith.id else favoriteHadithIds + hadith.id
                                    favoriteHadithIds = nextSaved
                                    favoritePrefs.edit().putStringSet("hadith_ids", nextSaved.map { it.toString() }.toSet()).apply()
                                },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(if (favoriteHadithIds.contains(hadith.id)) "★" else "☆", color = Gold, fontSize = 22.sp)
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
                    OutlinedButton(
                        enabled = !loadingMore,
                        onClick = {
                            if (!loadingMore) scope.launch {
                                loadingMore = true
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
                                loadingMore = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (loadingMore) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        else Text(
                            when (language) {
                                AppLanguage.EN -> "Load more narrations"
                                AppLanguage.AR -> "تحميل المزيد من الروايات"
                                AppLanguage.CKB -> "فەرموودەی زیاتر باربکە"
                            }
                        )
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
                    OutlinedButton(modifier = Modifier.weight(1f), onClick = {
                        val nextSaved = if (favoriteHadithIds.contains(hadith.id)) favoriteHadithIds - hadith.id else favoriteHadithIds + hadith.id
                        favoriteHadithIds = nextSaved
                        favoritePrefs.edit().putStringSet("hadith_ids", nextSaved.map { it.toString() }.toSet()).apply()
                    }) {
                        Text(if (favoriteHadithIds.contains(hadith.id)) "★" else "☆", color = Gold)
                        Spacer(Modifier.width(5.dp))
                        Text(if (language == AppLanguage.EN) "Save" else if (language == AppLanguage.AR) "حفظ" else "پاشەکەوت", fontSize = 11.sp)
                    }
                    OutlinedButton(modifier = Modifier.weight(1f), onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Mashxall Hadith", collection + "\n" + chapter + "\n\n" + cleanHadithDisplayText(hadith.arabic) + "\n\n" + cleanHadithDisplayText(hadith.sorani) + "\n\n" + cleanHadithDisplayText(hadith.english) + "\n" + numberInBook))
                        Toast.makeText(context, if (language == AppLanguage.EN) "Hadith copied" else if (language == AppLanguage.AR) "تم نسخ الحديث" else "فەرموودەکە کۆپی کرا", Toast.LENGTH_SHORT).show()
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

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AzkarScreen(language: AppLanguage, foreground: Color, cardColor: Color) {
    val context = LocalContext.current
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selectedCategory != 0) {
                    IconButton(onClick = { selectedCategory = 0; selectedItem = null }) {
                        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = Gold)
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        if (selectedCategory == 0) tr(language, "azkar")
                        else categories.firstOrNull { it.id == selectedCategory }?.let {
                            when (language) { AppLanguage.CKB -> it.ckb; AppLanguage.AR -> it.arabic; AppLanguage.EN -> it.english }
                        } ?: tr(language, "azkar"),
                        color = foreground, fontSize = 23.sp, fontWeight = FontWeight.Bold
                    )
                    Text(
                        when (language) {
                            AppLanguage.EN -> if (selectedCategory == 0) "Choose a topic to read its adhkar separately." else "${visibleItems.size} remembrance entries"
                            AppLanguage.AR -> if (selectedCategory == 0) "اختر قسمًا لعرض أذكاره في صفحة مستقلة." else "${visibleItems.size} ذكرًا"
                            AppLanguage.CKB -> if (selectedCategory == 0) "بابەتێک هەڵبژێرە بۆ خوێندنەوەی زیکرەکانی." else "${visibleItems.size} زیکر"
                        }, color = foreground.copy(alpha = 0.58f), fontSize = 11.sp
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            SearchBox(query, { query = it; if (it.isNotBlank()) selectedCategory = 0 }, tr(language, "search_azkar"))
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                items(listOf(0, 1, 2, 3)) { index ->
                    FilterChip(
                        selected = selectedTab == index,
                        onClick = {
                            selectedTab = index
                            selectedCategory = 0
                        },
                        label = {
                            Text(
                                when (language) {
                                    AppLanguage.CKB -> listOf("بەشەکان", "بەیانی", "ئێواران", "دڵخوازەکان")[index]
                                    AppLanguage.AR -> listOf("الأقسام", "الصباح", "المساء", "المفضلة")[index]
                                    AppLanguage.EN -> listOf("Categories", "Morning", "Evening", "Favourites")[index]
                                }, fontSize = 11.sp
                            )
                        }
                    )
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
                items(categories, key = { it.id }) { category ->
                    val label = when (language) {
                        AppLanguage.CKB -> category.ckb.ifBlank { category.arabic }
                        AppLanguage.AR -> category.arabic.ifBlank { category.ckb }
                        AppLanguage.EN -> category.english.ifBlank { category.ckb }
                    }
                    val count = allItems.count { it.categoryId == category.id }
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { selectedCategory = category.id },
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
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { selectedItem = item },
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
                                TextButton(onClick = {
                                    val id = item.id.toString()
                                    favoriteIds = if (favoriteIds.contains(id)) favoriteIds - id else favoriteIds + id
                                    favoritePrefs.edit().putStringSet("favorite_ids", favoriteIds).apply()
                                }) { Text(if (favoriteIds.contains(item.id.toString())) "★" else "☆", color = Gold, fontSize = 22.sp) }
                            }
                            Spacer(Modifier.height(7.dp))
                            Text(
                                cleanHadithDisplayText(item.arabic).take(155).let { if (item.arabic.length > 155) it.trimEnd() + "…" else it },
                                color = foreground, fontSize = 18.sp, lineHeight = 30.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    if (language == AppLanguage.EN) "Repeat: $completed / $count"
                                    else if (language == AppLanguage.AR) "التكرار: $completed / $count"
                                    else "ژمارە: $completed / $count",
                                    color = foreground.copy(alpha = 0.58f), fontSize = 10.sp, modifier = Modifier.weight(1f)
                                )
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
        var count by remember(item.id) { mutableIntStateOf(progressPrefs.getInt(item.id.toString(), 0)) }
        val targetCount = item.count.coerceAtLeast(1)
        val isFavorite = favoriteIds.contains(item.id.toString())
        ModalBottomSheet(
            onDismissRequest = { selectedItem = null },
            containerColor = cardColor,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(Modifier.fillMaxWidth().heightIn(max = 690.dp).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 10.dp)) {
                Text(title, color = Gold, fontSize = 21.sp, fontWeight = FontWeight.Bold, lineHeight = 29.sp)
                Spacer(Modifier.height(13.dp))
                Text(cleanHadithDisplayText(item.arabic), color = foreground, fontSize = 25.sp, lineHeight = 44.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
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
                        Text("$count / $targetCount", color = Gold, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                count = 0
                                progressPrefs.edit().putInt(item.id.toString(), 0).apply()
                            }) {
                                Text(if (language == AppLanguage.EN) "Reset" else if (language == AppLanguage.AR) "إعادة" else "سفر")
                            }
                            Button(onClick = {
                                count = if (count + 1 >= targetCount) 0 else count + 1
                                progressPrefs.edit().putInt(item.id.toString(), count).apply()
                            }) {
                                Icon(Icons.Outlined.CheckCircle, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(5.dp))
                                Text(if (language == AppLanguage.EN) "Count" else if (language == AppLanguage.AR) "تسبيح" else "ژماردن")
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val id = item.id.toString()
                        favoriteIds = if (isFavorite) favoriteIds - id else favoriteIds + id
                        favoritePrefs.edit().putStringSet("favorite_ids", favoriteIds).apply()
                    }
                ) {
                    Text(
                        when (language) {
                            AppLanguage.EN -> if (isFavorite) "★ Remove from favourites" else "☆ Add to favourites"
                            AppLanguage.AR -> if (isFavorite) "★ إزالة من المفضلة" else "☆ أضف إلى المفضلة"
                            AppLanguage.CKB -> if (isFavorite) "★ لە دڵخوازەکان بیسڕەوە" else "☆ بیخە ناو دڵخوازەکان"
                        }, color = if (isFavorite) Gold else foreground
                    )
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
            Text(tr(language, "names"), color = foreground, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text(
                when (language) {
                    AppLanguage.EN -> "${visibleNames.size} of ${names.size} names • ${favoriteIds.size} favourites"
                    AppLanguage.AR -> "${visibleNames.size} من ${names.size} اسمًا • ${favoriteIds.size} مفضلة"
                    AppLanguage.CKB -> "${visibleNames.size} لە ${names.size} ناو • ${favoriteIds.size} دڵخواز"
                }, color = foreground.copy(alpha = 0.54f), fontSize = 11.sp
            )
            Spacer(Modifier.height(9.dp))
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
                FilterChip(
                    selected = !favoritesOnly,
                    onClick = { favoritesOnly = false },
                    label = {
                        Text(
                            when (language) {
                                AppLanguage.EN -> "All names"
                                AppLanguage.AR -> "كل الأسماء"
                                AppLanguage.CKB -> "هەموو ناوەکان"
                            }, fontSize = 11.sp
                        )
                    }
                )
                FilterChip(
                    selected = favoritesOnly,
                    onClick = { favoritesOnly = true },
                    label = {
                        Text(
                            when (language) {
                                AppLanguage.EN -> "Favourites"
                                AppLanguage.AR -> "المفضلة"
                                AppLanguage.CKB -> "دڵخوازەکان"
                            }, fontSize = 11.sp
                        )
                    }
                )
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
            items(visibleNames, key = { it.number }) { name ->
                var expanded by rememberSaveable(name.number) { mutableStateOf(false) }
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
                    modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }
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
                            IconButton(onClick = {
                                val key = name.number.toString()
                                favoriteIds = if (favorite) favoriteIds - key else favoriteIds + key
                                favoritePrefs.edit().putStringSet("favorite_ids", favoriteIds).apply()
                            }) {
                                Text(if (favorite) "★" else "☆", color = Gold, fontSize = 23.sp)
                            }
                            Text(if (expanded) "−" else "+", color = Gold, fontSize = 22.sp)
                        }
                        if (expanded) {
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

@Composable
fun TasbihScreen(language: AppLanguage, foreground: Color, cardColor: Color) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("mashxall_preferences", Context.MODE_PRIVATE) }
    var presets by remember { mutableStateOf(emptyList<TasbihPreset>()) }
    var selectedId by rememberSaveable { mutableIntStateOf(prefs.getInt("tasbih_preset_id", 3)) }
    var count by rememberSaveable { mutableIntStateOf(prefs.getInt("tasbih_count_3", 0)) }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val selected = presets.firstOrNull { it.id == selectedId }

    LaunchedEffect(Unit) {
        presets = ContentRepository.loadTasbihPresets(context)
        if (presets.none { it.id == selectedId } && presets.isNotEmpty()) selectedId = presets.first().id
    }
    LaunchedEffect(selectedId, presets) {
        count = prefs.getInt("tasbih_count_$selectedId", 0)
        prefs.edit().putInt("tasbih_preset_id", selectedId).apply()
    }
    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(tr(language, "tasbih"), color = foreground, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        if (presets.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                items(presets, key = { it.id }) { preset ->
                    FilterChip(
                        selected = selectedId == preset.id,
                        onClick = { selectedId = preset.id; count = prefs.getInt("tasbih_count_${preset.id}", 0) },
                        label = { Text(preset.zikr.replace("\n", " ").take(18), fontSize = 11.sp, maxLines = 1) }
                    )
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
            if (count >= target) {
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
        if (language == AppLanguage.CKB && !selected?.meaningCkb.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(selected?.meaningCkb.orEmpty(), color = foreground.copy(alpha = 0.7f), fontSize = 12.sp, textAlign = TextAlign.Center, lineHeight = 18.sp)
        }
        Spacer(Modifier.height(24.dp))
        Box(
            Modifier.size(238.dp).clip(CircleShape).background(
                androidx.compose.ui.graphics.Brush.radialGradient(listOf(Color(0xFF1E7259), DeepGreen, CardDark))
            ).clickable {
                count += 1
                prefs.edit().putInt("tasbih_count_$selectedId", count).putInt("tasbih_count", count).apply()
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(count.toString(), color = Color.White, fontSize = 54.sp, fontWeight = FontWeight.ExtraBold)
                Text(if (language == AppLanguage.EN) "TAP TO COUNT" else if (language == AppLanguage.AR) "اضغط للعدّ" else "بۆ ژماردن لێی بدە", color = Gold, fontSize = 10.sp, letterSpacing = 1.4.sp)
            }
        }
        Spacer(Modifier.height(23.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { count = 0; prefs.edit().putInt("tasbih_count_$selectedId", 0).putInt("tasbih_count", 0).apply() }) { Text(tr(language, "reset")) }
            OutlinedButton(onClick = { count = 0; prefs.edit().putInt("tasbih_count_$selectedId", 0).putInt("tasbih_count", 0).apply() }) {
                Text(if (language == AppLanguage.EN) "New round" else if (language == AppLanguage.AR) "دورة جديدة" else "خولێکی نوێ")
            }
        }
    }
}

@Composable
fun AudioScreen(
    language: AppLanguage,
    foreground: Color,
    cardColor: Color,
    initialSurah: Int = 1,
    initialAyah: Int = 1,
    focusReciters: Boolean = false,
    autoPlayRequested: Boolean = false,
    autoPlayWholeSurah: Boolean = false
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("mashxall_preferences", Context.MODE_PRIVATE) }
    var audio by remember { mutableStateOf<List<AudioItem>>(emptyList()) }
    var reciters by remember { mutableStateOf<List<Reciter>>(emptyList()) }
    var preferredReciterId by rememberSaveable { mutableIntStateOf(prefs.getInt("preferred_reciter_id", -1)) }
    var reciterModeFilter by rememberSaveable { mutableIntStateOf(0) }
    var reciterFavoritesOnly by rememberSaveable { mutableStateOf(false) }
    var reciterFavoriteIds by remember {
        mutableStateOf(prefs.getStringSet("favorite_reciter_ids", emptySet()).orEmpty().toSet())
    }
    val preferredVerseReciter = reciters.firstOrNull { it.id == preferredReciterId && it.audioType == 0 }
    var autoPlayKey by rememberSaveable { mutableStateOf("") }
    var playing by rememberSaveable { mutableStateOf("") }
    var downloadedReciterFiles by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var downloadingUrl by remember { mutableStateOf("") }
    var downloadGeneration by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var selectedTab by rememberSaveable { mutableIntStateOf(if (focusReciters) 1 else 0) }
    var audioQuery by rememberSaveable { mutableStateOf("") }
    var surahText by rememberSaveable { mutableStateOf(initialSurah.coerceIn(1, 114).toString()) }
    var ayahText by rememberSaveable { mutableStateOf(initialAyah.coerceAtLeast(1).toString()) }
    val audioImportScope = rememberCoroutineScope()
    val customAudioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) audioImportScope.launch {
            try {
                ContentRepository.importCustomAdhanAudio(context, uri)
                audio = ContentRepository.loadAudioItems(context) + ContentRepository.loadCustomAdhanItems(context)
                audioQuery = ""
                Toast.makeText(context, when (language) {
                    AppLanguage.EN -> "Adhan audio imported and added to the library."
                    AppLanguage.AR -> "تم استيراد صوت الأذان وإضافته إلى المكتبة."
                    AppLanguage.CKB -> "دەنگی بانگ هاوردە کرا و زیاد کرا بۆ کتێبخانە."
                }, Toast.LENGTH_LONG).show()
            } catch (error: Exception) {
                Toast.makeText(context, error.message ?: when (language) {
                    AppLanguage.EN -> "Could not import this audio file."
                    AppLanguage.AR -> "تعذر استيراد هذا الملف الصوتي."
                    AppLanguage.CKB -> "نەتوانرا ئەم فایلە دەنگییە هاوردە بکرێت."
                }, Toast.LENGTH_LONG).show()
            }
        }
    }
    LaunchedEffect(initialSurah, initialAyah, focusReciters) {
        surahText = initialSurah.coerceIn(1, 114).toString()
        ayahText = initialAyah.coerceAtLeast(1).toString()
        selectedTab = if (focusReciters) 1 else 0
    }
    LaunchedEffect(Unit) {
        audio = ContentRepository.loadAudioItems(context) + ContentRepository.loadCustomAdhanItems(context)
        reciters = ContentRepository.loadReciters(context)
        loading = false
    }
    LaunchedEffect(reciters, focusReciters, autoPlayRequested, autoPlayWholeSurah, initialSurah, initialAyah, preferredReciterId) {
        val targetType = if (autoPlayWholeSurah) 1 else 0
        val preferred = reciters.firstOrNull { it.id == preferredReciterId && it.audioType == targetType }
            ?: reciters.firstOrNull { it.audioType == targetType }
        if (autoPlayRequested && focusReciters && preferred != null) {
            val url = ContentRepository.reciterSampleUrl(
                preferred,
                initialSurah.coerceIn(1, 114),
                if (autoPlayWholeSurah) 1 else initialAyah.coerceAtLeast(1)
            )
            val key = "$url|${initialSurah}|${if (autoPlayWholeSurah) 1 else initialAyah}"
            if (autoPlayKey != key) {
                autoPlayKey = key
                playing = url
                val offline = downloadedReciterFiles[url] ?: ContentRepository.findDownloadedReciterAudio(context, url)
                if (offline != null) {
                    AudioController.play(context, offline) { state -> if (!state) playing = "" }
                } else {
                    AudioController.playRemote(context, url) { state -> if (!state) playing = "" }
                }
            }
        } else if (autoPlayRequested && focusReciters) {
            Toast.makeText(
                context,
                if (language == AppLanguage.EN) {
                    if (autoPlayWholeSurah) "No full-surah reciter is available in the bundled library." else "No verse-by-verse reciter is available in the bundled library."
                } else if (language == AppLanguage.AR) {
                    if (autoPlayWholeSurah) "لا يوجد قارئ للتلاوة الكاملة للسورة في المكتبة المضمّنة." else "لا يوجد قارئ يدعم التلاوة آيةً بآية في المكتبة المضمّنة."
                } else {
                    if (autoPlayWholeSurah) "لە کتێبخانەی ناوخۆدا قورئان‌خوێنی تەواوی سوورەت بەردەست نییە." else "لە کتێبخانەی ناوخۆدا قورئان‌خوێنی ئایەت بە ئایەت بەردەست نییە."
                },
                Toast.LENGTH_LONG
            ).show()
        }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(tr(language, "audio"), color = foreground, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(tr(language, "offline"), color = foreground.copy(alpha = 0.58f), fontSize = 12.sp)
        Spacer(Modifier.height(9.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            items(listOf(0, 1, 2)) { index ->
                FilterChip(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index; AudioController.stop(); playing = "" },
                    label = {
                        Text(
                            when (index) {
                                0 -> if (language == AppLanguage.EN) "On-device audio" else if (language == AppLanguage.AR) "صوتيات الجهاز" else "دەنگی ناوخۆیی"
                                1 -> if (language == AppLanguage.EN) "Reciters" else if (language == AppLanguage.AR) "القرّاء" else "قورئان‌خوێنەکان"
                                else -> if (language == AppLanguage.EN) "Adhan & mosque audio" else if (language == AppLanguage.AR) "الأذان وأصوات المساجد" else "بانگ و دەنگی مزگەوت"
                            }
                        )
                    }
                )
            }
        }
        Spacer(Modifier.height(7.dp))
        SearchBox(audioQuery, { audioQuery = it }, tr(language, "search"))
        if (selectedTab == 2) {
            Spacer(Modifier.height(7.dp))
            OutlinedButton(onClick = { customAudioPicker.launch(arrayOf("audio/*")) }, modifier = Modifier.fillMaxWidth()) {
                Text(when (language) {
                    AppLanguage.EN -> "Import from phone"
                    AppLanguage.AR -> "استيراد من الهاتف"
                    AppLanguage.CKB -> "لە مۆبایلەوە هاوردە بکە"
                }, fontSize = 12.sp)
            }
        }
        if (selectedTab == 0 || selectedTab == 2) {
            val visibleAudio = audio.filter { item ->
                val name = (item.title + " " + item.assetPath + " " + item.group).lowercase(Locale.ROOT)
                (selectedTab == 0 || isAdhanAudioAsset(item.title, item.assetPath, item.group)) &&
                    (audioQuery.isBlank() || name.contains(audioQuery.trim().lowercase(Locale.ROOT)))
            }
            if (visibleAudio.isEmpty()) LoadingOrEmpty(loading, language, foreground, if (selectedTab == 2) tr(language, "no_adhan_audio") else tr(language, "no_data"))
            else LazyColumn(contentPadding = PaddingValues(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                items(visibleAudio, key = { it.assetPath }) { item ->
                    Card(colors = CardDefaults.cardColors(containerColor = cardColor), shape = RoundedCornerShape(18.dp)) {
                        Row(Modifier.fillMaxWidth().clickable {
                            if (playing == item.assetPath) {
                                AudioController.stop()
                                playing = ""
                            } else {
                                playing = item.assetPath
                                AudioController.play(context, item.assetPath) { state -> if (!state) playing = "" }
                            }
                        }.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (playing == item.assetPath) Icons.Outlined.PauseCircle else Icons.Outlined.PlayCircle, null, tint = Gold, modifier = Modifier.size(34.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(item.title, color = foreground, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(item.group, color = foreground.copy(alpha = 0.48f), fontSize = 10.sp)
                            }
                            Icon(Icons.Outlined.VolumeUp, null, tint = foreground.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        } else {
            val visibleReciters = reciters.filter { reciter ->
                val matchesMode = when (reciterModeFilter) {
                    1 -> reciter.audioType == 0
                    2 -> reciter.audioType == 1
                    else -> true
                }
                val matchesFavourite = !reciterFavoritesOnly || reciterFavoriteIds.contains(reciter.id.toString())
                matchesMode && matchesFavourite &&
                    (reciter.nameArabic + " " + reciter.nameEnglish + " " + reciter.baseUrl).contains(audioQuery, ignoreCase = true)
            }.sortedWith(
                compareByDescending<Reciter> { it.id == preferredReciterId }
                    .thenBy { it.nameEnglish.ifBlank { it.nameArabic }.lowercase(Locale.ROOT) }
            )
            Text(
                if (language == AppLanguage.EN) {
                    if (preferredVerseReciter != null) "Your preferred verse-by-verse reciter will start automatically when you open this screen from a Quran verse."
                    else "Streaming needs internet. Choose a verse-by-verse reciter below; it will be remembered and the Quran play button can use it next time."
                } else if (language == AppLanguage.AR) {
                    if (preferredVerseReciter != null) "سيبدأ قارئك المفضّل الذي يدعم تشغيل الآيات تلقائيًا عند فتح هذه الشاشة من آية في القرآن."
                    else "البث يحتاج إلى الإنترنت. اختر قارئًا يدعم تشغيل الآيات أدناه؛ سيُحفظ اختيارك لزر تشغيل الآية لاحقًا."
                } else {
                    if (preferredVerseReciter != null) "کاتێک لەم شاشەیە لە ئایەتێکی قورئانەوە بکەیتەوە، قورئان‌خوێنی دڵخواز کە خوێندنەوەی ئایەت بە ئایەت پشتیوانی دەکات خۆکارانە دەست پێدەکات."
                    else "بۆ پەخش ئینتەرنێت پێویستە. قورئان‌خوێنێک هەڵبژێرە کە خوێندنەوەی ئایەت بە ئایەت پشتیوانی بکات؛ دواتر دوگمەی گوێگرتن ئەوە بەکاردهێنێت."
                },
                color = foreground.copy(alpha = 0.62f), fontSize = 11.sp, lineHeight = 17.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                when (language) {
                    AppLanguage.EN -> "${reciters.size} source reciters • ${reciters.count { it.audioType == 0 }} verse-by-verse • ${reciters.count { it.audioType == 1 }} full-surah"
                    AppLanguage.AR -> "${reciters.size} قارئ من المصدر • ${reciters.count { it.audioType == 0 }} آية بآية • ${reciters.count { it.audioType == 1 }} سورة كاملة"
                    AppLanguage.CKB -> "${reciters.size} قورئان‌خوێنی سەرچاوە • ${reciters.count { it.audioType == 0 }} ئایەت بە ئایەت • ${reciters.count { it.audioType == 1 }} سوورەتی تەواو"
                }, color = foreground.copy(alpha = 0.56f), fontSize = 10.sp, lineHeight = 15.sp
            )
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(listOf(0, 1, 2, 3)) { filter ->
                    FilterChip(
                        selected = if (filter == 3) reciterFavoritesOnly else !reciterFavoritesOnly && reciterModeFilter == filter,
                        onClick = {
                            if (filter == 3) {
                                reciterFavoritesOnly = !reciterFavoritesOnly
                            } else {
                                reciterFavoritesOnly = false
                                reciterModeFilter = filter
                            }
                        },
                        label = {
                            Text(
                                when (language) {
                                    AppLanguage.EN -> listOf("All", "Verse-by-verse", "Full surah", "Favourites")[filter]
                                    AppLanguage.AR -> listOf("الكل", "آية بآية", "السورة كاملة", "المفضلة")[filter]
                                    AppLanguage.CKB -> listOf("هەموو", "ئایەت بە ئایەت", "سوورەتی تەواو", "دڵخوازەکان")[filter]
                                }, fontSize = 10.sp
                            )
                        }
                    )
                }
            }
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
            Spacer(Modifier.height(7.dp))
            if (reciters.isEmpty()) LoadingOrEmpty(loading, language, foreground)
            else if (visibleReciters.isEmpty()) LoadingOrEmpty(false, language, foreground, tr(language, "not_found"))
            else LazyColumn(contentPadding = PaddingValues(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(visibleReciters, key = { it.id }) { reciter ->
                    val url = ContentRepository.reciterSampleUrl(
                        reciter,
                        surahText.toIntOrNull()?.coerceIn(1, 114) ?: 1,
                        ayahText.toIntOrNull()?.coerceAtLeast(1) ?: 1
                    )
                    val downloadedReference = downloadedReciterFiles[url]
                        ?: remember(url, downloadGeneration) { ContentRepository.findDownloadedReciterAudio(context, url) }
                    Card(colors = CardDefaults.cardColors(containerColor = cardColor), shape = RoundedCornerShape(17.dp)) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(38.dp).clip(RoundedCornerShape(13.dp)).background(DeepGreen), contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.GraphicEq, null, tint = Gold, modifier = Modifier.size(22.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(reciter.nameArabic, color = foreground, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text(reciter.nameEnglish, color = foreground.copy(alpha = 0.58f), fontSize = 10.sp, maxLines = 1)
                                Text(
                    when {
                        reciter.audioType == 0 && language == AppLanguage.EN -> "Verse-by-verse"
                        reciter.audioType == 0 && language == AppLanguage.AR -> "آية بآية"
                        reciter.audioType == 0 -> "ئایەت بە ئایەت"
                        language == AppLanguage.EN -> "Surah audio"
                        language == AppLanguage.AR -> "صوت السورة"
                        else -> "دەنگی سوورەت"
                    },
                    color = Gold.copy(alpha = 0.85f), fontSize = 9.sp
                )
                                TextButton(
                                    onClick = {
                                        val key = reciter.id.toString()
                                        reciterFavoriteIds = if (reciterFavoriteIds.contains(key)) {
                                            reciterFavoriteIds - key
                                        } else {
                                            reciterFavoriteIds + key
                                        }
                                        prefs.edit().putStringSet("favorite_reciter_ids", reciterFavoriteIds).apply()
                                    },
                                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp),
                                    modifier = Modifier.height(25.dp)
                                ) {
                                    Text(
                                        if (reciterFavoriteIds.contains(reciter.id.toString())) {
                                            if (language == AppLanguage.EN) "★ Saved reciter" else if (language == AppLanguage.AR) "★ قارئ محفوظ" else "★ قورئان‌خوێنی پاشەکەوتکراو"
                                        } else {
                                            if (language == AppLanguage.EN) "☆ Add to favourites" else if (language == AppLanguage.AR) "☆ أضف للمفضلة" else "☆ زیادکردن بۆ دڵخوازەکان"
                                        },
                                        color = if (reciterFavoriteIds.contains(reciter.id.toString())) Gold else foreground.copy(alpha = 0.56f),
                                        fontSize = 9.sp,
                                        maxLines = 1
                                    )
                                }
                                if (reciter.audioType == 0) {
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
                                            fontSize = 9.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                TextButton(
                                    enabled = downloadedReference == null && downloadingUrl != url,
                                    onClick = {
                                        audioImportScope.launch {
                                            downloadingUrl = url
                                            try {
                                                val localReference = ContentRepository.downloadReciterAudio(context, url)
                                                downloadedReciterFiles = downloadedReciterFiles + (url to localReference)
                                                downloadGeneration++
                                                Toast.makeText(
                                                    context,
                                                    when (language) {
                                                        AppLanguage.EN -> "Saved for offline playback."
                                                        AppLanguage.AR -> "تم حفظ التلاوة للتشغيل دون إنترنت."
                                                        AppLanguage.CKB -> "تلاوەتەکە بۆ لێدانی بەبێ ئینتەرنێت پاشەکەوت کرا."
                                                    }, Toast.LENGTH_SHORT
                                                ).show()
                                            } catch (error: Exception) {
                                                Toast.makeText(
                                                    context,
                                                    error.message ?: when (language) {
                                                        AppLanguage.EN -> "Download failed. Check the connection."
                                                        AppLanguage.AR -> "فشل التنزيل. تحقق من الاتصال."
                                                        AppLanguage.CKB -> "داگرتن سەرکەوتوو نەبوو؛ پەیوەندی بپشکنە."
                                                    }, Toast.LENGTH_LONG
                                                ).show()
                                            } finally {
                                                downloadingUrl = ""
                                            }
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 3.dp, vertical = 0.dp),
                                    modifier = Modifier.height(27.dp)
                                ) {
                                    Text(
                                        when {
                                            downloadingUrl == url -> if (language == AppLanguage.EN) "Downloading…" else if (language == AppLanguage.AR) "جارٍ التنزيل…" else "دادەگیرێت…"
                                            downloadedReference != null -> if (language == AppLanguage.EN) "Offline ✓" else if (language == AppLanguage.AR) "دون إنترنت ✓" else "بێ ئینتەرنێت ✓"
                                            else -> if (language == AppLanguage.EN) "Download" else if (language == AppLanguage.AR) "تنزيل" else "داگرتن"
                                        }, fontSize = 9.sp, maxLines = 1
                                    )
                                }
                                IconButton(onClick = {
                                    if (reciter.audioType == 0) {
                                        preferredReciterId = reciter.id
                                        prefs.edit().putInt("preferred_reciter_id", reciter.id).apply()
                                    }
                                    if (playing == url) {
                                        AudioController.stop()
                                        playing = ""
                                    } else {
                                        playing = url
                                        autoPlayKey = "$url|${initialSurah}|${initialAyah}"
                                        val offline = downloadedReciterFiles[url] ?: ContentRepository.findDownloadedReciterAudio(context, url)
                                        if (offline != null) AudioController.play(context, offline) { state -> if (!state) playing = "" }
                                        else AudioController.playRemote(context, url) { state -> if (!state) playing = "" }
                                    }
                                }) {
                                    Icon(if (playing == url) Icons.Outlined.PauseCircle else Icons.Outlined.PlayCircle, null, tint = Gold, modifier = Modifier.size(29.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private object AudioController {
    private var player: MediaPlayer? = null

    fun stop() {
        try { player?.stop() } catch (_: Exception) {}
        try { player?.release() } catch (_: Exception) {}
        player = null
    }

    fun play(context: Context, assetPath: String, callback: (Boolean) -> Unit) {
        stop()
        try {
            val source = if (assetPath.startsWith("download:")) {
                val filename = assetPath.removePrefix("download:")
                require(filename.isNotBlank() && File(filename).name == filename) { "Invalid downloaded recitation reference." }
                val directory = File(context.filesDir, "recitations").canonicalFile
                File(directory, filename).canonicalFile.also { file ->
                    require(file.parentFile == directory && file.isFile && file.length() > 0L) {
                        "The downloaded recitation is no longer available."
                    }
                }
            } else if (assetPath.startsWith("custom:")) {
                val filename = assetPath.removePrefix("custom:")
                require(filename.isNotBlank() && File(filename).name == filename) { "Invalid imported audio reference." }
                val directory = File(context.filesDir, "adhan-custom").canonicalFile
                File(directory, filename).canonicalFile.also { file ->
                    require(file.parentFile == directory && file.isFile && file.length() > 0L) {
                        "The imported audio file is no longer available."
                    }
                }
            } else {
                val hash = MessageDigest.getInstance("SHA-256").digest(assetPath.toByteArray())
                    .take(8).joinToString("") { "%02x".format(it) }
                File(context.cacheDir, "audio-" + hash + "." + assetPath.substringAfterLast('.', "mp3")).also { copy ->
                    context.assets.open(assetPath).use { input -> copy.outputStream().use { input.copyTo(it) } }
                }
            }
            player = MediaPlayer().apply {
                setDataSource(source.absolutePath)
                setOnPreparedListener { it.start(); callback(true) }
                setOnCompletionListener { callback(false); stop() }
                setOnErrorListener { _, _, _ -> callback(false); stop(); true }
                prepareAsync()
            }
        } catch (error: Exception) {
            callback(false)
            Toast.makeText(context, error.message ?: tr(AppLanguage.CKB, "no_data"), Toast.LENGTH_LONG).show()
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
                    Toast.makeText(context, if (languageHint(context) == "en") "Stream unavailable; try another reciter or connection." else "تعذر تشغيل البث؛ جرّب قارئًا آخر أو تحقق من الاتصال.", Toast.LENGTH_LONG).show()
                    stop()
                    true
                }
                prepareAsync()
            }
        } catch (_: Exception) {
            callback(false)
            Toast.makeText(context, "Stream unavailable; check the connection.", Toast.LENGTH_LONG).show()
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
        Text(
            when (language) {
                AppLanguage.EN -> "Backup & restore"
                AppLanguage.AR -> "النسخ الاحتياطي والاستعادة"
                AppLanguage.CKB -> "پاشەکەوتکردن و گەڕاندنەوە"
            }, color = foreground, fontSize = 24.sp, fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(10.dp))
        Text(
            when (language) {
                AppLanguage.EN -> "Export your personal settings to a password-protected encrypted file, or restore it later on this or another Android phone."
                AppLanguage.AR -> "صدّر إعداداتك الشخصية إلى ملف مشفّر بكلمة مرور، أو استعده لاحقًا على هذا الهاتف أو هاتف Android آخر."
                AppLanguage.CKB -> "زانیارییە تایبەتییەکانت بە وشەی نهێنی لە فایلێکی نهێنیکراودا هەڵگرە و دواتر لە هەر مۆبایلێکی Android بیگەڕێنەوە."
            }, color = foreground.copy(alpha = 0.72f), fontSize = 13.sp, lineHeight = 21.sp
        )
        Spacer(Modifier.height(16.dp))
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
            Text(
                when (language) {
                    AppLanguage.EN -> "Book library"
                    AppLanguage.AR -> "مكتبة الكتب"
                    AppLanguage.CKB -> "کتێبخانەی کتێب"
                }, color = foreground, fontSize = 22.sp, fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                when (language) {
                    AppLanguage.EN -> "${books.size} catalogue records • ${pdfs.size} bundled PDFs • ${downloadedPaths.size} downloaded • ${favoriteIds.size} favourites"
                    AppLanguage.AR -> "${books.size} سجلًا • ${pdfs.size} PDF مرفق • ${downloadedPaths.size} منزّل • ${favoriteIds.size} مفضلة"
                    AppLanguage.CKB -> "${books.size} تۆمار • ${pdfs.size} PDFی ناوخۆیی • ${downloadedPaths.size} داگیراو • ${favoriteIds.size} دڵخواز"
                }, color = foreground.copy(alpha = 0.55f), fontSize = 11.sp
            )
            Spacer(Modifier.height(10.dp))
            SearchBox(query, { query = it }, tr(language, "search"))
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                FilterChip(
                    selected = !favoritesOnly,
                    onClick = { favoritesOnly = false },
                    label = { Text(if (language == AppLanguage.EN) "All books" else if (language == AppLanguage.AR) "كل الكتب" else "هەموو کتێبەکان", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = favoritesOnly,
                    onClick = { favoritesOnly = true },
                    label = { Text(if (language == AppLanguage.EN) "Favourites" else if (language == AppLanguage.AR) "المفضلة" else "دڵخوازەکان", fontSize = 11.sp) }
                )
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
                items(visiblePdfs, key = { it }) { path ->
                    Card(
                        Modifier.clickable { onOpenPdf(path) },
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
            items(visibleBooks, key = { "book-" + it.id }) { item ->
                val favorite = favoriteIds.contains(item.id.toString())
                Card(
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
                                    IconButton(
                                        onClick = {
                                            val key = item.id.toString()
                                            favoriteIds = if (favorite) favoriteIds - key else favoriteIds + key
                                            favoritePrefs.edit().putStringSet("book_ids", favoriteIds).apply()
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) { Text(if (favorite) "★" else "☆", color = Gold, fontSize = 21.sp) }
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
                        Button(
                            enabled = item.id !in downloadingIds,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
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
                        ) {
                            if (item.id in downloadingIds) {
                                CircularProgressIndicator(modifier = Modifier.size(17.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text(if (language == AppLanguage.EN) "Downloading PDF…" else if (language == AppLanguage.AR) "جارٍ تنزيل PDF…" else "PDF دادەگیرێت…")
                            } else {
                                Icon(
                                    if (localPdfPath != null) Icons.Outlined.MenuBook else Icons.Outlined.ChevronRight,
                                    null, modifier = Modifier.size(17.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    when (language) {
                                        AppLanguage.EN -> if (localPdfPath != null) "Open offline PDF" else "Download PDF and open"
                                        AppLanguage.AR -> if (localPdfPath != null) "فتح PDF دون إنترنت" else "تنزيل PDF وفتحه"
                                        AppLanguage.CKB -> if (localPdfPath != null) "PDF بکەرەوە (بێ ئینتەرنێت)" else "PDF دابگرە و بیکەرەوە"
                                    }
                                )
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
        Text(assetPath.substringAfterLast('/'), color = foreground, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 2)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { pageIndex = (pageIndex - 1).coerceAtLeast(0) }, enabled = pageIndex > 0 && !loading) {
                Text(tr(language, "previous"))
            }
            Text(if (pageCount > 0) tr(language, "page") + " " + (pageIndex + 1) + " / " + pageCount else tr(language, "open_pdf"), color = Gold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            OutlinedButton(onClick = { pageIndex = (pageIndex + 1).coerceAtMost(pageCount - 1) }, enabled = pageCount > 0 && pageIndex < pageCount - 1 && !loading) {
                Text(tr(language, "next"))
            }
        }
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxSize().weight(1f).clip(RoundedCornerShape(14.dp)).background(Color(0xFF1B2925)), contentAlignment = Alignment.Center) {
            when {
                loading -> CircularProgressIndicator(color = Gold)
                bitmap != null -> Image(bitmap!!.asImageBitmap(), contentDescription = assetPath.substringAfterLast('/'), contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
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
            Text(tr(language, "lexicon"), color = foreground, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                when (language) {
                    AppLanguage.EN -> "$totalMatches entries found • $offset loaded"
                    AppLanguage.AR -> "عُثر على $totalMatches مدخل • تم تحميل $offset"
                    AppLanguage.CKB -> "$totalMatches دەروازە دۆزرایەوە • $offset بارکراوە"
                }, color = foreground.copy(alpha = 0.54f), fontSize = 11.sp
            )
            Spacer(Modifier.height(8.dp))
            SearchBox(query, { query = it }, tr(language, "search_words"))
        }
        if (entries.isEmpty()) {
            LoadingOrEmpty(loading, language, foreground)
        } else {
            LazyColumn(
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(entries, key = { index, item -> "${item.title}:${item.root}:$index" }) { _, item ->
                    Card(colors = CardDefaults.cardColors(containerColor = cardColor), shape = RoundedCornerShape(16.dp)) {
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
                        OutlinedButton(
                            enabled = !loadingMore,
                            onClick = {
                                if (!loadingMore) scope.launch {
                                    loadingMore = true
                                    val next = ContentRepository.searchLexicon(context, query.trim(), offset, 100)
                                    entries = entries + next
                                    offset += next.size
                                    hasMore = next.size == 100 && offset < totalMatches
                                    loadingMore = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (loadingMore) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            else Text(
                                when (language) {
                                    AppLanguage.EN -> "Load more entries"
                                    AppLanguage.AR -> "تحميل المزيد من النتائج"
                                    AppLanguage.CKB -> "دەروازەی زیاتر باربکە"
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

internal fun requestCurrentLocation(
    context: Context,
    onLocation: (Location) -> Unit,
    onError: (String) -> Unit
) {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    if (manager == null) {
        onError("Location service is unavailable")
        return
    }
    val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        .filter { provider -> try { manager.isProviderEnabled(provider) } catch (_: Exception) { false } }
    if (providers.isEmpty()) {
        val openedSettings = runCatching {
            context.startActivity(
                Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.isSuccess
        onError(if (openedSettings) "LOCATION_SETTINGS_OPENED" else "LOCATION_SERVICES_OFF")
        return
    }

    val mainHandler = Handler(Looper.getMainLooper())
    val listeners = mutableListOf<LocationListener>()
    var bestLocation: Location? = null
    var finished = false
    var deferredFinish: Runnable? = null
    var timeout: Runnable? = null

    fun cleanup() {
        timeout?.let(mainHandler::removeCallbacks)
        deferredFinish?.let(mainHandler::removeCallbacks)
        listeners.forEach { listener ->
            try { manager.removeUpdates(listener) } catch (_: Exception) {}
        }
    }

    val finishBest = Runnable {
        if (!finished) {
            finished = true
            cleanup()
            val best = bestLocation
            when {
                best == null -> onError("NO_FRESH_LOCATION")
                best.hasAccuracy() && best.accuracy > 1500f -> onError("LOCATION_ACCURACY_TOO_LOW")
                else -> onLocation(best)
            }
        }
    }
    timeout = Runnable { finishBest.run() }
    mainHandler.postDelayed(timeout!!, 20_000L)

    providers.forEach { provider ->
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (finished) return
                val prior = bestLocation
                val newIsBetter = prior == null ||
                    (location.hasAccuracy() && (!prior.hasAccuracy() || location.accuracy < prior.accuracy))
                if (newIsBetter) bestLocation = Location(location)

                // Finish immediately for a good GPS fix; otherwise allow a short
                // window for the other provider to return a more accurate point.
                if (bestLocation?.hasAccuracy() == true && bestLocation!!.accuracy <= 15f) {
                    finishBest.run()
                } else {
                    deferredFinish?.let(mainHandler::removeCallbacks)
                    deferredFinish = Runnable { finishBest.run() }
                    mainHandler.postDelayed(deferredFinish!!, 6000L)
                }
            }
            @Deprecated("Deprecated by Android; retained for API 24 compatibility")
            override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) = Unit
            override fun onProviderEnabled(provider: String) = Unit
            override fun onProviderDisabled(provider: String) = Unit
        }
        listeners += listener
        try {
            manager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
        } catch (_: SecurityException) {
            if (!finished) {
                finished = true
                cleanup()
                onError("Location permission is required")
            }
            return@forEach
        } catch (_: IllegalArgumentException) {
            // Provider was disabled between discovery and request; try the other one.
        }
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

private fun hasValidPrayerTimesForScreen(row: PrayerRow): Boolean =
    listOf(row.fajr, row.sunrise, row.dhuhr, row.asr, row.maghrib, row.isha).all { value ->
        val parts = value.trim().split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull()
        val minute = parts.getOrNull(1)?.toIntOrNull()
        hour != null && minute != null && hour in 0..23 && minute in 0..59
    }

private fun formatPrayerCountdown(remainingMillis: Long): String {
    val totalSeconds = remainingMillis.coerceAtLeast(0L) / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerScreen(language: AppLanguage, foreground: Color, cardColor: Color, selectedCity: String, onCity: (String) -> Unit, onQibla: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("mashxall_preferences", Context.MODE_PRIVATE) }
    var cities by remember { mutableStateOf<List<City>>(emptyList()) }
    var times by remember { mutableStateOf<PrayerRow?>(null) }
    var tomorrowTimes by remember { mutableStateOf<PrayerRow?>(null) }
    var clockNowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val use24Hour = false
    var chooseCity by remember { mutableStateOf(false) }
    var citySearch by rememberSaveable { mutableStateOf("") }
    var searchingCities by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var locating by remember { mutableStateOf(false) }
    var locationMessage by remember { mutableStateOf("") }
    val savedGpsAccuracy = prefs.getString("gps_accuracy", null)?.toFloatOrNull()
    val savedGpsIsTooCoarse = savedGpsAccuracy != null && savedGpsAccuracy > 1500f
    var gpsLatitude by remember {
        mutableStateOf(if (savedGpsIsTooCoarse) null else prefs.getString("gps_latitude", null)?.toDoubleOrNull())
    }
    var gpsLongitude by remember {
        mutableStateOf(if (savedGpsIsTooCoarse) null else prefs.getString("gps_longitude", null)?.toDoubleOrNull())
    }
    var gpsAccuracy by remember { mutableStateOf(if (savedGpsIsTooCoarse) null else savedGpsAccuracy) }
    var manualLatitude by remember { mutableStateOf(prefs.getString("manual_city_latitude", null)?.toDoubleOrNull()) }
    var manualLongitude by remember { mutableStateOf(prefs.getString("manual_city_longitude", null)?.toDoubleOrNull()) }
    var manualCountry by rememberSaveable { mutableStateOf(prefs.getString("selected_city_country", "") ?: "") }
    // Match the source-app behavior: source timetable data takes priority. The
    // fixed astronomical settings are only a last-resort fallback when no source
    // city timetable is available; the user-facing calculation-method picker is
    // intentionally omitted so it cannot silently change original timetable data.
    val calculationMethod = "mwl"
    val asrSchool = "shafi"
    var adhanEnabled by rememberSaveable { mutableStateOf(prefs.getBoolean("adhan_enabled", false)) }
    var adhanDays by remember {
        mutableStateOf(
            prefs.getStringSet(PrayerAlarmScheduler.KEY_DAYS, null)?.toSet()
                ?: (Calendar.SUNDAY..Calendar.SATURDAY).map { it.toString() }.toSet()
        )
    }
    var adhanAssets by remember { mutableStateOf(emptyList<AudioItem>()) }
    var selectedAdhanAsset by rememberSaveable {
        mutableStateOf(prefs.getString("adhan_audio_asset", "") ?: "")
    }
    var adhanSoundQuery by rememberSaveable { mutableStateOf("") }
    var previewingAdhan by remember { mutableStateOf("") }
    var chooseAdhanSound by remember { mutableStateOf(false) }
    var adhanMessage by remember { mutableStateOf("") }
    val adhanImportScope = rememberCoroutineScope()
    val customAdhanPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) adhanImportScope.launch {
            try {
                val imported = ContentRepository.importCustomAdhanAudio(context, uri)
                adhanAssets = ContentRepository.loadAudioItems(context) + ContentRepository.loadCustomAdhanItems(context)
                selectedAdhanAsset = imported.assetPath
                adhanSoundQuery = ""
                prefs.edit().putString("adhan_audio_asset", imported.assetPath).apply()
                adhanMessage = when (language) {
                    AppLanguage.EN -> "Imported " + imported.title + ". It will be used for upcoming prayer alerts."
                    AppLanguage.AR -> "تم استيراد " + imported.title + " وسيُستخدم في تنبيهات الصلاة القادمة."
                    AppLanguage.CKB -> "دەنگی " + imported.title + " هاوردە کرا و لە ئاگادارکردنەوەکانی داهاتوودا بەکاردێت."
                }
                Toast.makeText(context, adhanMessage, Toast.LENGTH_LONG).show()
            } catch (error: Exception) {
                val message = error.message ?: when (language) {
                    AppLanguage.EN -> "Could not import this audio file."
                    AppLanguage.AR -> "تعذر استيراد هذا الملف الصوتي."
                    AppLanguage.CKB -> "نەتوانرا ئەم فایلە دەنگییە هاوردە بکرێت."
                }
                adhanMessage = message
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }
    val visibleCities = remember(cities, citySearch) {
        if (citySearch.isBlank()) cities
        else cities.filter { city ->
            city.name.contains(citySearch.trim(), ignoreCase = true) ||
                city.country.contains(citySearch.trim(), ignoreCase = true)
        }
    }

    suspend fun loadSelectedCityTimes(targetDate: Date): PrayerRow? {
        val method = PrayerCalculationMethod.MWL
        val school = AsrCalculationSchool.SHAFI
        val lat = gpsLatitude
        val lon = gpsLongitude
        if (lat != null && lon != null) {
            return ContentRepository.loadPrayerTimesAtLocation(context, lat, lon, targetDate, method, school)
        }
        val selectedName = selectedCity.trim()
        if (manualLatitude != null && manualLongitude != null) {
            val cityTable = if (selectedName.isBlank()) null
                else ContentRepository.loadPrayerTimes(context, selectedName, targetDate, manualCountry)
            return cityTable ?: ContentRepository.loadPrayerTimesAtLocation(
                context, manualLatitude!!, manualLongitude!!, targetDate, method, school
            )
        }
        if (selectedName.isBlank()) return null
        ContentRepository.loadPrayerTimes(context, selectedName, targetDate, manualCountry)?.let { return it }
        val city = ContentRepository.findCityByName(context, selectedName, manualCountry) ?: return null
        return PrayerTimeCalculator.calculate(targetDate, city.latitude, city.longitude, method, school)
    }

    val onFix: (Location) -> Unit = { fix ->
        locating = false
        if (fix.hasAccuracy() && fix.accuracy > 1500f) {
            // Do not replace a verified city/fix with a very coarse network estimate.
            locationMessage = when (language) {
                AppLanguage.EN -> "This location estimate is too broad (±${fix.accuracy.toInt()} m). Enable precise GPS or choose your city."
                AppLanguage.AR -> "تقدير الموقع واسع جدًا (±${fix.accuracy.toInt()} متر). فعّل GPS الدقيق أو اختر مدينتك."
                AppLanguage.CKB -> "ئەم هەڵسەنگاندنی شوێنە زۆر گشتییە (±${fix.accuracy.toInt()} مەتر)؛ GPSی ورد چالاک بکە یان شارەکەت هەڵبژێرە."
            }
        } else {
            // A usable new fix supersedes an older one so moving users do not stay
            // stuck at stale coordinates.
            gpsLatitude = fix.latitude
            gpsLongitude = fix.longitude
            gpsAccuracy = if (fix.hasAccuracy()) fix.accuracy else null
            prefs.edit()
                .putString("gps_latitude", fix.latitude.toString())
                .putString("gps_longitude", fix.longitude.toString())
                .putString("gps_accuracy", if (fix.hasAccuracy()) fix.accuracy.toString() else "")
                .apply()
            locationMessage = if (fix.hasAccuracy()) {
                val acc = fix.accuracy.toInt().toString()
                when (language) {
                    AppLanguage.EN -> "Fresh GPS/network fix; reported accuracy about ±$acc m"
                    AppLanguage.AR -> "تم تحديث الموقع الحالي؛ الدقة التقريبية ±$acc متر"
                    AppLanguage.CKB -> "شوێنی ئێستا نوێکرایەوە؛ وردبینی نزیکەی ±$acc مەتر"
                }
            } else {
                when (language) {
                    AppLanguage.EN -> "Fresh coordinates received; Android did not report accuracy."
                    AppLanguage.AR -> "تم تحديث الإحداثيات، لكن الهاتف لم يذكر دقة القياس."
                    AppLanguage.CKB -> "کۆئۆردیناتی نوێ وەرگیرا، بەڵام وردبینی نەدراوە."
                }
            }
        }
    }
    val onLocationError: (String) -> Unit = { error ->
        locating = false
        locationMessage = when (error) {
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
            "LOCATION_ACCURACY_TOO_LOW" -> when (language) {
                AppLanguage.EN -> "Location accuracy is too low; choose a city manually for reliable times."
                AppLanguage.AR -> "دقة الموقع منخفضة جدًا؛ اختر المدينة يدويًا للحصول على مواقيت موثوقة."
                AppLanguage.CKB -> "وردی شوێن زۆر کەمە؛ بۆ کاتی دروست شارەکەت بە دەستی هەڵبژێرە."
            }
            "NO_FRESH_LOCATION" -> when (language) {
                AppLanguage.EN -> "No fresh coordinates were received. Choose your city manually."
                AppLanguage.AR -> "لم تصل إحداثيات حديثة. اختر المدينة يدويًا."
                AppLanguage.CKB -> "کۆئۆردیناتی نوێ نەگەیشت؛ شارەکەت بە دەستی هەڵبژێرە."
            }
            else -> error
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
            locating = true
            locationMessage = ""
            requestCurrentLocation(context, onFix, onLocationError)
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    LaunchedEffect(Unit) {
        if (savedGpsIsTooCoarse) {
            prefs.edit()
                .remove("gps_latitude").remove("gps_longitude").remove("gps_accuracy")
                .putBoolean("prayer_location_prompted", false)
                .apply()
        }
        val alreadyAsked = prefs.getBoolean("prayer_location_prompted", false)
        if ((!alreadyAsked || savedGpsIsTooCoarse) && gpsLatitude == null && gpsLongitude == null) {
            prefs.edit().putBoolean("prayer_location_prompted", true).apply()
            locateNow()
        }
    }

    LaunchedEffect(citySearch) {
        searchingCities = true
        delay(250L)
        cities = ContentRepository.searchCities(context, citySearch.trim(), limit = 220)
        searchingCities = false
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
    LaunchedEffect(Unit) {
        // Keep the full original audio inventory available; file-name filtering
        // must never hide an audio asset from the user's explicit selection.
        adhanAssets = ContentRepository.loadAudioItems(context) + ContentRepository.loadCustomAdhanItems(context)
        val preferredKeywords = listOf(
            "adhan", "athan", "azan", "muezzin", "takbir", "takbeer", "madina", "madinah", "mecca",
            "makkah", "fajr", "hijaz", "abdulbasit", "abdul_basit", "abdulmajid",
            "mshari", "peshawa", "ramazan", "mansur"
        )
        if (adhanAssets.none { it.assetPath == selectedAdhanAsset }) {
            val recommended = adhanAssets.firstOrNull { sound ->
                val label = (sound.title + " " + sound.assetPath + " " + sound.group).lowercase(Locale.ROOT)
                preferredKeywords.any(label::contains)
            } ?: adhanAssets.firstOrNull()
            selectedAdhanAsset = recommended?.assetPath.orEmpty()
            prefs.edit().putString("adhan_audio_asset", selectedAdhanAsset).apply()
        }
    }
    LaunchedEffect(adhanEnabled, selectedAdhanAsset, adhanDays, gpsLatitude, gpsLongitude, manualLatitude, manualLongitude, manualCountry, selectedCity, calculationMethod, asrSchool) {
        prefs.edit().putStringSet(PrayerAlarmScheduler.KEY_DAYS, adhanDays).apply()
        if (adhanEnabled) {
            prefs.edit().putString("adhan_audio_asset", selectedAdhanAsset).apply()
            PrayerAlarmScheduler.scheduleUpcomingFromPreferences(context)
        } else {
            PrayerAlarmScheduler.cancelAll(context)
        }
    }
    LaunchedEffect(selectedCity, gpsLatitude, gpsLongitude, manualLatitude, manualLongitude, calculationMethod, asrSchool) {
        PrayerTimesWidgetProvider.requestRefresh(context)
    }
    LaunchedEffect(selectedCity, gpsLatitude, gpsLongitude, manualLatitude, manualLongitude, calculationMethod, asrSchool, currentLocalDateKey) {
        loading = true
        times = runCatching { loadSelectedCityTimes(Date()) }.getOrNull()
        loading = false
    }
    LaunchedEffect(
        selectedCity, gpsLatitude, gpsLongitude, manualLatitude, manualLongitude,
        manualCountry, calculationMethod, asrSchool, currentLocalDateKey
    ) {
        val tomorrow = Calendar.getInstance().apply {
            timeInMillis = clockNowMillis
            add(Calendar.DAY_OF_YEAR, 1)
        }.time
        tomorrowTimes = runCatching { loadSelectedCityTimes(tomorrow) }.getOrNull()
    }


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
        Text(tr(language, "prayer"), color = foreground, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Card(
            colors = CardDefaults.cardColors(containerColor = cardColor),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(Gold.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Schedule, contentDescription = null, tint = Gold)
                }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        when (language) {
                            AppLanguage.EN -> "Today's prayer times"
                            AppLanguage.AR -> "مواقيت الصلاة اليوم"
                            AppLanguage.CKB -> "کاتەکانی نوێژی ئەمڕۆ"
                        }, color = foreground, fontSize = 14.sp, fontWeight = FontWeight.Bold
                    )
                    Text(
                        SimpleDateFormat("EEEE, d MMMM yyyy", when (language) {
                            AppLanguage.EN -> Locale.ENGLISH
                            AppLanguage.AR -> Locale("ar")
                            AppLanguage.CKB -> Locale("ckb")
                        }).format(Date()),
                        color = foreground.copy(alpha = 0.58f), fontSize = 11.sp
                    )
                }
                Text(
                    when (language) { AppLanguage.EN -> "TODAY"; AppLanguage.AR -> "اليوم"; AppLanguage.CKB -> "ئەمڕۆ" },
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
                        Text(
                            if (gpsLatitude != null) {
                                when (language) {
                                    AppLanguage.EN -> "Current GPS location"
                                    AppLanguage.AR -> "موقع GPS الحالي"
                                    AppLanguage.CKB -> "شوێنی GPSی ئێستا"
                                }
                            } else selectedCity,
                            color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold
                        )
                        if (gpsLatitude != null && gpsLongitude != null) {
                            Text(String.format(Locale.US, "%.5f, %.5f", gpsLatitude, gpsLongitude), color = Color(0xFFB9CDC5), fontSize = 11.sp)
                        } else Text(tr(language, "select_city"), color = Color(0xFFB9CDC5), fontSize = 10.sp)
                    }
                    IconButton(onClick = locateNow) { Icon(Icons.Outlined.GpsFixed, null, tint = Gold) }
                    IconButton(onClick = { citySearch = ""; chooseCity = true }) { Icon(Icons.Outlined.Search, null, tint = Gold) }
                }
                Spacer(Modifier.height(7.dp))
                Text(
                    if (gpsLatitude != null) {
                        when (language) {
                            AppLanguage.EN -> "Nearby original city timetables are used when available; otherwise times are calculated from GPS coordinates."
                            AppLanguage.AR -> "يُستخدم جدول المدينة الأصلية القريبة عند توفره، وإلا تُحسب المواقيت من إحداثيات GPS."
                            AppLanguage.CKB -> "خشتەی شاری نزیک بەکاردێت ئەگەر هەبێت؛ ئەگەرنا کاتەکان بە کۆئۆردیناتی GPS حساب دەکرێن."
                        }
                    } else {
                        when (language) {
                            AppLanguage.EN -> "Tap the location icon to get a new GPS/network fix, or choose a city."
                            AppLanguage.AR -> "اضغط رمز الموقع للحصول على إحداثيات GPS حديثة، أو اختر مدينة."
                            AppLanguage.CKB -> "نیشانەی شوێن دابگرە بۆ وەرگرتنی کۆئۆردیناتی نوێ، یان شارێک هەڵبژێرە."
                        }
                    }, color = Color(0xFFB9CDC5), fontSize = 11.sp, lineHeight = 17.sp
                )
                if (times != null) {
                    Spacer(Modifier.height(7.dp))
                    Text(
                        when (language) {
                            AppLanguage.EN -> "Source: ${times!!.source}"
                            AppLanguage.AR -> "المصدر: ${times!!.source}"
                            AppLanguage.CKB -> "سەرچاوە: ${times!!.source}"
                        },
                        color = Color(0xFFF4E5B4),
                        fontSize = 10.sp,
                        lineHeight = 15.sp
                    )
                }
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
            Text(locationMessage, color = foreground.copy(alpha = 0.65f), fontSize = 11.sp)
        }
        Spacer(Modifier.height(14.dp))
        if (times == null && loading) {
            LoadingOrEmpty(true, language, foreground)
        } else if (times == null && gpsLatitude == null && manualLatitude == null && selectedCity.isBlank()) {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(0.8.dp, Gold.copy(alpha = 0.28f))
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Icon(Icons.Outlined.GpsFixed, contentDescription = null, tint = Gold, modifier = Modifier.size(30.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        when (language) {
                            AppLanguage.EN -> "Choose your location first"
                            AppLanguage.AR -> "حدّد موقعك أولًا"
                            AppLanguage.CKB -> "سەرەتا شوێنەکەت دیاری بکە"
                        }, color = foreground, fontSize = 15.sp, fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        when (language) {
                            AppLanguage.EN -> "No city is assumed. Allow location access or choose your city so the app does not show another city's prayer times."
                            AppLanguage.AR -> "لن نفترض مدينة تلقائيًا. اسمح بالموقع أو اختر مدينتك حتى لا تظهر مواقيت مدينة أخرى."
                            AppLanguage.CKB -> "هیچ شارێک بە خۆکارانە دانانرێت؛ مۆڵەتی شوێن بدە یان شارەکەت هەڵبژێرە بۆ ئەوەی کاتی شاری تر پیشان نەدرێت."
                        }, color = foreground.copy(alpha = 0.62f), fontSize = 12.sp, lineHeight = 19.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = locateNow, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Outlined.GpsFixed, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(5.dp))
                            Text(if (language == AppLanguage.EN) "Use GPS" else if (language == AppLanguage.AR) "استخدم GPS" else "GPS بەکاربهێنە", fontSize = 11.sp)
                        }
                        OutlinedButton(onClick = { citySearch = ""; chooseCity = true }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Outlined.Search, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(5.dp))
                            Text(if (language == AppLanguage.EN) "Choose city" else if (language == AppLanguage.AR) "اختر المدينة" else "شار هەڵبژێرە", fontSize = 11.sp)
                        }
                    }
                }
            }
        } else if (times == null) {
            LoadingOrEmpty(false, language, foreground, tr(language, "not_found"))
        } else {
            val timeItems = listOf(
                tr(language, "fajr") to formatPrayerTime(times!!.fajr, language, use24Hour),
                tr(language, "sunrise") to formatPrayerTime(times!!.sunrise, language, use24Hour),
                tr(language, "dhuhr") to formatPrayerTime(times!!.dhuhr, language, use24Hour),
                tr(language, "asr") to formatPrayerTime(times!!.asr, language, use24Hour),
                tr(language, "maghrib") to formatPrayerTime(times!!.maghrib, language, use24Hour),
                tr(language, "isha") to formatPrayerTime(times!!.isha, language, use24Hour)
            )
            timeItems.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    pair.forEach { (label, time) ->
                        Card(Modifier.weight(1f).padding(bottom = 10.dp), colors = CardDefaults.cardColors(containerColor = cardColor), shape = RoundedCornerShape(18.dp)) {
                            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                                Text(label, color = foreground.copy(alpha = 0.64f), fontSize = 12.sp)
                                Spacer(Modifier.height(7.dp))
                                Text(time, color = Gold, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            Text(
                when (language) {
                    AppLanguage.EN -> "Times shown in local phone time."
                    AppLanguage.AR -> "الأوقات بتوقيت الهاتف المحلي."
                    AppLanguage.CKB -> "کاتەکان بە کاتی مۆبایلەکەتن."
                }, color = foreground.copy(alpha = 0.48f), fontSize = 10.sp, lineHeight = 15.sp
            )
        }

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
                        Text(
                            tr(language, nextPrayerTarget.prayerKey),
                            color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(formatPrayerTime(nextPrayerTarget.timeText, language, use24Hour), color = Color(0xFFF1D78D), fontSize = 14.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            formatPrayerCountdown(nextPrayerTarget.targetMillis - clockNowMillis),
                            color = Gold, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold
                        )
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
                                AppLanguage.EN -> "Play a bundled adhan sound at prayer time"
                                AppLanguage.AR -> "تشغيل صوت الأذان المرفق عند دخول وقت الصلاة"
                                AppLanguage.CKB -> "لێدانی دەنگی بانگی ناوخۆیی لە کاتی نوێژ"
                            }, color = foreground.copy(alpha = 0.6f), fontSize = 11.sp, lineHeight = 17.sp
                        )
                    }
                    Switch(checked = adhanEnabled, onCheckedChange = { enable ->
                        if (!enable) {
                            adhanEnabled = false
                            prefs.edit().putBoolean("adhan_enabled", false).apply()
                            PrayerAlarmScheduler.cancelAll(context)
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
                if (adhanEnabled) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = { adhanSoundQuery = ""; chooseAdhanSound = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.VolumeUp, null, tint = Gold, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(8.dp))
                        val soundLabel = adhanAssets.firstOrNull { it.assetPath == selectedAdhanAsset }?.title
                            ?: selectedAdhanAsset.substringAfterLast('/')
                        Text(
                            when (language) {
                                AppLanguage.EN -> "Adhan sound: $soundLabel"
                                AppLanguage.AR -> "صوت الأذان: $soundLabel"
                                AppLanguage.CKB -> "دەنگی بانگ: $soundLabel"
                            }, fontSize = 11.sp, maxLines = 1
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        when (language) {
                            AppLanguage.EN -> "Repeat on selected weekdays"
                            AppLanguage.AR -> "تكرار التنبيه في أيام الأسبوع المحددة"
                            AppLanguage.CKB -> "دووبارەکردنەوە لە ڕۆژە دیاریکراوەکانی هەفتە"
                        }, color = foreground, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    val weekdayOptions = listOf(
                        Calendar.MONDAY to when (language) { AppLanguage.EN -> "Mon"; AppLanguage.AR -> "إثن"; AppLanguage.CKB -> "دووش" },
                        Calendar.TUESDAY to when (language) { AppLanguage.EN -> "Tue"; AppLanguage.AR -> "ثلا"; AppLanguage.CKB -> "سێ" },
                        Calendar.WEDNESDAY to when (language) { AppLanguage.EN -> "Wed"; AppLanguage.AR -> "أرب"; AppLanguage.CKB -> "چوار" },
                        Calendar.THURSDAY to when (language) { AppLanguage.EN -> "Thu"; AppLanguage.AR -> "خمي"; AppLanguage.CKB -> "پێنج" },
                        Calendar.FRIDAY to when (language) { AppLanguage.EN -> "Fri"; AppLanguage.AR -> "جمع"; AppLanguage.CKB -> "هەین" },
                        Calendar.SATURDAY to when (language) { AppLanguage.EN -> "Sat"; AppLanguage.AR -> "سبت"; AppLanguage.CKB -> "شەم" },
                        Calendar.SUNDAY to when (language) { AppLanguage.EN -> "Sun"; AppLanguage.AR -> "أحد"; AppLanguage.CKB -> "یەک" }
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(weekdayOptions, key = { it.first }) { (weekday, label) ->
                            val dayKey = weekday.toString()
                            FilterChip(
                                selected = adhanDays.contains(dayKey),
                                onClick = {
                                    val next = if (adhanDays.contains(dayKey)) adhanDays - dayKey else adhanDays + dayKey
                                    adhanDays = next
                                    prefs.edit().putStringSet(PrayerAlarmScheduler.KEY_DAYS, next).apply()
                                },
                                label = { Text(label, fontSize = 10.sp) }
                            )
                        }
                    }
                    if (adhanDays.isEmpty()) {
                        Text(
                            when (language) {
                                AppLanguage.EN -> "No weekdays selected; no prayer alerts will be scheduled."
                                AppLanguage.AR -> "لم تُحدَّد أيام؛ لن يتم جدولة تنبيهات الصلاة."
                                AppLanguage.CKB -> "هیچ ڕۆژێک دیاری نەکراوە؛ ئاگادارکردنەوەی نوێژ دانانرێت."
                            }, color = Gold, fontSize = 10.sp, lineHeight = 15.sp
                        )
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
        Card(Modifier.fillMaxWidth().clickable { onQibla() }, colors = CardDefaults.cardColors(containerColor = cardColor), shape = RoundedCornerShape(20.dp)) {
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
        title = {
            Text(
                when (language) {
                    AppLanguage.EN -> "Choose your city"
                    AppLanguage.AR -> "اختر مدينتك"
                    AppLanguage.CKB -> "شارەکەت هەڵبژێرە"
                }, color = foreground, fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = citySearch,
                    onValueChange = { citySearch = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    label = {
                        Text(
                            when (language) {
                                AppLanguage.EN -> "City or country"
                                AppLanguage.AR -> "المدينة أو الدولة"
                                AppLanguage.CKB -> "شار یان وڵات"
                            }
                        )
                    }
                )
                Spacer(Modifier.height(9.dp))
                when {
                    searchingCities -> Box(
                        Modifier.fillMaxWidth().height(110.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Gold)
                    }
                    visibleCities.isEmpty() -> Box(
                        Modifier.fillMaxWidth().height(95.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            when (language) {
                                AppLanguage.EN -> "No matching city. Try another spelling."
                                AppLanguage.AR -> "لم تُوجد مدينة مطابقة. جرّب كتابة أخرى."
                                AppLanguage.CKB -> "هیچ شارێکی هاوتا نەدۆزرایەوە؛ شێوازێکی تر بنووسە."
                            }, color = foreground.copy(alpha = 0.65f), fontSize = 12.sp
                        )
                    }
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 370.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(
                            visibleCities,
                            key = { city -> city.name + "|" + city.country + "|" + city.latitude.toString() + "|" + city.longitude.toString() }
                        ) { city ->
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    // A manual city choice is authoritative until the user
                                    // explicitly requests a fresh GPS fix.
                                    gpsLatitude = null
                                    gpsLongitude = null
                                    gpsAccuracy = null
                                    manualLatitude = city.latitude
                                    manualLongitude = city.longitude
                                    manualCountry = city.country
                                    locationMessage = ""
                                    locating = false
                                    prefs.edit()
                                        .remove("gps_latitude")
                                        .remove("gps_longitude")
                                        .remove("gps_accuracy")
                                        .putString("manual_city_latitude", city.latitude.toString())
                                        .putString("manual_city_longitude", city.longitude.toString())
                                        .putString("selected_city_country", city.country)
                                        .putString("selected_city", city.name)
                                        .putBoolean("selected_city_explicit", true)
                                        .putBoolean("prayer_location_prompted", true)
                                        .apply()
                                    onCity(city.name)
                                    chooseCity = false
                                    citySearch = ""
                                },
                                colors = CardDefaults.cardColors(containerColor = cardColor),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(0.6.dp, Gold.copy(alpha = 0.18f))
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Outlined.GpsFixed, null, tint = Gold, modifier = Modifier.size(21.dp))
                                    Spacer(Modifier.width(9.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(city.name, color = foreground, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        if (city.country.isNotBlank()) {
                                            Text(city.country, color = foreground.copy(alpha = 0.57f), fontSize = 10.sp)
                                        }
                                    }
                                    Icon(Icons.Outlined.ChevronRight, null, tint = Gold)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { chooseCity = false; citySearch = "" }) {
                Text(tr(language, "back"))
            }
        }
    )

    if (chooseAdhanSound) AlertDialog(
        onDismissRequest = {
            chooseAdhanSound = false
            adhanSoundQuery = ""
            previewingAdhan = ""
            AudioController.stop()
        },
        title = {
            Text(
                when (language) {
                    AppLanguage.EN -> "Choose adhan sound"
                    AppLanguage.AR -> "اختر صوت الأذان"
                    AppLanguage.CKB -> "دەنگی بانگ هەڵبژێرە"
                }, color = foreground, fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = adhanSoundQuery,
                    onValueChange = { adhanSoundQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    label = {
                        Text(
                            when (language) {
                                AppLanguage.EN -> "Search adhan / mosque audio"
                                AppLanguage.AR -> "ابحث في الأذان وأصوات المساجد"
                                AppLanguage.CKB -> "لە دەنگی بانگ و مزگەوت بگەڕێ"
                            }
                        )
                    }
                )
                Spacer(Modifier.height(8.dp))
                if (visibleAdhanAssets.isEmpty()) {
                    Text(
                        when (language) {
                            AppLanguage.EN -> "No bundled sound matched this search. You can import an audio file from your phone."
                            AppLanguage.AR -> "لا يوجد ملف صوتي مطابق؛ يمكنك استيراد ملف من هاتفك."
                            AppLanguage.CKB -> "هیچ دەنگێک نەدۆزرایەوە؛ دەتوانیت فایلێکی دەنگی لە مۆبایلەوە هاوردە بکەیت."
                        }, color = foreground.copy(alpha = 0.65f), fontSize = 12.sp, lineHeight = 18.sp
                    )
                } else LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 340.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    items(visibleAdhanAssets, key = { it.assetPath }) { sound ->
                        val isSelected = sound.assetPath == selectedAdhanAsset
                        Row(
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(13.dp))
                                .background(if (isSelected) Gold.copy(alpha = 0.11f) else Color.Transparent)
                                .clickable {
                                    selectedAdhanAsset = sound.assetPath
                                    prefs.edit().putString("adhan_audio_asset", sound.assetPath).apply()
                                }
                                .padding(horizontal = 7.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    selectedAdhanAsset = sound.assetPath
                                    prefs.edit().putString("adhan_audio_asset", sound.assetPath).apply()
                                }
                            )
                            Column(Modifier.weight(1f)) {
                                Text(sound.title, color = foreground, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, maxLines = 2)
                                Text(sound.group, color = foreground.copy(alpha = 0.52f), fontSize = 9.sp, maxLines = 1)
                            }
                            IconButton(
                                onClick = {
                                    if (previewingAdhan == sound.assetPath) {
                                        AudioController.stop()
                                        previewingAdhan = ""
                                    } else {
                                        previewingAdhan = sound.assetPath
                                        AudioController.play(context, sound.assetPath) { playingNow ->
                                            previewingAdhan = if (playingNow) sound.assetPath else ""
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    if (previewingAdhan == sound.assetPath) Icons.Outlined.PauseCircle else Icons.Outlined.PlayCircle,
                                    contentDescription = if (language == AppLanguage.EN) "Preview " + sound.title else if (language == AppLanguage.AR) "معاينة " + sound.title else "گوێگرتن " + sound.title,
                                    tint = Gold
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(9.dp))
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        AudioController.stop()
                        previewingAdhan = ""
                        chooseAdhanSound = false
                        adhanSoundQuery = ""
                        customAdhanPicker.launch(arrayOf("audio/*"))
                    }
                ) {
                    Icon(Icons.Outlined.VolumeUp, null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(
                        when (language) {
                            AppLanguage.EN -> "Import an adhan audio file from phone"
                            AppLanguage.AR -> "استيراد ملف أذان من الهاتف"
                            AppLanguage.CKB -> "هاوردەکردنی فایلی بانگ لە مۆبایل"
                        }, fontSize = 11.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                AudioController.stop()
                previewingAdhan = ""
                chooseAdhanSound = false
                adhanSoundQuery = ""
                prefs.edit().putString("adhan_audio_asset", selectedAdhanAsset).apply()
            }) {
                Text(
                    when (language) {
                        AppLanguage.EN -> "Use selected sound"
                        AppLanguage.AR -> "استخدام الصوت المختار"
                        AppLanguage.CKB -> "دەنگی هەڵبژێردراو بەکاربهێنە"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = {
                AudioController.stop()
                previewingAdhan = ""
                chooseAdhanSound = false
                adhanSoundQuery = ""
            }) { Text(tr(language, "back")) }
        }
    )
}

@Composable
fun QiblaScreen(language: AppLanguage, foreground: Color, cardColor: Color, selectedCity: String) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val prefs = remember { context.getSharedPreferences("mashxall_preferences", Context.MODE_PRIVATE) }
    var city by remember { mutableStateOf<City?>(null) }
    var azimuth by remember { mutableFloatStateOf(0f) }
    var sensorAvailable by remember { mutableStateOf(false) }
    var alignedWasTrue by remember { mutableStateOf(false) }
    val gpsLatitude = prefs.getString("gps_latitude", null)?.toDoubleOrNull()
    val gpsLongitude = prefs.getString("gps_longitude", null)?.toDoubleOrNull()
    val manualLatitude = prefs.getString("manual_city_latitude", null)?.toDoubleOrNull()
    val manualLongitude = prefs.getString("manual_city_longitude", null)?.toDoubleOrNull()
    val manualCountry = prefs.getString("selected_city_country", "") ?: ""

    // Qibla shares the verified location already acquired on Home/Prayer.
    // It deliberately has no second "locate me" button or duplicate permission flow.
    LaunchedEffect(gpsLatitude, gpsLongitude, manualLatitude, manualLongitude, manualCountry, selectedCity) {
        city = when {
            gpsLatitude != null && gpsLongitude != null -> City("GPS", gpsLatitude, gpsLongitude)
            manualLatitude != null && manualLongitude != null -> City(selectedCity, manualLatitude, manualLongitude, manualCountry)
            else -> ContentRepository.findCityByName(context, selectedCity, manualCountry)
        }
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

    val bearing = city?.let { qiblaBearing(it.latitude, it.longitude).toFloat() } ?: 0f
    val magneticDeclination = remember(city) {
        city?.let { GeomagneticField(it.latitude.toFloat(), it.longitude.toFloat(), 0f, System.currentTimeMillis()).declination } ?: 0f
    }
    val trueHeading = (azimuth + magneticDeclination + 360f) % 360f
    val relativeHeading = ((bearing - trueHeading + 540f) % 360f) - 180f
    val animatedHeading by androidx.compose.animation.core.animateFloatAsState(
        targetValue = relativeHeading,
        animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessLow),
        label = "qiblaHeading"
    )
    val isAligned = sensorAvailable && kotlin.math.abs(relativeHeading) <= 3.5f
    LaunchedEffect(isAligned) {
        if (isAligned && !alignedWasTrue) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        alignedWasTrue = isAligned
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(tr(language, "qibla"), color = foreground, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(5.dp))
        Text(
            when (language) {
                AppLanguage.EN -> "True-north corrected compass"
                AppLanguage.AR -> "بوصلة مصححة إلى الشمال الحقيقي"
                AppLanguage.CKB -> "کۆمپاس بە ڕاستکردنەوەی باکووری ڕاستەقینە"
            }, color = foreground.copy(alpha = 0.6f), fontSize = 11.sp
        )
        Spacer(Modifier.height(24.dp))
        Box(
            Modifier.size(286.dp).clip(CircleShape)
                .background(androidx.compose.ui.graphics.Brush.radialGradient(listOf(Color(0xFF185444), DeepGreen, Color(0xFF071E1A))))
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.fillMaxSize().clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                    val stroke = size.minDimension * 0.018f
                    drawCircle(color = Gold.copy(alpha = 0.9f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke))
                    drawCircle(color = Gold.copy(alpha = 0.16f), radius = size.minDimension * 0.39f, style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke * 0.7f))
                    for (index in 0 until 60) {
                        val angle = Math.toRadians(index * 6.0)
                        val outer = size.minDimension * 0.45f
                        val inner = if (index % 5 == 0) size.minDimension * 0.37f else size.minDimension * 0.415f
                        val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
                        val startPoint = androidx.compose.ui.geometry.Offset(
                            center.x + kotlin.math.sin(angle).toFloat() * inner,
                            center.y - kotlin.math.cos(angle).toFloat() * inner
                        )
                        val endPoint = androidx.compose.ui.geometry.Offset(
                            center.x + kotlin.math.sin(angle).toFloat() * outer,
                            center.y - kotlin.math.cos(angle).toFloat() * outer
                        )
                        drawLine(
                            color = if (index % 5 == 0) Gold else Gold.copy(alpha = 0.34f),
                            start = startPoint,
                            end = endPoint,
                            strokeWidth = if (index % 5 == 0) stroke * 0.75f else stroke * 0.35f
                        )
                    }
                }
                Text("N", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.TopCenter).padding(top = 22.dp))
                Text("E", color = Color.White.copy(alpha = 0.72f), fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 22.dp))
                Text("S", color = Color.White.copy(alpha = 0.72f), fontSize = 12.sp, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 22.dp))
                Text("W", color = Color.White.copy(alpha = 0.72f), fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterStart).padding(start = 22.dp))
                Icon(
                    Icons.Outlined.Explore, contentDescription = null,
                    tint = if (isAligned) Color(0xFF8CE3AF) else Gold,
                    modifier = Modifier.size(178.dp).rotate(animatedHeading)
                )
                Box(
                    Modifier.size(46.dp).clip(CircleShape).background(Color(0xFF091D18)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🕋", fontSize = 24.sp)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            if (gpsLatitude != null && gpsLongitude != null) {
                when (language) { AppLanguage.EN -> "Your current location"; AppLanguage.AR -> "موقعك الحالي"; AppLanguage.CKB -> "شوێنی ئێستات" }
            } else city?.name ?: selectedCity,
            color = Gold, fontSize = 15.sp, fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            when {
                isAligned -> when (language) {
                    AppLanguage.EN -> "You are facing the Qibla"
                    AppLanguage.AR -> "أنت الآن باتجاه القبلة"
                    AppLanguage.CKB -> "ئێستا ڕووت لە قیبلەیە"
                }
                !sensorAvailable -> when (language) {
                    AppLanguage.EN -> "Hold the phone flat and move it slowly to calibrate the compass."
                    AppLanguage.AR -> "أمسك الهاتف أفقيًا وحرّكه ببطء لمعايرة البوصلة."
                    AppLanguage.CKB -> "مۆبایلەکە ئاسۆیی بگرە و بە هێواشی بجوڵێنە بۆ ڕێکخستنی کۆمپاس."
                }
                else -> when (language) {
                    AppLanguage.EN -> "Rotate the phone until the arrow points to the alignment mark."
                    AppLanguage.AR -> "أدر الهاتف حتى يتجه السهم إلى علامة الاصطفاف."
                    AppLanguage.CKB -> "مۆبایلەکە بسووڕێنە تا تیرەکە لەگەڵ نیشانەی ڕێککەوتن بگونجێت."
                }
            },
            color = if (isAligned) Color(0xFF8CE3AF) else foreground.copy(alpha = 0.68f),
            fontSize = 12.sp, lineHeight = 19.sp, textAlign = TextAlign.Center,
            fontWeight = if (isAligned) FontWeight.Bold else FontWeight.Normal
        )
        Spacer(Modifier.height(11.dp))
        Text(
            String.format(Locale.US, "%.0f°", bearing) + " • " + tr(language, "degrees"),
            color = foreground, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold
        )
        if (sensorAvailable) {
            Spacer(Modifier.height(7.dp))
            Text(
                String.format(Locale.US, "%+.1f°", relativeHeading),
                color = if (isAligned) Color(0xFF8CE3AF) else foreground.copy(alpha = 0.57f),
                fontSize = 12.sp
            )
        }
        if (city == null) {
            Spacer(Modifier.height(12.dp))
            Text(
                when (language) {
                    AppLanguage.EN -> "No saved coordinates or selected city are available yet. Open Prayer from More and allow location or choose a city once."
                    AppLanguage.AR -> "لا توجد إحداثيات محفوظة أو مدينة مختارة. افتح الصلاة من المزيد واسمح بالموقع أو اختر المدينة مرة واحدة."
                    AppLanguage.CKB -> "هێشتا کۆئۆردینات یان شاری هەڵبژێردراو نییە؛ لە «زیاتر» نوێژ بکەرەوە و جارێک شوێن یان شار دیاری بکە."
                }, color = foreground.copy(alpha = 0.55f), fontSize = 10.sp, lineHeight = 16.sp, textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(18.dp))
    }
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
            Text(tr(language, "word_by_word"), color = foreground, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(tr(language, "word_by_word_sub"), color = foreground.copy(alpha = 0.6f), fontSize = 12.sp)
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
            items(items, key = { it.position }) { word ->
                Card(colors = CardDefaults.cardColors(containerColor = cardColor), shape = RoundedCornerShape(17.dp)) {
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
                                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable {
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
            Text(tr(language, "farmuda"), color = foreground, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                when (language) {
                    AppLanguage.EN -> "${records.size} source records • ${favorites.size} favourites"
                    AppLanguage.AR -> "${records.size} سجلًا من المصدر • ${favorites.size} مفضلة"
                    AppLanguage.CKB -> "${records.size} تۆماری سەرچاوە • ${favorites.size} دڵخواز"
                }, color = foreground.copy(alpha = 0.54f), fontSize = 11.sp
            )
            Spacer(Modifier.height(10.dp))
            SearchBox(query, { query = it }, tr(language, "search"))
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                FilterChip(
                    selected = !favoritesOnly,
                    onClick = { favoritesOnly = false },
                    label = { Text(if (language == AppLanguage.EN) "All records" else if (language == AppLanguage.AR) "كل السجلات" else "هەموو تۆمارەکان", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = favoritesOnly,
                    onClick = { favoritesOnly = true },
                    label = { Text(if (language == AppLanguage.EN) "Favourites" else if (language == AppLanguage.AR) "المفضلة" else "دڵخوازەکان", fontSize = 11.sp) }
                )
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
            items(visibleRecords, key = { it.id }) { item ->
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
                    modifier = Modifier.fillMaxWidth().clickable { selectedRecord = item }
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
                            IconButton(onClick = {
                                val key = item.id.toString()
                                favorites = if (isFavorite) favorites - key else favorites + key
                                favoritesPrefs.edit().putStringSet("farmuda_ids", favorites).apply()
                            }) {
                                Text(if (isFavorite) "★" else "☆", color = Gold, fontSize = 22.sp)
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
                        Text(if (favorites.contains(item.id.toString())) "★" else "☆", color = Gold)
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
