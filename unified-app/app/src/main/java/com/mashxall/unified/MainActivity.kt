package com.mashxall.unified

import android.os.Bundle
import android.os.SystemClock
import android.widget.Toast
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
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
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.MenuBook
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
import com.mashxall.unified.data.ContentRepository

data class MashxallPalettePreset(
    val id: String,
    val labelEn: String,
    val labelAr: String,
    val labelCkb: String,
    val ink: Color,
    val deepGreen: Color,
    val emerald: Color,
    val accent: Color,
    val paleAccent: Color,
    val softText: Color,
    val panel: Color,
    val mainText: Color
)

object MashxallPalette {
    val options = listOf(
        MashxallPalettePreset("emerald_gold", "Emerald & Gold", "زمردي وذهبي", "زومڕە و زێڕی",
            Color(0xFF071E1A), Color(0xFF103D32), Color(0xFF19725A), Color(0xFFC9A44C),
            Color(0xFFF4E5B4), Color(0xFFB9CDC5), Color(0xFF102B25), Color(0xFFF6F2E6)),
        MashxallPalettePreset("ocean_cyan", "Ocean & Cyan", "المحيط والسماوي", "شیناو و شینی کاڵ",
            Color(0xFF071927), Color(0xFF10354F), Color(0xFF1B6D8A), Color(0xFF52C7D7),
            Color(0xFFD4F7FA), Color(0xFFBCDDE7), Color(0xFF0C2A3D), Color(0xFFF2FBFC)),
        MashxallPalettePreset("amethyst", "Amethyst", "بنفسجي ملكي", "مۆرە شاهانە",
            Color(0xFF160D25), Color(0xFF39245A), Color(0xFF62418A), Color(0xFFC9A4FF),
            Color(0xFFE8DBFF), Color(0xFFCFC0E2), Color(0xFF251639), Color(0xFFFBF6FF)),
        MashxallPalettePreset("ruby_rose", "Ruby & Rose", "ياقوتي ووردي", "یاقووتی و پەمەیی",
            Color(0xFF1D0B12), Color(0xFF572833), Color(0xFF8F3F54), Color(0xFFF1A4B2),
            Color(0xFFFFE1DE), Color(0xFFE3BEC6), Color(0xFF321720), Color(0xFFFFF5F4)),
        MashxallPalettePreset("sapphire", "Sapphire", "ياقوت أزرق", "شینە یاقووتی",
            Color(0xFF07152A), Color(0xFF122E5D), Color(0xFF2457A2), Color(0xFF88B9FF),
            Color(0xFFDAE7FF), Color(0xFFB9CCE8), Color(0xFF0D2245), Color(0xFFF3F7FF))
    )

    var ink by mutableStateOf(options.first().ink)
        private set
    var deepGreen by mutableStateOf(options.first().deepGreen)
        private set
    var emerald by mutableStateOf(options.first().emerald)
        private set
    var accent by mutableStateOf(options.first().accent)
        private set
    var paleAccent by mutableStateOf(options.first().paleAccent)
        private set
    var softText by mutableStateOf(options.first().softText)
        private set
    var panel by mutableStateOf(options.first().panel)
        private set
    var mainText by mutableStateOf(options.first().mainText)
        private set

    fun apply(id: String) {
        val selected = options.firstOrNull { it.id == id } ?: options.first()
        ink = selected.ink
        deepGreen = selected.deepGreen
        emerald = selected.emerald
        accent = selected.accent
        paleAccent = selected.paleAccent
        softText = selected.softText
        panel = selected.panel
        mainText = selected.mainText
    }
}

private val Ink: Color get() = MashxallPalette.ink
private val DeepGreen: Color get() = MashxallPalette.deepGreen
private val Emerald: Color get() = MashxallPalette.emerald
private val Gold: Color get() = MashxallPalette.accent
private val PaleGold: Color get() = MashxallPalette.paleAccent
private val SoftText: Color get() = MashxallPalette.softText
private val Panel: Color get() = MashxallPalette.panel

