package com.mashxall.unified

import android.content.Context
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.font.FontWeight
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

private val Gold = Color(0xFFC9A44C)
private val DeepGreen = Color(0xFF103D32)
private val MainText = Color(0xFFF6F2E6)
private val CardDark = Color(0xFF102B25)

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
fun QuranScreen(language: AppLanguage, foreground: Color, cardColor: Color, initialSurah: Int, initialAyah: Int, onSurah: (Int) -> Unit, onTafsir: (Int, Int) -> Unit, onBookmark: (Int, Int) -> Unit, onOpenVerse: (Int, Int) -> Unit = { _, _ -> }, onPlayAudio: (Int, Int) -> Unit = { _, _ -> }, onImmersiveMushaf: (Boolean) -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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
    var showUnvowelled by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(mushafMode) { onImmersiveMushaf(mushafMode) }
    DisposableEffect(Unit) {
        onDispose { onImmersiveMushaf(false) }
    }
    androidx.activity.compose.BackHandler(enabled = mushafMode) {
        mushafMode = false
    }
    var mushafPage by rememberSaveable { mutableIntStateOf(prefs.getInt("last_mushaf_page", 1).coerceIn(1, 604)) }
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) { surahs = ContentRepository.loadSurahs(context) }
    LaunchedEffect(initialSurah) { selected = initialSurah }
    LaunchedEffect(selected, language, query, initialAyah, translationKey) {
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
    LaunchedEffect(verses, initialAyah, query) {
        if (query.isBlank() && verses.isNotEmpty()) {
            val startAt = verses.indexOfFirst { it.number >= initialAyah }
            if (startAt >= 0) listState.scrollToItem(startAt)
        }
    }
    val currentSurah = surahs.firstOrNull { it.number == selected }
    val currentTranslation = ContentRepository.quranTranslations.firstOrNull { it.id == translationKey }
        ?: ContentRepository.quranTranslations.first()
    Column(Modifier.fillMaxSize()) {
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
        if (mushafMode) {
            MushafPageViewer(
                language = language,
                foreground = foreground,
                page = mushafPage,
                onPageChange = { requested ->
                    mushafPage = requested.coerceIn(1, 604)
                    prefs.edit().putInt("last_mushaf_page", mushafPage).apply()
                },
                onExitFullscreen = { mushafMode = false },
                onVerseClick = { surahNumber, ayahNumber ->
                    selected = surahNumber
                    onSurah(surahNumber)
                    onOpenVerse(surahNumber, ayahNumber)
                    prefs.edit().putInt("last_surah", surahNumber).putInt("last_ayah_number", ayahNumber).apply()
                    query = ""
                    mushafMode = false
                }
            )
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
                            color = foreground, fontSize = 23.sp, lineHeight = 39.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth()
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
                                                    AppLanguage.CKB -> related.collectionArabic.ifBlank { related.collectionCode }
                                                }
                                                if (collectionLabel.isNotBlank()) {
                                                    Text(collectionLabel, color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Text(related.arabic, color = foreground, fontSize = 17.sp, lineHeight = 28.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                                                val translation = when (language) {
                                                    AppLanguage.CKB -> related.kurdish
                                                    AppLanguage.AR -> related.english
                                                    AppLanguage.EN -> related.english
                                                }
                                                if (translation.isNotBlank()) {
                                                    Spacer(Modifier.height(7.dp))
                                                    Text(translation, color = foreground.copy(alpha = 0.84f), fontSize = 12.sp, lineHeight = 20.sp)
                                                }
                                                if (language != AppLanguage.EN && related.english.isNotBlank() && related.english != translation) {
                                                    Spacer(Modifier.height(5.dp))
                                                    Text("English: " + related.english, color = foreground.copy(alpha = 0.65f), fontSize = 11.sp, lineHeight = 18.sp)
                                                }
                                                if (related.reference.isNotBlank()) {
                                                    Spacer(Modifier.height(6.dp))
                                                    Text(related.reference, color = foreground.copy(alpha = 0.56f), fontSize = 10.sp, lineHeight = 16.sp)
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
                            Text(verse.translation, color = foreground.copy(alpha = 0.83f), fontSize = 14.sp, lineHeight = 23.sp)
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

}

@Composable
private fun MushafPageViewer(
    language: AppLanguage,
    foreground: Color,
    page: Int,
    onPageChange: (Int) -> Unit,
    onExitFullscreen: () -> Unit,
    onVerseClick: (Int, Int) -> Unit
) {
    val context = LocalContext.current
    var bitmap by remember(page) { mutableStateOf<Bitmap?>(null) }
    var pageVerses by remember(page) { mutableStateOf<List<MushafPageVerse>>(emptyList()) }
    var loading by remember(page) { mutableStateOf(true) }
    var failed by remember(page) { mutableStateOf(false) }
    var pageInput by rememberSaveable { mutableStateOf(page.toString()) }
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
    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { onPageChange(page - 1) }, enabled = page > 1 && !loading) {
                Text(tr(language, "previous"), fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(tr(language, "mushaf_view"), color = Gold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(tr(language, "page") + " " + page + " / 604", color = foreground.copy(alpha = 0.6f), fontSize = 10.sp)
            }
            OutlinedButton(onClick = { onPageChange(page + 1) }, enabled = page < 604 && !loading) {
                Text(tr(language, "next"), fontSize = 11.sp)
            }
            OutlinedButton(onClick = onExitFullscreen) {
                Text(
                    when (language) {
                        AppLanguage.EN -> "Exit"
                        AppLanguage.AR -> "خروج"
                        AppLanguage.CKB -> "دەرچوون"
                    }, fontSize = 10.sp
                )
            }
        }
        Spacer(Modifier.height(7.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = pageInput,
                onValueChange = { pageInput = it.filter(Char::isDigit).take(3) },
                modifier = Modifier.width(112.dp),
                label = { Text(if (language == AppLanguage.EN) "Jump to page" else if (language == AppLanguage.AR) "اذهب إلى صفحة" else "بڕۆ بۆ پەڕە") },
                singleLine = true
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                pageInput.toIntOrNull()?.takeIf { it in 1..604 }?.let(onPageChange)
            }) {
                Text(if (language == AppLanguage.EN) "Go" else if (language == AppLanguage.AR) "اذهب" else "بڕۆ")
            }
        }
        Spacer(Modifier.height(7.dp))
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(13.dp),
            border = BorderStroke(1.dp, Gold.copy(alpha = 0.25f))
        ) {
            Box(Modifier.fillMaxSize().padding(5.dp), contentAlignment = Alignment.Center) {
                when {
                    loading -> CircularProgressIndicator(color = Gold)
                    bitmap != null -> Image(
                        bitmap!!.asImageBitmap(),
                        contentDescription = tr(language, "mushaf_view") + " " + page,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                    failed -> Text(tr(language, "mushaf_unavailable"), color = foreground, textAlign = TextAlign.Center, modifier = Modifier.padding(18.dp))
                }
            }
        }
        if (pageVerses.isNotEmpty()) {
            Spacer(Modifier.height(5.dp))
            Text(
                when (language) {
                    AppLanguage.EN -> "Verses on this page • ${pageVerses.size}"
                    AppLanguage.AR -> "آيات هذه الصفحة • ${pageVerses.size}"
                    AppLanguage.CKB -> "ئایەتەکانی ئەم پەڕەیە • ${pageVerses.size}"
                },
                color = Gold, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth()
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(pageVerses, key = { "${it.surah}:${it.ayah}" }) { verse ->
                    AssistChip(
                        onClick = { onVerseClick(verse.surah, verse.ayah) },
                        label = { Text("${verse.surah}:${verse.ayah}", fontSize = 10.sp) },
                        leadingIcon = { Icon(Icons.Outlined.MenuBook, null, tint = Gold, modifier = Modifier.size(13.dp)) }
                    )
                }
            }
        }
        Text(tr(language, "mushaf_offline"), color = foreground.copy(alpha = 0.55f), fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
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

@Composable
fun HadithScreen(language: AppLanguage, foreground: Color, cardColor: Color) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var query by rememberSaveable { mutableStateOf("") }
    var collections by remember { mutableStateOf<List<HadithCollection>>(emptyList()) }
    var selectedCollectionId by rememberSaveable { mutableIntStateOf(0) }
    var hadithItems by remember { mutableStateOf<List<HadithItem>>(emptyList()) }
    var totalMatches by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var offset by remember { mutableIntStateOf(0) }
    var hasMore by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        collections = ContentRepository.loadHadithCollections(context)
    }
    LaunchedEffect(query, selectedCollectionId) {
        loading = true
        loadingMore = false
        offset = 0
        hasMore = true
        val bookFilter = selectedCollectionId.takeIf { it > 0 }
        totalMatches = ContentRepository.loadHadithCount(context, query.trim(), bookFilter)
        hadithItems = ContentRepository.loadHadiths(context, query.trim(), offset = 0, limit = 100, bookId = bookFilter)
        offset = hadithItems.size
        hasMore = hadithItems.size == 100 && offset < totalMatches
        loading = false
        listState.scrollToItem(0)
    }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(tr(language, "hadith"), color = foreground, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(3.dp))
            Text(
                when (language) {
                    AppLanguage.EN -> "$totalMatches narrations found • $offset loaded"
                    AppLanguage.AR -> "عُثر على $totalMatches رواية • تم تحميل $offset"
                    AppLanguage.CKB -> "$totalMatches فەرموودە دۆزرایەوە • $offset بارکراوە"
                }, color = foreground.copy(alpha = 0.54f), fontSize = 11.sp
            )
            Spacer(Modifier.height(10.dp))
            SearchBox(query, { query = it }, tr(language, "search_hadith"))
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
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
        if (hadithItems.isEmpty()) LoadingOrEmpty(loading, language, foreground) else LazyColumn(
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
                Card(colors = CardDefaults.cardColors(containerColor = cardColor), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(15.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(collection.ifBlank { "Hadith #${hadith.id}" }, color = Gold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                if (kitab.isNotBlank()) Text(kitab, color = foreground.copy(alpha = 0.72f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                if (chapter.isNotBlank()) Text(chapter, color = foreground.copy(alpha = 0.55f), fontSize = 10.sp, lineHeight = 15.sp)
                                Text(
                                    if (language == AppLanguage.EN) "No. $numberInBook • ID ${hadith.id}"
                                    else if (language == AppLanguage.AR) "رقم $numberInBook • المعرّف ${hadith.id}"
                                    else "ژمارە $numberInBook • ناسنامە ${hadith.id}",
                                    color = foreground.copy(alpha = 0.42f), fontSize = 9.sp
                                )
                            }
                            IconButton(onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val textToCopy = buildString {
                                    append(collection).append("\n")
                                    if (kitab.isNotBlank()) append(kitab).append("\n")
                                    if (chapter.isNotBlank()) append(chapter).append("\n")
                                    append(hadith.arabic).append("\n\n")
                                    if (hadith.sorani.isNotBlank()) append(hadith.sorani).append("\n\n")
                                    if (hadith.english.isNotBlank()) append(hadith.english).append("\n\n")
                                    append("Reference: ").append(numberInBook)
                                }
                                clipboard.setPrimaryClip(ClipData.newPlainText("Mashxall Hadith", textToCopy))
                                Toast.makeText(context, if (language == AppLanguage.EN) "Hadith copied" else if (language == AppLanguage.AR) "تم نسخ الحديث" else "فەرموودەکە کۆپی کرا", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Outlined.ContentCopy, null, tint = Gold, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(Modifier.height(7.dp))
                        Text(hadith.arabic, color = foreground, fontSize = 18.sp, lineHeight = 31.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                        if (translatedText.isNotBlank() && translatedText != hadith.arabic) {
                            Spacer(Modifier.height(8.dp))
                            Text(translatedText.replace("<rr>", "").replace("</rr>", ""), color = foreground.copy(alpha = 0.82f), fontSize = 13.sp, lineHeight = 21.sp)
                        }
                        if (language != AppLanguage.EN && hadith.english.isNotBlank()) {
                            TextButton(onClick = { showEnglish = !showEnglish }) {
                                Text(if (showEnglish) {
                                    if (language == AppLanguage.AR) "إخفاء الإنجليزية" else "English: hide"
                                } else {
                                    if (language == AppLanguage.AR) "عرض الإنجليزية" else "English translation"
                                }, fontSize = 11.sp)
                            }
                            if (showEnglish) Text(hadith.english.replace("<rr>", "").replace("</rr>", ""), color = foreground.copy(alpha = 0.72f), fontSize = 12.sp, lineHeight = 19.sp)
                        }
                        if (hadith.note.isNotBlank() || hadith.footnote.isNotBlank()) {
                            TextButton(onClick = { showNotes = !showNotes }) {
                                Text(if (showNotes) {
                                    if (language == AppLanguage.EN) "Hide notes" else if (language == AppLanguage.AR) "إخفاء الملاحظات" else "تێبینی بشارەوە"
                                } else {
                                    if (language == AppLanguage.EN) "Notes and footnotes" else if (language == AppLanguage.AR) "الملاحظات والحواشي" else "تێبینی و پەراوێز"
                                }, fontSize = 11.sp)
                            }
                            if (showNotes) {
                                if (hadith.note.isNotBlank()) Text(hadith.note.replace("<rr>", "").replace("</rr>", ""), color = foreground.copy(alpha = 0.72f), fontSize = 12.sp, lineHeight = 19.sp)
                                if (hadith.footnote.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(hadith.footnote.replace("<rr>", "").replace("</rr>", ""), color = foreground.copy(alpha = 0.62f), fontSize = 11.sp, lineHeight = 18.sp)
                                }
                            }
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
                                    context, query.trim(), offset, 100, selectedCollectionId.takeIf { it > 0 }
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
}

@Composable
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
    var favoriteIds by remember {
        mutableStateOf(favoritePrefs.getStringSet("favorite_ids", emptySet()).orEmpty().toSet())
    }

    LaunchedEffect(Unit) { categories = ContentRepository.loadAzkarCategories(context) }
    LaunchedEffect(query) {
        loading = true
        allItems = ContentRepository.loadAzkar(context, language.key, query.trim())
        loading = false
    }
    val visibleItems = allItems.filter { item ->
        val tabMatch = when (selectedTab) {
            1 -> item.titleCkb.contains("بەیانی", true) || item.titleAr.contains("الصباح", true) || item.titleEn.contains("morning", true)
            2 -> item.titleCkb.contains("ئێوار", true) || item.titleAr.contains("المساء", true) || item.titleEn.contains("evening", true)
            3 -> favoriteIds.contains(item.id.toString())
            else -> true
        }
        tabMatch && (selectedCategory == 0 || item.categoryId == selectedCategory)
    }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(tr(language, "azkar"), color = foreground, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            SearchBox(query, { query = it }, tr(language, "search_azkar"))
            Spacer(Modifier.height(7.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                items(listOf(0, 1, 2, 3)) { index ->
                    FilterChip(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        label = {
                            Text(when (language) {
                                AppLanguage.CKB -> listOf("هەموو", "بەیانی", "ئێواران", "دڵخوازەکان")[index]
                                AppLanguage.AR -> listOf("الكل", "الصباح", "المساء", "المفضلة")[index]
                                AppLanguage.EN -> listOf("All", "Morning", "Evening", "Favourites")[index]
                            }, fontSize = 11.sp)
                        }
                    )
                }
            }
            Spacer(Modifier.height(7.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                item {
                    FilterChip(
                        selected = selectedCategory == 0,
                        onClick = { selectedCategory = 0 },
                        label = { Text(if (language == AppLanguage.EN) "All categories" else if (language == AppLanguage.AR) "كل الأقسام" else "هەموو بەشەکان", fontSize = 10.sp) }
                    )
                }
                items(categories, key = { it.id }) { category ->
                    val label = when (language) {
                        AppLanguage.CKB -> category.ckb
                        AppLanguage.AR -> category.arabic
                        AppLanguage.EN -> category.english
                    }
                    FilterChip(
                        selected = selectedCategory == category.id,
                        onClick = { selectedCategory = category.id },
                        label = { Text(label, fontSize = 10.sp, maxLines = 1) }
                    )
                }
            }
        }
        if (visibleItems.isEmpty()) LoadingOrEmpty(loading, language, foreground) else LazyColumn(
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(visibleItems, key = { it.id }) { item ->
                var count by rememberSaveable(item.id) { mutableIntStateOf(progressPrefs.getInt(item.id.toString(), 0)) }
                val isFavorite = favoriteIds.contains(item.id.toString())
                val categoryLabel = when (language) {
                    AppLanguage.CKB -> item.categoryCkb
                    AppLanguage.AR -> item.categoryArabic
                    AppLanguage.EN -> item.categoryEnglish
                }
                Card(colors = CardDefaults.cardColors(containerColor = cardColor), shape = RoundedCornerShape(20.dp), border = BorderStroke(0.6.dp, Gold.copy(alpha = 0.18f))) {
                    Column(Modifier.padding(15.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                if (categoryLabel.isNotBlank()) Text(categoryLabel, color = foreground.copy(alpha = 0.45f), fontSize = 10.sp)
                                Text(
                                    when (language) { AppLanguage.CKB -> item.titleCkb; AppLanguage.AR -> item.titleAr; AppLanguage.EN -> item.titleEn },
                                    color = Gold, fontWeight = FontWeight.Bold, fontSize = 12.sp
                                )
                            }
                            TextButton(onClick = {
                                val id = item.id.toString()
                                favoriteIds = if (isFavorite) favoriteIds - id else favoriteIds + id
                                favoritePrefs.edit().putStringSet("favorite_ids", favoriteIds).apply()
                            }) { Text(if (isFavorite) "★" else "☆", color = Gold, fontSize = 21.sp) }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(item.arabic, color = foreground, fontSize = 18.sp, lineHeight = 31.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                        val translation = when (language) { AppLanguage.CKB -> item.ckb; AppLanguage.AR -> ""; AppLanguage.EN -> item.english }
                        if (translation.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(translation, color = foreground.copy(alpha = 0.78f), fontSize = 13.sp, lineHeight = 21.sp)
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (language == AppLanguage.EN) "Repeat: $count / " + item.count.coerceAtLeast(1)
                                else if (language == AppLanguage.AR) "التكرار: $count / " + item.count.coerceAtLeast(1)
                                else "ژمارە: $count / " + item.count.coerceAtLeast(1),
                                color = foreground.copy(alpha = 0.64f), fontSize = 11.sp, modifier = Modifier.weight(1f)
                            )
                            Button(onClick = {
                                val target = item.count.coerceAtLeast(1)
                                count = if (count + 1 >= target) 0 else count + 1
                                progressPrefs.edit().putInt(item.id.toString(), count).apply()
                            }) {
                                Icon(Icons.Outlined.CheckCircle, null, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(5.dp))
                                Text(if (language == AppLanguage.EN) "Count" else if (language == AppLanguage.AR) "تسبيح" else "ژماردن")
                            }
                        }
                        if (item.referenceCkb.isNotBlank()) Text(item.referenceCkb, color = foreground.copy(alpha = 0.45f), fontSize = 10.sp)
                    }
                }
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
                    AppLanguage.AR -> name.arabic
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
                                Text(description, color = foreground, fontSize = 12.sp, lineHeight = 18.sp)
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
    focusReciters: Boolean = false
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("mashxall_preferences", Context.MODE_PRIVATE) }
    var audio by remember { mutableStateOf<List<AudioItem>>(emptyList()) }
    var reciters by remember { mutableStateOf<List<Reciter>>(emptyList()) }
    var preferredReciterId by rememberSaveable { mutableIntStateOf(prefs.getInt("preferred_reciter_id", -1)) }
    val preferredVerseReciter = reciters.firstOrNull { it.id == preferredReciterId && it.audioType == 0 }
    var autoPlayKey by rememberSaveable { mutableStateOf("") }
    var playing by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var selectedTab by rememberSaveable { mutableIntStateOf(if (focusReciters) 1 else 0) }
    var audioQuery by rememberSaveable { mutableStateOf("") }
    var surahText by rememberSaveable { mutableStateOf(initialSurah.coerceIn(1, 114).toString()) }
    var ayahText by rememberSaveable { mutableStateOf(initialAyah.coerceAtLeast(1).toString()) }
    LaunchedEffect(initialSurah, initialAyah, focusReciters) {
        surahText = initialSurah.coerceIn(1, 114).toString()
        ayahText = initialAyah.coerceAtLeast(1).toString()
        selectedTab = if (focusReciters) 1 else 0
    }
    LaunchedEffect(Unit) {
        audio = ContentRepository.loadAudioItems(context)
        reciters = ContentRepository.loadReciters(context)
        loading = false
    }
    LaunchedEffect(reciters, focusReciters, initialSurah, initialAyah, preferredReciterId) {
        val preferred = reciters.firstOrNull { it.id == preferredReciterId && it.audioType == 0 }
        if (focusReciters && preferred != null) {
            val url = ContentRepository.reciterSampleUrl(
                preferred,
                initialSurah.coerceIn(1, 114),
                initialAyah.coerceAtLeast(1)
            )
            val key = "$url|${initialSurah}|${initialAyah}"
            if (autoPlayKey != key) {
                autoPlayKey = key
                playing = url
                AudioController.playRemote(context, url) { state ->
                    if (!state) playing = ""
                }
            }
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
        if (selectedTab == 0 || selectedTab == 2) {
            val adhanKeywords = listOf(
                "adhan", "athan", "azan", "muezzin", "takbir", "takbeer", "madina", "madinah",
                "mecca", "makkah", "fajr", "hijaz", "abdulbasit", "abdul_basit", "abdulmajid",
                "majid", "mansur", "mshari", "naqshbandi", "nasr", "peshawa", "rahid", "ramazan"
            )
            val visibleAudio = audio.filter { item ->
                val name = (item.title + " " + item.assetPath + " " + item.group).lowercase(Locale.ROOT)
                (selectedTab == 0 || adhanKeywords.any { name.contains(it) }) &&
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
                (reciter.nameArabic + " " + reciter.nameEnglish + " " + reciter.baseUrl).contains(audioQuery, ignoreCase = true)
            }
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
                    Card(colors = CardDefaults.cardColors(containerColor = cardColor), shape = RoundedCornerShape(17.dp)) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(38.dp).clip(RoundedCornerShape(13.dp)).background(DeepGreen), contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.GraphicEq, null, tint = Gold, modifier = Modifier.size(22.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(reciter.nameArabic, color = foreground, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text(reciter.nameEnglish, color = foreground.copy(alpha = 0.58f), fontSize = 10.sp, maxLines = 1)
                                Text(if (reciter.audioType == 0) "Verse-by-verse" else "Surah audio", color = Gold.copy(alpha = 0.85f), fontSize = 9.sp)
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
                                    AudioController.playRemote(context, url) { state -> if (!state) playing = "" }
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
            Toast.makeText(context, tr(AppLanguage.CKB, "no_data"), Toast.LENGTH_SHORT).show()
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
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) {
            status = if (language == AppLanguage.EN) "Backup cancelled." else if (language == AppLanguage.AR) "تم إلغاء النسخ الاحتياطي." else "پاشەکەوتکردن هەڵوەشایەوە."
        } else {
            try {
                val json = createMashxallBackup(context)
                val stream = context.contentResolver.openOutputStream(uri) ?: error("Could not open the selected destination")
                stream.bufferedWriter(Charsets.UTF_8).use { it.write(json.toString(2)) }
                status = when (language) {
                    AppLanguage.EN -> "Backup saved successfully."
                    AppLanguage.AR -> "تم حفظ النسخة الاحتياطية بنجاح."
                    AppLanguage.CKB -> "پاشەکەوت بە سەرکەوتوویی هەڵگیرا."
                }
                Toast.makeText(context, status, Toast.LENGTH_LONG).show()
            } catch (_: Exception) {
                status = when (language) {
                    AppLanguage.EN -> "Could not save the backup. Choose another folder."
                    AppLanguage.AR -> "تعذر حفظ النسخة الاحتياطية. اختر مجلدًا آخر."
                    AppLanguage.CKB -> "نەتوانرا پاشەکەوت بگیرێت؛ فۆڵدەرێکی تر هەڵبژێرە."
                }
                Toast.makeText(context, status, Toast.LENGTH_LONG).show()
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val json = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                    org.json.JSONObject(it.readText())
                } ?: error("Could not read the selected file")
                restoreMashxallBackup(context, json)
                status = when (language) {
                    AppLanguage.EN -> "Backup restored. Reopening saved data…"
                    AppLanguage.AR -> "تمت الاستعادة. جارٍ إعادة فتح البيانات…"
                    AppLanguage.CKB -> "پاشەکەوت گەڕێندرایەوە؛ داتاکان نوێ دەکرێنەوە…"
                }
                Toast.makeText(context, status, Toast.LENGTH_LONG).show()
                (context as? android.app.Activity)?.recreate()
            } catch (_: Exception) {
                status = when (language) {
                    AppLanguage.EN -> "Restore failed. Select a valid Mashxall backup file."
                    AppLanguage.AR -> "فشلت الاستعادة. اختر ملف نسخة احتياطية صالحًا من مەشخەڵ."
                    AppLanguage.CKB -> "گەڕاندنەوە سەرکەوتوو نەبوو؛ فایلی پاشەکەوتی دروستی مەشخەڵ هەڵبژێرە."
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
                AppLanguage.EN -> "Export your personal settings to a JSON file, or restore them later on this or another Android phone."
                AppLanguage.AR -> "صدّر إعداداتك الشخصية إلى ملف JSON، أو استعدها لاحقًا على هذا الهاتف أو هاتف Android آخر."
                AppLanguage.CKB -> "ڕێکخستن و زانیارییە تایبەتییەکانت وەک فایلی JSON هەڵگرە و دواتر لە مۆبایلێکی تر بیگەڕێنەوە."
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
        Button(onClick = { exportLauncher.launch("Mashxall-Backup.json") }, modifier = Modifier.fillMaxWidth()) {
            Text(if (language == AppLanguage.EN) "Save backup file" else if (language == AppLanguage.AR) "حفظ ملف النسخة الاحتياطية" else "فایلی پاشەکەوت هەڵبگرە")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { importLauncher.launch(arrayOf("application/json", "text/json", "application/octet-stream")) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (language == AppLanguage.EN) "Restore from a backup file" else if (language == AppLanguage.AR) "استعادة من ملف نسخة احتياطية" else "لە فایلی پاشەکەوتەوە بیگەڕێنەوە")
        }
        Spacer(Modifier.height(12.dp))
        if (status.isNotBlank()) Text(status, color = foreground.copy(alpha = 0.75f), fontSize = 12.sp, lineHeight = 18.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            when (language) {
                AppLanguage.EN -> "Only personal settings and progress are backed up; the bundled Quran, tafsir, audio and books remain in the installed app."
                AppLanguage.AR -> "تحتوي النسخة على الإعدادات والتقدم الشخصي فقط؛ أما المصحف والتفسير والصوتيات والكتب فتبقى ضمن التطبيق."
                AppLanguage.CKB -> "پاشەکەوت تەنها ڕێکخستن و پێشکەوتنی تایبەتی دەگرێتەوە؛ قورئان، تەفسیر و کتێبەکان لە ناو ئەپەکەدا دەمێننەوە."
            }, color = foreground.copy(alpha = 0.5f), fontSize = 11.sp, lineHeight = 17.sp
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
    val visiblePdfs = if (query.isBlank()) pdfs else pdfs.filter { path ->
        path.substringAfterLast('/').contains(query.trim(), ignoreCase = true) ||
            path.substringBeforeLast('/').contains(query.trim(), ignoreCase = true)
    }
    val visibleBooks = books.filter { !favoritesOnly || favoriteIds.contains(it.id.toString()) }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(tr(language, "books"), color = foreground, fontSize = 22.sp, fontWeight = FontWeight.Bold)
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

private fun requestCurrentLocation(
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
        onError("Turn on Location/GPS on your phone, then try again")
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
            if (best != null) onLocation(best)
            else onError("No fresh location fix. Turn on GPS and try again outdoors.")
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
                    mainHandler.postDelayed(deferredFinish!!, 3500L)
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
    var chooseCity by remember { mutableStateOf(false) }
    var citySearch by rememberSaveable { mutableStateOf("") }
    var dayOffset by rememberSaveable { mutableIntStateOf(0) }
    var showDatePicker by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var locating by remember { mutableStateOf(false) }
    var locationMessage by remember { mutableStateOf("") }
    var gpsLatitude by remember { mutableStateOf(prefs.getString("gps_latitude", null)?.toDoubleOrNull()) }
    var gpsLongitude by remember { mutableStateOf(prefs.getString("gps_longitude", null)?.toDoubleOrNull()) }
    var gpsAccuracy by remember { mutableStateOf(prefs.getString("gps_accuracy", null)?.toFloatOrNull()) }
    var manualLatitude by remember { mutableStateOf(prefs.getString("manual_city_latitude", null)?.toDoubleOrNull()) }
    var manualLongitude by remember { mutableStateOf(prefs.getString("manual_city_longitude", null)?.toDoubleOrNull()) }
    val manualCountry = prefs.getString("selected_city_country", "") ?: ""
    var calculationMethod by rememberSaveable { mutableStateOf(prefs.getString("prayer_method", "mwl") ?: "mwl") }
    var asrSchool by rememberSaveable { mutableStateOf(prefs.getString("asr_school", "shafi") ?: "shafi") }
    var adhanEnabled by rememberSaveable { mutableStateOf(prefs.getBoolean("adhan_enabled", false)) }
    var adhanAssets by remember { mutableStateOf(emptyList<AudioItem>()) }
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
            city.name.contains(citySearch.trim(), ignoreCase = true) ||
                city.country.contains(citySearch.trim(), ignoreCase = true)
        }
    }

    val onFix: (Location) -> Unit = { fix ->
        // A new fix supersedes the previous location even if its reported accuracy
        // is a little worse: otherwise a user who moved could stay stuck at stale
        // coordinates until the new sensor accuracy beats an old measurement.
        locating = false
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
    val onLocationError: (String) -> Unit = { error ->
        locating = false
        locationMessage = error
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

    LaunchedEffect(citySearch) {
        delay(250L)
        cities = ContentRepository.searchCities(context, citySearch.trim(), limit = 220)
    }
    LaunchedEffect(Unit) {
        while (true) {
            clockNowMillis = System.currentTimeMillis()
            delay(1_000L)
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
        adhanAssets = ContentRepository.loadAudioItems(context)
        val preferredKeywords = listOf(
            "adhan", "athan", "azan", "muezzin", "madina", "madinah", "mecca",
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
    LaunchedEffect(adhanEnabled, selectedAdhanAsset, gpsLatitude, gpsLongitude, manualLatitude, manualLongitude, manualCountry, selectedCity, calculationMethod, asrSchool) {
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
    LaunchedEffect(selectedCity, gpsLatitude, gpsLongitude, manualLatitude, manualLongitude, calculationMethod, asrSchool, dayOffset) {
        var loadedDate = ""
        while (true) {
            val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val requestedKey = "$todayKey:$dayOffset"
            if (requestedKey != loadedDate) {
                loading = true
                val targetDate = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, dayOffset)
                }.time
                times = when {
                    gpsLatitude != null && gpsLongitude != null -> ContentRepository.loadPrayerTimesAtLocation(
                        context, gpsLatitude!!, gpsLongitude!!, targetDate,
                        PrayerCalculationMethod.fromId(calculationMethod),
                        AsrCalculationSchool.fromId(asrSchool)
                    )
                    manualLatitude != null && manualLongitude != null -> {
                        val sourceCityTimes = ContentRepository.loadPrayerTimes(
                            context, selectedCity, targetDate, manualCountry
                        )
                        sourceCityTimes ?: ContentRepository.loadPrayerTimesAtLocation(
                            context, manualLatitude!!, manualLongitude!!, targetDate,
                            PrayerCalculationMethod.fromId(calculationMethod),
                            AsrCalculationSchool.fromId(asrSchool)
                        )
                    }
                    else -> ContentRepository.loadPrayerTimes(context, selectedCity, targetDate)
                }
                loadedDate = requestedKey
                loading = false
            }
            delay(60_000L)
        }
    }
    LaunchedEffect(
        selectedCity, gpsLatitude, gpsLongitude, manualLatitude, manualLongitude,
        manualCountry, calculationMethod, asrSchool, dayOffset, currentLocalDateKey
    ) {
        if (dayOffset != 0) {
            tomorrowTimes = null
        } else {
            val tomorrow = Calendar.getInstance().apply {
                timeInMillis = clockNowMillis
                add(Calendar.DAY_OF_YEAR, 1)
            }.time
            tomorrowTimes = when {
                gpsLatitude != null && gpsLongitude != null ->
                    ContentRepository.loadPrayerTimesAtLocation(
                        context, gpsLatitude!!, gpsLongitude!!, tomorrow,
                        PrayerCalculationMethod.fromId(calculationMethod),
                        AsrCalculationSchool.fromId(asrSchool)
                    )
                manualLatitude != null && manualLongitude != null -> {
                    ContentRepository.loadPrayerTimes(context, selectedCity, tomorrow, manualCountry)
                        ?: ContentRepository.loadPrayerTimesAtLocation(
                            context, manualLatitude!!, manualLongitude!!, tomorrow,
                            PrayerCalculationMethod.fromId(calculationMethod),
                            AsrCalculationSchool.fromId(asrSchool)
                        )
                }
                else -> ContentRepository.loadPrayerTimes(context, selectedCity, tomorrow)
            }
        }
    }
    val nextPrayerTarget = remember(times, tomorrowTimes, dayOffset, clockNowMillis) {
        val currentTimes = times
        if (dayOffset != 0 || currentTimes == null) {
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

    val displayDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, dayOffset) }.time
    val displayLocalDate = Instant.ofEpochMilli(displayDate.time).atZone(ZoneId.systemDefault()).toLocalDate()
    val displayDateText = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(displayDate)
    val datePickerState = key(dayOffset) {
        rememberDatePickerState(
            initialSelectedDateMillis = displayLocalDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            yearRange = (LocalDate.now().year - 1)..(LocalDate.now().year + 1)
        )
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(tr(language, "prayer"), color = foreground, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Card(colors = CardDefaults.cardColors(containerColor = cardColor), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { dayOffset = (dayOffset - 1).coerceAtLeast(-366) }, enabled = dayOffset > -366) {
                    Text("‹", color = Gold, fontSize = 22.sp)
                }
                Column(
                    Modifier.weight(1f).clickable { showDatePicker = true },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        when (language) {
                            AppLanguage.EN -> if (dayOffset == 0) "Today" else if (dayOffset < 0) "Past date" else "Upcoming date"
                            AppLanguage.AR -> if (dayOffset == 0) "اليوم" else if (dayOffset < 0) "تاريخ سابق" else "تاريخ قادم"
                            AppLanguage.CKB -> if (dayOffset == 0) "ئەمڕۆ" else if (dayOffset < 0) "ڕۆژی ڕابردوو" else "ڕۆژی داهاتوو"
                        }, color = foreground, fontWeight = FontWeight.SemiBold, fontSize = 12.sp
                    )
                    Text(displayDateText, color = foreground.copy(alpha = 0.65f), fontSize = 11.sp)
                }
                IconButton(onClick = { dayOffset = (dayOffset + 1).coerceAtMost(366) }, enabled = dayOffset < 366) {
                    Text("›", color = Gold, fontSize = 22.sp)
                }
                IconButton(onClick = { showDatePicker = true }) {
                    Text("▦", color = Gold, fontSize = 19.sp)
                }
                if (dayOffset != 0) {
                    TextButton(onClick = { dayOffset = 0 }) {
                        Text(when (language) {
                            AppLanguage.EN -> "Today"
                            AppLanguage.AR -> "اليوم"
                            AppLanguage.CKB -> "ئەمڕۆ"
                        }, fontSize = 10.sp)
                    }
                }
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
        Text(if (language == AppLanguage.EN) "Calculation method" else if (language == AppLanguage.AR) "طريقة الحساب" else "شێوازی حسابکردن", color = foreground, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            items(listOf("mwl", "umm_al_qura", "egypt", "karachi", "isna")) { rule ->
                val label = when (rule) {
                    "umm_al_qura" -> "Umm al-Qura"
                    "egypt" -> if (language == AppLanguage.EN) "Egypt" else if (language == AppLanguage.AR) "مصر" else "مەسر"
                    "karachi" -> "Karachi"
                    "isna" -> "ISNA"
                    else -> if (language == AppLanguage.EN) "MWL" else if (language == AppLanguage.AR) "رابطة العالم الإسلامي" else "یەکێتی جیهانی"
                }
                FilterChip(selected = calculationMethod == rule, onClick = {
                    calculationMethod = rule
                    prefs.edit().putString("prayer_method", rule).apply()
                }, label = { Text(label, fontSize = 10.sp, maxLines = 1) })
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (language == AppLanguage.EN) "Asr method" else if (language == AppLanguage.AR) "طريقة العصر" else "شێوازی عەسر", color = foreground.copy(alpha = 0.7f), fontSize = 12.sp, modifier = Modifier.weight(1f))
            FilterChip(selected = asrSchool == "shafi", onClick = {
                asrSchool = "shafi"; prefs.edit().putString("asr_school", "shafi").apply()
            }, label = { Text(if (language == AppLanguage.EN) "Standard" else if (language == AppLanguage.AR) "الشافعي" else "ستاندارد", fontSize = 10.sp) })
            Spacer(Modifier.width(6.dp))
            FilterChip(selected = asrSchool == "hanafi", onClick = {
                asrSchool = "hanafi"; prefs.edit().putString("asr_school", "hanafi").apply()
            }, label = { Text(if (language == AppLanguage.EN) "Hanafi" else if (language == AppLanguage.AR) "الحنفي" else "حەنەفی", fontSize = 10.sp) })
        }
        Spacer(Modifier.height(10.dp))
        if (dayOffset == 0 && nextPrayerTarget != null) {
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
                        Text(nextPrayerTarget.timeText, color = Color(0xFFF1D78D), fontSize = 14.sp)
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
        if (times == null) LoadingOrEmpty(loading, language, foreground) else {
            val timeItems = listOf(
                tr(language, "fajr") to times!!.fajr, tr(language, "sunrise") to times!!.sunrise,
                tr(language, "dhuhr") to times!!.dhuhr, tr(language, "asr") to times!!.asr,
                tr(language, "maghrib") to times!!.maghrib, tr(language, "isha") to times!!.isha
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
                    AppLanguage.EN -> "Source: ${times!!.source}. Times shown in local phone time."
                    AppLanguage.AR -> "المصدر: ${times!!.source}. الأوقات بتوقيت الهاتف المحلي."
                    AppLanguage.CKB -> "سەرچاوە: ${times!!.source}. کاتەکان بە کاتی مۆبایلەکەتن."
                }, color = foreground.copy(alpha = 0.48f), fontSize = 10.sp, lineHeight = 15.sp
            )
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
    if (showDatePicker) DatePickerDialog(
        onDismissRequest = { showDatePicker = false },
        confirmButton = {
            TextButton(onClick = {
                val selectedMillis = datePickerState.selectedDateMillis
                if (selectedMillis != null) {
                    val selectedDate = Instant.ofEpochMilli(selectedMillis).atZone(ZoneOffset.UTC).toLocalDate()
                    dayOffset = ChronoUnit.DAYS.between(LocalDate.now(), selectedDate).toInt().coerceIn(-366, 366)
                }
                showDatePicker = false
            }) {
                Text(if (language == AppLanguage.EN) "Apply" else if (language == AppLanguage.AR) "تطبيق" else "جێبەجێکردن")
            }
        },
        dismissButton = {
            TextButton(onClick = { showDatePicker = false }) {
                Text(tr(language, "back"))
            }
        }
    ) {
        DatePicker(
            state = datePickerState,
            title = {
                Text(
                    when (language) {
                        AppLanguage.EN -> "Choose prayer date"
                        AppLanguage.AR -> "اختر تاريخ المواقيت"
                        AppLanguage.CKB -> "بەرواری کاتەکانی نوێژ هەڵبژێرە"
                    }, modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp)
                )
            },
            showModeToggle = true
        )
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
                    items(visibleCities, key = { "${it.name}|${it.country}|${it.latitude}|${it.longitude}" }) { city ->
                        Column(
                            Modifier.fillMaxWidth().clickable {
                                onCity(city.name)
                                gpsLatitude = null
                                gpsLongitude = null
                                gpsAccuracy = null
                                manualLatitude = city.latitude
                                manualLongitude = city.longitude
                                prefs.edit()
                                    .remove("gps_latitude").remove("gps_longitude").remove("gps_accuracy")
                                    .putString("manual_city_latitude", city.latitude.toString())
                                    .putString("manual_city_longitude", city.longitude.toString())
                                    .putString("selected_city_country", city.country)
                                    .apply()
                                chooseCity = false
                            }.padding(vertical = 9.dp, horizontal = 4.dp)
                        ) {
                            Text(city.name, color = foreground, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            if (city.country.isNotBlank()) Text(city.country, color = foreground.copy(alpha = 0.52f), fontSize = 10.sp)
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
                        AppLanguage.EN -> "${visibleAdhanAssets.size} of ${adhanAssets.size} audio files"
                        AppLanguage.AR -> "${visibleAdhanAssets.size} من ${adhanAssets.size} ملفًا صوتيًا"
                        AppLanguage.CKB -> "${visibleAdhanAssets.size} لە ${adhanAssets.size} فایل دەنگییەکە"
                    }, color = foreground.copy(alpha = 0.55f), fontSize = 10.sp
                )
                LazyColumn(Modifier.height(340.dp)) {
                items(visibleAdhanAssets, key = { it.assetPath }) { sound ->
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            AudioController.stop()
                            previewingAdhan = ""
                            selectedAdhanAsset = sound.assetPath
                            prefs.edit().putString("adhan_audio_asset", sound.assetPath).apply()
                            chooseAdhanSound = false
                        }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedAdhanAsset == sound.assetPath, onClick = {
                            AudioController.stop()
                            previewingAdhan = ""
                            selectedAdhanAsset = sound.assetPath
                            prefs.edit().putString("adhan_audio_asset", sound.assetPath).apply()
                            chooseAdhanSound = false
                        })
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(sound.title, color = foreground, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(sound.assetPath.substringAfterLast('/'), color = foreground.copy(alpha = 0.5f), fontSize = 10.sp)
                        }
                        IconButton(onClick = {
                            if (previewingAdhan == sound.assetPath) {
                                AudioController.stop()
                                previewingAdhan = ""
                            } else {
                                AudioController.stop()
                                previewingAdhan = sound.assetPath
                                AudioController.play(context, sound.assetPath) { active ->
                                    if (!active) previewingAdhan = ""
                                }
                            }
                        }, modifier = Modifier.size(36.dp)) {
                            Icon(
                                if (previewingAdhan == sound.assetPath) Icons.Outlined.PauseCircle else Icons.Outlined.PlayCircle,
                                contentDescription = if (language == AppLanguage.EN) "Preview audio" else if (language == AppLanguage.AR) "معاينة الصوت" else "تاقیکردنەوەی دەنگ",
                                tint = Gold,
                                modifier = Modifier.size(22.dp)
                            )
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
    var city by remember { mutableStateOf<City?>(null) }
    var azimuth by remember { mutableFloatStateOf(0f) }
    var sensorAvailable by remember { mutableStateOf(false) }
    val prefs = remember { context.getSharedPreferences("mashxall_preferences", Context.MODE_PRIVATE) }
    var gpsLatitude by remember { mutableStateOf(prefs.getString("gps_latitude", null)?.toDoubleOrNull()) }
    var gpsLongitude by remember { mutableStateOf(prefs.getString("gps_longitude", null)?.toDoubleOrNull()) }
    val manualLatitude = prefs.getString("manual_city_latitude", null)?.toDoubleOrNull()
    val manualLongitude = prefs.getString("manual_city_longitude", null)?.toDoubleOrNull()
    val manualCountry = prefs.getString("selected_city_country", "") ?: ""
    var locating by remember { mutableStateOf(false) }
    var locationMessage by remember { mutableStateOf("") }
    val onFix: (Location) -> Unit = { fix ->
        gpsLatitude = fix.latitude
        gpsLongitude = fix.longitude
        prefs.edit()
            .putString("gps_latitude", fix.latitude.toString())
            .putString("gps_longitude", fix.longitude.toString())
            .putString("gps_accuracy", if (fix.hasAccuracy()) fix.accuracy.toString() else "")
            .apply()
        locating = false
        locationMessage = if (fix.hasAccuracy()) {
            when (language) {
                AppLanguage.EN -> "Location accuracy reported by Android: about ±${fix.accuracy.toInt()} m"
                AppLanguage.AR -> "دقة الموقع التي يذكرها الهاتف: نحو ±${fix.accuracy.toInt()} متر"
                AppLanguage.CKB -> "وردبینی شوێن بەپێی مۆبایل: نزیکەی ±${fix.accuracy.toInt()} مەتر"
            }
        } else {
            when (language) {
                AppLanguage.EN -> "Coordinates received; Android did not report accuracy."
                AppLanguage.AR -> "تم تحديد الإحداثيات، لكن الهاتف لم يذكر دقة القياس."
                AppLanguage.CKB -> "کۆئۆردینات وەرگیرا، بەڵام وردبینی نەدراوە."
            }
        }
    }
    val onLocationError: (String) -> Unit = { error -> locating = false; locationMessage = error }
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
            locationMessage = when (language) {
                AppLanguage.EN -> "Location permission was not granted; Qibla is using the selected city."
                AppLanguage.AR -> "لم تمنح إذن الموقع؛ ستُستخدم المدينة المختارة."
                AppLanguage.CKB -> "مۆڵەتی شوێن نەدرا؛ شاری هەڵبژێردراو بەکاردێت."
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
    LaunchedEffect(selectedCity, gpsLatitude, gpsLongitude, manualLatitude, manualLongitude, manualCountry) {
        city = when {
            gpsLatitude != null && gpsLongitude != null -> City("GPS", gpsLatitude!!, gpsLongitude!!)
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
    val bearing = city?.let { qiblaBearing(it.latitude, it.longitude) } ?: 0.0
    val relativeHeading = ((bearing.toFloat() - azimuth + 540f) % 360f) - 180f
    val animatedHeading by androidx.compose.animation.core.animateFloatAsState(
        targetValue = relativeHeading,
        animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessLow),
        label = "qiblaHeading"
    )
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(tr(language, "qibla"), color = foreground, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(28.dp))
        Box(Modifier.size(260.dp).clip(CircleShape).background(androidx.compose.ui.graphics.Brush.radialGradient(listOf(DeepGreen, Color(0xFF071E1A)))), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Explore, null, tint = Gold, modifier = Modifier.size(160.dp).rotate(animatedHeading))
            Text("N", color = MainText, modifier = Modifier.align(Alignment.TopCenter).padding(top = 18.dp))
            Text("🕋", fontSize = 26.sp)
        }
        Spacer(Modifier.height(24.dp))
        Text(city?.name ?: selectedCity, color = Gold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            if (sensorAvailable) {
                when (language) {
                    AppLanguage.CKB -> "ڕووگێڕانی کۆمپاوی مۆبایل چالاکە"
                    AppLanguage.AR -> "تم تصحيح الاتجاه ببوصلة الهاتف"
                    AppLanguage.EN -> "Direction calibrated by phone compass"
                }
            } else {
                when (language) {
                    AppLanguage.CKB -> "ئاڕاستەی گشتی بەپێی شار"
                    AppLanguage.AR -> "الاتجاه المحسوب حسب إحداثيات المدينة"
                    AppLanguage.EN -> "Estimated direction from city coordinates"
                }
            },
            color = foreground.copy(alpha = 0.65f), fontSize = 12.sp, textAlign = TextAlign.Center
        )
        Text(bearing.toInt().toString() + "° " + tr(language, "degrees"), color = foreground, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = locateNow, enabled = !locating) {
            if (locating) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            } else {
                Icon(Icons.Outlined.GpsFixed, null, tint = Gold, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(
                when (language) {
                    AppLanguage.EN -> if (locating) "Finding your location…" else "Use my current location"
                    AppLanguage.AR -> if (locating) "جارٍ تحديد موقعك…" else "استخدم موقعي الحالي"
                    AppLanguage.CKB -> if (locating) "شوێن دیاری دەکرێت…" else "شوێنی ئێستام بەکاربهێنە"
                }
            )
        }
        if (locationMessage.isNotBlank()) {
            Spacer(Modifier.height(5.dp))
            Text(locationMessage, color = foreground.copy(alpha = 0.65f), fontSize = 10.sp, textAlign = TextAlign.Center, lineHeight = 16.sp)
        }
        Spacer(Modifier.height(12.dp))
        Text(tr(language, "data_notice"), color = foreground.copy(alpha = 0.52f), fontSize = 11.sp, textAlign = TextAlign.Center, lineHeight = 18.sp)
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
    LaunchedEffect(initialSurah, initialAyah) {
        if (selected) {
            loading = true
            items = ContentRepository.loadWordAnalysis(context, initialSurah, initialAyah)
            verseContext = ContentRepository.loadAyahs(context, initialSurah, language.key, translationKey = "none").firstOrNull { it.number == initialAyah }
            loading = false
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
                    selected = true
                    onLoad(s, a)
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
                var expanded by rememberSaveable(item.id) { mutableStateOf(false) }
                val isFavorite = favorites.contains(item.id.toString())
                val title = item.title.ifBlank { item.badiniTitle }
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }
                ) {
                    Column(Modifier.padding(15.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(title, color = Gold, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                val key = item.id.toString()
                                favorites = if (isFavorite) favorites - key else favorites + key
                                favoritesPrefs.edit().putStringSet("farmuda_ids", favorites).apply()
                            }) {
                                Text(if (isFavorite) "★" else "☆", color = Gold, fontSize = 22.sp)
                            }
                            Text(if (expanded) "−" else "+", color = Gold, fontSize = 20.sp)
                        }
                        Spacer(Modifier.height(7.dp))
                        when (language) {
                            AppLanguage.CKB -> {
                                if (item.sorani.isNotBlank()) Text(item.sorani, color = foreground.copy(alpha = 0.84f), fontSize = 13.sp, lineHeight = 21.sp)
                                else if (item.arabic.isNotBlank()) Text(item.arabic, color = foreground, fontSize = 16.sp, lineHeight = 27.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                            }
                            AppLanguage.AR -> {
                                if (item.arabic.isNotBlank()) Text(item.arabic, color = foreground, fontSize = 16.sp, lineHeight = 27.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                                else Text(item.title, color = foreground, fontSize = 13.sp, lineHeight = 20.sp)
                            }
                            AppLanguage.EN -> {
                                if (item.arabic.isNotBlank()) {
                                    Text(item.arabic, color = foreground, fontSize = 16.sp, lineHeight = 27.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                                    Spacer(Modifier.height(5.dp))
                                }
                                Text(
                                    "The supplied source includes Arabic and Kurdish descriptions for this item; no separate English translation is present.",
                                    color = foreground.copy(alpha = 0.57f), fontSize = 10.sp, lineHeight = 16.sp
                                )
                            }
                        }
                        if (expanded) {
                            Spacer(Modifier.height(10.dp))
                            if (item.title.isNotBlank()) {
                                Text(if (language == AppLanguage.EN) "Source title" else if (language == AppLanguage.AR) "العنوان الأصلي" else "ناونیشانی سەرچاوە", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(item.title, color = foreground, fontSize = 12.sp, lineHeight = 18.sp)
                            }
                            if (item.arabic.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(if (language == AppLanguage.EN) "Arabic description" else if (language == AppLanguage.AR) "الوصف العربي" else "ڕوونکردنەوەی عەرەبی", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(item.arabic, color = foreground.copy(alpha = 0.82f), fontSize = 12.sp, lineHeight = 19.sp)
                            }
                            if (item.sorani.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(if (language == AppLanguage.EN) "Sorani Kurdish" else if (language == AppLanguage.AR) "الكردية السورانية" else "کوردی سۆرانی", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(item.sorani, color = foreground.copy(alpha = 0.82f), fontSize = 12.sp, lineHeight = 19.sp)
                            }
                            if (item.badiniTitle.isNotBlank() && item.badiniTitle != item.title) {
                                Spacer(Modifier.height(6.dp))
                                Text(if (language == AppLanguage.EN) "Badini title" else if (language == AppLanguage.AR) "العنوان الباديني" else "ناونیشانی بادینی", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(item.badiniTitle, color = foreground, fontSize = 12.sp, lineHeight = 19.sp)
                            }
                            if (item.badini.isNotBlank() && item.badini != item.sorani) {
                                Spacer(Modifier.height(6.dp))
                                Text(if (language == AppLanguage.EN) "Badini Kurdish" else if (language == AppLanguage.AR) "الكردية البادينية" else "کوردی بادینی", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(item.badini, color = foreground.copy(alpha = 0.82f), fontSize = 12.sp, lineHeight = 19.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
