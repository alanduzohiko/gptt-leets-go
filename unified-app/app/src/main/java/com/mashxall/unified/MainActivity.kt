package com.mashxall.unified

import android.Manifest
import android.widget.Toast
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mashxall.unified.data.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val Ink = Color(0xFF071E1A)
private val DeepGreen = Color(0xFF103D32)
private val Emerald = Color(0xFF19725A)
private val PaleGold = Color(0xFFF4E5B4)
private val SoftText = Color(0xFFB9CDC5)
private val Panel = Color(0xFF102B25)

/**
 * Native press response for large navigation cards: slight spring scale, the
 * standard Material ripple, and a light haptic acknowledgement on activation.
 */
private fun Modifier.premiumPress(onClick: () -> Unit): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.975f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "mashxallCardPressScale"
    )
    val haptic = LocalHapticFeedback.current
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }.clickable(
        interactionSource = interactionSource,
        indication = LocalIndication.current,
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        }
    )
}

internal object MashxallAppearance {
    var accent by mutableStateOf(Color(0xFFC9A44C))

    fun colorFor(key: String): Color = when (key) {
        "emerald" -> Color(0xFF38B58A)
        "sapphire" -> Color(0xFF6A9FE8)
        "violet" -> Color(0xFFAD92DD)
        "rose" -> Color(0xFFE28EAB)
        "turquoise" -> Color(0xFF4CB9C8)
        else -> Color(0xFFC9A44C)
    }
}

private val Gold: Color get() = MashxallAppearance.accent

enum class AppLanguage(val key: String, val label: String) {
    CKB("ckb", "کوردی"), AR("ar", "العربية"), EN("en", "English");
    companion object {
        fun from(value: String?) = entries.firstOrNull { it.key == value } ?: CKB
    }
}

internal fun homeBackExitMessage(language: AppLanguage): String = when (language) {
    AppLanguage.EN -> "Press Back again to exit"
    AppLanguage.AR -> "اضغط رجوع مرة أخرى للخروج"
    AppLanguage.CKB -> "بۆ دەرچوون جارێکی تر دوگمەی گەڕانەوە دابگرە"
}

enum class AppScreen(val key: String) {
    HOME("home"), QURAN("quran"), WORSHIP("worship"), PRAYER("prayer"), MORE("more"),
    TAFSIR("tafsir"), HADITH("hadith"), AZKAR("azkar"), NAMES("names"), TASBIH("tasbih"),
    AUDIO("audio"), LEXICON("lexicon"), WORD_BY_WORD("word_by_word"), FARMUDA("farmuda"), QIBLA("qibla"), BOOKMARKS("bookmarks"), SETTINGS("settings"), BACKUP("backup")
}

/** Saved GPS coordinates are valid only with a recent, real Location.time timestamp. */
internal const val MAX_SAVED_GPS_FIX_AGE_MILLIS: Long = 30L * 60L * 1000L

internal fun isUsableSavedGpsFix(
    latitude: Double?,
    longitude: Double?,
    fixTimeMillis: Long,
    nowMillis: Long,
    maxAgeMillis: Long = MAX_SAVED_GPS_FIX_AGE_MILLIS
): Boolean {
    if (latitude == null || longitude == null ||
        !latitude.isFinite() || latitude !in -90.0..90.0 ||
        !longitude.isFinite() || longitude !in -180.0..180.0 ||
        fixTimeMillis <= 0L || maxAgeMillis < 0L
    ) return false
    val age = nowMillis - fixTimeMillis
    return age in 0L..maxAgeMillis
}

internal fun resolvePrayerLocationMode(
    savedMode: String?,
    hasSavedGpsCoordinates: Boolean,
    hasManualCityCoordinates: Boolean
): String = when (savedMode?.lowercase(Locale.ROOT)) {
    "gps" -> "gps"
    "city" -> "city"
    else -> when {
        hasSavedGpsCoordinates -> "gps"
        hasManualCityCoordinates -> "city"
        else -> "gps"
    }
}

internal fun shouldAutoRequestLocation(
    gpsLatitude: Double?,
    gpsLongitude: Double?,
    selectedCity: String,
    manualLatitude: Double?,
    manualLongitude: Double?,
    locationMode: String? = null
): Boolean {
    if (locationMode.equals("city", ignoreCase = true)) return false
    val hasGpsFix = gpsLatitude != null && gpsLongitude != null
    val hasManualCity = selectedCity.isNotBlank() && manualLatitude != null && manualLongitude != null
    if (hasGpsFix) return false
    if (locationMode.equals("gps", ignoreCase = true)) return true
    return !hasManualCity
}

private data class HomeModule(
    val screen: AppScreen, val titleKey: String, val subtitleKey: String,
    val icon: ImageVector, val color: Color
)

private val homeModules: List<HomeModule> get() = listOf(
    HomeModule(AppScreen.QURAN, "quran", "quran_sub", Icons.Outlined.MenuBook, Gold),
    HomeModule(AppScreen.BOOKMARKS, "bookmarks", "bookmarks_sub", Icons.Outlined.BookmarkBorder, Color(0xFFE0B86C)),
    HomeModule(AppScreen.HADITH, "hadith", "hadith_sub", Icons.Outlined.LibraryBooks, Color(0xFFA99AE8)),
    HomeModule(AppScreen.AZKAR, "azkar", "azkar_sub", Icons.Outlined.Spa, Color(0xFF9CCB7B)),
    HomeModule(AppScreen.PRAYER, "prayer", "prayer_sub", Icons.Outlined.Schedule, Color(0xFF82B7E8)),
        HomeModule(AppScreen.NAMES, "names", "names_sub", Icons.Outlined.Favorite, Color(0xFFE39FB2)),
    HomeModule(AppScreen.QIBLA, "qibla", "qibla_sub", Icons.Outlined.Explore, Color(0xFF85C4C6)),
    HomeModule(AppScreen.LEXICON, "lexicon", "lexicon_sub", Icons.Outlined.Search, Color(0xFFB3A2F0)),
    HomeModule(AppScreen.FARMUDA, "farmuda", "farmuda_sub", Icons.Outlined.VolunteerActivism, Color(0xFFE6A879)),
    HomeModule(AppScreen.TASBIH, "tasbih", "tasbih_sub", Icons.Outlined.Spa, Color(0xFF80C6B1)),
)