enum class AppLanguage(val key: String, val label: String) {
    CKB("ckb", "کوردی"), AR("ar", "العربية"), EN("en", "English");
    companion object {
        fun from(value: String?) = entries.firstOrNull { it.key == value } ?: CKB
    }
}

enum class AppScreen(val key: String) {
    HOME("home"), QURAN("quran"), LIBRARY("library"), WORSHIP("worship"), PRAYER("prayer"),
    TAFSIR("tafsir"), HADITH("hadith"), AZKAR("azkar"), NAMES("names"), TASBIH("tasbih"),
    AUDIO("audio"), BOOKS("books"), LEXICON("lexicon"), WORD_BY_WORD("word_by_word"), FARMUDA("farmuda"), QIBLA("qibla"), BOOKMARKS("bookmarks"), SETTINGS("settings"), BACKUP("backup")
}

private data class HomeModule(
    val screen: AppScreen, val titleKey: String, val subtitleKey: String,
    val icon: ImageVector, val color: Color
)

private val homeModules = listOf(
    HomeModule(AppScreen.QURAN, "quran", "quran_sub", Icons.Outlined.MenuBook, Gold),
    HomeModule(AppScreen.BOOKMARKS, "bookmarks", "bookmarks_sub", Icons.Outlined.BookmarkBorder, Color(0xFFE0B86C)),
    HomeModule(AppScreen.TAFSIR, "tafsir", "tafsir_sub", Icons.Outlined.AutoStories, Color(0xFF80C6B1)),
    HomeModule(AppScreen.HADITH, "hadith", "hadith_sub", Icons.Outlined.LibraryBooks, Color(0xFFA99AE8)),
    HomeModule(AppScreen.AZKAR, "azkar", "azkar_sub", Icons.Outlined.Spa, Color(0xFF9CCB7B)),
    HomeModule(AppScreen.PRAYER, "prayer", "prayer_sub", Icons.Outlined.Schedule, Color(0xFF82B7E8)),
    HomeModule(AppScreen.AUDIO, "audio", "audio_sub", Icons.Outlined.GraphicEq, Color(0xFFE6A879)),
    HomeModule(AppScreen.NAMES, "names", "names_sub", Icons.Outlined.Favorite, Color(0xFFE39FB2)),
    HomeModule(AppScreen.QIBLA, "qibla", "qibla_sub", Icons.Outlined.Explore, Color(0xFF85C4C6)),
    HomeModule(AppScreen.BOOKS, "books", "books_sub", Icons.Outlined.LibraryBooks, Color(0xFFE1C36D)),
    HomeModule(AppScreen.LEXICON, "lexicon", "lexicon_sub", Icons.Outlined.Search, Color(0xFFB3A2F0)),
    HomeModule(AppScreen.WORD_BY_WORD, "word_by_word", "word_by_word_sub", Icons.Outlined.AutoStories, Color(0xFF71BFC1)),
    HomeModule(AppScreen.FARMUDA, "farmuda", "farmuda_sub", Icons.Outlined.VolunteerActivism, Color(0xFFE6A879)),
    HomeModule(AppScreen.TASBIH, "tasbih", "tasbih_sub", Icons.Outlined.Spa, Color(0xFF80C6B1)),
    HomeModule(AppScreen.LIBRARY, "library", "library_sub", Icons.Outlined.AutoStories, Gold)
)

