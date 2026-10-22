package com.mashxall.unified

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Explore
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.mashxall.unified.data.*
import java.io.File
import java.security.MessageDigest
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
fun QuranScreen(language: AppLanguage, foreground: Color, cardColor: Color, initialSurah: Int, onSurah: (Int) -> Unit, onTafsir: (Int, Int) -> Unit) {
    val context = LocalContext.current
    var surahs by remember { mutableStateOf<List<Surah>>(emptyList()) }
    var selected by rememberSaveable { mutableIntStateOf(initialSurah) }
    var verses by remember { mutableStateOf<List<Ayah>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var query by rememberSaveable { mutableStateOf("") }
    var chooseSurah by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { surahs = ContentRepository.loadSurahs(context) }
    LaunchedEffect(initialSurah) { selected = initialSurah }
    LaunchedEffect(selected, language, query) {
        loading = true
        verses = ContentRepository.loadAyahs(context, selected, language.key, query.trim())
        loading = false
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
            Spacer(Modifier.height(5.dp))
            Text(currentSurah?.let { it.ayahCount.toString() + " • " + tr(language, "verse") } ?: "", color = foreground.copy(alpha = 0.55f), fontSize = 11.sp)
        }
        if (verses.isEmpty()) LoadingOrEmpty(loading, language, foreground) else LazyColumn(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                context.getSharedPreferences("mashxall_bookmarks", 0).edit()
                                    .putString("last_ayah", verse.surah.toString() + ":" + verse.number).apply()
                                Toast.makeText(context, tr(language, "available_data"), Toast.LENGTH_SHORT).show()
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
                var count by rememberSaveable(item.id) { mutableIntStateOf(0) }
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
                            Button(onClick = { count = if (count + 1 >= item.count.coerceAtLeast(1)) 0 else count + 1 }) {
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
    var playing by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { audio = ContentRepository.loadAudioItems(context); loading = false }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(tr(language, "audio"), color = foreground, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp)); Text(tr(language, "offline"), color = foreground.copy(alpha = 0.58f), fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        if (audio.isEmpty()) LoadingOrEmpty(loading, language, foreground) else LazyColumn(contentPadding = PaddingValues(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            items(audio, key = { it.assetPath }) { item ->
                Card(colors = CardDefaults.cardColors(containerColor = cardColor), shape = RoundedCornerShape(18.dp)) {
                    Row(Modifier.fillMaxWidth().clickable {
                        if (playing == item.assetPath) { AudioController.stop(); playing = "" }
                        else { playing = item.assetPath; AudioController.play(context, item.assetPath) { state -> if (!state) playing = "" } }
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
}

@Composable
fun BooksScreen(language: AppLanguage, foreground: Color, cardColor: Color) {
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
                    Card(colors = CardDefaults.cardColors(containerColor = cardColor), shape = RoundedCornerShape(15.dp)) {
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

@Composable
fun PrayerScreen(language: AppLanguage, foreground: Color, cardColor: Color, selectedCity: String, onCity: (String) -> Unit, onQibla: () -> Unit) {
    val context = LocalContext.current
    var cities by remember { mutableStateOf<List<City>>(emptyList()) }
    var times by remember { mutableStateOf<PrayerRow?>(null) }
    var chooseCity by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { cities = ContentRepository.loadCities(context) }
    LaunchedEffect(selectedCity) { loading = true; times = ContentRepository.loadPrayerTimes(context, selectedCity); loading = false }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(tr(language, "prayer"), color = foreground, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Card(colors = CardDefaults.cardColors(containerColor = DeepGreen), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth().clickable { chooseCity = true }) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Explore, null, tint = Gold, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text(tr(language, "city"), color = Color(0xFFF4E5B4), fontSize = 11.sp)
                    Text(selectedCity, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Text(tr(language, "select_city"), color = Color(0xFFB9CDC5), fontSize = 10.sp)
                }
                Icon(Icons.Outlined.Search, null, tint = Gold)
            }
        }
        Spacer(Modifier.height(15.dp))
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
        Text(tr(language, "data_notice"), color = foreground.copy(alpha = 0.45f), fontSize = 10.sp, lineHeight = 15.sp)
    }
    if (chooseCity) AlertDialog(
        onDismissRequest = { chooseCity = false }, title = { Text(tr(language, "select_city")) },
        text = {
            LazyColumn(Modifier.height(420.dp)) {
                items(cities, key = { it.name }) { city ->
                    Text(city.name, Modifier.fillMaxWidth().clickable { onCity(city.name); chooseCity = false }.padding(vertical = 10.dp), color = foreground)
                }
            }
        },
        confirmButton = { TextButton(onClick = { chooseCity = false }) { Text(tr(language, "back")) } }
    )
}

@Composable
fun QiblaScreen(language: AppLanguage, foreground: Color, cardColor: Color, selectedCity: String) {
    val context = LocalContext.current
    var city by remember { mutableStateOf<City?>(null) }
    LaunchedEffect(selectedCity) { city = ContentRepository.loadCities(context).firstOrNull { it.name.equals(selectedCity, true) } }
    val bearing = city?.let { qiblaBearing(it.latitude, it.longitude) } ?: 0.0
    Column(Modifier.fillMaxSize().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(tr(language, "qibla"), color = foreground, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(28.dp))
        Box(Modifier.size(260.dp).clip(CircleShape).background(androidx.compose.ui.graphics.Brush.radialGradient(listOf(DeepGreen, Color(0xFF071E1A)))), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Explore, null, tint = Gold, modifier = Modifier.size(160.dp).rotate(bearing.toFloat()))
            Text("N", color = MainText, modifier = Modifier.align(Alignment.TopCenter).padding(top = 18.dp))
            Text("🕋", fontSize = 26.sp)
        }
        Spacer(Modifier.height(24.dp))
        Text(city?.name ?: selectedCity, color = Gold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(tr(language, "qibla_heading"), color = foreground.copy(alpha = 0.65f), fontSize = 12.sp)
        Text(bearing.toInt().toString() + "° " + tr(language, "degrees"), color = foreground, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(18.dp))
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
    var surahText by rememberSaveable { mutableStateOf(initialSurah.toString()) }
    var ayahText by rememberSaveable { mutableStateOf(initialAyah.toString()) }
    var items by remember { mutableStateOf<List<WordItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(false) }
    LaunchedEffect(initialSurah, initialAyah) {
        if (selected) {
            loading = true
            items = ContentRepository.loadWordAnalysis(context, initialSurah, initialAyah)
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
            }
            items(items, key = { it.position }) { word ->
                Card(colors = CardDefaults.cardColors(containerColor = cardColor), shape = RoundedCornerShape(17.dp)) {
                    Column(Modifier.fillMaxWidth().padding(13.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(28.dp).clip(CircleShape).background(DeepGreen), contentAlignment = Alignment.Center) {
                                Text(word.position.toString(), color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(word.arabicShape, color = foreground, fontSize = 23.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
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
}

@Composable
fun FarmudaScreen(language: AppLanguage, foreground: Color, cardColor: Color) {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    var records by remember { mutableStateOf<List<FarmudaItem>>(emptyList()) }
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