private val textMap = mapOf(
    "ckb" to mapOf(
        "home" to "سەرەتا", "more" to "زیاتر", "quran" to "قورئانی پیرۆز", "quran_sub" to "خوێندنەوە و گەڕان", "tafsir" to "تەفسیرو وەرگێڕان", "tafsir_sub" to "چەندین تەفسیر", "hadith" to "فەرموودەکان", "hadith_sub" to "گەڕان لە فەرموودە", "azkar" to "زیکر و دوعاکان", "azkar_sub" to "بەیانی، ئێواران و زیاتر", "prayer" to "کاتەکانی بانگ", "prayer_sub" to "کاتی نوێژ و شار", "audio" to "دەنگ و تلاوەت", "audio_sub" to "فایلە دەنگییە ناوخۆییەکان", "names" to "ناوە پیرۆزەکان", "names_sub" to "٩٩ ناوی خودا", "qibla" to "ئاڕاستەی قیبلە", "qibla_sub" to "ئاڕاستە بەپێی شوێن", "lexicon" to "فەرهەنگی وشەکان", "lexicon_sub" to "مانا و ڕەگی وشە", "word_by_word" to "شیکردنەوەی وشە بە وشە", "word_by_word_sub" to "ڕەگ و ڕێزمانی قورئان", "farmuda" to "فەرموودە و دوعاکان", "farmuda_sub" to "دەق و زانیاری زیادە", "tasbih" to "تەسبیح", "tasbih_sub" to "ژماردنی زیکر", "welcome" to "هەموو زانیارییە ئیسلامییەکانت لە شوێنێکدا", "bookmarks" to "نیشانکراوەکان", "bookmarks_sub" to "ئایەتە پاشەکەوتکراوەکان", "surah" to "سوورەت", "ayah" to "ئایەت", "verse" to "ئایەت", "book_online" to "زانیاری کتێب", "mushaf_view" to "مصحەفی پیرۆز", "verse_view" to "ئایەت بە ئایەت", "mushaf_unavailable" to "نەتوانرا پەڕەی مصحف بخوێندرێتەوە", "mushaf_offline" to "٦٠٤ پەڕەی قورئان بەبێ ئینتەرنێت", "no_bookmarks" to "هیچ ئایەتێک نیشان نەکراوە", "no_adhan_audio" to "دەنگی بانگ لەم داتایەدا نەدۆزرایەوە", "page" to "پەڕە", "previous" to "پێشوو", "next" to "دواتر", "open_pdf" to "کردنەوەی PDF", "settings" to "ڕێکخستن", "backup_title" to "پاشەکەوتکردن", "backup_subtitle" to "پاراستنی داتا و گەڕاندنەوە", "back" to "گەڕانەوە", "home_title" to "دڵت ڕووناک بکە", "home_hint" to "ئەمڕۆ بە چەند خولەکێک خوێندنەوە دەست پێ بکە", "quick_access" to "دەستگەیشتنی خێرا", "continue_reading" to "بەردەوام بە لە خوێندنەوە", "language" to "زمان", "offline" to "زۆربەی زانیارییەکان بەبێ ئینتەرنێت کار دەکەن", "about_data" to "سەرچاوەی داتا و مافەکان", "data_notice" to "هەندێک کتێب پێویستی بە ئینتەرنێت یان شیکردنەوەی تایبەت هەیە.", "search" to "گەڕان...", "choose_surah" to "سوورەت هەڵبژێرە", "select_tafsir" to "تەفسیر هەڵبژێرە", "search_hadith" to "لە فەرموودە بگەڕێ...", "search_words" to "وشە یان ڕەگ بگەڕێ...", "city" to "شار", "select_city" to "شار هەڵبژێرە", "fajr" to "بەیانی", "sunrise" to "هەڵهاتنی خۆر", "dhuhr" to "نیوەڕۆ", "asr" to "عەسر", "maghrib" to "مەغریب", "isha" to "عیشا", "search_azkar" to "لە زیکرەکان بگەڕێ...", "arabic" to "عەرەبی", "translation" to "وەرگێڕان", "reference" to "سەرچاوە", "reset" to "سفرکردنەوە", "play" to "لێدان", "available_data" to "داتای بەردەست", "no_data" to "ناوەڕۆک لەم کاتەدا بەردەست نییە؛ دووبارە هەوڵ بدەرەوە.", "not_found" to "هیچ ئەنجامێک نەدۆزرایەوە.", "qibla_heading" to "قیبلە لە ڕووی باکوورەوە", "degrees" to "پلە", "library_hint" to "قورئان، حدیث، تەفسیر، کتێب و دەنگ"
    ),
    "ar" to mapOf(
        "home" to "الرئيسية", "more" to "المزيد", "quran" to "القرآن الكريم", "quran_sub" to "قراءة وبحث", "tafsir" to "التفسير والترجمة", "tafsir_sub" to "تفاسير متعددة", "hadith" to "الأحاديث", "hadith_sub" to "البحث في الأحاديث", "azkar" to "الأذكار والأدعية", "azkar_sub" to "الصباح والمساء والمزيد", "prayer" to "مواقيت الصلاة", "prayer_sub" to "المواقيت والمدينة", "audio" to "التلاوات والصوتيات", "audio_sub" to "الملفات الصوتية المحلية", "names" to "أسماء الله الحسنى", "names_sub" to "الأسماء التسعة والتسعون", "qibla" to "اتجاه القبلة", "qibla_sub" to "الاتجاه حسب المدينة", "lexicon" to "معجم الكلمات", "lexicon_sub" to "المعاني والجذور", "word_by_word" to "تفسير كلمة بكلمة", "word_by_word_sub" to "الجذور والتحليل النحوي", "farmuda" to "المأثورات والأدعية", "farmuda_sub" to "أدعية ونصوص إضافية", "tasbih" to "المسبحة", "tasbih_sub" to "عدّ الذكر", "welcome" to "مكتبتك الإسلامية في مكان واحد", "bookmarks" to "العلامات المرجعية", "bookmarks_sub" to "الآيات المحفوظة", "surah" to "السورة", "ayah" to "الآية", "verse" to "آية", "book_online" to "معلومات الكتاب", "mushaf_view" to "عرض المصحف", "verse_view" to "عرض الآيات", "mushaf_unavailable" to "تعذر فتح صفحة المصحف. تحقق من ملفات البيانات.", "mushaf_offline" to "٦٠٤ صفحة من المصحف تعمل دون إنترنت", "no_bookmarks" to "لا توجد آيات محفوظة بعد", "no_adhan_audio" to "لا توجد ملفات أذان ضمن البيانات المتاحة", "page" to "صفحة", "previous" to "السابق", "next" to "التالي", "open_pdf" to "فتح PDF", "settings" to "الإعدادات", "backup_title" to "النسخ الاحتياطي", "backup_subtitle" to "حفظ البيانات واستعادتها", "back" to "رجوع", "home_title" to "أضئ قلبك بالمعرفة", "quick_access" to "وصول سريع", "continue_reading" to "متابعة القراءة", "language" to "اللغة", "offline" to "تعمل معظم المكتبة دون اتصال بالإنترنت", "about_data" to "مصادر البيانات والحقوق", "data_notice" to "بعض الكتب تتطلب الإنترنت أو معالجة خاصة.", "search" to "بحث...", "choose_surah" to "اختر السورة", "select_tafsir" to "اختر التفسير", "search_hadith" to "ابحث في الأحاديث...", "search_words" to "ابحث عن كلمة أو جذر...", "city" to "المدينة", "select_city" to "اختر المدينة", "fajr" to "الفجر", "sunrise" to "الشروق", "dhuhr" to "الظهر", "asr" to "العصر", "maghrib" to "المغرب", "isha" to "العشاء", "search_azkar" to "ابحث في الأذكار...", "arabic" to "العربية", "translation" to "الترجمة", "reference" to "المصدر", "reset" to "إعادة", "play" to "تشغيل", "available_data" to "البيانات المتاحة", "no_data" to "تعذّر تحميل المحتوى حاليًا؛ حاول مجددًا أو تحقق من توفر البيانات.", "not_found" to "لم يتم العثور على نتائج.", "qibla_heading" to "اتجاه القبلة من الشمال", "degrees" to "درجة", "library_hint" to "القرآن، الحديث، التفسير، الكتب والصوتيات"
    ),
    "en" to mapOf(
        "home" to "Home", "more" to "More", "quran" to "The Holy Quran", "quran_sub" to "Read and search", "tafsir" to "Tafsir & translation", "tafsir_sub" to "Multiple commentaries", "hadith" to "Hadith", "hadith_sub" to "Search narrations", "azkar" to "Adhkar & duas", "azkar_sub" to "Morning, evening and more", "prayer" to "Prayer times", "prayer_sub" to "Times and city", "audio" to "Recitations & audio", "audio_sub" to "Bundled local media", "names" to "Names of Allah", "names_sub" to "The 99 names", "qibla" to "Qibla direction", "qibla_sub" to "Direction by selected city", "lexicon" to "Word lexicon", "lexicon_sub" to "Meanings and roots", "word_by_word" to "Word-by-word analysis", "word_by_word_sub" to "Roots and Quranic grammar", "farmuda" to "Supplications & sayings", "farmuda_sub" to "Additional texts and duas", "tasbih" to "Digital tasbih", "tasbih_sub" to "Count remembrance", "welcome" to "Your Islamic library in one place", "bookmarks" to "Bookmarks", "bookmarks_sub" to "Saved verses", "surah" to "Surah", "ayah" to "Ayah", "verse" to "verses", "book_online" to "Book details", "mushaf_view" to "Mushaf pages", "verse_view" to "Verse list", "mushaf_unavailable" to "Could not read this Mushaf page; verify the bundled data.", "mushaf_offline" to "All 604 Mushaf pages work offline", "no_bookmarks" to "No saved verses yet", "no_adhan_audio" to "No adhan-specific audio was found in the supplied data", "page" to "Page", "previous" to "Previous", "next" to "Next", "open_pdf" to "Open PDF", "settings" to "Settings", "backup_title" to "Backup & restore", "backup_subtitle" to "Protect and restore data", "back" to "Back", "home_title" to "Let knowledge light your heart", "quick_access" to "Quick access", "continue_reading" to "Continue reading", "language" to "Language", "offline" to "Most of the library works offline", "about_data" to "Data sources & rights", "data_notice" to "Some books need internet access or format-specific processing.", "search" to "Search...", "choose_surah" to "Choose a surah", "select_tafsir" to "Choose tafsir", "search_hadith" to "Search Hadith...", "search_words" to "Search words or roots...", "city" to "City", "select_city" to "Choose city", "fajr" to "Fajr", "sunrise" to "Sunrise", "dhuhr" to "Dhuhr", "asr" to "Asr", "maghrib" to "Maghrib", "isha" to "Isha", "search_azkar" to "Search adhkar...", "arabic" to "Arabic", "translation" to "Translation", "reference" to "Reference", "reset" to "Reset", "play" to "Play", "available_data" to "Available content", "no_data" to "This content could not be loaded right now. Try again or check data availability.", "not_found" to "No results found.", "qibla_heading" to "Qibla bearing from north", "degrees" to "degrees", "library_hint" to "Quran, Hadith, tafsir, books and audio"
    )
)

fun tr(language: AppLanguage, key: String): String =
    textMap[language.key]?.get(key) ?: textMap["en"]?.get(key) ?: key

