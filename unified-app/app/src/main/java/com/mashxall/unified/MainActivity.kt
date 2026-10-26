package com.mashxall.unified

import android.Manifest
import android.content.Context
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Ink = Color(0xFF071E1A)
private val DeepGreen = Color(0xFF103D32)
private val Emerald = Color(0xFF19725A)
private val PaleGold = Color(0xFFF4E5B4)
private val SoftText = Color(0xFFB9CDC5)
private val Panel = Color(0xFF102B25)

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

enum class AppScreen(val key: String) {
    HOME("home"), QURAN("quran"), AZKAR("azkar"), MORE("more"), SETTINGS("settings"),
    PRAYER("prayer"), QIBLA("qibla"), HADITH("hadith"), FARMUDA("farmuda"),
    NAMES("names"), TASBIH("tasbih"), AUDIO("audio"), BOOKMARKS("bookmarks"),
    LEXICON("lexicon"), TAFSIR("tafsir"), WORD_BY_WORD("word_by_word"),
    BACKUP("backup"),
    // Legacy destinations remain recognized only so old links can be normalized
    // to More; they are no longer visible as navigation items.
    LIBRARY("library"), BOOKS("books"), WORSHIP("worship")

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
        "more" to "زیاتر", "home" to "سەرەتا", "quran" to "قورئانی پیرۆز", "quran_sub" to "خوێندنەوە و گەڕان", "tafsir" to "تەفسیرو وەرگێڕان", "tafsir_sub" to "چەندین تەفسیر", "hadith" to "فەرموودەکان", "hadith_sub" to "گەڕان لە فەرموودە", "azkar" to "زیکر و دوعاکان", "azkar_sub" to "بەیانی، ئێواران و زیاتر", "prayer" to "کاتەکانی بانگ", "prayer_sub" to "کاتی نوێژ و شار", "audio" to "دەنگ و تلاوەت", "audio_sub" to "فایلە دەنگییە ناوخۆییەکان", "names" to "ناوە پیرۆزەکان", "names_sub" to "٩٩ ناوی خودا", "qibla" to "ئاڕاستەی قیبلە", "qibla_sub" to "ئاڕاستە بەپێی شوێن", "books" to "کتێبخانە", "books_sub" to "کتێب و PDF", "lexicon" to "فەرهەنگی وشەکان", "lexicon_sub" to "مانا و ڕەگی وشە", "word_by_word" to "شیکردنەوەی وشە بە وشە", "word_by_word_sub" to "ڕەگ و ڕێزمانی قورئان", "farmuda" to "فەرموودە و دوعاکان", "farmuda_sub" to "دەق و زانیاری زیادە", "tasbih" to "تەسبیح", "tasbih_sub" to "ژماردنی زیکر", "library" to "کتێبخانەی کتێب", "library_sub" to "کتێب و PDF", "welcome" to "هەموو زانیارییە ئیسلامییەکانت لە شوێنێکدا", "bookmarks" to "نیشانکراوەکان", "bookmarks_sub" to "ئایەتە پاشەکەوتکراوەکان", "surah" to "سوورەت", "ayah" to "ئایەت", "verse" to "ئایەت", "book_online" to "زانیاری کتێب", "mushaf_view" to "مصحەفی پیرۆز", "verse_view" to "ئایەت بە ئایەت", "mushaf_unavailable" to "نەتوانرا پەڕەی مصحف بخوێندرێتەوە", "mushaf_offline" to "٦٠٤ پەڕەی قورئان بەبێ ئینتەرنێت", "no_bookmarks" to "هیچ ئایەتێک نیشان نەکراوە", "no_adhan_audio" to "دەنگی بانگ لەم داتایەدا نەدۆزرایەوە", "page" to "پەڕە", "previous" to "پێشوو", "next" to "دواتر", "open_pdf" to "کردنەوەی PDF", "settings" to "ڕێکخستن", "back" to "گەڕانەوە", "home_title" to "دڵت ڕووناک بکە", "home_hint" to "ئەمڕۆ بە چەند خولەکێک خوێندنەوە دەست پێ بکە", "quick_access" to "دەستگەیشتنی خێرا", "featured" to "پێشنیاری ئەمڕۆ", "continue_reading" to "بەردەوام بە لە خوێندنەوە", "language" to "زمان", "offline" to "زۆربەی زانیارییەکان بەبێ ئینتەرنێت کار دەکەن", "about_data" to "سەرچاوەی داتا و مافەکان", "data_notice" to "هەندێک کتێب پێویستی بە ئینتەرنێت یان شیکردنەوەی تایبەت هەیە.", "search" to "گەڕان...", "choose_surah" to "سوورەت هەڵبژێرە", "select_tafsir" to "تەفسیر هەڵبژێرە", "search_hadith" to "لە فەرموودە بگەڕێ...", "search_words" to "وشە یان ڕەگ بگەڕێ...", "city" to "شار", "select_city" to "شار هەڵبژێرە", "fajr" to "بەیانی", "sunrise" to "هەڵهاتنی خۆر", "dhuhr" to "نیوەڕۆ", "asr" to "عەسر", "maghrib" to "مەغریب", "isha" to "عیشا", "search_azkar" to "لە زیکرەکان بگەڕێ...", "arabic" to "عەرەبی", "translation" to "وەرگێڕان", "reference" to "سەرچاوە", "reset" to "سفرکردنەوە", "play" to "لێدان", "available_data" to "داتای بەردەست", "no_data" to "داتاکان بارنەکراون. تکایە وەشانی data-enabled دروست بکە.", "not_found" to "هیچ ئەنجامێک نەدۆزرایەوە.", "qibla_heading" to "قیبلە لە ڕووی باکوورەوە", "degrees" to "پلە", "library_hint" to "قورئان، حدیث، تەفسیر، کتێب و دەنگ"
    ),
    "ar" to mapOf(
        "more" to "المزيد", "home" to "الرئيسية", "quran" to "القرآن الكريم", "quran_sub" to "قراءة وبحث", "tafsir" to "التفسير والترجمة", "tafsir_sub" to "تفاسير متعددة", "hadith" to "الأحاديث", "hadith_sub" to "البحث في الأحاديث", "azkar" to "الأذكار والأدعية", "azkar_sub" to "الصباح والمساء والمزيد", "prayer" to "مواقيت الصلاة", "prayer_sub" to "المواقيت والمدينة", "audio" to "التلاوات والصوتيات", "audio_sub" to "الملفات الصوتية المحلية", "names" to "أسماء الله الحسنى", "names_sub" to "الأسماء التسعة والتسعون", "qibla" to "اتجاه القبلة", "qibla_sub" to "الاتجاه حسب المدينة", "books" to "المكتبة", "books_sub" to "كتب وملفات PDF", "lexicon" to "معجم الكلمات", "lexicon_sub" to "المعاني والجذور", "word_by_word" to "تفسير كلمة بكلمة", "word_by_word_sub" to "الجذور والتحليل النحوي", "farmuda" to "المأثورات والأدعية", "farmuda_sub" to "أدعية ونصوص إضافية", "tasbih" to "المسبحة", "tasbih_sub" to "عدّ الذكر", "library" to "مكتبة الكتب", "library_sub" to "كتب وملفات PDF", "welcome" to "مكتبتك الإسلامية في مكان واحد", "bookmarks" to "العلامات المرجعية", "bookmarks_sub" to "الآيات المحفوظة", "surah" to "السورة", "ayah" to "الآية", "verse" to "آية", "book_online" to "معلومات الكتاب", "mushaf_view" to "عرض المصحف", "verse_view" to "عرض الآيات", "mushaf_unavailable" to "تعذر فتح صفحة المصحف. تحقق من ملفات البيانات.", "mushaf_offline" to "٦٠٤ صفحة من المصحف تعمل دون إنترنت", "no_bookmarks" to "لا توجد آيات محفوظة بعد", "no_adhan_audio" to "لا توجد ملفات أذان ضمن البيانات المتاحة", "page" to "صفحة", "previous" to "السابق", "next" to "التالي", "open_pdf" to "فتح PDF", "settings" to "الإعدادات", "back" to "رجوع", "home_title" to "أضئ قلبك بالمعرفة", "quick_access" to "وصول سريع", "featured" to "اختيار اليوم", "continue_reading" to "متابعة القراءة", "language" to "اللغة", "offline" to "تعمل معظم المكتبة دون اتصال بالإنترنت", "about_data" to "مصادر البيانات والحقوق", "data_notice" to "بعض الكتب تتطلب الإنترنت أو معالجة خاصة.", "search" to "بحث...", "choose_surah" to "اختر السورة", "select_tafsir" to "اختر التفسير", "search_hadith" to "ابحث في الأحاديث...", "search_words" to "ابحث عن كلمة أو جذر...", "city" to "المدينة", "select_city" to "اختر المدينة", "fajr" to "الفجر", "sunrise" to "الشروق", "dhuhr" to "الظهر", "asr" to "العصر", "maghrib" to "المغرب", "isha" to "العشاء", "search_azkar" to "ابحث في الأذكار...", "arabic" to "العربية", "translation" to "الترجمة", "reference" to "المصدر", "reset" to "إعادة", "play" to "تشغيل", "available_data" to "البيانات المتاحة", "no_data" to "لم تُحمّل البيانات بعد. أنشئ نسخة data-enabled من GitHub Actions.", "not_found" to "لم يتم العثور على نتائج.", "qibla_heading" to "اتجاه القبلة من الشمال", "degrees" to "درجة", "library_hint" to "القرآن، الحديث، التفسير، الكتب والصوتيات"
    ),
    "en" to mapOf(
        "more" to "More", "home" to "Home", "quran" to "The Holy Quran", "quran_sub" to "Read and search", "tafsir" to "Tafsir & translation", "tafsir_sub" to "Multiple commentaries", "hadith" to "Hadith", "hadith_sub" to "Search narrations", "azkar" to "Adhkar & duas", "azkar_sub" to "Morning, evening and more", "prayer" to "Prayer times", "prayer_sub" to "Times and city", "audio" to "Recitations & audio", "audio_sub" to "Bundled local media", "names" to "Names of Allah", "names_sub" to "The 99 names", "qibla" to "Qibla direction", "qibla_sub" to "Direction by selected city", "books" to "Book library", "books_sub" to "Books and PDFs", "lexicon" to "Word lexicon", "lexicon_sub" to "Meanings and roots", "word_by_word" to "Word-by-word analysis", "word_by_word_sub" to "Roots and Quranic grammar", "farmuda" to "Supplications & sayings", "farmuda_sub" to "Additional texts and duas", "tasbih" to "Digital tasbih", "tasbih_sub" to "Count remembrance", "library" to "Book library", "library_sub" to "Books and PDFs", "welcome" to "Your Islamic library in one place", "bookmarks" to "Bookmarks", "bookmarks_sub" to "Saved verses", "surah" to "Surah", "ayah" to "Ayah", "verse" to "verses", "book_online" to "Book details", "mushaf_view" to "Mushaf pages", "verse_view" to "Verse list", "mushaf_unavailable" to "Could not read this Mushaf page; verify the bundled data.", "mushaf_offline" to "All 604 Mushaf pages work offline", "no_bookmarks" to "No saved verses yet", "no_adhan_audio" to "No adhan-specific audio was found in the supplied data", "page" to "Page", "previous" to "Previous", "next" to "Next", "open_pdf" to "Open PDF", "settings" to "Settings", "back" to "Back", "home_title" to "Let knowledge light your heart", "quick_access" to "Quick access", "featured" to "Today's pick", "continue_reading" to "Continue reading", "language" to "Language", "offline" to "Most of the library works offline", "about_data" to "Data sources & rights", "data_notice" to "Some books need internet access or format-specific processing.", "search" to "Search...", "choose_surah" to "Choose a surah", "select_tafsir" to "Choose tafsir", "search_hadith" to "Search Hadith...", "search_words" to "Search words or roots...", "city" to "City", "select_city" to "Choose city", "fajr" to "Fajr", "sunrise" to "Sunrise", "dhuhr" to "Dhuhr", "asr" to "Asr", "maghrib" to "Maghrib", "isha" to "Isha", "search_azkar" to "Search adhkar...", "arabic" to "Arabic", "translation" to "Translation", "reference" to "Reference", "reset" to "Reset", "play" to "Play", "available_data" to "Available content", "no_data" to "Content data is not loaded yet. Build the data-enabled variant from GitHub Actions.", "not_found" to "No results found.", "qibla_heading" to "Qibla bearing from north", "degrees" to "degrees", "library_hint" to "Quran, Hadith, tafsir, books and audio"
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

private fun normalizeAppRoute(candidate: String?): String? {
    val route = when (candidate) {
        "library", "books", "worship" -> "more"
        else -> candidate
    }
    return route?.takeIf { key ->
        AppScreen.entries.any { it.key == key } || key == "pdf" || key == "about_data"
    }
}

@Composable
fun MashxallApp(startScreen: String? = null) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("mashxall_preferences", 0) }
    var language by rememberSaveable { mutableStateOf(AppLanguage.from(prefs.getString("language", "ckb"))) }
    var accentPalette by rememberSaveable { mutableStateOf(prefs.getString("accent_palette", "gold") ?: "gold") }
    SideEffect { MashxallAppearance.accent = MashxallAppearance.colorFor(accentPalette) }
    var screen by rememberSaveable {
        mutableStateOf(
            normalizeAppRoute(startScreen) ?: AppScreen.HOME.key
        )
    }
    var navigationStack by rememberSaveable { mutableStateOf(emptyList<String>()) }
    LaunchedEffect(startScreen) {
        val requested = normalizeAppRoute(startScreen)
        if (requested != null) {
            screen = requested
            navigationStack = emptyList()
        }
    }
    var dark by rememberSaveable { mutableStateOf(prefs.getBoolean("dark_mode", true)) }
    var selectedSurah by rememberSaveable { mutableIntStateOf(prefs.getInt("last_surah", 1).coerceIn(1, 114)) }
    var selectedAyah by rememberSaveable { mutableIntStateOf(prefs.getInt("last_ayah_number", 1).coerceAtLeast(1)) }
    var resumeMushafRequested by rememberSaveable { mutableStateOf(false) }
    var openSelectedVerseRequested by rememberSaveable { mutableStateOf(false) }
    var openQuranReaderSettingsRequested by rememberSaveable { mutableStateOf(false) }
    var audioStartsAtReciters by rememberSaveable { mutableStateOf(false) }
    var audioAutoPlayRequested by rememberSaveable { mutableStateOf(false) }
    var selectedCity by rememberSaveable { mutableStateOf(prefs.getString("selected_city", "Erbil") ?: "Erbil") }
    var selectedPdf by rememberSaveable { mutableStateOf("") }
    var immersiveMushaf by rememberSaveable { mutableStateOf(false) }
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
        val route = normalizeAppRoute(target) ?: target
        if (route == screen) return
        navigationStack = if (route in rootScreens) {
            emptyList()
        } else {
            (navigationStack + screen).takeLast(32)
        }
        screen = route
    }

    fun navigateBack() {
        if (navigationStack.isNotEmpty()) {
            screen = navigationStack.last()
            navigationStack = navigationStack.dropLast(1)
        } else {
            screen = AppScreen.HOME.key
        }
        audioStartsAtReciters = false
        audioAutoPlayRequested = false
    }

    BackHandler(enabled = !isRoot) { navigateBack() }
    BackHandler(enabled = isRoot && screen == AppScreen.SETTINGS.key) {
        screen = AppScreen.HOME.key
        navigationStack = emptyList()
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
                        Box(Modifier.fillMaxSize()) {
                            when (screen) {
                                "home" -> HomeScreen(
                                    language, foreground, cardColor, selectedCity,
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
                                        selectedSurah = s
                                        selectedAyah = a
                                        prefs.edit().putInt("last_surah", s).putInt("last_ayah_number", a).apply()
                                        openQuranReaderSettingsRequested = false
                                        openSelectedVerseRequested = true
                                        navigateTo("quran")
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
                                        audioAutoPlayRequested = true
                                        prefs.edit().putInt("last_surah", s).putInt("last_ayah_number", a).apply()
                                        navigateTo("audio")
                                    },
                                    onImmersiveMushaf = { immersiveMushaf = it },
                                    resumeLastMushaf = resumeMushafRequested,
                                    onResumeMushafHandled = { resumeMushafRequested = false },
                                    openSelectedVerse = openSelectedVerseRequested,
                                    onSelectedVerseHandled = { openSelectedVerseRequested = false },
                                    openReaderSettings = openQuranReaderSettingsRequested,
                                    onReaderSettingsHandled = { openQuranReaderSettingsRequested = false },
                                    onOpenSourcePdf = { path ->
                                        selectedPdf = path
                                        navigateTo("pdf")
                                    }
                                )
                                "library", "books", "worship" -> MoreScreen(language, foreground, cardColor, ::navigateTo)
                                "about_data" -> DataSourcesScreen(language, foreground, cardColor)
                                "prayer" -> PrayerScreen(language, foreground, cardColor, selectedCity, onCity = {
                                    selectedCity = it
                                    prefs.edit().putString("selected_city", it).apply()
                                }, onQibla = { navigateTo("qibla") })
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
                                    autoPlayRequested = audioAutoPlayRequested
                                )
                                "books" -> BooksScreen(language, foreground, cardColor, onOpenPdf = { path -> selectedPdf = path; navigateTo("pdf") })
                                "pdf" -> PdfReaderScreen(language, foreground, selectedPdf)
                                "bookmarks" -> BookmarksScreen(language, foreground, cardColor, onOpen = { s, a ->
                                    selectedSurah = s
                                    selectedAyah = a
                                    prefs.edit().putInt("last_surah", s).putInt("last_ayah_number", a).apply()
                                    resumeMushafRequested = false
                                    openQuranReaderSettingsRequested = false
                                    openSelectedVerseRequested = true
                                    navigationStack = emptyList()
                                    screen = "quran"
                                })
                                "lexicon" -> LexiconScreen(language, foreground, cardColor)
                                "word_by_word" -> WordAnalysisScreen(language, foreground, cardColor, selectedSurah, selectedAyah, onLoad = { s, a -> selectedSurah = s; selectedAyah = a })
                                "farmuda" -> FarmudaScreen(language, foreground, cardColor)
                                "qibla" -> QiblaScreen(language, foreground, cardColor, selectedCity)
                                "settings" -> SettingsScreen(language, foreground, cardColor, dark, accentPalette,
                                    onAccent = {
                                        accentPalette = it
                                        MashxallAppearance.accent = MashxallAppearance.colorFor(it)
                                        prefs.edit().putString("accent_palette", it).apply()
                                    },
                                    onDark = { dark = it; prefs.edit().putBoolean("dark_mode", it).apply() }, onLanguage = {
                                    language = it
                                    prefs.edit().putString("language", it.key).apply()
                                }, onDataInfo = { navigateTo("about_data") }, onBackup = { navigateTo("backup") }, onAudio = {
                                    audioStartsAtReciters = true
                                    audioAutoPlayRequested = false
                                    navigateTo("audio")
                                }, onPrayer = { navigateTo("prayer") }, onQuran = {
                                    resumeMushafRequested = false
                                    openSelectedVerseRequested = false
                                    openQuranReaderSettingsRequested = true
                                    navigateTo("quran")
                                })
                                "backup" -> BackupRestoreScreen(language, foreground, cardColor)
                                else -> HomeScreen(language, foreground, cardColor, selectedCity, onOpen = { target -> navigateTo(target.key) })
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
private fun BottomNavigationBar(
    screen: String,
    language: AppLanguage,
    cardColor: Color,
    foreground: Color,
    onSelect: (String) -> Unit
) {
    val items = listOf(
        Triple("home", tr(language, "home"), Icons.Outlined.Home),
        Triple("quran", tr(language, "quran"), Icons.Outlined.MenuBook),
        Triple("azkar", tr(language, "azkar"), Icons.Outlined.Spa),
        Triple("more", tr(language, "more"), Icons.Outlined.MoreVert),
        Triple("settings", tr(language, "settings"), Icons.Outlined.Settings)
    )
    NavigationBar(
        containerColor = cardColor,
        contentColor = foreground,
        windowInsets = WindowInsets.navigationBars
    ) {
        items.forEach { (key, title, icon) ->
            NavigationBarItem(
                selected = screen == key,
                onClick = { onSelect(key) },
                icon = { Icon(icon, contentDescription = title) },
                label = { Text(title, fontSize = 9.sp, maxLines = 1) },
                alwaysShowLabel = true
            )
        }
    }
}

private data class MoreDestination(
    val key: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
private fun MoreScreen(
    language: AppLanguage,
    foreground: Color,
    cardColor: Color,
    onOpen: (String) -> Unit
) {
    val destinations = listOf(
        MoreDestination("prayer", tr(language, "prayer"), tr(language, "prayer_sub"), Icons.Outlined.Schedule, Color(0xFF82B7E8)),
        MoreDestination("qibla", tr(language, "qibla"), tr(language, "qibla_sub"), Icons.Outlined.Explore, Color(0xFF85C4C6)),
        MoreDestination("hadith", tr(language, "hadith"), tr(language, "hadith_sub"), Icons.Outlined.LibraryBooks, Color(0xFFA99AE8)),
        MoreDestination("farmuda", tr(language, "farmuda"), tr(language, "farmuda_sub"), Icons.Outlined.VolunteerActivism, Color(0xFFE6A879)),
        MoreDestination("names", tr(language, "names"), tr(language, "names_sub"), Icons.Outlined.Favorite, Color(0xFFE39FB2)),
        MoreDestination("tasbih", tr(language, "tasbih"), tr(language, "tasbih_sub"), Icons.Outlined.Spa, Color(0xFF80C6B1)),
        MoreDestination("audio", tr(language, "audio"), tr(language, "audio_sub"), Icons.Outlined.GraphicEq, Color(0xFF9BC8A2)),
        MoreDestination("bookmarks", tr(language, "bookmarks"), tr(language, "bookmarks_sub"), Icons.Outlined.BookmarkBorder, Color(0xFFE0B86C)),
        MoreDestination("lexicon", tr(language, "lexicon"), tr(language, "lexicon_sub"), Icons.Outlined.Search, Color(0xFFB3A2F0)),
        MoreDestination(
            "backup",
            when (language) { AppLanguage.EN -> "Backup & restore"; AppLanguage.AR -> "النسخ الاحتياطي والاستعادة"; AppLanguage.CKB -> "پاشەکەوت و گەڕاندنەوە" },
            when (language) { AppLanguage.EN -> "Protect personal settings and bookmarks"; AppLanguage.AR -> "حماية الإعدادات والعلامات الشخصية"; AppLanguage.CKB -> "پاراستنی ڕێکخستن و نیشانە تایبەتییەکان" },
            Icons.Outlined.Tune, Color(0xFF8BAFCF)
        ),
        MoreDestination("about_data", tr(language, "about_data"), tr(language, "data_notice"), Icons.Outlined.Language, Color(0xFFC9A44C))
    )
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
            tr(language, "more"),
            color = foreground,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(5.dp))
        Text(
            when (language) {
                AppLanguage.EN -> "Prayer, Qibla, audio, study tools and personal utilities"
                AppLanguage.AR -> "الصلاة والقبلة والصوتيات وأدوات الدراسة والميزات الشخصية"
                AppLanguage.CKB -> "نوێژ، قیبلە، دەنگ، ئامرازی خوێندنەوە و تایبەتمەندییە کەسییەکان"
            }, color = foreground.copy(alpha = 0.6f), fontSize = 12.sp, lineHeight = 18.sp
        )
        Spacer(Modifier.height(16.dp))
        destinations.chunked(2).forEachIndexed { rowIndex, pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                pair.forEachIndexed { columnIndex, destination ->
                    AnimatedVisibility(
                        visible = true,
                        modifier = Modifier.weight(1f).padding(bottom = 10.dp),
                        enter = fadeIn(animationSpec = tween(360, delayMillis = ((rowIndex * 2 + columnIndex) * 24).coerceAtMost(260))) +
                            slideInVertically(initialOffsetY = { it / 12 }, animationSpec = tween(360, delayMillis = ((rowIndex * 2 + columnIndex) * 24).coerceAtMost(260)))
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth().animateContentSize().clickable { onOpen(destination.key) },
                            colors = CardDefaults.cardColors(containerColor = cardColor),
                            shape = RoundedCornerShape(21.dp),
                            border = BorderStroke(0.8.dp, destination.color.copy(alpha = 0.42f))
                        ) {
                            Column(
                                Modifier.fillMaxWidth()
                                    .background(Brush.linearGradient(listOf(destination.color.copy(alpha = 0.13f), cardColor, cardColor)))
                                    .padding(13.dp)
                                    .height(116.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(
                                    Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(destination.color.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(destination.icon, contentDescription = null, tint = destination.color, modifier = Modifier.size(24.dp))
                                }
                                Column {
                                    Text(destination.title, color = foreground, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    Spacer(Modifier.height(3.dp))
                                    Text(destination.subtitle, color = foreground.copy(alpha = 0.6f), fontSize = 9.sp, lineHeight = 14.sp, maxLines = 2)
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

private fun prayerSourceLabel(source: String, language: AppLanguage): String {
    val normalized = source.substringBefore(" • ").lowercase(Locale.ROOT)
    return when {
        normalized.startsWith("noor") -> when (language) {
            AppLanguage.EN -> "Noor timetable"
            AppLanguage.AR -> "جدول نور"
            AppLanguage.CKB -> "خشتەی نور"
        }
        normalized.startsWith("salawati") -> when (language) {
            AppLanguage.EN -> "Salawati timetable"
            AppLanguage.AR -> "جدول صلواتي"
            AppLanguage.CKB -> "خشتەی صلواتی"
        }
        normalized.contains("calculated") || normalized.contains("astronomical") -> when (language) {
            AppLanguage.EN -> "Calculated from coordinates"
            AppLanguage.AR -> "حساب فلكي من الإحداثيات"
            AppLanguage.CKB -> "حسابکردن لە کۆئۆردیناتەکانەوە"
        }
        else -> ""
    }
}

internal fun formatPrayerTime(raw: String, language: AppLanguage, use24Hour: Boolean): String {
    val match = Regex("""(\d{1,2}):(\d{2})(?: ?(AM|PM))?""", RegexOption.IGNORE_CASE).find(raw.trim())
        ?: return raw
    var hour = match.groupValues[1].toIntOrNull() ?: return raw
    val minuteNumber = match.groupValues[2].toIntOrNull() ?: return raw
    val minute = match.groupValues[2]
    val suffix = match.groupValues.getOrNull(3).orEmpty().uppercase(Locale.ROOT)
    if (minuteNumber !in 0..59) return raw
    if (suffix.isNotBlank() && hour !in 1..12) return raw
    if (suffix.isBlank() && hour !in 0..23) return raw

    val hour24 = if (suffix.isNotBlank()) (hour % 12) + if (suffix == "PM") 12 else 0 else hour % 24
    if (use24Hour) return String.format(Locale.US, "%02d:%s", hour24, minute)
    val isPm = if (suffix.isNotBlank()) suffix == "PM" else hour >= 12
    if (suffix.isBlank()) hour %= 24
    val hour12 = when (hour % 12) { 0 -> 12; else -> hour % 12 }
    val marker = if (language == AppLanguage.AR) {
        if (isPm) "مساءً" else "صباحًا"
    } else if (language == AppLanguage.CKB) {
        if (isPm) "د.ن" else "پ.ن"
    } else {
        if (isPm) "PM" else "AM"
    }
    return String.format(Locale.US, "%d:%s %s", hour12, minute, marker)
}

@Composable
private fun HomeScreen(
    language: AppLanguage,
    foreground: Color,
    cardColor: Color,
    selectedCity: String,
    onOpen: (AppScreen) -> Unit,
    onResumeReading: () -> Unit = { onOpen(AppScreen.QURAN) }
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("mashxall_preferences", 0) }
    var prayerTimes by remember { mutableStateOf<PrayerRow?>(null) }
    var loadingTimes by remember { mutableStateOf(true) }
    var clockNow by remember { mutableStateOf(System.currentTimeMillis()) }
    var use24Hour by remember { mutableStateOf(prefs.getBoolean("time_format_24h", false)) }
    var gpsLatitude by remember { mutableStateOf(prefs.getString("gps_latitude", null)?.toDoubleOrNull()) }
    var gpsLongitude by remember { mutableStateOf(prefs.getString("gps_longitude", null)?.toDoubleOrNull()) }
    var manualCityLatitude by remember { mutableStateOf(prefs.getString("manual_city_latitude", null)?.toDoubleOrNull()) }
    var manualCityLongitude by remember { mutableStateOf(prefs.getString("manual_city_longitude", null)?.toDoubleOrNull()) }
    var locationMessage by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            clockNow = System.currentTimeMillis()
            use24Hour = prefs.getBoolean("time_format_24h", false)
            delay(60_000L)
        }
    }
    LaunchedEffect(
        selectedCity,
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(clockNow)),
        gpsLatitude,
        gpsLongitude,
        manualCityLatitude,
        manualCityLongitude,
        prefs.getString("prayer_method", "mwl"),
        prefs.getString("asr_school", "shafi"),
        prefs.getString("selected_city_country", "")
    ) {
        loadingTimes = true
        val date = Date(clockNow)
        val country = prefs.getString("selected_city_country", "") ?: ""
        val method = PrayerCalculationMethod.fromId(prefs.getString("prayer_method", "mwl") ?: "mwl")
        val asr = AsrCalculationSchool.fromId(prefs.getString("asr_school", "shafi") ?: "shafi")
        prayerTimes = runCatching {
            when {
                gpsLatitude != null && gpsLongitude != null ->
                    ContentRepository.loadPrayerTimesAtLocation(context, gpsLatitude!!, gpsLongitude!!, date, method, asr)
                manualCityLatitude != null && manualCityLongitude != null ->
                    ContentRepository.loadPrayerTimes(context, selectedCity, date, country)
                        ?: ContentRepository.loadPrayerTimesAtLocation(context, manualCityLatitude!!, manualCityLongitude!!, date, method, asr)
                else -> ContentRepository.loadPrayerTimes(context, selectedCity, date, country)
                    ?: ContentRepository.findCityByName(context, selectedCity, country)?.let { city ->
                        PrayerTimeCalculator.calculate(date, city.latitude, city.longitude, method, asr)
                    }
            }
        }.getOrNull()
        loadingTimes = false
    }

    val saveLocationFix: (Location) -> Unit = { fix ->
        gpsLatitude = fix.latitude
        gpsLongitude = fix.longitude
        prefs.edit()
            .putString("gps_latitude", fix.latitude.toString())
            .putString("gps_longitude", fix.longitude.toString())
            .putString("gps_accuracy", if (fix.hasAccuracy()) fix.accuracy.toString() else "")
            .putBoolean("prayer_location_prompted", true)
            .apply()
        locationMessage = if (fix.hasAccuracy()) {
            val accuracy = fix.accuracy.toInt()
            when (language) {
                AppLanguage.EN -> "Location updated • reported accuracy ±$accuracy m"
                AppLanguage.AR -> "تم تحديث الموقع • الدقة التقريبية ±$accuracy متر"
                AppLanguage.CKB -> "شوێن نوێکرایەوە • وردبینی نزیکەی ±$accuracy مەتر"
            }
        } else {
            when (language) {
                AppLanguage.EN -> "Location updated."
                AppLanguage.AR -> "تم تحديث الموقع."
                AppLanguage.CKB -> "شوێن نوێکرایەوە."
            }
        }
    }
    val handleLocationError: (String) -> Unit = { error ->
        locationMessage = when (error) {
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
        if (!prefs.getBoolean("prayer_location_prompted", false) && gpsLatitude == null && gpsLongitude == null) {
            prefs.edit().putBoolean("prayer_location_prompted", true).apply()
            requestHomeLocation()
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
                        val displayedLocation = if (gpsLatitude != null && gpsLongitude != null) {
                            when (language) {
                                AppLanguage.EN -> "Current location"
                                AppLanguage.AR -> "الموقع الحالي"
                                AppLanguage.CKB -> "شوێنی ئێستا"
                            }
                        } else selectedCity
                        val sourceLabel = prayerTimes?.source?.let { prayerSourceLabel(it, language) }.orEmpty()
                        Text(
                            listOf(displayedLocation, sourceLabel).filter { it.isNotBlank() }.joinToString(" • "),
                            color = foreground.copy(alpha = 0.56f), fontSize = 10.sp, maxLines = 1
                        )
                    }
                    Icon(Icons.Outlined.Explore, contentDescription = null, tint = Gold)
                }
                Spacer(Modifier.height(14.dp))
                when {
                    loadingTimes -> Box(Modifier.fillMaxWidth().height(46.dp), contentAlignment = Alignment.Center) {
                        androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(21.dp), color = Gold, strokeWidth = 2.dp)
                    }
                    prayerEntries.isEmpty() -> Text(
                        when (language) {
                            AppLanguage.EN -> "Choose your city or allow location access to load prayer times."
                            AppLanguage.AR -> "اختر مدينتك أو اسمح بالوصول إلى الموقع لعرض المواقيت."
                            AppLanguage.CKB -> "شارەکەت هەڵبژێرە یان مۆڵەتی شوێن بدە بۆ پیشاندانی کاتەکان."
                        }, color = foreground.copy(alpha = 0.68f), fontSize = 12.sp, lineHeight = 19.sp
                    )
                    else -> prayerEntries.chunked(3).forEach { trio ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            trio.forEach { (key, label, time) ->
                                Column(
                                    Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                                        .background(if (key == "maghrib") Gold.copy(alpha = 0.09f) else foreground.copy(alpha = 0.035f))
                                        .padding(horizontal = 8.dp, vertical = 11.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(label, color = foreground.copy(alpha = 0.65f), fontSize = 10.sp, maxLines = 1)
                                    Spacer(Modifier.height(5.dp))
                                    Text(time, color = if (key == "maghrib") Gold else foreground, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
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
            modifier = Modifier.fillMaxWidth().clickable { onResumeReading() },
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
                            modifier = Modifier.fillMaxWidth().clickable { onOpen(module.screen) }.animateContentSize(),
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

private data class SourceLibrarySummary(
    val id: String,
    val filename: String,
    val sha256: String,
    val sizeBytes: Long,
    val splitCount: Int,
    val fileCount: Int,
    val databaseCount: Int,
    val audioCount: Int,
    val fontCount: Int,
    val pdfCount: Int
)

private data class SourceInventorySummary(
    val bundleSha256: String,
    val sourceApps: Int,
    val files: Int,
    val databases: Int,
    val audioFiles: Int,
    val fontFiles: Int,
    val pdfDocuments: Int,
    val mushafSvgPages: Int,
    val totalContentBytes: Long,
    val sources: List<SourceLibrarySummary>
)

private fun readSourceInventory(context: Context): SourceInventorySummary {
    val json = context.assets.open("content_catalog.json").bufferedReader().use { it.readText() }
    val root = org.json.JSONObject(json)
    val counts = root.getJSONObject("counts")

    fun countForSource(arrayName: String, sourceId: String): Int {
        val array = root.optJSONArray(arrayName) ?: return 0
        return (0 until array.length()).count { index ->
            array.optJSONObject(index)?.optString("source_id") == sourceId
        }
    }

    val sourcesJson = root.getJSONArray("sources")
    val sources = buildList {
        for (index in 0 until sourcesJson.length()) {
            val source = sourcesJson.getJSONObject(index)
            val id = source.optString("id")
            val splits = source.optJSONArray("splits")
            add(
                SourceLibrarySummary(
                    id = id,
                    filename = source.optString("filename"),
                    sha256 = source.optString("sha256"),
                    sizeBytes = source.optLong("size_bytes"),
                    splitCount = splits?.length() ?: 0,
                    fileCount = countForSource("files", id),
                    databaseCount = countForSource("databases", id),
                    audioCount = countForSource("audio", id),
                    fontCount = countForSource("fonts", id),
                    pdfCount = countForSource("documents", id)
                )
            )
        }
    }

    return SourceInventorySummary(
        bundleSha256 = root.optString("bundle_sha256"),
        sourceApps = counts.optInt("source_apps"),
        files = counts.optInt("files"),
        databases = counts.optInt("databases"),
        audioFiles = counts.optInt("audio_files"),
        fontFiles = counts.optInt("font_files"),
        pdfDocuments = counts.optInt("pdf_documents"),
        mushafSvgPages = counts.optInt("mushaf_svg_pages"),
        totalContentBytes = counts.optLong("total_content_bytes"),
        sources = sources
    )
}

private fun sourceLibraryTitle(id: String, language: AppLanguage): String = when (id) {
    "noor_tafsir_1.2.2" -> when (language) {
        AppLanguage.EN -> "Noor Tafsir"
        AppLanguage.AR -> "نور التفسير"
        AppLanguage.CKB -> "تەفسیری نوور"
    }
    "khairah_1.2" -> when (language) {
        AppLanguage.EN -> "Khairah"
        AppLanguage.AR -> "خيرة"
        AppLanguage.CKB -> "خێرە"
    }
    "salawati_3.1.3" -> when (language) {
        AppLanguage.EN -> "Salawati"
        AppLanguage.AR -> "صلواتي"
        AppLanguage.CKB -> "صلەواتی"
    }
    else -> id
}

private fun sourceLibraryDescription(id: String, language: AppLanguage): String = when (id) {
    "noor_tafsir_1.2.2" -> when (language) {
        AppLanguage.EN -> "Quran and translations, 12 Noor tafsir sources, Hadith and verse links, word-by-word grammar, Quran lexicon, adhkar, Farmuda texts, names, books/PDF guides and Arabic fonts."
        AppLanguage.AR -> "بيانات القرآن والترجمات و12 مصدرًا من تفاسير نور، والأحاديث المرتبطة بالآيات، والتحليل كلمة بكلمة، ومعجم القرآن، والأذكار والمأثورات والأسماء والكتب وأدلة PDF والخطوط العربية."
        AppLanguage.CKB -> "داتای قورئان و وەرگێڕان، ١٢ سەرچاوەی تەفسیری نوور، فەرموودەی پەیوەست بە ئایەت، شیکردنەوەی وشە بە وشە، فەرهەنگی قورئان، زیکر، مەئسورات، ناوەکان، کتێب و ڕێنمای PDF و فۆنتی عەرەبی."
    }
    "khairah_1.2" -> when (language) {
        AppLanguage.EN -> "The 604-page offline graphical Mushaf, page/verse hit geometry and four additional tafsir sources. The supplied page artwork is monochrome vector data, not a second coloured Tajweed edition."
        AppLanguage.AR -> "المصحف الرسومي دون اتصال من 604 صفحات، وهندسة مواضع الآيات داخل الصفحات، وأربعة مصادر تفسير إضافية. رسومات الصفحات المتوفرة أحادية اللون وليست طبعة تجويد ملوّنة ثانية."
        AppLanguage.CKB -> "مصحەفی گرافیکی ٦٠٤ پەڕەی ناوخۆیی، هندەسەی شوێنی ئایەت لە پەڕەدا و چوار سەرچاوەی تەفسیر زیادە. نەخشەی پەڕەکان یەکڕەنگە و چاپێکی دووەمی تەجویدی ڕەنگاوڕەنگ نییە."
    }
    "salawati_3.1.3" -> when (language) {
        AppLanguage.EN -> "City and date prayer timetables, a 166,404-location catalogue, 45,749 prayer-date records, adhkar with language translations, 99-name translations, and bundled audio."
        AppLanguage.AR -> "جداول الصلاة حسب المدينة والتاريخ، وفهرس يضم 166,404 موقعًا و45,749 سجلًا لمواقيت الصلاة، وأذكار بترجمات متعددة، وترجمات الأسماء التسعة والتسعين، وملفات صوتية مرفقة."
        AppLanguage.CKB -> "خشتەی کاتی نوێژ بەپێی شار و ڕۆژ، فهرستی ١٦٦٬٤٠٤ شوێن و ٤٥٬٧٤٩ تۆماری کاتی نوێژ، زیکر بە وەرگێڕانی جیاواز، وەرگێڕانی ٩٩ ناو و فایلە دەنگییەکان."
    }
    else -> ""
}

@Composable
private fun DataSourcesScreen(language: AppLanguage, foreground: Color, cardColor: Color) {
    val sources = listOf(
        Triple(
            when (language) { AppLanguage.EN -> "Quran & recitation"; AppLanguage.AR -> "القرآن والتلاوات"; AppLanguage.CKB -> "قورئان و تلاوەت" },
            when (language) {
                AppLanguage.EN -> "The bundled 604-page graphic Mushaf, surah and verse index, translations, tafsir links and reciter metadata."
                AppLanguage.AR -> "المصحف الرسومي المضمّن من 604 صفحات، وفهرس السور والآيات والترجمات والتفاسير وبيانات القراء."
                AppLanguage.CKB -> "مصحەفی گرافیکی ٦٠٤ پەڕەیی، فهرستی سوورەت و ئایەت، وەرگێڕان، تەفسیر و زانیاری قورئان‌خوێن."
            }, Icons.Outlined.MenuBook
        ),
        Triple(
            when (language) { AppLanguage.EN -> "Tafsir Noor"; AppLanguage.AR -> "تفسير نور"; AppLanguage.CKB -> "تەفسیری نوور" },
            when (language) {
                AppLanguage.EN -> "Noor commentary and its bundled prayer and city-reference database where supplied by the source bundle."
                AppLanguage.AR -> "تفاسير نور وقاعدة مواقيت الصلاة والمدن المرفقة حيث توفرها حزمة المصدر."
                AppLanguage.CKB -> "تەفسیری نوور و بنکەدراوەی کاتی نوێژ و شارەکان بەو ڕادەیەی لە سەرچاوەدا هەیە."
            }, Icons.Outlined.AutoStories
        ),
        Triple(
            when (language) { AppLanguage.EN -> "Khairah"; AppLanguage.AR -> "خيرة"; AppLanguage.CKB -> "خێرە" },
            when (language) {
                AppLanguage.EN -> "The packaged supplication, remembrance and religious-content records from the supplied app data."
                AppLanguage.AR -> "بيانات الأدعية والأذكار والمحتوى الديني المضمّنة من مصدر التطبيق."
                AppLanguage.CKB -> "تۆمارەکانی دوعا، زیکر و ناوەڕۆکی ئایینیی هێنراو لە سەرچاوەی ئەپەکە."
            }, Icons.Outlined.Spa
        ),
        Triple(
            when (language) { AppLanguage.EN -> "Salawati"; AppLanguage.AR -> "صلواتي"; AppLanguage.CKB -> "صلواتی" },
            when (language) {
                AppLanguage.EN -> "The source city/location catalogue, fixed prayer timetables, and dependent-city rules are used before astronomical fallback."
                AppLanguage.AR -> "فهرس المدن والمواقع وجداول الصلاة الثابتة وعلاقات المدن التابعة، قبل اللجوء إلى الحساب الفلكي."
                AppLanguage.CKB -> "فهرستی شار و شوێن، خشتەی کاتی نوێژی سەرچاوە و یاسای شارە وابەستەکان پێش حسابی ئەستێرەناسی بەکاردێت."
            }, Icons.Outlined.Schedule
        )
    )
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(tr(language, "about_data"), color = foreground, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(7.dp))
        Text(
            when (language) {
                AppLanguage.EN -> "This page lists the original sources that are bundled into Mashxall. Individual features only appear when their underlying source data is present."
                AppLanguage.AR -> "تعرض هذه الصفحة المصادر الأصلية المضمّنة في مەشخەڵ. لا تُعرض ميزة على أنها متاحة إلا بوجود بياناتها الفعلية."
                AppLanguage.CKB -> "ئەم پەڕەیە سەرچاوە ڕەسەنەکانی ناو مەشخەڵ پیشان دەدات؛ تایبەتمەندی تەنها کاتێک بەردەستە کە داتای ڕاستەقینەی هەبێت."
            }, color = foreground.copy(alpha = 0.63f), fontSize = 12.sp, lineHeight = 20.sp
        )
        Spacer(Modifier.height(16.dp))
        sources.forEach { (title, description, icon) ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(0.7.dp, Gold.copy(alpha = 0.2f))
            ) {
                Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.Top) {
                    Box(
                        Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Gold.copy(alpha = 0.13f)),
                        contentAlignment = Alignment.Center
                    ) { Icon(icon, contentDescription = null, tint = Gold, modifier = Modifier.size(24.dp)) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(title, color = foreground, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.height(5.dp))
                        Text(description, color = foreground.copy(alpha = 0.67f), fontSize = 11.sp, lineHeight = 18.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            when (language) {
                AppLanguage.EN -> "Important: this audit screen is informational. Prayer accuracy still depends on matching the user's real coordinates or the exact city/country selection."
                AppLanguage.AR -> "مهم: هذه صفحة معلومات عن المصادر؛ دقة الصلاة تعتمد على إحداثيات المستخدم الفعلية أو اختيار المدينة والدولة بدقة."
                AppLanguage.CKB -> "گرنگ: ئەم پەڕەیە بۆ زانیارییە؛ وردی کاتی نوێژ بە کۆئۆردیناتی ڕاستەقینە یان هەڵبژاردنی دروستی شار و وڵاتەوە پەیوەستە."
            }, color = foreground.copy(alpha = 0.52f), fontSize = 10.sp, lineHeight = 16.sp
        )
        Spacer(Modifier.height(18.dp))
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
    onAccent: (String) -> Unit,
    onDark: (Boolean) -> Unit,
    onLanguage: (AppLanguage) -> Unit,
    onDataInfo: () -> Unit,
    onBackup: () -> Unit,
    onAudio: () -> Unit,
    onPrayer: () -> Unit,
    onQuran: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("mashxall_preferences", 0) }
    var use24Hour by remember { mutableStateOf(prefs.getBoolean("time_format_24h", false)) }
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
        Spacer(Modifier.height(9.dp))
        Card(colors = CardDefaults.cardColors(containerColor = cardColor), shape = RoundedCornerShape(18.dp)) {
            Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Schedule, null, tint = Gold, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        when (language) { AppLanguage.EN -> "Prayer time format"; AppLanguage.AR -> "تنسيق وقت الصلاة"; AppLanguage.CKB -> "شێوازی کاتی نوێژ" },
                        color = foreground, fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        when (language) { AppLanguage.EN -> "12-hour format is the default"; AppLanguage.AR -> "نظام 12 ساعة هو الافتراضي"; AppLanguage.CKB -> "شێوازی ١٢ کاتژمێر بە بنەڕەتە" },
                        color = foreground.copy(alpha = 0.55f), fontSize = 11.sp
                    )
                }
                FilterChip(
                    selected = !use24Hour,
                    onClick = {
                        use24Hour = false
                        prefs.edit().putBoolean("time_format_24h", false).apply()
                    },
                    label = { Text("12 h") }
                )
                Spacer(Modifier.width(5.dp))
                FilterChip(
                    selected = use24Hour,
                    onClick = {
                        use24Hour = true
                        prefs.edit().putBoolean("time_format_24h", true).apply()
                    },
                    label = { Text("24 h") }
                )
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
                when (language) { AppLanguage.EN -> "City, GPS permission, calculation method and adhan alerts"; AppLanguage.AR -> "المدينة وإذن GPS وطريقة الحساب وتنبيهات الأذان"; AppLanguage.CKB -> "شار، مۆڵەتی GPS، شێوازی حساب و ئاگادارکردنەوەی بانگ" },
                Icons.Outlined.Schedule to onPrayer
            ),
            Triple(
                when (language) { AppLanguage.EN -> "Reciters & adhan library"; AppLanguage.AR -> "مكتبة القراء والمؤذنين"; AppLanguage.CKB -> "کتێبخانەی قورئان‌خوێن و موئەزین" },
                when (language) { AppLanguage.EN -> "Browse included audio and available reciters"; AppLanguage.AR -> "تصفّح الصوتيات والقراء المتاحين"; AppLanguage.CKB -> "گەڕان لە دەنگ و قورئان‌خوێنە بەردەستەکان" },
                Icons.Outlined.GraphicEq to onAudio
            ),
            Triple(
                when (language) { AppLanguage.EN -> "Backup & restore"; AppLanguage.AR -> "النسخ الاحتياطي والاستعادة"; AppLanguage.CKB -> "پاشەکەوتکردن و گەڕاندنەوە" },
                when (language) { AppLanguage.EN -> "Protect personal notes, bookmarks and preferences"; AppLanguage.AR -> "حماية الملاحظات والعلامات والإعدادات الشخصية"; AppLanguage.CKB -> "پاراستنی تێبینی، نیشانە و ڕێکخستنە تایبەتییەکان" },
                Icons.Outlined.BookmarkBorder to onBackup
            ),
            Triple(
                when (language) { AppLanguage.EN -> "Data sources & book library"; AppLanguage.AR -> "مصادر البيانات ومكتبة الكتب"; AppLanguage.CKB -> "سەرچاوەی داتا و کتێبخانە" },
                when (language) { AppLanguage.EN -> "Offline content, book catalogue and source notes"; AppLanguage.AR -> "المحتوى دون اتصال وفهرس الكتب ومصادر البيانات"; AppLanguage.CKB -> "ناوەڕۆکی ناوخۆیی، فهرستی کتێب و سەرچاوەی داتا" },
                Icons.Outlined.LibraryBooks to onDataInfo
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