private val textMap = mapOf(
    "ckb" to mapOf(
        "home" to "سەرەتا", "more" to "زیاتر", "quran" to "قورئانی پیرۆز", "quran_sub" to "خوێندنەوە و گەڕان", "tafsir" to "تەفسیرو وەرگێڕان", "tafsir_sub" to "چەندین تەفسیر", "hadith" to "فەرموودەکان", "hadith_sub" to "گەڕان لە فەرموودە", "azkar" to "زیکر و دوعاکان", "azkar_sub" to "بەیانی، ئێواران و زیاتر", "prayer" to "کاتەکانی بانگ", "prayer_sub" to "کاتی نوێژ و شار", "audio" to "دەنگ و تلاوەت", "audio_sub" to "فایلە دەنگییە ناوخۆییەکان", "names" to "ناوە پیرۆزەکان", "names_sub" to "٩٩ ناوی خودا", "qibla" to "ئاڕاستەی قیبلە", "qibla_sub" to "ئاڕاستە بەپێی شوێن", "books" to "کتێبخانە", "books_sub" to "کتێب و PDF", "lexicon" to "فەرهەنگی وشەکان", "lexicon_sub" to "مانا و ڕەگی وشە", "word_by_word" to "شیکردنەوەی وشە بە وشە", "word_by_word_sub" to "ڕەگ و ڕێزمانی قورئان", "farmuda" to "فەرموودە و دوعاکان", "farmuda_sub" to "دەق و زانیاری زیادە", "tasbih" to "تەسبیح", "tasbih_sub" to "ژماردنی زیکر", "library" to "کتێبخانەی ئیسلامی", "library_sub" to "قورئان، حدیث و زانست", "more_sub" to "گەیشتن بە هەموو تایبەتمەندییەکان", "welcome" to "هەموو زانیارییە ئیسلامییەکانت لە شوێنێکدا", "bookmarks" to "نیشانکراوەکان", "bookmarks_sub" to "ئایەتە پاشەکەوتکراوەکان", "surah" to "سوورەت", "ayah" to "ئایەت", "verse" to "ئایەت", "book_online" to "زانیاری کتێب", "mushaf_view" to "مصحەفی پیرۆز", "verse_view" to "ئایەت بە ئایەت", "mushaf_unavailable" to "نەتوانرا پەڕەی مصحف بخوێندرێتەوە", "mushaf_offline" to "٦٠٤ پەڕەی قورئان بەبێ ئینتەرنێت", "no_bookmarks" to "هیچ ئایەتێک نیشان نەکراوە", "no_adhan_audio" to "دەنگی بانگ لەم داتایەدا نەدۆزرایەوە", "page" to "پەڕە", "previous" to "پێشوو", "next" to "دواتر", "open_pdf" to "کردنەوەی PDF", "settings" to "ڕێکخستن", "back" to "گەڕانەوە", "home_title" to "دڵت ڕووناک بکە", "home_hint" to "ئەمڕۆ بە چەند خولەکێک خوێندنەوە دەست پێ بکە", "quick_access" to "دەستگەیشتنی خێرا", "featured" to "پێشنیاری ئەمڕۆ", "continue_reading" to "بەردەوام بە لە خوێندنەوە", "language" to "زمان", "offline" to "زۆربەی زانیارییەکان بەبێ ئینتەرنێت کار دەکەن", "about_data" to "سەرچاوەی داتا و مافەکان", "data_notice" to "هەندێک کتێب پێویستی بە ئینتەرنێت یان شیکردنەوەی تایبەت هەیە.", "search" to "گەڕان...", "choose_surah" to "سوورەت هەڵبژێرە", "select_tafsir" to "تەفسیر هەڵبژێرە", "search_hadith" to "لە فەرموودە بگەڕێ...", "search_words" to "وشە یان ڕەگ بگەڕێ...", "city" to "شار", "select_city" to "شار هەڵبژێرە", "fajr" to "بەیانی", "sunrise" to "هەڵهاتنی خۆر", "dhuhr" to "نیوەڕۆ", "asr" to "عەسر", "maghrib" to "مەغریب", "isha" to "عیشا", "search_azkar" to "لە زیکرەکان بگەڕێ...", "arabic" to "عەرەبی", "translation" to "وەرگێڕان", "reference" to "سەرچاوە", "reset" to "سفرکردنەوە", "play" to "لێدان", "available_data" to "داتای بەردەست", "no_data" to "داتاکان بارنەکراون. تکایە وەشانی data-enabled دروست بکە.", "not_found" to "هیچ ئەنجامێک نەدۆزرایەوە.", "qibla_heading" to "قیبلە لە ڕووی باکوورەوە", "degrees" to "پلە", "library_hint" to "قورئان، حدیث، تەفسیر، کتێب و دەنگ"
    ),
    "ar" to mapOf(
        "home" to "الرئيسية", "more" to "المزيد", "quran" to "القرآن الكريم", "quran_sub" to "قراءة وبحث", "tafsir" to "التفسير والترجمة", "tafsir_sub" to "تفاسير متعددة", "hadith" to "الأحاديث", "hadith_sub" to "البحث في الأحاديث", "azkar" to "الأذكار والأدعية", "azkar_sub" to "الصباح والمساء والمزيد", "prayer" to "مواقيت الصلاة", "prayer_sub" to "المواقيت والمدينة", "audio" to "التلاوات والصوتيات", "audio_sub" to "الملفات الصوتية المحلية", "names" to "أسماء الله الحسنى", "names_sub" to "الأسماء التسعة والتسعون", "qibla" to "اتجاه القبلة", "qibla_sub" to "الاتجاه حسب المدينة", "books" to "المكتبة", "books_sub" to "كتب وملفات PDF", "lexicon" to "معجم الكلمات", "lexicon_sub" to "المعاني والجذور", "word_by_word" to "تفسير كلمة بكلمة", "word_by_word_sub" to "الجذور والتحليل النحوي", "farmuda" to "المأثورات والأدعية", "farmuda_sub" to "أدعية ونصوص إضافية", "tasbih" to "المسبحة", "tasbih_sub" to "عدّ الذكر", "library" to "المكتبة الإسلامية", "library_sub" to "قرآن وحديث وعلوم", "more_sub" to "استكشف بقية الأدوات والميزات الإسلامية", "welcome" to "مكتبتك الإسلامية في مكان واحد", "bookmarks" to "العلامات المرجعية", "bookmarks_sub" to "الآيات المحفوظة", "surah" to "السورة", "ayah" to "الآية", "verse" to "آية", "book_online" to "معلومات الكتاب", "mushaf_view" to "عرض المصحف", "verse_view" to "عرض الآيات", "mushaf_unavailable" to "تعذر فتح صفحة المصحف. تحقق من ملفات البيانات.", "mushaf_offline" to "٦٠٤ صفحة من المصحف تعمل دون إنترنت", "no_bookmarks" to "لا توجد آيات محفوظة بعد", "no_adhan_audio" to "لا توجد ملفات أذان ضمن البيانات المتاحة", "page" to "صفحة", "previous" to "السابق", "next" to "التالي", "open_pdf" to "فتح PDF", "settings" to "الإعدادات", "back" to "رجوع", "home_title" to "أضئ قلبك بالمعرفة", "quick_access" to "وصول سريع", "featured" to "اختيار اليوم", "continue_reading" to "متابعة القراءة", "language" to "اللغة", "offline" to "تعمل معظم المكتبة دون اتصال بالإنترنت", "about_data" to "مصادر البيانات والحقوق", "data_notice" to "بعض الكتب تتطلب الإنترنت أو معالجة خاصة.", "search" to "بحث...", "choose_surah" to "اختر السورة", "select_tafsir" to "اختر التفسير", "search_hadith" to "ابحث في الأحاديث...", "search_words" to "ابحث عن كلمة أو جذر...", "city" to "المدينة", "select_city" to "اختر المدينة", "fajr" to "الفجر", "sunrise" to "الشروق", "dhuhr" to "الظهر", "asr" to "العصر", "maghrib" to "المغرب", "isha" to "العشاء", "search_azkar" to "ابحث في الأذكار...", "arabic" to "العربية", "translation" to "الترجمة", "reference" to "المصدر", "reset" to "إعادة", "play" to "تشغيل", "available_data" to "البيانات المتاحة", "no_data" to "لم تُحمّل البيانات بعد. أنشئ نسخة data-enabled من GitHub Actions.", "not_found" to "لم يتم العثور على نتائج.", "qibla_heading" to "اتجاه القبلة من الشمال", "degrees" to "درجة", "library_hint" to "القرآن، الحديث، التفسير، الكتب والصوتيات"
    ),
    "en" to mapOf(
        "home" to "Home", "more" to "More", "quran" to "The Holy Quran", "quran_sub" to "Read and search", "tafsir" to "Tafsir & translation", "tafsir_sub" to "Multiple commentaries", "hadith" to "Hadith", "hadith_sub" to "Search narrations", "azkar" to "Adhkar & duas", "azkar_sub" to "Morning, evening and more", "prayer" to "Prayer times", "prayer_sub" to "Times and city", "audio" to "Recitations & audio", "audio_sub" to "Bundled local media", "names" to "Names of Allah", "names_sub" to "The 99 names", "qibla" to "Qibla direction", "qibla_sub" to "Direction by selected city", "books" to "Book library", "books_sub" to "Books and PDFs", "lexicon" to "Word lexicon", "lexicon_sub" to "Meanings and roots", "word_by_word" to "Word-by-word analysis", "word_by_word_sub" to "Roots and Quranic grammar", "farmuda" to "Supplications & sayings", "farmuda_sub" to "Additional texts and duas", "tasbih" to "Digital tasbih", "tasbih_sub" to "Count remembrance", "library" to "Islamic library", "library_sub" to "Quran, Hadith and studies", "more_sub" to "Explore the extra features and tools", "welcome" to "Your Islamic library in one place", "bookmarks" to "Bookmarks", "bookmarks_sub" to "Saved verses", "surah" to "Surah", "ayah" to "Ayah", "verse" to "verses", "book_online" to "Book details", "mushaf_view" to "Mushaf pages", "verse_view" to "Verse list", "mushaf_unavailable" to "Could not read this Mushaf page; verify the bundled data.", "mushaf_offline" to "All 604 Mushaf pages work offline", "no_bookmarks" to "No saved verses yet", "no_adhan_audio" to "No adhan-specific audio was found in the supplied data", "page" to "Page", "previous" to "Previous", "next" to "Next", "open_pdf" to "Open PDF", "settings" to "Settings", "back" to "Back", "home_title" to "Let knowledge light your heart", "quick_access" to "Quick access", "featured" to "Today's pick", "continue_reading" to "Continue reading", "language" to "Language", "offline" to "Most of the library works offline", "about_data" to "Data sources & rights", "data_notice" to "Some books need internet access or format-specific processing.", "search" to "Search...", "choose_surah" to "Choose a surah", "select_tafsir" to "Choose tafsir", "search_hadith" to "Search Hadith...", "search_words" to "Search words or roots...", "city" to "City", "select_city" to "Choose city", "fajr" to "Fajr", "sunrise" to "Sunrise", "dhuhr" to "Dhuhr", "asr" to "Asr", "maghrib" to "Maghrib", "isha" to "Isha", "search_azkar" to "Search adhkar...", "arabic" to "Arabic", "translation" to "Translation", "reference" to "Reference", "reset" to "Reset", "play" to "Play", "available_data" to "Available content", "no_data" to "Content data is not loaded yet. Build the data-enabled variant from GitHub Actions.", "not_found" to "No results found.", "qibla_heading" to "Qibla bearing from north", "degrees" to "degrees", "library_hint" to "Quran, Hadith, tafsir, books and audio"
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
    var screen by rememberSaveable {
        mutableStateOf(
            startScreen?.takeIf { candidate ->
                AppScreen.entries.any { it.key == candidate } || candidate == "pdf"
            } ?: AppScreen.HOME.key
        )
    }
    var navigationStack by rememberSaveable { mutableStateOf(emptyList<String>()) }
    LaunchedEffect(startScreen) {
        val requested = startScreen?.takeIf { candidate ->
            AppScreen.entries.any { it.key == candidate } || candidate == "pdf"
        }
        if (requested != null) {
            screen = requested
            navigationStack = emptyList()
        }
    }
    var dark by rememberSaveable { mutableStateOf(prefs.getBoolean("dark_mode", true)) }
    var paletteId by rememberSaveable { mutableStateOf(prefs.getString("color_palette", "emerald_gold") ?: "emerald_gold") }
    LaunchedEffect(paletteId) { MashxallPalette.apply(paletteId) }
    var selectedSurah by rememberSaveable { mutableIntStateOf(prefs.getInt("last_surah", 1).coerceIn(1, 114)) }
    var selectedAyah by rememberSaveable { mutableIntStateOf(prefs.getInt("last_ayah_number", 1).coerceAtLeast(1)) }
    var audioStartsAtReciters by rememberSaveable { mutableStateOf(false) }
    var selectedCity by rememberSaveable { mutableStateOf(prefs.getString("selected_city", "") ?: "") }
    var selectedPdf by rememberSaveable { mutableStateOf("") }
    var immersiveMushaf by rememberSaveable { mutableStateOf(false) }
    val layoutDirection = if (language == AppLanguage.EN) LayoutDirection.Ltr else LayoutDirection.Rtl
    val rootScreens = setOf("home", "quran", "azkar", "library", "settings")
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
            screen = if (screen in rootScreens) AppScreen.HOME.key else AppScreen.LIBRARY.key
        }
        audioStartsAtReciters = false
    }

    var lastBackPressAt by remember { mutableStateOf(0L) }
    BackHandler(enabled = true) {
        if (navigationStack.isNotEmpty()) {
            navigateBack()
        } else if (screen != AppScreen.HOME.key) {
            screen = AppScreen.HOME.key
            navigationStack = emptyList()
            audioStartsAtReciters = false
        } else {
            val now = SystemClock.elapsedRealtime()
            if (now - lastBackPressAt < 2000L) {
                activity?.finish()
            } else {
                lastBackPressAt = now
                Toast.makeText(
                    context,
                    when (language) {
                        AppLanguage.EN -> "Press Back again to exit"
                        AppLanguage.AR -> "اضغط رجوع مرة أخرى للخروج"
                        AppLanguage.CKB -> "بۆ دەرچوون جارێکی تر گەڕانەوە دابگرە"
                    },
                    Toast.LENGTH_SHORT
                ).show()
            }
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
                        }
                    }
                ) { padding ->
                    Column(Modifier.fillMaxSize().padding(if (immersiveMushaf) PaddingValues(0.dp) else padding)) {
                        if (!immersiveMushaf) AppHeader(
                            language = language, foreground = foreground, isDark = dark,
                            onLanguage = { language = it; prefs.edit().putString("language", it.key).apply() },
                            onBack = { navigateBack() },
                            onSettings = { navigateTo(AppScreen.SETTINGS.key) }, showBack = !isRoot, showSettings = screen != AppScreen.SETTINGS.key
                        )
                        Box(Modifier.fillMaxSize()) {
                            when (screen) {
                                "home" -> HomeScreen(language, foreground, cardColor) { navigateTo(it.key) }
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
                                        prefs.edit().putInt("last_surah", s).putInt("last_ayah_number", a).apply()
                                        navigateTo("audio")
                                    },
                                    onImmersiveMushaf = { immersiveMushaf = it }
                                )
                                "library" -> LibraryScreen(language, foreground, cardColor) { navigateTo(it.key) }
                                "worship" -> WorshipScreen(language, foreground, cardColor) { navigateTo(it.key) }
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
                                "audio" -> AudioScreen(language, foreground, cardColor, selectedSurah, selectedAyah, audioStartsAtReciters)
                                "books" -> BooksScreen(language, foreground, cardColor, onOpenPdf = { path -> selectedPdf = path; navigateTo("pdf") })
                                "pdf" -> PdfReaderScreen(language, foreground, selectedPdf)
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
                                "settings" -> SettingsScreen(language, foreground, cardColor, dark, paletteId, onDark = { dark = it; prefs.edit().putBoolean("dark_mode", it).apply() }, onPalette = { selected ->
                                    paletteId = selected
                                    prefs.edit().putString("color_palette", selected).apply()
                                }, onLanguage = {
                                    language = it
                                    prefs.edit().putString("language", it.key).apply()
                                }, onDataInfo = { navigateTo("library") }, onBackup = { navigateTo("backup") })
                                "backup" -> BackupRestoreScreen(language, foreground, cardColor)
                                else -> HomeScreen(language, foreground, cardColor) { navigateTo(it.key) }
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
    onSettings: () -> Unit, showBack: Boolean, showSettings: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth().background(if (isDark) Ink else Color(0xFFF4F1E7)).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showBack) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = tr(language, "back"), tint = Gold)
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
        if (!showBack && showSettings) IconButton(onClick = onSettings, modifier = Modifier.size(36.dp)) {
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
        Triple("library", tr(language, "more"), Icons.Outlined.Tune),
        Triple("settings", tr(language, "settings"), Icons.Outlined.Settings)
    )
    NavigationBar(containerColor = cardColor, contentColor = foreground, windowInsets = WindowInsets.navigationBars) {
        items.forEach { (key, title, icon) ->
            NavigationBarItem(
                selected = screen == key, onClick = { onSelect(key) },
                icon = { Icon(icon, contentDescription = title) },
                label = { Text(title, fontSize = 9.sp, maxLines = 1) }, alwaysShowLabel = true
            )
        }
    }
}