class MainActivity : ComponentActivity() {
    companion object {
        const val EXTRA_OPEN_SCREEN = "com.mashxall.unified.OPEN_SCREEN"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(7, 30, 26)
        window.navigationBarColor = android.graphics.Color.rgb(7, 30, 26)
        setContent { MashxallApp(startScreen = intent.getStringExtra(EXTRA_OPEN_SCREEN)) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        setContent { MashxallApp(startScreen = intent.getStringExtra(EXTRA_OPEN_SCREEN)) }
    }
}

@Composable
fun MashxallApp(startScreen: String? = null) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("mashxall_preferences", 0) }
    var language by rememberSaveable { mutableStateOf(AppLanguage.from(prefs.getString("language", "ckb"))) }
    var use24Hour by rememberSaveable { mutableStateOf(prefs.getBoolean("time_format_24h", false)) }
    var accentPalette by rememberSaveable { mutableStateOf(prefs.getString("accent_palette", "gold") ?: "gold") }
    SideEffect { MashxallAppearance.accent = MashxallAppearance.colorFor(accentPalette) }
    var screen by rememberSaveable {
        mutableStateOf(
            startScreen?.takeIf { candidate ->
                AppScreen.entries.any { it.key == candidate }
            } ?: AppScreen.HOME.key
        )
    }
    var navigationStack by rememberSaveable { mutableStateOf(emptyList<String>()) }
    LaunchedEffect(startScreen) {
        val requested = startScreen?.takeIf { candidate ->
            AppScreen.entries.any { it.key == candidate }
        }
        if (requested != null) {
            screen = requested
            navigationStack = emptyList()
        }
    }
    var dark by rememberSaveable { mutableStateOf(prefs.getBoolean("dark_mode", true)) }
    var selectedSurah by rememberSaveable { mutableIntStateOf(prefs.getInt("last_surah", 1).coerceIn(1, 114)) }
    var selectedAyah by rememberSaveable { mutableIntStateOf(prefs.getInt("last_ayah_number", 1).coerceAtLeast(1)) }
    var resumeMushafRequested by rememberSaveable { mutableStateOf(false) }
    var quranSettingsRequested by rememberSaveable { mutableStateOf(false) }
    var audioStartsAtReciters by rememberSaveable { mutableStateOf(false) }
    var audioStartsAtAdhan by rememberSaveable { mutableStateOf(false) }
    var audioAutoPlayRequested by rememberSaveable { mutableStateOf(false) }
    var selectedCity by rememberSaveable {
        mutableStateOf(
            prefs.getString("selected_city", null)?.takeIf { saved ->
                saved.isNotBlank() && (
                    !saved.equals("Erbil", ignoreCase = true) ||
                        (prefs.getString("manual_city_latitude", null) != null &&
                            prefs.getString("manual_city_longitude", null) != null)
                    )
            } ?: ""
        )
    }
    var immersiveMushaf by rememberSaveable { mutableStateOf(false) }
    var lastHomeBackPressAt by rememberSaveable { mutableStateOf(0L) }
    val layoutDirection = if (language == AppLanguage.EN) LayoutDirection.Ltr else LayoutDirection.Rtl
    val rootScreens = setOf("home", "quran", "azkar", "more", "settings")
    val isRoot = screen in rootScreens
    val activity = context as? ComponentActivity
    val window = activity?.window
    SideEffect {
        window?.let { target ->
            val controller = WindowInsetsControllerCompat(target, target.decorView)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (immersiveMushaf) controller.hide(WindowInsetsCompat.Type.systemBars())
            else controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
    DisposableEffect(window) {
        onDispose { window?.let { WindowInsetsControllerCompat(it, it.decorView).show(WindowInsetsCompat.Type.systemBars()) } }
    }

    fun navigateTo(target: String) {
        if (target == screen) return
        navigationStack = if (target in rootScreens) {
            emptyList()
        } else {
            (navigationStack + screen).takeLast(32)
        }
        screen = target
    }

    fun navigateBack() {
        if (navigationStack.isNotEmpty()) {
            screen = navigationStack.last()
            navigationStack = navigationStack.dropLast(1)
        } else {
            screen = AppScreen.HOME.key
        }
        audioStartsAtReciters = false
        audioStartsAtAdhan = false
        audioAutoPlayRequested = false
    }

    LaunchedEffect(screen) {
        lastHomeBackPressAt = 0L
    }

    BackHandler(enabled = !immersiveMushaf && screen != AppScreen.HOME.key) { navigateBack() }
    BackHandler(enabled = !immersiveMushaf && screen == AppScreen.HOME.key) {
        val now = System.currentTimeMillis()
        if (lastHomeBackPressAt > 0L && now - lastHomeBackPressAt <= 2000L) {
            activity?.finish()
        } else {
            lastHomeBackPressAt = now
            Toast.makeText(context, homeBackExitMessage(language), Toast.LENGTH_SHORT).show()
        }
    }

    val background = if (dark) Ink else Color(0xFFF4F1E7)
    val foreground = if (dark) Color(0xFFF6F2E6) else Color(0xFF102A23)
    val cardColor = if (dark) Panel else Color.White
    MaterialTheme(
        colorScheme = if (dark) androidx.compose.material3.darkColorScheme(
            primary = Gold, onPrimary = Ink, secondary = PaleGold, background = Ink, surface = Panel,
            onSurface = Color(0xFFF6F2E6), onBackground = Color(0xFFF6F2E6), surfaceVariant = Color(0xFF163A30)
        ) else androidx.compose.material3.lightColorScheme(
            primary = DeepGreen, onPrimary = Color.White, secondary = Gold, background = Color(0xFFF4F1E7),
            surface = Color.White, onSurface = Color(0xFF102A23), onBackground = Color(0xFF102A23)
        )
    ) {
        CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides layoutDirection) {
            Surface(color = background, modifier = Modifier.fillMaxSize()) {
                Scaffold(
                    containerColor = background,
                    contentWindowInsets = if (immersiveMushaf) WindowInsets(0.dp, 0.dp, 0.dp, 0.dp) else WindowInsets.statusBars,
                    bottomBar = {
                        if (isRoot && !immersiveMushaf) BottomNavigationBar(screen, language, cardColor, foreground) { target ->
                            navigationStack = emptyList()
                            screen = target
                            audioStartsAtReciters = false
                            audioStartsAtAdhan = false
                            audioAutoPlayRequested = false
                        }
                    }
                ) { padding ->
                    Column(Modifier.fillMaxSize().padding(if (immersiveMushaf) PaddingValues(0.dp) else padding)) {
                        if (!immersiveMushaf) AppHeader(
                            language = language, foreground = foreground, isDark = dark,
                            onLanguage = { language = it; prefs.edit().putString("language", it.key).apply() },
                            onBack = { navigateBack() },
                            onSettings = { navigateTo(AppScreen.SETTINGS.key) }, showBack = !isRoot || screen == AppScreen.SETTINGS.key
                        )
                        AnimatedContent(
                            targetState = screen,
                            modifier = Modifier.fillMaxSize(),
                            transitionSpec = {
                                val enterFrom = if (layoutDirection == LayoutDirection.Ltr) 1 else -1
                                val exitTo = -enterFrom
                                (fadeIn(animationSpec = tween(240, easing = FastOutSlowInEasing)) +
                                    slideInHorizontally(animationSpec = tween(260, easing = FastOutSlowInEasing)) { fullWidth ->
                                        fullWidth * enterFrom / 16
                                    }) togetherWith
                                    (fadeOut(animationSpec = tween(170)) +
                                        slideOutHorizontally(animationSpec = tween(180, easing = FastOutSlowInEasing)) { fullWidth ->
                                            fullWidth * exitTo / 24
                                        })
                            },
                            label = "Mashxall native screen transition"
                        ) { visibleScreen ->
                            Box(Modifier.fillMaxSize()) {
                                when (visibleScreen) {
                                "more" -> MoreScreen(language, foreground, cardColor) { target -> navigateTo(target.key) }
                                "home" -> HomeScreen(
                                    language, foreground, cardColor, selectedCity, use24Hour,
                                    onOpen = { target -> navigateTo(target.key) },
                                    onResumeReading = {
                                        // First-time users should still see the surah list;
                                        // resume only after a real Mushaf page was saved.
                                        resumeMushafRequested = prefs.contains("last_mushaf_page")
                                        navigateTo(AppScreen.QURAN.key)
                                    }
                                )
                                "quran" -> QuranScreen(
                                    language, foreground, cardColor, selectedSurah, selectedAyah,
                                    onSurah = { s ->
                                        selectedSurah = s
                                        selectedAyah = 1
                                        prefs.edit().putInt("last_surah", s).putInt("last_ayah_number", 1).apply()
                                    },
                                    onTafsir = { s, a ->
                                        selectedSurah = s; selectedAyah = a
                                        prefs.edit().putInt("last_surah", s).putInt("last_ayah_number", a).apply()
                                        navigateTo("tafsir")
                                    },
                                    onBookmark = { s, a ->
                                        selectedSurah = s; selectedAyah = a
                                        val saved = prefs.getStringSet("bookmarks", emptySet())?.toMutableSet() ?: mutableSetOf()
                                        saved.add(s.toString() + ":" + a.toString())
                                        prefs.edit().putInt("last_surah", s).putInt("last_ayah_number", a).putStringSet("bookmarks", saved).apply()
                                    },
                                    onOpenVerse = { s, a ->
                                        selectedSurah = s
                                        selectedAyah = a
                                        prefs.edit().putInt("last_surah", s).putInt("last_ayah_number", a).apply()
                                    },
                                    onPlayAudio = { s, a ->
                                        selectedSurah = s
                                        selectedAyah = a
                                        audioStartsAtReciters = true
                                        audioStartsAtAdhan = false
                                        audioAutoPlayRequested = true
                                        prefs.edit().putInt("last_surah", s).putInt("last_ayah_number", a).apply()
                                        navigateTo("audio")
                                    },
                                    onBrowseReciters = { s, a ->
                                        selectedSurah = s
                                        selectedAyah = a
                                        audioStartsAtReciters = true
                                        audioStartsAtAdhan = false
                                        audioAutoPlayRequested = false
                                        prefs.edit().putInt("last_surah", s).putInt("last_ayah_number", a).apply()
                                        navigateTo(AppScreen.AUDIO.key)
                                    },
                                    onImmersiveMushaf = { immersiveMushaf = it },
                                    resumeLastMushaf = resumeMushafRequested,
                                    onResumeMushafHandled = { resumeMushafRequested = false },
                                    openSettingsOnEntry = quranSettingsRequested,
                                    onSettingsRequestConsumed = { quranSettingsRequested = false }
                                )
                                                                "about_data" -> LibraryScreen(language, foreground, cardColor) { navigateTo(it.key) }
                                "worship" -> WorshipScreen(language, foreground, cardColor) { navigateTo(it.key) }
                                "prayer" -> PrayerScreen(language, foreground, cardColor, selectedCity, use24Hour = use24Hour, onCity = { city ->
                                    selectedCity = city.name
                                    prefs.edit()
                                        .putString("prayer_location_mode", "city")
                                        .putString("selected_city", city.name)
                                        .putString("selected_city_country", city.country)
                                        .putString("manual_city_latitude", city.latitude.toString())
                                        .putString("manual_city_longitude", city.longitude.toString())
                                        .remove("gps_latitude").remove("gps_longitude")
                                        .remove("gps_location_timestamp").remove("gps_accuracy")
                                        .apply()
                                }, onQibla = { navigateTo("qibla") },
                                    onBrowseAdhanLibrary = {
                                        audioStartsAtAdhan = true
                                        audioStartsAtReciters = false
                                        audioAutoPlayRequested = false
                                        navigateTo(AppScreen.AUDIO.key)
                                    })
                                "tafsir" -> TafsirScreen(language, foreground, cardColor, selectedSurah, selectedAyah, onChangeVerse = { s, a ->
                                    selectedSurah = s; selectedAyah = a
                                    prefs.edit().putInt("last_surah", s).putInt("last_ayah_number", a).apply()
                                })
                                "hadith" -> HadithScreen(language, foreground, cardColor)
                                "azkar" -> AzkarScreen(language, foreground, cardColor)
                                "names" -> NamesScreen(language, foreground, cardColor)
                                "tasbih" -> TasbihScreen(language, foreground, cardColor)
                                "audio" -> AudioScreen(
                                    language, foreground, cardColor, selectedSurah, selectedAyah,
                                    focusReciters = audioStartsAtReciters,
                                    focusAdhan = audioStartsAtAdhan,
                                    autoPlayRequested = audioAutoPlayRequested
                                )
                                                                                                "bookmarks" -> BookmarksScreen(language, foreground, cardColor, onOpen = { s, a ->
                                    selectedSurah = s; selectedAyah = a
                                    prefs.edit().putInt("last_surah", s).putInt("last_ayah_number", a).apply()
                                    navigationStack = emptyList()
                                    screen = "quran"
                                })
                                "lexicon" -> LexiconScreen(language, foreground, cardColor)
                                "word_by_word" -> WordAnalysisScreen(language, foreground, cardColor, selectedSurah, selectedAyah, onLoad = { s, a -> selectedSurah = s; selectedAyah = a })
                                "farmuda" -> FarmudaScreen(language, foreground, cardColor)
                                "qibla" -> QiblaScreen(language, foreground, cardColor, selectedCity)
                                "settings" -> SettingsScreen(language, foreground, cardColor, dark, accentPalette, use24Hour,
                                    onAccent = {
                                        accentPalette = it
                                        MashxallAppearance.accent = MashxallAppearance.colorFor(it)
                                        prefs.edit().putString("accent_palette", it).apply()
                                    },
                                    onDark = { dark = it; prefs.edit().putBoolean("dark_mode", it).apply() }, onLanguage = {
                                    language = it
                                    prefs.edit().putString("language", it.key).apply()
                                }, onTimeFormatChange = { enabled ->
                                    use24Hour = enabled
                                    prefs.edit().putBoolean("time_format_24h", enabled).apply()
                                }, onDataInfo = { navigateTo("about_data") }, onBackup = { navigateTo("backup") }, onAudio = {
                                    audioStartsAtReciters = true
                                    audioStartsAtAdhan = false
                                    audioAutoPlayRequested = false
                                    navigateTo("audio")
                                }, onAdhan = {
                                    audioStartsAtReciters = false
                                    audioStartsAtAdhan = true
                                    audioAutoPlayRequested = false
                                    navigateTo("audio")
                                }, onPrayer = { navigateTo("prayer") }, onQuran = {
                                    quranSettingsRequested = true
                                    navigateTo("quran")
                                })
                                "backup" -> BackupRestoreScreen(language, foreground, cardColor)
                                else -> HomeScreen(language, foreground, cardColor, selectedCity, use24Hour, onOpen = { target -> navigateTo(target.key) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppHeader(
    language: AppLanguage, foreground: Color, isDark: Boolean,
    onLanguage: (AppLanguage) -> Unit, onBack: () -> Unit,
    onSettings: () -> Unit, showBack: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth().background(if (isDark) Ink else Color(0xFFF4F1E7)).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showBack) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Outlined.Explore, contentDescription = tr(language, "back"), tint = Gold)
            }
        } else {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(listOf(DeepGreen, Emerald))), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.NightsStay, contentDescription = null, tint = Gold, modifier = Modifier.size(25.dp))
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("مـــەشــخـــەڵ", color = foreground, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            Text("M A S H X A L L", color = Gold, fontSize = 9.sp, letterSpacing = 2.2.sp)
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            items(AppLanguage.entries) { lang ->
                FilterChip(
                    selected = language == lang, onClick = { onLanguage(lang) },
                    label = { Text(lang.label, fontSize = 10.sp, maxLines = 1) },
                    modifier = Modifier.height(30.dp),
                    border = BorderStroke(0.6.dp, if (language == lang) Gold else foreground.copy(alpha = 0.18f))
                )
            }
        }
        if (!showBack) IconButton(onClick = onSettings, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Outlined.Settings, contentDescription = tr(language, "settings"), tint = Gold)
        }
    }
}

@Composable
private fun BottomNavigationBar(screen: String, language: AppLanguage, cardColor: Color, foreground: Color, onSelect: (String) -> Unit) {
    val items = listOf(
        Triple("home", tr(language, "home"), Icons.Outlined.Home),
        Triple("quran", tr(language, "quran"), Icons.Outlined.MenuBook),
        Triple("azkar", tr(language, "azkar"), Icons.Outlined.Spa),
        Triple("more", tr(language, "more"), Icons.Outlined.MoreHoriz),
        Triple("settings", tr(language, "settings"), Icons.Outlined.Settings)
    )
    NavigationBar(containerColor = cardColor, contentColor = foreground, windowInsets = WindowInsets.navigationBars) {
        items.forEach { (key, title, icon) ->
            NavigationBarItem(
                selected = screen == key, onClick = { onSelect(key) },
                icon = {
                    Icon(
                        icon,
                        contentDescription = title,
                        modifier = Modifier.size(21.dp)
                    )
                },
                label = { Text(title, fontSize = 9.sp, maxLines = 1) },
                alwaysShowLabel = true
            )
        }
    }
}


@Composable
private fun MoreScreen(
    language: AppLanguage,
    foreground: Color,
    cardColor: Color,
    onOpen: (AppScreen) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    val modules = listOf(
        HomeModule(AppScreen.PRAYER, "prayer", "prayer_sub", Icons.Outlined.Schedule, Color(0xFF82B7E8)),
        HomeModule(AppScreen.HADITH, "hadith", "hadith_sub", Icons.Outlined.LibraryBooks, Color(0xFFA99AE8)),
        HomeModule(AppScreen.QIBLA, "qibla", "qibla_sub", Icons.Outlined.Explore, Color(0xFF85C4C6)),
        HomeModule(AppScreen.AUDIO, "audio", "audio_sub", Icons.Outlined.GraphicEq, Color(0xFFE6A879)),
        HomeModule(AppScreen.NAMES, "names", "names_sub", Icons.Outlined.Favorite, Color(0xFFE39FB2)),
        HomeModule(AppScreen.TASBIH, "tasbih", "tasbih_sub", Icons.Outlined.Spa, Color(0xFF80C6B1)),
        HomeModule(AppScreen.FARMUDA, "farmuda", "farmuda_sub", Icons.Outlined.VolunteerActivism, Color(0xFFE6A879)),
        HomeModule(AppScreen.LEXICON, "lexicon", "lexicon_sub", Icons.Outlined.Search, Color(0xFFB3A2F0)),
        HomeModule(AppScreen.BOOKMARKS, "bookmarks", "bookmarks_sub", Icons.Outlined.BookmarkBorder, Gold),
        HomeModule(AppScreen.BACKUP, "backup_title", "backup_subtitle", Icons.Outlined.Tune, Emerald),
    )
    val visibleModules = modules.filter { module ->
        query.isBlank() ||
            tr(language, module.titleKey).contains(query.trim(), ignoreCase = true) ||
            tr(language, module.subtitleKey).contains(query.trim(), ignoreCase = true)
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    when (language) { AppLanguage.EN -> "More features"; AppLanguage.AR -> "المزيد من الميزات"; AppLanguage.CKB -> "تایبەتمەندیی زیاتر" },
                    color = foreground, fontSize = 24.sp, fontWeight = FontWeight.Bold
                )
                Text(
                    when (language) {
                        AppLanguage.EN -> "Audio, Qibla, Hadith and personal tools"
                        AppLanguage.AR -> "الصوتيات والقبلة والأحاديث والأدوات الشخصية"
                        AppLanguage.CKB -> "دەنگ، قیبلە، فەرموودە و ئامرازە کەسییەکان"
                    }, color = foreground.copy(alpha = 0.58f), fontSize = 11.sp
                )
            }
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(15.dp)).background(Gold.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Outlined.MoreHoriz, contentDescription = null, tint = Gold, modifier = Modifier.size(25.dp)) }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            label = { Text(when (language) { AppLanguage.EN -> "Find a feature"; AppLanguage.AR -> "ابحث عن ميزة"; AppLanguage.CKB -> "بەدوای تایبەتمەندییەکدا بگەڕێ" }) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) }
        )
        Spacer(Modifier.height(10.dp))
        if (visibleModules.isEmpty()) {
            Text(tr(language, "not_found"), color = foreground.copy(alpha = 0.6f), fontSize = 13.sp)
        } else {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                visibleModules.chunked(2).forEachIndexed { rowIndex, pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
                        pair.forEachIndexed { columnIndex, module ->
                            val delayMillis = ((rowIndex * 2 + columnIndex) * 28).coerceAtMost(360)
                            AnimatedVisibility(
                                visible = true,
                                modifier = Modifier.weight(1f),
                                enter = fadeIn(animationSpec = tween(320, delayMillis = delayMillis)) +
                                    slideInVertically(initialOffsetY = { it / 9 }, animationSpec = tween(320, delayMillis = delayMillis))
                            ) {
                                Card(
                                    modifier = Modifier.fillMaxWidth().height(121.dp)
                                        .animateContentSize()
                                        .premiumPress {
                                            onOpen(module.screen)
                                        },
                                    colors = CardDefaults.cardColors(containerColor = cardColor),
                                    shape = RoundedCornerShape(21.dp),
                                    border = BorderStroke(0.8.dp, module.color.copy(alpha = 0.46f))
                                ) {
                                    Column(
                                        Modifier.fillMaxSize()
                                            .background(Brush.linearGradient(listOf(module.color.copy(alpha = 0.12f), cardColor)))
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Box(
                                            Modifier.size(36.dp).clip(RoundedCornerShape(12.dp))
                                                .background(module.color.copy(alpha = 0.17f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(module.icon, contentDescription = null, tint = module.color, modifier = Modifier.size(21.dp))
                                        }
                                        Column {
                                            Text(
                                                tr(language, module.titleKey),
                                                color = foreground, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1
                                            )
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                tr(language, module.subtitleKey),
                                                color = foreground.copy(alpha = 0.59f), fontSize = 9.sp, maxLines = 2, lineHeight = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

private fun prayerSourceLabel(source: String, language: AppLanguage): String {
    val normalized = source.substringBefore(" • ").lowercase(Locale.ROOT)
    return if (normalized.startsWith("salawati")) {
        when (language) {
            AppLanguage.EN -> "Salawati timetable"
            AppLanguage.AR -> "جدول صلواتي"
            AppLanguage.CKB -> "خشتەی صلواتی"
        }
    } else {
        ""
    }
}

internal fun formatPrayerTime(raw: String, language: AppLanguage, use24Hour: Boolean): String {
    // Match the complete source value so an existing AM/PM suffix can never be
    // silently ignored by a partial Regex.find() match.
    val match = Regex("""^(\d{1,2}):(\d{2})(?:\s*(AM|PM))?$""", RegexOption.IGNORE_CASE)
        .matchEntire(raw.trim()) ?: return raw
    val sourceHour = match.groupValues[1].toIntOrNull() ?: return raw
    val minute = match.groupValues[2]
    val minuteNumber = minute.toIntOrNull() ?: return raw
    val suffix = match.groupValues[3].uppercase(Locale.ROOT)
    if (minuteNumber !in 0..59) return raw
    if (suffix.isNotBlank() && sourceHour !in 1..12) return raw
    if (suffix.isBlank() && sourceHour !in 0..23) return raw

    val hour24 = when {
        suffix == "AM" -> sourceHour % 12
        suffix == "PM" -> (sourceHour % 12) + 12
        else -> sourceHour
    }
    if (use24Hour) return String.format(Locale.US, "%02d:%s", hour24, minute)

    val hour12 = when (hour24 % 12) { 0 -> 12; else -> hour24 % 12 }
    val isPm = hour24 >= 12
    val marker = if (language == AppLanguage.AR) {
        if (isPm) "مساءً" else "صباحًا"
    } else if (language == AppLanguage.CKB) {
        if (isPm) "د.ن" else "پ.ن"
    } else {
        if (isPm) "PM" else "AM"
    }
    return String.format(Locale.US, "%d:%s %s", hour12, minute, marker)
}


internal data class UpcomingPrayerTarget(val key: String, val timeMillis: Long)

private fun parseSourcePrayerClock(raw: String): Pair<Int, Int>? {
    val match = Regex("""^([01]\d|2[0-3]):([0-5]\d)$""").matchEntire(raw.trim()) ?: return null
    val hour = match.groupValues[1].toIntOrNull() ?: return null
    val minute = match.groupValues[2].toIntOrNull() ?: return null
    return hour to minute
}

/** Resolve the next named prayer only from valid original Salawati rows. Sunrise is not an adhan. */
internal fun nextSourcePrayerTarget(
    today: PrayerRow?,
    tomorrow: PrayerRow?,
    nowMillis: Long
): UpcomingPrayerTarget? {
    // Enforce the same one-source policy at the consumer boundary, not only in repository callers.
    val currentDay = ContentRepository.salawatiOnlyPrayerTimes(today) ?: return null
    val candidates = listOf(
        "fajr" to currentDay.fajr,
        "dhuhr" to currentDay.dhuhr,
        "asr" to currentDay.asr,
        "maghrib" to currentDay.maghrib,
        "isha" to currentDay.isha
    )
    for ((key, raw) in candidates) {
        val (hour, minute) = parseSourcePrayerClock(raw) ?: return null
        val target = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.timeInMillis > nowMillis) return UpcomingPrayerTarget(key, target.timeInMillis)
    }

    // After Isha, do not guess tomorrow's Fajr: require tomorrow's own Salawati row.
    val tomorrowRow = ContentRepository.salawatiOnlyPrayerTimes(tomorrow) ?: return null
    val (hour, minute) = parseSourcePrayerClock(tomorrowRow.fajr) ?: return null
    val nextDay = Calendar.getInstance().apply {
        timeInMillis = nowMillis
        add(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return nextDay.timeInMillis.takeIf { it > nowMillis }?.let { UpcomingPrayerTarget("fajr", it) }
}

@Composable
private fun HomePrayerCountdown(
    today: PrayerRow?,
    tomorrow: PrayerRow?,
    language: AppLanguage,
    use24Hour: Boolean,
    foreground: Color,
    cardColor: Color,
    onNextPrayerChange: (String?) -> Unit
) {
    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1_000L)
        }
    }
    val next = remember(today, tomorrow, nowMillis / 1_000L) {
        nextSourcePrayerTarget(today, tomorrow, nowMillis)
    }
    val nowText = formatPrayerTime(SimpleDateFormat("HH:mm", Locale.US).format(Date(nowMillis)), language, use24Hour)
    val prayerName = when (next?.key) {
        "fajr" -> tr(language, "fajr")
        "dhuhr" -> tr(language, "dhuhr")
        "asr" -> tr(language, "asr")
        "maghrib" -> tr(language, "maghrib")
        "isha" -> tr(language, "isha")
        else -> ""
    }
    val prayerTimeText = next?.let {
        formatPrayerTime(SimpleDateFormat("HH:mm", Locale.US).format(Date(it.timeMillis)), language, use24Hour)
    }.orEmpty()
    val countdown = next?.let {
        val seconds = ((it.timeMillis - nowMillis).coerceAtLeast(0L) + 999L) / 1_000L
        String.format(Locale.US, "%02d:%02d:%02d", seconds / 3_600L, (seconds % 3_600L) / 60L, seconds % 60L)
    }.orEmpty()
    LaunchedEffect(next?.key) { onNextPrayerChange(next?.key) }

    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = cardColor.copy(alpha = 0.92f)),
        shape = RoundedCornerShape(17.dp),
        border = BorderStroke(1.dp, Gold.copy(alpha = 0.18f))
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(39.dp).clip(RoundedCornerShape(13.dp)).background(Gold.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Schedule, contentDescription = null, tint = Gold, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    when (language) {
                        AppLanguage.EN -> "Current time"
                        AppLanguage.AR -> "الوقت الحالي"
                        AppLanguage.CKB -> "کاتی ئێستا"
                    },
                    color = foreground.copy(alpha = 0.58f), fontSize = 10.sp
                )
                Text(nowText, color = foreground, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    when (language) {
                        AppLanguage.EN -> if (next == null) "Next prayer unavailable" else "Next: $prayerName"
                        AppLanguage.AR -> if (next == null) "الصلاة القادمة غير متاحة" else "القادمة: $prayerName"
                        AppLanguage.CKB -> if (next == null) "نوێژی دواتر بەردەست نییە" else "دواتر: $prayerName"
                    },
                    color = Gold, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1
                )
                if (next != null) {
                    Text(prayerTimeText, color = foreground, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    AnimatedContent(
                        targetState = countdown,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(120)) +
                                slideInVertically(
                                    animationSpec = tween(160, easing = FastOutSlowInEasing)
                                ) { height -> height / 3 }) togetherWith
                                (fadeOut(animationSpec = tween(100)) +
                                    slideOutVertically(
                                        animationSpec = tween(100, easing = FastOutSlowInEasing)
                                    ) { height -> -height / 3 })
                        },
                        label = "Next prayer countdown digit transition"
                    ) { animatedCountdown ->
                        Text(
                            animatedCountdown,
                            color = Gold,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        when (language) {
                            AppLanguage.EN -> "Requires tomorrow's verified source row after Isha"
                            AppLanguage.AR -> "يتطلب جدول صلواتي موثوقًا للغد بعد العشاء"
                            AppLanguage.CKB -> "دوای عیشا پێویستی بە خشتەی پشتڕاستکراوی صەڵەواتی بۆ سبەیە"
                        },
                        color = foreground.copy(alpha = 0.56f), fontSize = 9.sp, maxLines = 2
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    language: AppLanguage,
    foreground: Color,
    cardColor: Color,
    selectedCity: String,
    use24Hour: Boolean,
    onOpen: (AppScreen) -> Unit,
    onResumeReading: () -> Unit = { onOpen(AppScreen.QURAN) }
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("mashxall_preferences", 0) }
    var prayerTimes by remember { mutableStateOf<PrayerRow?>(null) }
    var tomorrowPrayerTimes by remember { mutableStateOf<PrayerRow?>(null) }
    var upcomingPrayerKey by remember { mutableStateOf<String?>(null) }
    var loadingTimes by remember { mutableStateOf(true) }
    var clockNow by remember { mutableStateOf(System.currentTimeMillis()) }
    var locationMode by rememberSaveable {
        mutableStateOf(resolvePrayerLocationMode(
            prefs.getString("prayer_location_mode", null),
            prefs.getString("gps_latitude", null)?.toDoubleOrNull() != null &&
                prefs.getString("gps_longitude", null)?.toDoubleOrNull() != null,
            prefs.getString("manual_city_latitude", null)?.toDoubleOrNull() != null &&
                prefs.getString("manual_city_longitude", null)?.toDoubleOrNull() != null
        ))
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
    var locationMessage by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            clockNow = System.currentTimeMillis()
            delay(60_000L)
        }
    }
    LaunchedEffect(selectedCity, SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(clockNow)), gpsLatitude, gpsLongitude, prefs.getString("selected_city_country", "")) {
        loadingTimes = true
        val date = Date(clockNow)
        val country = prefs.getString("selected_city_country", "") ?: ""
        prayerTimes = runCatching {
            when {
                locationMode == "gps" && gpsLatitude != null && gpsLongitude != null ->
                    ContentRepository.loadPrayerTimesAtLocation(context, gpsLatitude!!, gpsLongitude!!, date)
                locationMode == "gps" -> null
                else -> ContentRepository.loadPrayerTimes(context, selectedCity, date, country)
                    ?: ContentRepository.findCityByName(context, selectedCity, country)?.let { city ->
                        ContentRepository.loadPrayerTimesAtLocation(context, city.latitude, city.longitude, date)
                    }
            }
        }.getOrNull()
        loadingTimes = false
    }

    LaunchedEffect(selectedCity, SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(clockNow)), gpsLatitude, gpsLongitude, prefs.getString("selected_city_country", "")) {
        val tomorrowCalendar = Calendar.getInstance().apply {
            time = Date(clockNow)
            add(Calendar.DAY_OF_YEAR, 1)
        }
        val tomorrowDate = tomorrowCalendar.time
        val country = prefs.getString("selected_city_country", "") ?: ""
        tomorrowPrayerTimes = runCatching {
            when {
                locationMode == "gps" && gpsLatitude != null && gpsLongitude != null ->
                    ContentRepository.loadPrayerTimesAtLocation(context, gpsLatitude!!, gpsLongitude!!, tomorrowDate)
                locationMode == "gps" -> null
                else -> ContentRepository.loadPrayerTimes(context, selectedCity, tomorrowDate, country)
                    ?: ContentRepository.findCityByName(context, selectedCity, country)?.let { city ->
                        ContentRepository.loadPrayerTimesAtLocation(context, city.latitude, city.longitude, tomorrowDate)
                    }
            }
        }.getOrNull()
    }

    val saveLocationFix: (Location) -> Unit = { fix ->
        val receivedAt = System.currentTimeMillis()
        val isFreshFix = isRecentLocationFix(fix.time, receivedAt, 60_000L)
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
            .putBoolean("prayer_location_prompted", true)
            .apply()
        locationMessage = if (!isFreshFix) {
            val ageMinutes = ((receivedAt - fix.time).coerceAtLeast(0L) / 60_000L)
            when (language) {
                AppLanguage.EN -> "Using last known location (~$ageMinutes min old) while refreshing…"
                AppLanguage.AR -> "يُستخدم آخر موقع محفوظ (عمره نحو $ageMinutes دقيقة) أثناء التحديث…"
                AppLanguage.CKB -> "شوێنی پێشوو بەکاردێت (نزیکەی $ageMinutes خولەک) تا نوێ بکرێتەوە…"
            }
        } else if (fix.hasAccuracy()) {
            val accuracy = fix.accuracy.toInt()
            when (language) {
                AppLanguage.EN -> "Live location fix • accuracy ±$accuracy m"
                AppLanguage.AR -> "تم تحديث الموقع مباشرة • الدقة ±$accuracy متر"
                AppLanguage.CKB -> "شوێنی ڕاستەوخۆ نوێکرایەوە • وردبینی ±$accuracy مەتر"
            }
        } else {
            when (language) {
                AppLanguage.EN -> "Live location updated."
                AppLanguage.AR -> "تم تحديث الموقع مباشرة."
                AppLanguage.CKB -> "شوێنی ڕاستەوخۆ نوێکرایەوە."
            }
        }
    }
    val handleLocationError: (String) -> Unit = { error ->
        locationMessage = when (error) {
            "LOCATION_LAST_KNOWN_ONLY" -> when (language) {
                AppLanguage.EN -> "The recent saved location is shown, but Android could not refresh it. Turn on Location and retry."
                AppLanguage.AR -> "الموقع المحفوظ حديثًا ظاهر، لكن Android لم يستطع تحديثه. فعّل الموقع ثم أعد المحاولة."
                AppLanguage.CKB -> "شوێنی پاشەکەوتکراوی نزیک پیشان دەدرێت، بەڵام ئەندرۆید نوێی نەکردەوە؛ شوێن چالاک بکە و دووبارە هەوڵ بدەوە."
            }
            "LOCATION_SETTINGS_OPENED" -> when (language) {
                AppLanguage.EN -> "Location settings opened. Turn on Location, return here, then retry."
                AppLanguage.AR -> "فُتحت إعدادات الموقع. فعّل الموقع، ثم ارجع إلى التطبيق وأعد المحاولة."
                AppLanguage.CKB -> "ڕێکخستنەکانی شوێن کرانەوە؛ شوێن چالاک بکە، بگەڕێوە و دووبارە هەوڵ بدەوە."
            }
            "LOCATION_SERVICES_OFF" -> when (language) {
                AppLanguage.EN -> "Location services are off. Turn them on in Android settings."
                AppLanguage.AR -> "خدمات الموقع متوقفة. فعّلها من إعدادات Android."
                AppLanguage.CKB -> "خزمەتگوزاری شوێن ناچالاکە؛ لە ڕێکخستنەکانی Android چالاکی بکە."
            }
            else -> when (language) {
                AppLanguage.EN -> "Could not obtain location. Choose your city manually."
                AppLanguage.AR -> "تعذر تحديد الموقع. اختر مدينتك يدويًا."
                AppLanguage.CKB -> "نەتوانرا شوێن دیاری بکرێت؛ شارەکەت بە دەستی هەڵبژێرە."
            }
        }
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        prefs.edit().putBoolean("prayer_location_prompted", true).apply()
        val allowed = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (allowed) requestCurrentLocation(context, saveLocationFix, handleLocationError)
        else locationMessage = when (language) {
            AppLanguage.EN -> "Location permission was denied. Choose your city in prayer settings."
            AppLanguage.AR -> "رُفض إذن الموقع. اختر مدينتك من إعدادات الصلاة."
            AppLanguage.CKB -> "مۆڵەتی شوێن ڕەتکرایەوە؛ لە ڕێکخستنەکانی نوێژ شارەکەت هەڵبژێرە."
        }
    }
    val requestHomeLocation: () -> Unit = {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (fine || coarse) requestCurrentLocation(context, saveLocationFix, handleLocationError)
        else locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }
    LaunchedEffect(Unit) {
        // An explicit city choice is authoritative until the user taps "Use my
        // location". Do not silently replace a manually selected Kurdistan city
        // with GPS just because the saved GPS coordinates are empty.
        val manualLatitude = prefs.getString("manual_city_latitude", null)?.toDoubleOrNull()
        val manualLongitude = prefs.getString("manual_city_longitude", null)?.toDoubleOrNull()
        if (shouldAutoRequestLocation(gpsLatitude, gpsLongitude, selectedCity, manualLatitude, manualLongitude, locationMode)) {
            val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val previouslyPrompted = prefs.getBoolean("prayer_location_prompted", false)
            if (fine || coarse) {
                // GPS is the selected source but no fresh fix exists: acquire it, do not fall back to a saved city.
                requestHomeLocation()
            } else if (!previouslyPrompted) {
                prefs.edit().putBoolean("prayer_location_prompted", true).apply()
                requestHomeLocation()
            } else if (locationMode == "gps" && (gpsLatitude == null || gpsLongitude == null)) {
                locationMessage = when (language) {
                    AppLanguage.EN -> "Current location is selected but Android location permission is off. Allow location or choose a city in Prayer settings."
                    AppLanguage.AR -> "الموقع الحالي محدد لكن إذن الموقع في Android متوقف. اسمح بالموقع أو اختر مدينة من إعدادات الصلاة."
                    AppLanguage.CKB -> "شوێنی ئێستا هەڵبژێردراوە بەڵام مۆڵەتی شوێنی ئەندرۆید ناچالاکە؛ مۆڵەت بدە یان لە ڕێکخستنەکانی نوێژ شارێک هەڵبژێرە."
                }
            }
        }
    }

    val prayerEntries = prayerTimes?.let { times ->
        listOf(
            Triple("fajr", tr(language, "fajr"), formatPrayerTime(times.fajr, language, use24Hour)),
            Triple("sunrise", tr(language, "sunrise"), formatPrayerTime(times.sunrise, language, use24Hour)),
            Triple("dhuhr", tr(language, "dhuhr"), formatPrayerTime(times.dhuhr, language, use24Hour)),
            Triple("asr", tr(language, "asr"), formatPrayerTime(times.asr, language, use24Hour)),
            Triple("maghrib", tr(language, "maghrib"), formatPrayerTime(times.maghrib, language, use24Hour)),
            Triple("isha", tr(language, "isha"), formatPrayerTime(times.isha, language, use24Hour))
        )
    }.orEmpty()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth().clickable { onOpen(AppScreen.PRAYER) },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = cardColor),
            border = BorderStroke(1.dp, Gold.copy(alpha = 0.35f))
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(Gold.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Schedule, contentDescription = null, tint = Gold, modifier = Modifier.size(23.dp))
                    }
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            when (language) { AppLanguage.EN -> "Prayer times"; AppLanguage.AR -> "مواقيت الصلاة"; AppLanguage.CKB -> "کاتەکانی نوێژ" },
                            color = foreground, fontWeight = FontWeight.Bold, fontSize = 17.sp
                        )
                        val nearestSourceCity = prayerTimes?.city?.takeIf { gpsLatitude != null && gpsLongitude != null }
                            ?.let { sourceCity -> localizedKurdistanCityName(sourceCity, language.key).ifBlank { sourceCity } }
                        val displayedLocation = if (locationMode == "gps") {
                            if (gpsLatitude != null && gpsLongitude != null) {
                                val coordinates = String.format(Locale.US, "%.4f, %.4f", gpsLatitude, gpsLongitude)
                                when (language) {
                                    AppLanguage.EN -> if (nearestSourceCity != null) "Your GPS position • nearest Salawati schedule: $nearestSourceCity" else "Current GPS coordinates: $coordinates"
                                    AppLanguage.AR -> if (nearestSourceCity != null) "موقعك عبر GPS • أقرب جدول صلواتي: $nearestSourceCity" else "إحداثيات GPS الحالية: $coordinates"
                                    AppLanguage.CKB -> if (nearestSourceCity != null) "شوێنی GPSی تۆ • نزیکترین خشتەی صەڵەواتی: $nearestSourceCity" else "کۆئۆردیناتی GPSی ئێستا: $coordinates"
                                }
                            } else when (language) {
                                AppLanguage.EN -> "Getting your current location…"
                                AppLanguage.AR -> "جارٍ تحديد موقعك الحالي…"
                                AppLanguage.CKB -> "شوێنی ئێستات دیاری دەکرێت…"
                            }
                        } else localizedKurdistanCityName(selectedCity, language.key).ifBlank {
                            when (language) {
                                AppLanguage.EN -> "Choose a city or use your location"
                                AppLanguage.AR -> "اختر مدينة أو استخدم موقعك"
                                AppLanguage.CKB -> "شارێک هەڵبژێرە یان شوێنی خۆت بەکاربهێنە"
                            }
                        }
                        val sourceLabel = prayerTimes?.source?.let { prayerSourceDescription(it, language) }.orEmpty()
                        Column(Modifier.weight(1f)) {
                            Text(
                                displayedLocation,
                                color = foreground.copy(alpha = 0.64f), fontSize = 10.sp, lineHeight = 14.sp, maxLines = 2
                            )
                            if (sourceLabel.isNotBlank()) {
                                Text(
                                    sourceLabel,
                                    color = foreground.copy(alpha = 0.46f), fontSize = 9.sp, lineHeight = 13.sp, maxLines = 2
                                )
                            }
                        }
                    }
                    Icon(Icons.Outlined.Explore, contentDescription = null, tint = Gold)
                }
                Spacer(Modifier.height(14.dp))
                HomePrayerCountdown(
                    today = prayerTimes,
                    tomorrow = tomorrowPrayerTimes,
                    language = language,
                    use24Hour = use24Hour,
                    foreground = foreground,
                    cardColor = cardColor,
                    onNextPrayerChange = { upcomingPrayerKey = it }
                )
                Spacer(Modifier.height(10.dp))
                when {
                    loadingTimes -> Box(Modifier.fillMaxWidth().height(46.dp), contentAlignment = Alignment.Center) {
                        androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(21.dp), color = Gold, strokeWidth = 2.dp)
                    }
                    prayerEntries.isEmpty() -> Text(
                        when {
                            gpsLatitude != null && gpsLongitude != null -> when (language) {
                                AppLanguage.EN -> "No verified Salawati timetable was found near these coordinates. Refresh location or choose a nearby supported city; the app will not invent times."
                                AppLanguage.AR -> "لم يُعثر على جدول صلواتي موثوق قريب من هذه الإحداثيات. حدّث الموقع أو اختر مدينة قريبة مدعومة؛ لن يخمّن التطبيق أوقاتًا."
                                AppLanguage.CKB -> "خشتەی پشتڕاستکراوەی صەڵەواتی لە نزیک ئەم کۆئۆردیناتانەدا نەدۆزرایەوە؛ شوێن نوێ بکەرەوە یان شاریەکی نزیک هەڵبژێرە."
                            }
                            else -> when (language) {
                                AppLanguage.EN -> "Allow location access to load the original prayer timetable for your area."
                                AppLanguage.AR -> "اسمح بالوصول إلى الموقع لتحميل جدول الصلاة الأصلي لمنطقتك."
                                AppLanguage.CKB -> "مۆڵەتی شوێن بدە بۆ بارکردنی خشتەی ڕەسەنی نوێژ بۆ ناوچەکەت."
                            }
                        }, color = foreground.copy(alpha = 0.68f), fontSize = 12.sp, lineHeight = 19.sp
                    )
                    else -> prayerEntries.chunked(3).forEach { trio ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            trio.forEach { (key, label, time) ->
                                Column(
                                    Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                                        .background(if (key == upcomingPrayerKey) Gold.copy(alpha = 0.11f) else foreground.copy(alpha = 0.035f))
                                        .padding(horizontal = 8.dp, vertical = 11.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(label, color = foreground.copy(alpha = 0.65f), fontSize = 10.sp, maxLines = 1)
                                    Spacer(Modifier.height(5.dp))
                                    Text(time, color = if (key == upcomingPrayerKey) Gold else foreground, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                }
                            }
                        }
                        if (trio.firstOrNull()?.first == "fajr") Spacer(Modifier.height(8.dp))
                    }
                }
                if (locationMessage.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(locationMessage, color = if (locationMessage.contains("±")) foreground.copy(alpha = 0.58f) else Gold, fontSize = 10.sp, lineHeight = 15.sp)
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        when (language) {
                            AppLanguage.EN -> "Use my location"
                            AppLanguage.AR -> "استخدم موقعي"
                            AppLanguage.CKB -> "شوێنی ئێستام بەکاربهێنە"
                        }, color = Gold, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { requestHomeLocation() }.padding(end = 12.dp)
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        when (language) {
                            AppLanguage.EN -> "Prayer settings"
                            AppLanguage.AR -> "إعدادات الصلاة"
                            AppLanguage.CKB -> "ڕێکخستنەکانی نوێژ"
                        }, color = foreground.copy(alpha = 0.63f), fontSize = 11.sp,
                        modifier = Modifier.clickable { onOpen(AppScreen.PRAYER) }
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Card(
            modifier = Modifier.fillMaxWidth().premiumPress { onResumeReading() },
            colors = CardDefaults.cardColors(containerColor = cardColor),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(0.8.dp, Gold.copy(alpha = 0.34f))
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(Gold.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.MenuBook, contentDescription = null, tint = Gold, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        when (language) { AppLanguage.EN -> "Continue reading"; AppLanguage.AR -> "متابعة القراءة"; AppLanguage.CKB -> "بەردەوام بە لە خوێندنەوە" },
                        color = foreground, fontSize = 13.sp, fontWeight = FontWeight.Bold
                    )
                    Text(
                        when (language) { AppLanguage.EN -> "Return to your last saved Mushaf page"; AppLanguage.AR -> "العودة إلى آخر صفحة محفوظة من المصحف"; AppLanguage.CKB -> "گەڕانەوە بۆ دوا پەڕەی مصحەفی پاشەکەوتکراو" },
                        color = foreground.copy(alpha = 0.58f), fontSize = 10.sp, lineHeight = 15.sp
                    )
                }
                Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = Gold)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(tr(language, "quick_access"), color = foreground, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(12.dp))
        homeModules.chunked(2).forEachIndexed { rowIndex, pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(11.dp), modifier = Modifier.fillMaxWidth()) {
                pair.forEachIndexed { columnIndex, module ->
                    val delayMillis = ((rowIndex * 2 + columnIndex) * 36).coerceAtMost(480)
                    AnimatedVisibility(
                        visible = true,
                        modifier = Modifier.weight(1f).padding(bottom = 10.dp),
                        enter = fadeIn(animationSpec = tween(durationMillis = 520, delayMillis = delayMillis)) +
                            slideInVertically(initialOffsetY = { height -> height / 7 }, animationSpec = tween(durationMillis = 520, delayMillis = delayMillis))
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth().animateContentSize().premiumPress { onOpen(module.screen) },
                            shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = cardColor),
                            border = BorderStroke(0.8.dp, module.color.copy(alpha = 0.42f))
                        ) {
                            Column(
                                Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(module.color.copy(alpha = 0.10f), cardColor, cardColor))).padding(14.dp).height(104.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(module.color.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                                    Icon(module.icon, contentDescription = null, tint = module.color, modifier = Modifier.size(23.dp))
                                }
                                Column {
                                    Text(tr(language, module.titleKey), color = foreground, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    Spacer(Modifier.height(2.dp))
                                    Text(tr(language, module.subtitleKey), color = foreground.copy(alpha = 0.62f), fontSize = 10.sp, maxLines = 1)
                                }
                            }
                        }
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(8.dp))
        Card(colors = CardDefaults.cardColors(containerColor = cardColor), shape = RoundedCornerShape(22.dp)) {
            Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Language, tint = Gold, contentDescription = null, modifier = Modifier.size(27.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(tr(language, "offline"), color = foreground, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text(tr(language, "data_notice"), color = foreground.copy(alpha = 0.56f), fontSize = 10.sp, lineHeight = 15.sp)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
fun LibraryScreen(language: AppLanguage, foreground: Color, cardColor: Color, onOpen: (AppScreen) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(
            tr(language, "about_data"),
            color = foreground, fontSize = 24.sp, fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            when (language) {
                AppLanguage.EN -> "Source assets from Noor Tafsir, Khairah and Salawati are catalogued separately, including Quran and tafsir data, Hadith, word analysis, lexicons, prayer data, adhkar, books, fonts and bundled audio where supplied."
                AppLanguage.AR -> "تُفهرس أصول نور وخيرة وصلواتي كلٌّ بحسب مصدره، بما يشمل بيانات القرآن والتفسير والأحاديث وتحليل الكلمات والمعاجم ومواقيت الصلاة والأذكار والكتب والخطوط والصوتيات المرفقة عند توفرها."
                AppLanguage.CKB -> "سەرچاوەکانی نوور و خێرە و صەڵەواتی بە جیا فهرست کراون؛ لەوانە داتای قورئان و تەفسیر، فەرموودە، شیکردنەوەی وشە، فەرهەنگ، کاتی نوێژ، زیکر، کتێب، فۆنت و دەنگی ناوخۆیی ئەگەر لە سەرچاوەکەدا هەبێت."
            }, color = foreground.copy(alpha = 0.62f), fontSize = 13.sp, lineHeight = 21.sp
        )
        Spacer(Modifier.height(18.dp))
        Spacer(Modifier.height(18.dp))
        Text(tr(language, "about_data"), color = foreground, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(tr(language, "data_notice"), color = foreground.copy(alpha = 0.62f), fontSize = 12.sp, lineHeight = 19.sp)
    }
}

@Composable
fun WorshipScreen(language: AppLanguage, foreground: Color, cardColor: Color, onOpen: (AppScreen) -> Unit) {
    val worshipModules = listOf(
        AppScreen.AZKAR to Icons.Outlined.Spa,
        AppScreen.FARMUDA to Icons.Outlined.VolunteerActivism,
        AppScreen.NAMES to Icons.Outlined.Favorite,
        AppScreen.TASBIH to Icons.Outlined.Spa
    )
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(
            when (language) { AppLanguage.EN -> "Worship"; AppLanguage.AR -> "العبادات والأذكار"; AppLanguage.CKB -> "پەرستش و زیکر" },
            color = foreground, fontSize = 24.sp, fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            when (language) {
                AppLanguage.EN -> "Each topic opens its own focused reading screen. Audio libraries are managed separately in settings."
                AppLanguage.AR -> "يفتح كل موضوع شاشة مستقلة للقراءة؛ أما مكتبات الصوت فتُدار من الإعدادات."
                AppLanguage.CKB -> "هەر بابەتێک شاشەی تایبەتی خۆی هەیە؛ کتێبخانەی دەنگ لە ڕێکخستنەکانەوە بەڕێوەدەبرێت."
            }, color = foreground.copy(alpha = 0.58f), fontSize = 12.sp, lineHeight = 19.sp
        )
        Spacer(Modifier.height(16.dp))
        worshipModules.forEach { (target, icon) ->
            Card(
                Modifier.fillMaxWidth().padding(bottom = 10.dp).clickable { onOpen(target) },
                colors = CardDefaults.cardColors(containerColor = cardColor),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(0.8.dp, Gold.copy(alpha = 0.2f))
            ) {
                Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(Gold.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = Gold, modifier = Modifier.size(26.dp))
                    }
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f)) {
                        Text(tr(language, target.key), color = foreground, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(tr(language, target.key + "_sub"), color = foreground.copy(alpha = 0.56f), fontSize = 11.sp)
                    }
                    Icon(Icons.Outlined.ChevronRight, null, tint = Gold.copy(alpha = 0.8f))
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    language: AppLanguage,
    foreground: Color,
    cardColor: Color,
    dark: Boolean,
    accentPalette: String,
    use24Hour: Boolean,
    onAccent: (String) -> Unit,
    onDark: (Boolean) -> Unit,
    onLanguage: (AppLanguage) -> Unit,
    onTimeFormatChange: (Boolean) -> Unit,
    onDataInfo: () -> Unit,
    onBackup: () -> Unit,
    onAudio: () -> Unit,
    onAdhan: () -> Unit,
    onPrayer: () -> Unit,
    onQuran: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("mashxall_preferences", 0) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(tr(language, "settings"), color = foreground, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            when (language) {
                AppLanguage.EN -> "Personalize reading, prayer, audio and the appearance of Mashxall."
                AppLanguage.AR -> "خصّص القراءة ومواقيت الصلاة والصوتيات ومظهر مەشخەڵ."
                AppLanguage.CKB -> "خوێندنەوە، کاتی نوێژ، دەنگ و ڕووکاری مەشخەڵ بەپێی دڵخوازی خۆت ڕێکبخە."
            }, color = foreground.copy(alpha = 0.62f), fontSize = 12.sp, lineHeight = 19.sp
        )
        Spacer(Modifier.height(18.dp))

        Text(
            when (language) { AppLanguage.EN -> "LANGUAGE"; AppLanguage.AR -> "اللغة"; AppLanguage.CKB -> "زمان" },
            color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp
        )
        Spacer(Modifier.height(9.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            AppLanguage.entries.forEach { item ->
                FilterChip(
                    selected = item == language,
                    onClick = { onLanguage(item) },
                    label = { Text(item.label, fontSize = 12.sp) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            when (language) { AppLanguage.EN -> "ACCENT COLOUR"; AppLanguage.AR -> "لون التمييز"; AppLanguage.CKB -> "ڕەنگی جیاکەرەوە" },
            color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            when (language) {
                AppLanguage.EN -> "This changes the actual highlight, icon, button and reading-action colour across the app."
                AppLanguage.AR -> "يغيّر لون الإبراز والأيقونات والأزرار وإجراءات القراءة في التطبيق."
                AppLanguage.CKB -> "ڕەنگی دەستنیشانکردن، ئایکۆن، دوگمە و کرداری خوێندنەوە لە هەموو ئەپەکەدا دەگۆڕێت."
            }, color = foreground.copy(alpha = 0.58f), fontSize = 11.sp, lineHeight = 17.sp
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            items(listOf("gold", "emerald", "sapphire", "violet", "rose", "turquoise")) { palette ->
                val label = when (language) {
                    AppLanguage.EN -> when (palette) {
                        "emerald" -> "Emerald"; "sapphire" -> "Blue"; "violet" -> "Violet"; "rose" -> "Rose"; "turquoise" -> "Teal"; else -> "Gold"
                    }
                    AppLanguage.AR -> when (palette) {
                        "emerald" -> "زمردي"; "sapphire" -> "أزرق"; "violet" -> "بنفسجي"; "rose" -> "وردي"; "turquoise" -> "فيروزي"; else -> "ذهبي"
                    }
                    AppLanguage.CKB -> when (palette) {
                        "emerald" -> "زەمرەدی"; "sapphire" -> "شین"; "violet" -> "مۆر"; "rose" -> "پەمەیی"; "turquoise" -> "فیرۆزەیی"; else -> "زێڕین"
                    }
                }
                FilterChip(
                    selected = accentPalette == palette,
                    onClick = { onAccent(palette) },
                    label = { Text(label, fontSize = 11.sp) },
                    leadingIcon = {
                        Box(Modifier.size(11.dp).clip(CircleShape).background(MashxallAppearance.colorFor(palette)))
                    }
                )
            }
        }
        Spacer(Modifier.height(18.dp))

        Text(
            when (language) { AppLanguage.EN -> "APPEARANCE"; AppLanguage.AR -> "المظهر"; AppLanguage.CKB -> "ڕووکار" },
            color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp
        )
        Spacer(Modifier.height(9.dp))
        Card(
            modifier = Modifier.fillMaxWidth().clickable { onDark(!dark) },
            colors = CardDefaults.cardColors(containerColor = cardColor), shape = RoundedCornerShape(18.dp)
        ) {
            Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Tune, null, tint = Gold, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        when (language) { AppLanguage.EN -> "Dark / light theme"; AppLanguage.AR -> "المظهر الداكن / الفاتح"; AppLanguage.CKB -> "ڕووکاری تاریک / ڕوون" },
                        color = foreground, fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        when (language) { AppLanguage.EN -> "Switch the app-wide palette"; AppLanguage.AR -> "تغيير ألوان التطبيق بالكامل"; AppLanguage.CKB -> "گۆڕینی ڕەنگی گشتی ئەپەکە" },
                        color = foreground.copy(alpha = 0.55f), fontSize = 11.sp
                    )
                }
                FilterChip(selected = dark, onClick = { onDark(!dark) }, label = { Text(if (dark) "ON" else "OFF") })
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            when (language) { AppLanguage.EN -> "TIME FORMAT"; AppLanguage.AR -> "تنسيق الوقت"; AppLanguage.CKB -> "فۆرماتی کات" },
            color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            when (language) {
                AppLanguage.EN -> "Prayer times default to 12-hour display. Choose 24-hour display only if you prefer it."
                AppLanguage.AR -> "عرض المواقيت الافتراضي هو 12 ساعة؛ اختر 24 ساعة إذا كنت تفضّل ذلك."
                AppLanguage.CKB -> "پیشاندانی کاتەکانی نوێژ بە بنەڕەت ١٢ کاتژمێرە؛ ئەگەر پێت باشە ٢٤ کاتژمێر هەڵبژێرە."
            },
            color = foreground.copy(alpha = 0.58f), fontSize = 11.sp, lineHeight = 17.sp
        )
        Spacer(Modifier.height(9.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = cardColor),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                Text(
                    when (language) { AppLanguage.EN -> "Time display"; AppLanguage.AR -> "طريقة عرض الوقت"; AppLanguage.CKB -> "شێوازی پیشاندانی کات" },
                    color = foreground, fontWeight = FontWeight.SemiBold, fontSize = 13.sp
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = !use24Hour,
                        onClick = { onTimeFormatChange(false) },
                        label = {
                            Text(
                                when (language) { AppLanguage.EN -> "12-hour"; AppLanguage.AR -> "12 ساعة"; AppLanguage.CKB -> "١٢ کاتژمێر" },
                                fontSize = 11.sp
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = use24Hour,
                        onClick = { onTimeFormatChange(true) },
                        label = {
                            Text(
                                when (language) { AppLanguage.EN -> "24-hour"; AppLanguage.AR -> "24 ساعة"; AppLanguage.CKB -> "٢٤ کاتژمێر" },
                                fontSize = 11.sp
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            when (language) { AppLanguage.EN -> "READING & WORSHIP"; AppLanguage.AR -> "القراءة والعبادة"; AppLanguage.CKB -> "خوێندنەوە و پەرستش" },
            color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp
        )
        Spacer(Modifier.height(9.dp))
        val actions = listOf(
            Triple(
                when (language) { AppLanguage.EN -> "Quran reader settings"; AppLanguage.AR -> "إعدادات قارئ القرآن"; AppLanguage.CKB -> "ڕێکخستنەکانی خوێندنەوەی قورئان" },
                when (language) { AppLanguage.EN -> "Mushaf, translations, tafsir and verse tools"; AppLanguage.AR -> "المصحف والترجمات والتفسير وأدوات الآية"; AppLanguage.CKB -> "مصحەف، وەرگێڕان، تەفسیر و ئامرازەکانی ئایەت" },
                Icons.Outlined.MenuBook to onQuran
            ),
            Triple(
                when (language) { AppLanguage.EN -> "Prayer & location"; AppLanguage.AR -> "الصلاة والموقع"; AppLanguage.CKB -> "نوێژ و شوێن" },
                when (language) { AppLanguage.EN -> "Original Salawati timetables, device location and adhan alerts"; AppLanguage.AR -> "جداول صلواتي الأصلية وموقع الهاتف وتنبيهات الأذان"; AppLanguage.CKB -> "خشتە ڕەسەنەکانی صەڵەواتی، شوێنی مۆبایل و ئاگادارکردنەوەی بانگ" },
                Icons.Outlined.Schedule to onPrayer
            ),
            Triple(
                when (language) { AppLanguage.EN -> "Quran reciters"; AppLanguage.AR -> "قرّاء القرآن"; AppLanguage.CKB -> "قورئان‌خوێنەکان" },
                when (language) { AppLanguage.EN -> "Choose a reciter and full-surah or verse-by-verse playback"; AppLanguage.AR -> "اختر القارئ والتشغيل آيةً بآية أو السورة كاملة"; AppLanguage.CKB -> "قورئان‌خوێن و لێدانی ئایەت بە ئایەت یان سوورەتی تەواو هەڵبژێرە" },
                Icons.Outlined.GraphicEq to onAudio
            ),
            Triple(
                when (language) { AppLanguage.EN -> "Adhan & muezzins"; AppLanguage.AR -> "الأذان والمؤذنون"; AppLanguage.CKB -> "بانگ و موئەزینەکان" },
                when (language) { AppLanguage.EN -> "Open the separate adhan and mosque-audio collection"; AppLanguage.AR -> "افتح مجموعة الأذان وأصوات المساجد بشكل مستقل"; AppLanguage.CKB -> "کۆمەڵەی بانگ و دەنگی مزگەوت بە جیاوازی بکەرەوە" },
                Icons.Outlined.NotificationsActive to onAdhan
            ),
            Triple(
                when (language) { AppLanguage.EN -> "Backup & restore"; AppLanguage.AR -> "النسخ الاحتياطي والاستعادة"; AppLanguage.CKB -> "پاشەکەوتکردن و گەڕاندنەوە" },
                when (language) { AppLanguage.EN -> "Protect personal notes, bookmarks and preferences"; AppLanguage.AR -> "حماية الملاحظات والعلامات والإعدادات الشخصية"; AppLanguage.CKB -> "پاراستنی تێبینی، نیشانە و ڕێکخستنە تایبەتییەکان" },
                Icons.Outlined.BookmarkBorder to onBackup
            ),
            Triple(
                when (language) { AppLanguage.EN -> "Data sources & rights"; AppLanguage.AR -> "مصادر البيانات والحقوق"; AppLanguage.CKB -> "سەرچاوەی داتا و مافەکان" },
                when (language) { AppLanguage.EN -> "Source details, licensing and offline availability"; AppLanguage.AR -> "تفاصيل المصادر والتراخيص والمحتوى المتاح دون اتصال"; AppLanguage.CKB -> "وردەکاری سەرچاوەکان، مۆڵەت و ناوەڕۆکی بێ ئینتەرنێت" },
                Icons.Outlined.Info to onDataInfo
            )
        )
        actions.forEach { (title, subtitle, item) ->
            val (icon, callback) = item
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 9.dp).clickable { callback() },
                colors = CardDefaults.cardColors(containerColor = cardColor),
                shape = RoundedCornerShape(19.dp),
                border = BorderStroke(0.7.dp, Gold.copy(alpha = 0.18f))
            ) {
                Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(Gold.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = Gold, modifier = Modifier.size(23.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(title, color = foreground, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(3.dp))
                        Text(subtitle, color = foreground.copy(alpha = 0.58f), fontSize = 10.sp, lineHeight = 15.sp)
                    }
                    Text("›", color = Gold, fontSize = 25.sp)
                }
            }
        }
    }
}

