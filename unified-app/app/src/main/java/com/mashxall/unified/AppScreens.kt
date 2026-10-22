package com.mashxall.unified

import android.content.Context
import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import android.graphics.Bitmap
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
import android.widget.Toast
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.mashxall.unified.data.*
import com.caverock.androidsvg.SVG
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
fun QuranScreen(language: AppLanguage, foreground: Color, cardColor: Color, initialSurah: Int, initialAyah: Int, onSurah: (Int) -> Unit, onTafsir: (Int, Int) -> Unit, onBookmark: (Int, Int) -> Unit) {
    val context = LocalContext.current
    var surahs by remember { mutableStateOf<List<Surah>>(emptyList()) }
    var selected by rememberSaveable { mutableIntStateOf(initialSurah) }
    var verses by remember { mutableStateOf<List<Ayah>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var query by rememberSaveable { mutableStateOf("") }
    var chooseSurah by remember { mutableStateOf(false) }
    var mushafMode by rememberSaveable { mutableStateOf(false) }
    var mushafPage by rememberSaveable { mutableIntStateOf(1) }
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) { surahs = ContentRepository.loadSurahs(context) }
    LaunchedEffect(initialSurah) { selected = initialSurah }
    LaunchedEffect(selected, language, query, initialAyah) {
        loading = true
        val loaded = ContentRepository.loadAyahs(context, selected, language.key, query.trim())
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
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(tr(language, "quran"), color = foreground, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(currentSurah?.arabicName ?: tr(language, "choose_surah"), color = Gold, fontSize = 14.sp)
                }
                OutlinedButton(onClick = { chooseSurah = true }) { Text(tr(language, "choose_surah"), fontSize = 11.sp) }
            }
            Spacer(Modifier.height(8.dp))
            SearchBox(query, { query = it }, tr(language, "search"))
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
                Text(currentSurah?.let { it.ayahCount.toString() + " • " + tr(language, "verse") } ?: "", color = foreground.copy(alpha = 0.55f), fontSize = 11.sp)
            }
        }
        if (mushafMode) {
            MushafPageViewer(language, foreground, mushafPage) { requested ->
                mushafPage = requested.coerceIn(1, 604)
            }
        } else if (verses.isEmpty()) {
            LoadingOrEmpty(loading, language, foreground)
        } else LazyColumn(state = listState, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(verses, key = { it.surah.toString() + ":" + it.number }) { verse ->
                Card(colors = CardDefaults.cardColors(containerColor = cardColor), shape = RoundedCornerShape(20.dp), border = BorderStroke(0.6.dp, Gold.copy(alpha = 0.18f))) {
                    Column(Modifier.padding(15.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(32.dp).clip(CircleShape).background(DeepGreen), contentAlignment = Alignment.Center) {
                                Text(verse.number.toString(), color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(tr(language, "ayah") + " " + verse.number, color = foreground.copy(alpha = 0.5f), fontSize = 11.sp)
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { onTafsir(verse.surah, verse.number) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Outlined.AutoStories, tr(language, "tafsir"), tint = Gold, modifier = Modifier.size(19.dp))
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
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(verse.arabic, color = foreground, fontSize = 23.sp, lineHeight = 39.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
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
                    Row(Modifier.fillMaxWidth().clickable { selected = item.number; onSurah(item.number); chooseSurah = false }.padding(vertical = 9.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(item.number.toString(), color = Gold, modifier = Modifier.width(32.dp))
                        Text(item.arabicName, color = foreground, modifier = Modifier.weight(1f))
                        Text(item.englishName, color = foreground.copy(alpha = 0.5f), fontSize = 10.sp)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { chooseSurah = false }) { Text(tr(language, "back")) } }
    )
}

@Composable
private fun MushafPageViewer(
    language: AppLanguage,
    foreground: Color,
    page: Int,
    onPageChange: (Int) -> Unit
) {
    val context = LocalContext.current
    var bitmap by remember(page) { mutableStateOf<Bitmap?>(null) }
    var loading by remember(page) { mutableStateOf(true) }
    var failed by remember(page) { mutableStateOf(false) }
    LaunchedEffect(page) {
        loading = true
        failed = false
        bitmap = null
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
        }
        Spacer(Modifier.height(8.dp))
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
        Spacer(Modifier.height(5.dp))
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
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(tr(language, "bookmarks"), color = foreground, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        if (bookmarks.isEmpty()) {
            Text(tr(language, "no_bookmarks"), color = foreground.copy(alpha = 0.6f), fontSize = 13.sp)
        } else {
            LazyColumn(contentPadding = PaddingValues(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(bookmarks, key = { it.first.toString() + ":" + it.second.toString() }) { saved ->
                    Card(colors = CardDefaults.cardColors(containerColor = cardColor), shape = RoundedCornerShape(17.dp)) {
                        Row(Modifier.fillMaxWidth().clickable { onOpen(saved.first, saved.second) }.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.BookmarkBorder, null, tint = Gold)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(tr(language, "surah") + " " + saved.first, color = foreground, fontWeight = FontWeight.SemiBold)
                                Text(tr(language, "ayah") + " " + saved.second, color = foreground.copy(alpha = 0.55f), fontSize = 12.sp)
                            }
                            TextButton(onClick = {
                                val remaining = bookmarks.filterNot { it == saved }
                                bookmarks = remaining
                                prefs.edit().putStringSet("bookmarks", remaining.map { it.first.toString() + ":" + it.second.toString() }.toSet()).apply()
                            }) { Text("×", color = Gold, fontSize = 20.sp) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TafsirScreen(language: AppLanguage, foreground: Color, cardColor: Color, surah: Int, ayah: Int, onChangeVerse: (Int, Int) -> Unit) {
    val context = LocalContext.current
    var selectedBook by rememberSaveable { mutableStateOf(if (language == AppLanguage.EN) "sahih" else if (language == AppLanguage.AR) "mukhtasar" else "roshn") }
    var text by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(selectedBook, surah, ayah) { loading = true; text = ContentRepository.loadTafsir(context, selectedBook, surah, ayah); loading = false }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(16.dp)) {
            Text(tr(language, "tafsir"), color = foreground, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                items(ContentRepository.tafsirBooks.filter { it.id != "noor" }) { book ->
                    FilterChip(selected = selectedBook == book.id, onClick = { selectedBook = book.id }, label = { Text(tafsirLabel(book, language), fontSize = 11.sp, maxLines = 1) })
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { onChangeVerse(surah, (ayah - 1).coerceAtLeast(1)) }) { Text("‹") }
                Text(tr(language, "surah") + " " + surah + " • " + tr(language, "ayah") + " " + ayah, color = foreground, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                OutlinedButton(onClick = { onChangeVerse(surah, ayah + 1) }) { Text("›") }
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
}
private fun tafsirLabel(book: TafsirBook, language: AppLanguage): String = when (language) {
    AppLanguage.CKB -> book.labelCkb
    AppLanguage.AR -> book.labelAr
    AppLanguage.EN -> book.labelEn
}

@Composable
fun HadithScreen(language: AppLanguage, foreground: Color, cardColor: Color) {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    var items by remember { mutableStateOf<List<HadithItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(query) { loading = true; items = ContentRepository.loadHadiths(context, query.trim()); loading = false }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(tr(language, "hadith"), color = foreground, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp)); SearchBox(query, { query = it }, tr(language, "search_hadith"))
        }
        if (items.isEmpty()) LoadingOrEmpty(loading, language, foreground) else LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(items, key = { it.id }) { item ->
                Card(colors = CardDefaults.cardColors(containerColor = cardColor), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(15.dp)) {
                        Text("#" + item.id, color = Gold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Spacer(Modifier.height(7.dp))
                        Text(item.arabic, color = foreground, fontSize = 18.sp, lineHeight = 31.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                        val body = when (language) { AppLanguage.CKB -> item.sorani; AppLanguage.AR -> item.arabic; AppLanguage.EN -> item.english }
                        if (body.isNotBlank() && body != item.arabic) {
                            Spacer(Modifier.height(8.dp))
                            Text(body.replace("<rr>", "").replace("</rr>", ""), color = foreground.copy(alpha = 0.8f), fontSize = 13.sp, lineHeight = 21.sp)
                        }
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
    var query by rememberSaveable { mutableStateOf("") }
    var items by remember { mutableStateOf(emptyList<AzkarItem>()) }
    var loading by remember { mutableStateOf(true) }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(query, selectedTab) {
        loading = true
        items = ContentRepository.loadAzkar(context, language.key, query.trim()).filter {
            selectedTab == 0 || (selectedTab == 1 && it.titleCkb.contains("بەیانی", true)) || (selectedTab == 2 && it.titleCkb.contains("ئێوار", true))
        }
        loading = false
    }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(tr(language, "azkar"), color = foreground, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp)); SearchBox(query, { query = it }, tr(language, "search_azkar"))
            Spacer(Modifier.height(7.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                items(listOf(0, 1, 2)) { index ->
                    FilterChip(selected = selectedTab == index, onClick = { selectedTab = index }, label = {
                        Text(when (language) {
                            AppLanguage.CKB -> listOf("هەموو", "بەیانی", "ئێواران")[index]
                            AppLanguage.AR -> listOf("الكل", "الصباح", "المساء")[index]
                            AppLanguage.EN -> listOf("All", "Morning", "Evening")[index]
                        })
                    })
                }
            }
        }
        if (items.isEmpty()) LoadingOrEmpty(loading, language, foreground) else LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(items, key = { it.id }) { item ->
                var count by rememberSaveable(item.id) { mutableIntStateOf(progressPrefs.getInt(item.id.toString(), 0)) }
                Card(colors = CardDefaults.cardColors(containerColor = cardColor), shape = RoundedCornerShape(20.dp), border = BorderStroke(0.6.dp, Gold.copy(alpha = 0.18f))) {
                    Column(Modifier.padding(15.dp)) {
                        Text(when (language) { AppLanguage.CKB -> item.titleCkb; AppLanguage.AR -> item.titleAr; AppLanguage.EN -> item.titleEn }, color = Gold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(item.arabic, color = foreground, fontSize = 18.sp, lineHeight = 31.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                        val translation = when (language) { AppLanguage.CKB -> item.ckb; AppLanguage.AR -> ""; AppLanguage.EN -> item.english }
                        if (translation.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(translation, color = foreground.copy(alpha = 0.78f), fontSize = 13.sp, lineHeight = 21.sp)
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (language == AppLanguage.EN) "Repeat: $count / " + item.count else if (language == AppLanguage.AR) "التكرار: $count / " + item.count else "ژمارە: $count / " + item.count, color = foreground.copy(alpha = 0.64f), fontSize = 11.sp, modifier = Modifier.weight(1f))
                            Button(onClick = {
                                count = if (count + 1 >= item.count.coerceAtLeast(1)) 0 else count + 1
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
    var names by remember { mutableStateOf<List<AllahName>>(emptyList()) }
    LaunchedEffect(Unit) { names = ContentRepository.loadNames(context) }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(tr(language, "names"), color = foreground, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        if (names.isEmpty()) LoadingOrEmpty(false, language, foreground) else LazyColumn(contentPadding = PaddingValues(vertical = 7.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            items(names, key = { it.number }) { name ->
                Card(colors = CardDefaults.cardColors(containerColor = cardColor), shape = RoundedCornerShape(18.dp)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(42.dp).clip(CircleShape).background(DeepGreen), contentAlignment = Alignment.Center) {
                            Text(name.number.toString(), color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(name.arabic, color = Gold, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                            Text(when (language) { AppLanguage.CKB -> name.ckb; AppLanguage.AR -> name.arabic; AppLanguage.EN -> name.english }, color = foreground, fontSize = 12.sp, lineHeight = 18.sp)
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
    var count by rememberSaveable { mutableIntStateOf(prefs.getInt("tasbih_count", 0)) }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(tr(language, "tasbih"), color = foreground, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(if (language == AppLanguage.EN) "SubhanAllah • Alhamdulillah • Allahu Akbar" else if (language == AppLanguage.AR) "سبحان الله • الحمد لله • الله أكبر" else "سوبحان الله • الحمد لله • الله أكبر", color = Gold, textAlign = TextAlign.Center, fontSize = 14.sp)
        Spacer(Modifier.height(28.dp))
        Box(Modifier.size(238.dp).clip(CircleShape).background(androidx.compose.ui.graphics.Brush.radialGradient(listOf(Color(0xFF1E7259), DeepGreen, CardDark))).clickable {
            count += 1
            prefs.edit().putInt("tasbih_count", count).apply()
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }, contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(count.toString(), color = Color.White, fontSize = 54.sp, fontWeight = FontWeight.ExtraBold)
                Text(if (language == AppLanguage.EN) "TAP TO COUNT" else if (language == AppLanguage.AR) "اضغط للعدّ" else "بۆ ژماردن لێی بدە", color = Gold, fontSize = 10.sp, letterSpacing = 1.4.sp)
            }
        }
        Spacer(Modifier.height(23.dp))
        OutlinedButton(onClick = { count = 0; prefs.edit().putInt("tasbih_count", 0).apply() }) { Text(tr(language, "reset")) }
    }
}

@Composable
fun AudioScreen(language: AppLanguage, foreground: Color, cardColor: Color) {
    val context = LocalContext.current
    var audio by remember { mutableStateOf<List<AudioItem>>(emptyList()) }
    var reciters by remember { mutableStateOf<List<Reciter>>(emptyList()) }
    var playing by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var surahText by rememberSaveable { mutableStateOf("1") }
    var ayahText by rememberSaveable { mutableStateOf("1") }
    LaunchedEffect(Unit) {
        audio = ContentRepository.loadAudioItems(context)
        reciters = ContentRepository.loadReciters(context)
        loading = false
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
        if (selectedTab == 0 || selectedTab == 2) {
            val visibleAudio = if (selectedTab == 2) audio.filter { item ->
                val name = (item.title + " " + item.assetPath + " " + item.group).lowercase()
                listOf("adhan", "athan", "azan", "madina", "mecca", "fajr", "hijaz", "takbir", "ramazan", "nasr", "mshari").any { name.contains(it) }
            } else audio
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
            Text(
                if (language == AppLanguage.EN) "Streaming requires an internet connection. Pick a verse to audition a reciter."
                else if (language == AppLanguage.AR) "يحتاج البث إلى اتصال بالإنترنت. اختر آية لتجربة القارئ."
                else "بۆ گوێگرتن پێویستە ئینتەرنێتت هەبێت؛ ئایەتێک هەڵبژێرە بۆ تاقیکردنەوەی دەنگ.",
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
            else LazyColumn(contentPadding = PaddingValues(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(reciters, key = { it.id }) { reciter ->
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
                            }
                            IconButton(onClick = {
                                if (playing == url) {
                                    AudioController.stop()
                                    playing = ""
                                } else {
                                    playing = url
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

@Composable
fun BooksScreen(language: AppLanguage, foreground: Color, cardColor: Color, onOpenPdf: (String) -> Unit) {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    var books by remember { mutableStateOf<List<BookItem>>(emptyList()) }
    var pdfs by remember { mutableStateOf<List<String>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(query) { loading = true; books = ContentRepository.loadBooks(context, query.trim()); pdfs = if (query.isBlank()) ContentRepository.loadPdfPaths(context) else emptyList(); loading = false }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(tr(language, "books"), color = foreground, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp)); SearchBox(query, { query = it }, tr(language, "search"))
        }
        if (books.isEmpty() && pdfs.isEmpty()) LoadingOrEmpty(loading, language, foreground) else LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            if (pdfs.isNotEmpty()) {
                item { Text("PDF • " + pdfs.size, color = Gold, fontWeight = FontWeight.Bold) }
                items(pdfs, key = { it }) { path ->
                    Card(Modifier.clickable { onOpenPdf(path) }, colors = CardDefaults.cardColors(containerColor = cardColor), shape = RoundedCornerShape(15.dp)) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.MenuBook, null, tint = Gold)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(path.substringAfterLast('/'), color = foreground, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                Text(path.substringBeforeLast('/'), color = foreground.copy(alpha = 0.5f), fontSize = 9.sp)
                            }
                        }
                    }
                }
            }
            if (books.isNotEmpty()) item { Text(tr(language, "books"), color = Gold, fontWeight = FontWeight.Bold) }
            items(books, key = { "book-" + it.id }) { item ->
                Card(colors = CardDefaults.cardColors(containerColor = cardColor), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text(item.title, color = foreground, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        if (item.writer.isNotBlank()) Text(item.writer, color = Gold, fontSize = 11.sp)
                        if (item.description.isNotBlank()) Text(item.description.take(320), color = foreground.copy(alpha = 0.68f), fontSize = 11.sp, lineHeight = 17.sp)
                        Text(item.pages.toString() + " • " + tr(language, "book_online"), color = foreground.copy(alpha = 0.45f), fontSize = 10.sp)
                        if (item.url.startsWith("http")) TextButton(onClick = {
                            try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.url))) }
                            catch (_: Exception) { Toast.makeText(context, tr(language, "no_data"), Toast.LENGTH_SHORT).show() }
                        }) { Text(if (language == AppLanguage.EN) "Open external link" else if (language == AppLanguage.AR) "فتح الرابط" else "کردنەوەی بەستەر") }
                    }
                }
            }
        }
    }
}

@Composable
fun PdfReaderScreen(language: AppLanguage, foreground: Color, assetPath: String) {
    val context = LocalContext.current
    var pageIndex by rememberSaveable(assetPath) { mutableIntStateOf(0) }
    var pageCount by remember(assetPath) { mutableIntStateOf(0) }
    var bitmap by remember(assetPath) { mutableStateOf<Bitmap?>(null) }
    var loading by remember { mutableStateOf(true) }
    var loadFailed by remember { mutableStateOf(false) }

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
                    val localPdf = File(context.cacheDir, "mashxall-$digest.pdf")
                    if (!localPdf.exists() || localPdf.length() == 0L) {
                        context.assets.open(assetPath).use { input ->
                            localPdf.outputStream().use { output -> input.copyTo(output, 128 * 1024) }
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
    var query by rememberSaveable { mutableStateOf("") }
    var entries by remember { mutableStateOf<List<LexiconItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(query) { loading = true; entries = ContentRepository.searchLexicon(context, query.trim()); loading = false }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(tr(language, "lexicon"), color = foreground, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp)); SearchBox(query, { query = it }, tr(language, "search_words"))
        }
        if (entries.isEmpty()) LoadingOrEmpty(loading, language, foreground) else LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(entries, key = { it.title + it.root }) { item ->
                Card(colors = CardDefaults.cardColors(containerColor = cardColor), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text(item.title, color = Gold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        if (item.root.isNotBlank()) Text(item.root, color = foreground.copy(alpha = 0.55f), fontSize = 10.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(item.content.take(1000), color = foreground, fontSize = 12.sp, lineHeight = 19.sp)
                        if (item.reference.isNotBlank()) Text(item.reference, color = foreground.copy(alpha = 0.5f), fontSize = 10.sp)
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
    var bestAccuracy = Float.MAX_VALUE
    var delivered = 0
    var gotFix = false
    val listeners = mutableListOf<LocationListener>()
    val mainHandler = Handler(Looper.getMainLooper())
    val timeout = Runnable {
        if (!gotFix) {
            listeners.forEach { item -> try { manager.removeUpdates(item) } catch (_: Exception) {} }
            onError("No fresh location fix. Turn on GPS and try again outdoors.")
        }
    }
    mainHandler.postDelayed(timeout, 20_000L)
    providers.forEach { provider ->
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                delivered++
                gotFix = true
                mainHandler.removeCallbacks(timeout)
                if ((location.hasAccuracy() && location.accuracy < bestAccuracy) || bestAccuracy == Float.MAX_VALUE) {
                    bestAccuracy = if (location.hasAccuracy()) location.accuracy else Float.MAX_VALUE - 1f
                    onLocation(location)
                }
                if (delivered >= providers.size) {
                    listeners.forEach { item -> try { manager.removeUpdates(item) } catch (_: Exception) {} }
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
            mainHandler.removeCallbacks(timeout)
            onError("Location permission is required")
        } catch (_: IllegalArgumentException) {
            delivered++
        }
    }
}

@Composable
fun PrayerScreen(language: AppLanguage, foreground: Color, cardColor: Color, selectedCity: String, onCity: (String) -> Unit, onQibla: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("mashxall_preferences", Context.MODE_PRIVATE) }
    var cities by remember { mutableStateOf<List<City>>(emptyList()) }
    var times by remember { mutableStateOf<PrayerRow?>(null) }
    var chooseCity by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var locating by remember { mutableStateOf(false) }
    var locationMessage by remember { mutableStateOf("") }
    var gpsLatitude by remember { mutableStateOf(prefs.getString("gps_latitude", null)?.toDoubleOrNull()) }
    var gpsLongitude by remember { mutableStateOf(prefs.getString("gps_longitude", null)?.toDoubleOrNull()) }
    var gpsAccuracy by remember { mutableStateOf(prefs.getString("gps_accuracy", null)?.toFloatOrNull()) }
    var calculationMethod by rememberSaveable { mutableStateOf(prefs.getString("prayer_method", "mwl") ?: "mwl") }
    var asrSchool by rememberSaveable { mutableStateOf(prefs.getString("asr_school", "shafi") ?: "shafi") }

    val onFix: (Location) -> Unit = { fix ->
        locating = false
        if (gpsLatitude == null || !fix.hasAccuracy() || gpsAccuracy == null || fix.accuracy <= gpsAccuracy!!) {
            gpsLatitude = fix.latitude
            gpsLongitude = fix.longitude
            gpsAccuracy = if (fix.hasAccuracy()) fix.accuracy else null
            prefs.edit()
                .putString("gps_latitude", fix.latitude.toString())
                .putString("gps_longitude", fix.longitude.toString())
                .putString("gps_accuracy", if (fix.hasAccuracy()) fix.accuracy.toString() else "")
                .apply()
            cities.minByOrNull { city ->
                PrayerTimeCalculator.distanceKm(fix.latitude, fix.longitude, city.latitude, city.longitude)
            }?.let { onCity(it.name) }
            locationMessage = if (fix.hasAccuracy()) {
                val acc = fix.accuracy.toInt().toString()
                when (language) {
                    AppLanguage.EN -> "Location accuracy reported by Android: about ±$acc m"
                    AppLanguage.AR -> "دقة الموقع التي يذكرها الهاتف: نحو ±$acc متر"
                    AppLanguage.CKB -> "وردبینی شوێن بەپێی مۆبایل: نزیکەی ±$acc مەتر"
                }
            } else {
                when (language) {
                    AppLanguage.EN -> "Coordinates received; Android did not report accuracy."
                    AppLanguage.AR -> "تم تحديد الإحداثيات، لكن الهاتف لم يذكر دقة القياس."
                    AppLanguage.CKB -> "کۆئۆردینات وەرگیرا، بەڵام وردبینی نەدراوە."
                }
            }
        }
    }
    val onLocationError: (String) -> Unit = { error ->
        locating = false
        locationMessage = error
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

    LaunchedEffect(Unit) { cities = ContentRepository.loadCities(context) }
    LaunchedEffect(selectedCity, gpsLatitude, gpsLongitude, calculationMethod, asrSchool) {
        loading = true
        times = if (gpsLatitude != null && gpsLongitude != null) {
            ContentRepository.loadPrayerTimesAtLocation(
                context, gpsLatitude!!, gpsLongitude!!, Date(),
                PrayerCalculationMethod.fromId(calculationMethod),
                AsrCalculationSchool.fromId(asrSchool)
            )
        } else {
            ContentRepository.loadPrayerTimes(context, selectedCity)
        }
        loading = false
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(tr(language, "prayer"), color = foreground, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
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
                    IconButton(onClick = { chooseCity = true }) { Icon(Icons.Outlined.Search, null, tint = Gold) }
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
        onDismissRequest = { chooseCity = false }, title = { Text(tr(language, "select_city")) },
        text = {
            LazyColumn(Modifier.height(420.dp)) {
                items(cities, key = { it.name }) { city ->
                    Text(city.name, Modifier.fillMaxWidth().clickable {
                        onCity(city.name)
                        gpsLatitude = null
                        gpsLongitude = null
                        gpsAccuracy = null
                        prefs.edit().remove("gps_latitude").remove("gps_longitude").remove("gps_accuracy").apply()
                        chooseCity = false
                    }.padding(vertical = 10.dp), color = foreground)
                }
            }
        },
        confirmButton = { TextButton(onClick = { chooseCity = false }) { Text(tr(language, "back")) } }
    )
}

undefinedist<FarmudaItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(query) {
        loading = true
        records = ContentRepository.loadFarmuda(context, query.trim())
        loading = false
    }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(tr(language, "farmuda"), color = foreground, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            SearchBox(query, { query = it }, tr(language, "search"))
        }
        if (records.isEmpty()) LoadingOrEmpty(loading, language, foreground)
        else LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            items(records, key = { it.id }) { item ->
                Card(colors = CardDefaults.cardColors(containerColor = cardColor), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(15.dp)) {
                        Text(item.title.ifBlank { item.badiniTitle }, color = Gold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(7.dp))
                        val translation = when (language) {
                            AppLanguage.CKB -> item.sorani.ifBlank { item.badini }
                            AppLanguage.AR -> item.arabic
                            AppLanguage.EN -> item.arabic
                        }
                        if (item.arabic.isNotBlank()) {
                            Text(item.arabic, color = foreground, fontSize = 17.sp, lineHeight = 28.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(7.dp))
                        }
                        if (translation.isNotBlank()) Text(translation, color = foreground.copy(alpha = 0.82f), fontSize = 13.sp, lineHeight = 21.sp)
                    }
                }
            }
        }
    }
}