@Composable
private fun HomeScreen(language: AppLanguage, foreground: Color, cardColor: Color, onOpen: (AppScreen) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp)) {
        HomePrayerTimesPanel(language, foreground, cardColor) { onOpen(AppScreen.PRAYER) }
        Spacer(Modifier.height(22.dp))
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
        Text(tr(language, "more"), color = foreground, fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Text(tr(language, "more_sub"), color = foreground.copy(alpha = 0.6f), fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))
        listOf(
            AppScreen.PRAYER to Icons.Outlined.Schedule,
            AppScreen.QIBLA to Icons.Outlined.Explore,
            AppScreen.AZKAR to Icons.Outlined.Spa,
            AppScreen.TAFSIR to Icons.Outlined.AutoStories,
            AppScreen.HADITH to Icons.Outlined.LibraryBooks,
            AppScreen.BOOKMARKS to Icons.Outlined.BookmarkBorder,
            AppScreen.NAMES to Icons.Outlined.Favorite,
            AppScreen.TASBIH to Icons.Outlined.Spa,
            AppScreen.AUDIO to Icons.Outlined.GraphicEq,
            AppScreen.BOOKS to Icons.Outlined.MenuBook,
            AppScreen.LEXICON to Icons.Outlined.Search,
            AppScreen.WORD_BY_WORD to Icons.Outlined.AutoStories,
            AppScreen.FARMUDA to Icons.Outlined.VolunteerActivism
        ).forEach { (target, icon) ->
            Card(Modifier.fillMaxWidth().padding(bottom = 10.dp).clickable { onOpen(target) }, colors = CardDefaults.cardColors(containerColor = cardColor), shape = RoundedCornerShape(20.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, tint = Gold, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(tr(language, target.key), color = foreground, fontWeight = FontWeight.Bold)
                        Text(tr(language, target.key + "_sub"), color = foreground.copy(alpha = 0.5f), fontSize = 11.sp, maxLines = 2)
                    }
                    Icon(Icons.Outlined.Explore, null, tint = Gold.copy(alpha = 0.8f))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(tr(language, "about_data"), color = foreground, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(tr(language, "data_notice"), color = foreground.copy(alpha = 0.62f), fontSize = 12.sp, lineHeight = 19.sp)
    }
}

@Composable
fun WorshipScreen(language: AppLanguage, foreground: Color, cardColor: Color, onOpen: (AppScreen) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(tr(language, "azkar"), color = foreground, fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(7.dp))
        Text(tr(language, "welcome"), color = foreground.copy(alpha = 0.56f), fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))
        listOf(
            AppScreen.AZKAR to Icons.Outlined.Spa, AppScreen.NAMES to Icons.Outlined.Favorite,
            AppScreen.TASBIH to Icons.Outlined.Spa, AppScreen.AUDIO to Icons.Outlined.GraphicEq
        ).forEach { (target, icon) ->
            Card(Modifier.fillMaxWidth().padding(bottom = 11.dp).clickable { onOpen(target) }, colors = CardDefaults.cardColors(containerColor = cardColor), shape = RoundedCornerShape(22.dp)) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(46.dp).clip(RoundedCornerShape(16.dp)).background(Gold.copy(alpha = 0.14f)), Alignment.Center) {
                        Icon(icon, null, tint = Gold, modifier = Modifier.size(25.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(tr(language, target.key), color = foreground, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(tr(language, target.key + "_sub"), color = foreground.copy(alpha = 0.56f), fontSize = 11.sp)
                    }
                    Icon(Icons.Outlined.VolunteerActivism, null, tint = Gold.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    language: AppLanguage, foreground: Color, cardColor: Color, dark: Boolean, paletteId: String,
    onDark: (Boolean) -> Unit, onPalette: (String) -> Unit,
    onLanguage: (AppLanguage) -> Unit, onDataInfo: () -> Unit, onBackup: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(tr(language, "settings"), color = foreground, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))
        Text(tr(language, "language"), color = foreground, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppLanguage.entries.forEach { item -> FilterChip(selected = item == language, onClick = { onLanguage(item) }, label = { Text(item.label) }) }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            when (language) {
                AppLanguage.EN -> "Color palette"
                AppLanguage.AR -> "ألوان التطبيق"
                AppLanguage.CKB -> "ڕەنگی ڕووکار"
            }, color = foreground, fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(end = 2.dp)) {
            items(MashxallPalette.options, key = { it.id }) { option ->
                val selected = paletteId == option.id
                Card(
                    modifier = Modifier.width(142.dp).clickable { onPalette(option.id) },
                    shape = RoundedCornerShape(17.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    border = BorderStroke(if (selected) 1.8.dp else 0.7.dp, if (selected) option.accent else foreground.copy(alpha = 0.14f))
                ) {
                    Column(Modifier.padding(11.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            listOf(option.deepGreen, option.accent, option.paleAccent).forEach { swatch ->
                                Box(Modifier.padding(end = 5.dp).size(20.dp).clip(CircleShape).background(swatch))
                            }
                            Spacer(Modifier.weight(1f))
                            if (selected) Text("✓", color = option.accent, fontWeight = FontWeight.ExtraBold)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            when (language) {
                                AppLanguage.EN -> option.labelEn
                                AppLanguage.AR -> option.labelAr
                                AppLanguage.CKB -> option.labelCkb
                            }, color = foreground, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(cardColor).clickable { onDark(!dark) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Tune, null, tint = Gold)
            Spacer(Modifier.width(12.dp))
            Text(if (language == AppLanguage.EN) "Dark luxurious theme" else if (language == AppLanguage.AR) "المظهر الداكن الفاخر" else "ڕووکارە تاریک و فەخمەکە", color = foreground, modifier = Modifier.weight(1f))
            FilterChip(selected = dark, onClick = { onDark(!dark) }, label = { Text(if (dark) "ON" else "OFF") })
        }
        Spacer(Modifier.height(22.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = cardColor),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().clickable { onBackup() }
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.BookmarkBorder, null, tint = Gold, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        when (language) {
                            AppLanguage.EN -> "Backup & restore"
                            AppLanguage.AR -> "النسخ الاحتياطي والاستعادة"
                            AppLanguage.CKB -> "پاشەکەوتکردن و گەڕاندنەوە"
                        }, color = foreground, fontWeight = FontWeight.Bold
                    )
                    Text(
                        when (language) {
                            AppLanguage.EN -> "Save bookmarks, notes and personal preferences to a file"
                            AppLanguage.AR -> "حفظ العلامات والملاحظات والإعدادات في ملف"
                            AppLanguage.CKB -> "نیشانەکان، تێبینی و ڕێکخستنەکانت لە فایلی پاشەکەوتدا هەڵبگرە"
                        }, color = foreground.copy(alpha = 0.6f), fontSize = 11.sp, lineHeight = 16.sp
                    )
                }
                Text("›", color = Gold, fontSize = 25.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Card(colors = CardDefaults.cardColors(containerColor = cardColor), shape = RoundedCornerShape(20.dp), modifier = Modifier.clickable { onDataInfo() }) {
            Column(Modifier.padding(16.dp)) {
                Text(tr(language, "about_data"), color = foreground, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(tr(language, "data_notice"), color = foreground.copy(alpha = 0.6f), fontSize = 12.sp, lineHeight = 18.sp)
            }
        }
    }
}
