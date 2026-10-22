package com.mashxall.unified.data

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import android.text.Html
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream
import org.json.JSONArray
import org.json.JSONObject
import org.tukaani.xz.XZInputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Surah(val number: Int, val arabicName: String, val englishName: String, val ayahCount: Int, val meaningEnglish: String = "", val place: String = "", val uniqueTitle: String = "")
data class QuranTranslation(val id: String, val ckb: String, val arabic: String, val english: String)
data class Ayah(val surah: Int, val number: Int, val arabic: String, val translation: String, val page: Int = 0)
data class HadithItem(
    val id: Int, val arabic: String, val sorani: String, val english: String, val bookId: Int,
    val bookName: String = "", val bookNameAr: String = "", val bookNameEn: String = "",
    val chapterName: String = "", val chapterNameAr: String = "", val chapterNameEn: String = ""
)
data class AzkarItem(val id: Int, val titleCkb: String, val titleAr: String, val titleEn: String, val arabic: String, val ckb: String, val english: String, val count: Int, val referenceCkb: String)
data class AllahName(val number: Int, val arabic: String, val ckb: String, val badini: String, val english: String)
data class City(val name: String, val latitude: Double, val longitude: Double)
data class PrayerRow(
    val city: String, val date: String, val fajr: String, val sunrise: String,
    val dhuhr: String, val asr: String, val maghrib: String, val isha: String,
    val source: String = "timetable", val latitude: Double? = null, val longitude: Double? = null
)
data class TafsirBook(val id: String, val labelCkb: String, val labelAr: String, val labelEn: String, val assetPath: String, val language: String)
data class BookItem(val id: Int, val title: String, val writer: String, val description: String, val pages: Int, val url: String)
data class LexiconItem(val title: String, val content: String, val reference: String, val root: String)
data class WordPart(
    val glyphText: String,
    val typeId: Int,
    val tag: String,
    val labelCkb: String,
    val labelArabic: String,
    val labelEnglish: String,
    val colorHex: String
)
data class WordItem(
    val position: Int, val root: String, val grammar: String, val arabicShape: String,
    val parts: List<WordPart> = emptyList()
)
private data class WordTypeInfo(val tag: String, val english: String, val arabic: String, val ckb: String, val color: String)
data class AudioItem(val assetPath: String, val title: String, val group: String)
data class Reciter(val id: Int, val nameArabic: String, val nameEnglish: String, val audioType: Int, val baseUrl: String, val order: Int)
data class FarmudaItem(val id: Int, val title: String, val arabic: String, val sorani: String, val badiniTitle: String, val badini: String, val preferredTime: Int)
data class ContentCounts(val databases: Int, val audioFiles: Int, val fonts: Int, val pdfs: Int, val assetFiles: Int)
private data class SalawatiAzkarText(val ckb: String, val english: String, val reference: String)
private data class KhairahPageMeta(
    val id: Int, val volume: String, val page: String, val offset: Int,
    val length: Int, val byteCount: Int, val sha256: String
)
private data class MushafSvgMeta(
    val page: Int, val offset: Int, val compressedLength: Int,
    val uncompressedLength: Int, val sha256: String
)

private fun normalizeArabicKey(value: String): String = value.lowercase(Locale.ROOT).filterNot { ch ->
    ch in '\u064B'..'\u065F' || ch == '\u0670' || ch == '\u0640' || !ch.isLetterOrDigit()
}

object ContentRepository {
    const val QURAN_DB = "content/noor_tafsir_1.2.2/base/assets/quran.db"
    const val PRAYER_DB = "content/noor_tafsir_1.2.2/base/assets/prayertimes.db"
    const val HADITH_DB = "content/noor_tafsir_1.2.2/base/assets/db_hadith.db"
    const val WORD_DB = "content/noor_tafsir_1.2.2/base/assets/db_word_by_word.db"
    const val TRANSLITERATION_DB = "content/noor_tafsir_1.2.2/base/assets/kurdish_transliteration.db"
    const val LEXICON_DB = "content/noor_tafsir_1.2.2/base/assets/db_words_lexicon.db"
    const val AZKAR_DB = "content/salawati_3.1.3/base/assets/database/muslim_db_v2.5.1.db"
    const val SALAWATI_DB = AZKAR_DB
    const val BOOKS_DB = "content/noor_tafsir_1.2.2/base/assets/tn_books.db"
    private const val MUSHAF_SVG_BIN = "content/khairah_1.2/base/assets/mushaf/pages.bin"
    private const val MUSHAF_SVG_INDEX = "mushaf_svg_index.json"
    @Volatile private var cachedMushafIndex: Map<Int, MushafSvgMeta>? = null

    val quranTranslations = listOf(
        QuranTranslation("puxta", "پوختە", "كردي (پۇختە)", "Sorani — Puxta"),
        QuranTranslation("raman", "ڕەمان", "كردي (رەمان)", "Sorani — Raman"),
        QuranTranslation("asan", "ئاسان", "كردي (ئاسان)", "Sorani — Asan"),
        QuranTranslation("sanahi", "ساناهی (بادینی)", "كردي باديني", "Badini — Sanahi"),
        QuranTranslation("sahih", "Sahih International", "Sahih International", "English — Sahih International"),
        QuranTranslation("translit", "خوێندنەوەی بە پیتی لاتینی", "النطق الكردي بالحروف اللاتينية", "Kurdish transliteration"),
        QuranTranslation("none", "بێ وەرگێڕان", "النص العربي فقط", "Arabic text only")
    )

    val tafsirBooks = listOf(
        TafsirBook("roshn", "ڕۆشن", "تفسير ڕۆشن", "Roshn Kurdish Tafsir", "content/noor_tafsir_1.2.2/base/assets/tafseeri_roshn.db", "ckb"),
        TafsirBook("hazhar", "هەژار", "تفسير هەژار", "Hazhar Kurdish Tafsir", "content/noor_tafsir_1.2.2/base/assets/tafseeri_hazhar.db", "ckb"),
        TafsirBook("maisar", "ميسەر", "التيسير", "Muyassar Tafsir", "content/noor_tafsir_1.2.2/base/assets/tafseer_maisar.db", "ckb"),
        TafsirBook("sahih", "Sahih International", "Sahih International", "Sahih International", "content/noor_tafsir_1.2.2/base/assets/tafseer_sahih_international.db", "en"),
        TafsirBook("noor", "تفسير نور", "تفسیر نور", "Noor Tafsir", "content/noor_tafsir_1.2.2/base/assets/tafseeri_noor.db", "fa"),
        TafsirBook("hawrami", "هەورامی", "التفسير الهورامي", "Hawrami Tafsir", "content/noor_tafsir_1.2.2/base/assets/tafseeri_hawrami.db", "ckb"),
        TafsirBook("runahi", "ڕووناهی بادینی", "التفسير الباديني", "Runahi Badini Tafsir", "content/noor_tafsir_1.2.2/base/assets/tafseeri_runahi_badini.db", "bad"),
        TafsirBook("zhian", "ژیان بادینی", "تفسير ژيان", "Zhian Badini Tafsir", "content/noor_tafsir_1.2.2/base/assets/tafseeri_zhian_badini.db", "bad"),
        TafsirBook("saadi", "تفسير السعدي", "تفسير السعدي", "Saadi Tafsir", "content/noor_tafsir_1.2.2/base/assets/tafseeri_saadi.db", "ar"),
        TafsirBook("mukhtasar", "مختصر", "المختصر", "Mukhtasar Tafsir", "content/noor_tafsir_1.2.2/base/assets/tafseeri_mukhtasar.db", "ar"),
        TafsirBook("rebar", "ڕێبەر", "تفسير ريبار", "Rebar Tafsir (Sorani)", "content/noor_tafsir_1.2.2/base/assets/tafseer_rebar.db", "ckb"),
        TafsirBook("tawhid", "تەوحید", "تفسير التوحيد", "Tawhid Tafsir", "content/noor_tafsir_1.2.2/base/assets/tafseeri_tawhid.db", "ar"),
        TafsirBook("khairah_ibn_kathir", "ابن کەثیر", "تفسير ابن كثير", "Ibn Kathir (Khairah)", "content/khairah_1.2/base/assets/tafsir/ibn-kathir.sqlite", "ar"),
        TafsirBook("khairah_muyassar", "التفسير الميسّر", "التفسير الميسر", "Al-Muyassar (Khairah)", "content/khairah_1.2/base/assets/tafsir/muyassar.sqlite", "ar"),
        TafsirBook("khairah_saadi", "تفسير السعدي", "تفسير السعدي", "Al-Saadi (Khairah)", "content/khairah_1.2/base/assets/tafsir/saadi.sqlite", "ar"),
        TafsirBook("khairah_tabari", "تفسير الطبري", "تفسير الطبري", "Al-Tabari (Khairah)", "content/khairah_1.2/base/assets/tafsir/tabari.sqlite", "ar")
    )

    private fun digest(text: String): String = MessageDigest.getInstance("SHA-256")
        .digest(text.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }.take(20)

    private fun openDb(context: Context, assetPath: String): SQLiteDatabase {
        val databaseDir = File(context.filesDir, "content-databases").apply { mkdirs() }
        val file = File(databaseDir, "\${digest(assetPath)}.db")
        synchronized(file.absolutePath.intern()) {
            if (!file.exists() || file.length() == 0L) {
                val temp = File(databaseDir, "\${file.name}.tmp")
                context.assets.open(assetPath).use { input ->
                    temp.outputStream().use { output -> input.copyTo(output, 256 * 1024) }
                }
                if (!temp.renameTo(file)) {
                    temp.copyTo(file, overwrite = true)
                    temp.delete()
                }
            }
        }
        return SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
    }

    private fun <T> queryDb(context: Context, assetPath: String, fallback: T, action: (SQLiteDatabase) -> T): T =
        try { openDb(context, assetPath).use(action) } catch (error: Exception) {
            Log.w("MashxallData", "Content query failed for $assetPath", error)
            fallback
        }

    private fun Cursor.string(name: String, default: String = ""): String {
        val i = getColumnIndex(name)
        return if (i >= 0 && !isNull(i)) getString(i) ?: default else default
    }
    private fun Cursor.int(name: String, default: Int = 0): Int {
        val i = getColumnIndex(name)
        return if (i >= 0 && !isNull(i)) getInt(i) else default
    }
    private fun Cursor.double(name: String, default: Double = 0.0): Double {
        val i = getColumnIndex(name)
        return if (i >= 0 && !isNull(i)) getDouble(i) else default
    }


    private fun mushafSvgIndex(context: Context): Map<Int, MushafSvgMeta> {
        cachedMushafIndex?.let { return it }
        synchronized(this) {
            cachedMushafIndex?.let { return it }
            val root = context.assets.open(MUSHAF_SVG_INDEX).bufferedReader(Charsets.UTF_8).use {
                JSONObject(it.readText())
            }
            val rows = root.getJSONArray("pages")
            val result = buildMap<Int, MushafSvgMeta> {
                for (index in 0 until rows.length()) {
                    val row = rows.getJSONObject(index)
                    val page = row.getInt("page")
                    put(page, MushafSvgMeta(
                        page = page,
                        offset = row.getInt("offset"),
                        compressedLength = row.getInt("compressed_length"),
                        uncompressedLength = row.getInt("uncompressed_length"),
                        sha256 = row.getString("sha256")
                    ))
                }
            }
            if (root.optInt("page_count") != 604 || result.size != 604) {
                throw IllegalStateException("Mushaf page index is incomplete")
            }
            cachedMushafIndex = result
            return result
        }
    }

    suspend fun loadMushafPageSvg(context: Context, page: Int): String? = withContext(Dispatchers.IO) {
        val meta = try { mushafSvgIndex(context)[page.coerceIn(1, 604)] }
        catch (error: Exception) {
            Log.e("MashxallData", "Mushaf page index could not be loaded", error)
            null
        } ?: return@withContext null
        val compressed = try {
            context.assets.openFd(MUSHAF_SVG_BIN).use { descriptor ->
                descriptor.createInputStream().use { input ->
                    var remaining = meta.offset.toLong()
                    while (remaining > 0L) {
                        val skipped = input.skip(remaining)
                        if (skipped > 0L) {
                            remaining -= skipped
                        } else {
                            if (input.read() < 0) throw java.io.EOFException("Mushaf page offset exceeds asset size")
                            remaining--
                        }
                    }
                    val buffer = ByteArray(meta.compressedLength)
                    var written = 0
                    while (written < buffer.size) {
                        val count = input.read(buffer, written, buffer.size - written)
                        if (count < 0) throw java.io.EOFException("Truncated Mushaf page stream")
                        written += count
                    }
                    buffer
                }
            }
        } catch (error: Exception) {
            Log.e("MashxallData", "Could not read compressed Mushaf page ${meta.page}", error)
            return@withContext null
        }
        val raw = try {
            XZInputStream(ByteArrayInputStream(compressed)).use { it.readBytes() }
        } catch (error: Exception) {
            Log.e("MashxallData", "Could not decompress Mushaf page ${meta.page}", error)
            return@withContext null
        }
        if (raw.size != meta.uncompressedLength || sha256Hex(raw) != meta.sha256) {
            Log.e("MashxallData", "Mushaf page integrity check failed: ${meta.page}")
            return@withContext null
        }
        val svg = String(raw, Charsets.UTF_8)
        if (!svg.contains("Mushaf_Page_%03d".format(Locale.US, meta.page)) || !svg.contains("</svg>")) {
            Log.e("MashxallData", "Mushaf SVG identity does not match page ${meta.page}")
            return@withContext null
        }
        svg
    }

    suspend fun loadSurahs(context: Context): List<Surah> = withContext(Dispatchers.IO) {
        queryDb(context, QURAN_DB, emptyList()) { db ->
            db.rawQuery(
                "SELECT surah_no,name_arabic,name_english,ayah_number,surah_mean_english,place,unique_title FROM surah_name ORDER BY surah_no",
                null
            ).use { c ->
                buildList {
                    while (c.moveToNext()) add(
                        Surah(
                            c.int("surah_no"),
                            c.string("name_arabic"),
                            c.string("name_english"),
                            c.int("ayah_number"),
                            c.string("surah_mean_english"),
                            c.string("place"),
                            c.string("unique_title")
                        )
                    )
                }
            }
        }
    }

    suspend fun loadAyahs(
        context: Context,
        surah: Int,
        language: String,
        query: String = "",
        translationKey: String? = null
    ): List<Ayah> = withContext(Dispatchers.IO) {
        val selectedTranslation = translationKey ?: when (language) {
            "en" -> "sahih"
            "ar" -> "none"
            else -> "puxta"
        }
        val translationColumn = when (selectedTranslation) {
            "raman" -> "translation_raman"
            "asan" -> "translation_asan"
            "sanahi" -> "translation_sanahi"
            "sahih", "none", "translit" -> ""
            else -> "translation_puxta"
        }
        val english = if (selectedTranslation == "sahih") {
            queryDb(context, tafsirBooks.first { it.id == "sahih" }.assetPath, emptyMap<Int, String>()) { db ->
                db.rawQuery("SELECT verse_id,text FROM quran WHERE surah_id=?", arrayOf(surah.toString())).use { c ->
                    buildMap { while (c.moveToNext()) put(c.int("verse_id"), c.string("text")) }
                }
            }
        } else emptyMap()
        val transliteration = if (selectedTranslation == "translit") {
            queryDb(context, TRANSLITERATION_DB, emptyMap<Int, String>()) { db ->
                db.rawQuery("SELECT verse_id,text FROM quran WHERE surah_id=?", arrayOf(surah.toString())).use { c ->
                    buildMap {
                        while (c.moveToNext()) {
                            val html = c.string("text")
                            val plain = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString().trim()
                            put(c.int("verse_id"), plain)
                        }
                    }
                }
            }
        } else emptyMap()
        queryDb(context, QURAN_DB, emptyList()) { db ->
            val translationSelect = if (translationColumn.isBlank()) "'' AS translation_text" else "$translationColumn AS translation_text"
            val sql = "SELECT surah_id,verse_id,quran_arabic,$translationSelect,page FROM quran WHERE surah_id=? ORDER BY verse_id"
            db.rawQuery(sql, arrayOf(surah.toString())).use { c ->
                buildList {
                    while (c.moveToNext()) {
                        val number = c.int("verse_id")
                        val arabic = c.string("quran_arabic")
                        val translation = when (selectedTranslation) {
                            "sahih" -> english[number].orEmpty()
                            "translit" -> transliteration[number].orEmpty()
                            "none" -> ""
                            else -> c.string("translation_text")
                        }
                        if (query.isBlank() || arabic.contains(query, true) || translation.contains(query, true) || number.toString() == query) {
                            add(Ayah(surah, number, arabic, translation, c.int("page")))
                        }
                    }
                }
            }
        }
    }

    suspend fun loadTafsir(context: Context, bookId: String, surah: Int, ayah: Int): String = withContext(Dispatchers.IO) {
        val book = tafsirBooks.firstOrNull { it.id == bookId } ?: tafsirBooks.first()
        if (book.id.startsWith("khairah_")) {
            return@withContext loadKhairahTafsirPages(context, book, surah, ayah)
        }
        return@withContext queryDb(context, book.assetPath, "") { db ->
            val tables = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null).use { c ->
                buildSet { while (c.moveToNext()) add(c.getString(0)) }
            }
            val cols = if ("quran" in tables) {
                db.rawQuery("PRAGMA table_info(quran)", null).use { c ->
                    buildSet { while (c.moveToNext()) add(c.getString(1)) }
                }
            } else emptySet()
            if (!setOf("surah_id", "verse_id", "text").all { it in cols }) {
                ""
            } else {
                db.rawQuery("SELECT text FROM quran WHERE surah_id=? AND verse_id=? LIMIT 1", arrayOf(surah.toString(), ayah.toString())).use { c ->
                    if (c.moveToFirst()) c.string("text") else ""
                }
            }
        }
    }

    /**
     * Khairah stores its tafsir pages as independent raw-DEFLATE streams.
     * The SQLite index gives the byte offsets, sizes and SHA-256 for each page,
     * and maps each Quran verse to a range of page IDs. Verify every decoded
     * page before showing it; on any mismatch, return a clear empty result.
     */
    private fun loadKhairahTafsirPages(context: Context, book: TafsirBook, surah: Int, ayah: Int): String {
        val metas = queryDb(context, book.assetPath, emptyList<KhairahPageMeta>()) { db ->
            db.rawQuery(
                """
                SELECT p.id,p.volume,p.page,p.offset,p.length,p.byte_count,p.sha256
                FROM ayahs a JOIN pages p ON p.id BETWEEN a.start_id AND a.end_id
                WHERE a.surah=? AND ? BETWEEN a.first_ayah AND a.last_ayah
                ORDER BY p.id
                """.trimIndent(),
                arrayOf(surah.toString(), ayah.toString())
            ).use { c ->
                buildList {
                    while (c.moveToNext()) add(
                        KhairahPageMeta(
                            c.int("id"), c.string("volume"), c.string("page"),
                            c.int("offset"), c.int("length"), c.int("byte_count"),
                            c.string("sha256")
                        )
                    )
                }
            }
        }
        if (metas.isEmpty()) return ""
        val binaryPath = book.assetPath.substringBeforeLast('.') + ".bin"
        val bytes = try {
            context.assets.open(binaryPath).use { it.readBytes() }
        } catch (error: Exception) {
            Log.w("MashxallData", "Khairah page file unavailable: $binaryPath", error)
            return ""
        }
        val out = StringBuilder()
        for (meta in metas) {
            if (meta.offset < 0 || meta.length <= 0 ||
                meta.offset > bytes.size || meta.length > bytes.size - meta.offset) {
                Log.w("MashxallData", "Invalid page boundary for ${book.id}, page ${meta.id}")
                continue
            }
            val raw = try {
                val inflater = Inflater(true)
                try {
                    InflaterInputStream(
                        ByteArrayInputStream(bytes, meta.offset, meta.length),
                        inflater,
                        16 * 1024
                    ).use { input -> input.readBytes() }
                } finally {
                    inflater.end()
                }
            } catch (error: Exception) {
                Log.w("MashxallData", "Could not inflate ${book.id} page ${meta.id}", error)
                continue
            }
            if (raw.size != meta.byteCount || sha256Hex(raw) != meta.sha256) {
                Log.w("MashxallData", "Page integrity check failed for ${book.id}, page ${meta.id}")
                continue
            }
            try {
                val array = JSONArray(String(raw, Charsets.UTF_8))
                val pageText = StringBuilder()
                for (i in 0 until array.length()) {
                    val paragraph = array.optJSONObject(i) ?: continue
                    val sourceText = paragraph.optString("text", "")
                    if (sourceText.isBlank()) continue
                    val plain = Html.fromHtml(sourceText, Html.FROM_HTML_MODE_LEGACY).toString().trim()
                    if (plain.isNotBlank()) {
                        if (pageText.isNotEmpty()) pageText.append("\n\n")
                        pageText.append(plain)
                    }
                }
                if (pageText.isNotEmpty()) {
                    if (out.isNotEmpty()) out.append("\n\n────────────────────\n\n")
                    val volumePage = buildString {
                        if (meta.volume.isNotBlank()) append("المجلد ").append(meta.volume)
                        if (meta.page.isNotBlank()) {
                            if (isNotEmpty()) append(" • ")
                            append("الصفحة ").append(meta.page)
                        }
                    }
                    if (volumePage.isNotBlank()) out.append("📖 ").append(volumePage).append("\n\n")
                    out.append(pageText)
                }
            } catch (error: Exception) {
                Log.w("MashxallData", "Could not parse Khairah page ${meta.id}", error)
            }
        }
        return out.toString().trim()
    }

    private fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    suspend fun loadHadiths(
        context: Context,
        query: String = "",
        offset: Int = 0,
        limit: Int = 100
    ): List<HadithItem> = withContext(Dispatchers.IO) {
        queryDb(context, HADITH_DB, emptyList()) { db ->
            val term = "%${query.trim()}%"
            val filter = if (query.isBlank()) "" else """
                WHERE h.text LIKE ? OR h.text_ar LIKE ? OR h.text_en LIKE ?
                   OR b.name LIKE ? OR b.name_ar LIKE ? OR b.name_en LIKE ?
                   OR k.title LIKE ? OR k.title_ar LIKE ? OR k.title_en LIKE ?
            """.trimIndent()
            val sql = """
                SELECT h.id,h.text_ar,h.text,h.text_en,h.book_id,
                       COALESCE(b.name,'') AS book_name,
                       COALESCE(b.name_ar,'') AS book_name_ar,
                       COALESCE(b.name_en,'') AS book_name_en,
                       COALESCE(k.title,'') AS chapter_name,
                       COALESCE(k.title_ar,'') AS chapter_name_ar,
                       COALESCE(k.title_en,'') AS chapter_name_en
                FROM hadiths h
                LEFT JOIN book b ON b.id=h.book_id
                LEFT JOIN kitab k ON k.id=h.kitab_id
                $filter
                ORDER BY h.id
                LIMIT ? OFFSET ?
            """.trimIndent()
            val args = if (query.isBlank()) {
                arrayOf(limit.coerceIn(1, 250).toString(), offset.coerceAtLeast(0).toString())
            } else {
                arrayOf(
                    term, term, term, term, term, term, term, term, term,
                    limit.coerceIn(1, 250).toString(), offset.coerceAtLeast(0).toString()
                )
            }
            db.rawQuery(sql, args).use { c ->
                buildList {
                    while (c.moveToNext()) add(
                        HadithItem(
                            id = c.int("id"),
                            arabic = c.string("text_ar"),
                            sorani = c.string("text"),
                            english = c.string("text_en"),
                            bookId = c.int("book_id"),
                            bookName = c.string("book_name"),
                            bookNameAr = c.string("book_name_ar"),
                            bookNameEn = c.string("book_name_en"),
                            chapterName = c.string("chapter_name"),
                            chapterNameAr = c.string("chapter_name_ar"),
                            chapterNameEn = c.string("chapter_name_en")
                        )
                    )
                }
            }
        }
    }

    suspend fun loadAzkar(context: Context, language: String, query: String = ""): List<AzkarItem> = withContext(Dispatchers.IO) {
        // Noor supplies repetition counts and the useful morning/evening grouping.
        // Enrich those records with Salawati's fuller Kurdish/English wording and
        // references where the same Arabic source text is present.
        val primary = queryDb(context, PRAYER_DB, emptyList<AzkarItem>()) { db ->
            val sql = """
                SELECT it._id AS item_id, ch.ckb AS title_ckb, ch.ar AS title_ar, ch.en AS title_en,
                       it.ar AS arabic, it.ckb AS ckb_text, it.en AS en_text, it.counter AS item_count,
                       (SELECT refs.ckb FROM azkar_references refs WHERE refs.chapter_id=it.chapter_id ORDER BY refs._id LIMIT 1) AS ref_ckb
                FROM azkar_items it
                LEFT JOIN azkar_chapters ch ON ch._id=it.chapter_id
                WHERE it.ckb LIKE ? OR it.ar LIKE ? OR it.en LIKE ? OR ch.ckb LIKE ? OR ch.ar LIKE ? OR ch.en LIKE ?
                ORDER BY it._id LIMIT 350
            """.trimIndent()
            val term = if (query.isBlank()) "%" else "%$query%"
            db.rawQuery(sql, arrayOf(term, term, term, term, term, term)).use { c ->
                buildList { while (c.moveToNext()) add(AzkarItem(c.int("item_id"), c.string("title_ckb"), c.string("title_ar"), c.string("title_en"), c.string("arabic"), c.string("ckb_text"), c.string("en_text"), c.int("item_count"), c.string("ref_ckb"))) }
            }
        }
        val translations = queryDb(context, AZKAR_DB, emptyMap<String, SalawatiAzkarText>()) { db ->
            val sql = """
                SELECT it.item AS arabic,
                       (SELECT t.item_translation FROM azkar_item_translation t WHERE t.item_id=it._id AND t.language='ckb' LIMIT 1) AS ckb_text,
                       (SELECT t.item_translation FROM azkar_item_translation t WHERE t.item_id=it._id AND t.language='en' LIMIT 1) AS en_text,
                       (SELECT r.reference FROM azkar_reference ref JOIN azkar_reference_translation r ON r.reference_id=ref._id WHERE ref.item_id=it._id AND r.language='ckb' LIMIT 1) AS ref_ckb
                FROM azkar_item it
            """.trimIndent()
            db.rawQuery(sql, null).use { c ->
                buildMap {
                    while (c.moveToNext()) {
                        val key = normalizeArabicKey(c.string("arabic"))
                        if (key.isNotBlank()) put(key, SalawatiAzkarText(c.string("ckb_text"), c.string("en_text"), c.string("ref_ckb")))
                    }
                }
            }
        }
        if (primary.isNotEmpty()) {
            primary.map { item ->
                val extra = translations[normalizeArabicKey(item.arabic)]
                if (extra == null) item else item.copy(
                    ckb = extra.ckb.ifBlank { item.ckb },
                    english = extra.english.ifBlank { item.english },
                    referenceCkb = extra.reference.ifBlank { item.referenceCkb }
                )
            }
        } else {
            // Graceful fallback if Noor's prayer-time DB is absent: use the
            // complete Salawati collection instead of an empty screen.
            queryDb(context, AZKAR_DB, emptyList<AzkarItem>()) { db ->
                val term = if (query.isBlank()) "%" else "%$query%"
                val sql = """
                    SELECT it._id AS item_id,
                           COALESCE((SELECT t.chapter_name FROM azkar_chapter_translation t WHERE t.chapter_id=it.chapter_id AND t.language='ckb' LIMIT 1),'') AS title_ckb,
                           COALESCE((SELECT t.chapter_name FROM azkar_chapter_translation t WHERE t.chapter_id=it.chapter_id AND t.language='ar' LIMIT 1),'') AS title_ar,
                           COALESCE((SELECT t.chapter_name FROM azkar_chapter_translation t WHERE t.chapter_id=it.chapter_id AND t.language='en' LIMIT 1),'') AS title_en,
                           it.item AS arabic,
                           COALESCE((SELECT t.item_translation FROM azkar_item_translation t WHERE t.item_id=it._id AND t.language='ckb' LIMIT 1),'') AS ckb_text,
                           COALESCE((SELECT t.item_translation FROM azkar_item_translation t WHERE t.item_id=it._id AND t.language='en' LIMIT 1),'') AS en_text,
                           COALESCE((SELECT r.reference FROM azkar_reference ref JOIN azkar_reference_translation r ON r.reference_id=ref._id WHERE ref.item_id=it._id AND r.language='ckb' LIMIT 1),'') AS ref_ckb
                    FROM azkar_item it
                    WHERE it.item LIKE ? OR EXISTS(SELECT 1 FROM azkar_item_translation t WHERE t.item_id=it._id AND t.item_translation LIKE ?)
                    ORDER BY it._id LIMIT 350
                """.trimIndent()
                db.rawQuery(sql, arrayOf(term, term)).use { c ->
                    buildList { while (c.moveToNext()) add(AzkarItem(c.int("item_id"), c.string("title_ckb"), c.string("title_ar"), c.string("title_en"), c.string("arabic"), c.string("ckb_text"), c.string("en_text"), 1, c.string("ref_ckb"))) }
                }
            }
        }
    }

    suspend fun loadNames(context: Context): List<AllahName> = withContext(Dispatchers.IO) {
        queryDb(context, PRAYER_DB, emptyList()) { db ->
            db.rawQuery("SELECT _id,arabic,sorani,badini,english FROM allah_names ORDER BY _id", null).use { c ->
                buildList { while (c.moveToNext()) add(AllahName(c.int("_id") + 1, c.string("arabic"), c.string("sorani"), c.string("badini"), c.string("english"))) }
            }
        }
    }

    suspend fun loadCities(context: Context): List<City> = withContext(Dispatchers.IO) {
        val noorCities = queryDb(context, PRAYER_DB, emptyList<City>()) { db ->
            db.rawQuery("SELECT city,lat,lon FROM cities ORDER BY city", null).use { c ->
                buildList { while (c.moveToNext()) add(City(c.string("city"), c.double("lat"), c.double("lon"))) }
            }
        }
        val salawatiCities = queryDb(context, SALAWATI_DB, emptyList<City>()) { db ->
            db.rawQuery(
                """
                SELECT DISTINCT l.name,l.latitude,l.longitude
                FROM location l JOIN prayer_time pt ON pt.location_id=l._id
                ORDER BY l.name
                """.trimIndent(), null
            ).use { c ->
                buildList { while (c.moveToNext()) add(City(c.string("name"), c.double("latitude"), c.double("longitude"))) }
            }
        }
        (noorCities + salawatiCities)
            .distinctBy { it.name.trim().lowercase(Locale.ROOT) }
            .sortedBy { it.name.lowercase(Locale.ROOT) }
    }

    suspend fun loadPrayerTimes(context: Context, city: String, date: Date = Date()): PrayerRow? = withContext(Dispatchers.IO) {
        val md = SimpleDateFormat("MM-dd", Locale.US).format(date)
        val noor = queryDb(context, PRAYER_DB, null as PrayerRow?) { db ->
            db.rawQuery("SELECT city,date,fajr,sunrise,dhuhr,asr,maghrib,isha FROM staticprayertimes WHERE lower(city)=lower(?) AND date=? LIMIT 1", arrayOf(city, md)).use { c ->
                if (c.moveToFirst()) PrayerRow(c.string("city"), c.string("date"), c.string("fajr"), c.string("sunrise"), c.string("dhuhr"), c.string("asr"), c.string("maghrib"), c.string("isha"), source = "noor:${c.string("city")}") else null
            }
        }
        if (noor != null) return@withContext noor
        queryDb(context, SALAWATI_DB, null as PrayerRow?) { db ->
            db.rawQuery(
                """
                SELECT l.name AS city,pt.date,pt.fajr,pt.sunrise,pt.dhuhr,pt.asr,pt.maghrib,pt.isha,
                       l.latitude AS lat,l.longitude AS lon
                FROM location l JOIN prayer_time pt ON pt.location_id=l._id
                WHERE lower(l.name)=lower(?) AND pt.date=? LIMIT 1
                """.trimIndent(), arrayOf(city, md)
            ).use { c ->
                if (c.moveToFirst()) PrayerRow(c.string("city"), c.string("date"), c.string("fajr"), c.string("sunrise"), c.string("dhuhr"), c.string("asr"), c.string("maghrib"), c.string("isha"), source = "salawati:${c.string("city")}", latitude = c.double("lat"), longitude = c.double("lon")) else null
            }
        }
    }

    suspend fun loadPrayerTimesAtLocation(
        context: Context,
        latitude: Double,
        longitude: Double,
        date: Date = Date(),
        method: PrayerCalculationMethod = PrayerCalculationMethod.MWL,
        asrSchool: AsrCalculationSchool = AsrCalculationSchool.SHAFI,
        staticRadiusKm: Double = 7.0
    ): PrayerRow = withContext(Dispatchers.IO) {
        val nearest = loadCities(context)
            .map { city -> city to PrayerTimeCalculator.distanceKm(latitude, longitude, city.latitude, city.longitude) }
            .filter { it.second <= staticRadiusKm }
            .sortedBy { it.second }
        for ((city, distance) in nearest) {
            val row = loadPrayerTimes(context, city.name, date)
            if (row != null) {
                return@withContext row.copy(
                    source = "timetable:${city.name}:${"%.1f".format(Locale.US, distance)}km",
                    latitude = latitude,
                    longitude = longitude
                )
            }
        }
        PrayerTimeCalculator.calculate(date, latitude, longitude, method, asrSchool)
    }

    suspend fun loadBooks(context: Context, query: String = ""): List<BookItem> = withContext(Dispatchers.IO) {
        queryDb(context, BOOKS_DB, emptyList()) { db ->
            val like = if (query.isBlank()) "%" else "%$query%"
            db.rawQuery("SELECT id,title,writer,description,page_number,url FROM book_data WHERE title LIKE ? OR writer LIKE ? OR description LIKE ? ORDER BY sorter,title LIMIT 150", arrayOf(like, like, like)).use { c ->
                buildList { while (c.moveToNext()) add(BookItem(c.int("id"), c.string("title"), c.string("writer"), c.string("description"), c.int("page_number"), c.string("url"))) }
            }
        }
    }

    suspend fun searchLexicon(context: Context, query: String): List<LexiconItem> = withContext(Dispatchers.IO) {
        queryDb(context, LEXICON_DB, emptyList()) { db ->
            val like = if (query.isBlank()) "%" else "%$query%"
            db.rawQuery("SELECT w.title,w.content,r.reference_name,w.root FROM word_list w LEFT JOIN \"references\" r ON r.id=w.reference WHERE w.title LIKE ? OR w.content LIKE ? OR w.root LIKE ? ORDER BY w.title LIMIT 100", arrayOf(like, like, like)).use { c ->
                buildList { while (c.moveToNext()) add(LexiconItem(c.string("title"), c.string("content"), c.string("reference_name"), c.string("root"))) }
            }
        }
    }

    private fun decodeQuranGlyphs(encoded: String): String {
        val value = encoded.trim()
        if (value.isBlank()) return ""
        if (value.length % 4 != 0 || value.any { it.digitToIntOrNull(16) == null }) return value
        return value.chunked(4).mapNotNull { token ->
            token.toIntOrNull(16)?.takeIf { it in 0..0xFFFF }?.toChar()
        }.joinToString("")
    }

    suspend fun loadWordAnalysis(context: Context, surah: Int, ayah: Int): List<WordItem> = withContext(Dispatchers.IO) {
        queryDb(context, WORD_DB, emptyList()) { db ->
            val types = db.rawQuery("SELECT id,tag,english,arabic,kurdish,color FROM word_type", null).use { c ->
                buildMap<Int, WordTypeInfo> {
                    while (c.moveToNext()) put(
                        c.int("id"),
                        WordTypeInfo(c.string("tag"), c.string("english"), c.string("arabic"), c.string("kurdish"), c.string("color"))
                    )
                }
            }
            val sql = """
                SELECT position,root,irab,word_count,
                       arabic1,arabic2,arabic3,arabic4,arabic5,
                       word_type_id1,word_type_id2,word_type_id3,word_type_id4,word_type_id5
                FROM quran_corpus
                WHERE surah_id=? AND verse_id=?
                ORDER BY position
            """.trimIndent()
            db.rawQuery(sql, arrayOf(surah.toString(), ayah.toString())).use { c ->
                buildList {
                    while (c.moveToNext()) {
                        val count = c.int("word_count").coerceIn(1, 5)
                        val parts = (1..count).mapNotNull { slot ->
                            val encoded = c.string("arabic$slot")
                            if (encoded.isBlank()) null else {
                                val typeId = c.int("word_type_id$slot")
                                val type = types[typeId]
                                WordPart(
                                    glyphText = decodeQuranGlyphs(encoded),
                                    typeId = typeId,
                                    tag = type?.tag.orEmpty(),
                                    labelCkb = type?.ckb.orEmpty(),
                                    labelArabic = type?.arabic.orEmpty(),
                                    labelEnglish = type?.english.orEmpty(),
                                    colorHex = type?.color.orEmpty()
                                )
                            }
                        }
                        add(
                            WordItem(
                                position = c.int("position"),
                                root = c.string("root"),
                                grammar = c.string("irab"),
                                arabicShape = parts.joinToString(" ") { it.glyphText },
                                parts = parts
                            )
                        )
                    }
                }
            }
        }
    }

    suspend fun loadReciters(context: Context): List<Reciter> = withContext(Dispatchers.IO) {
        queryDb(context, QURAN_DB, emptyList()) { db ->
            db.rawQuery("SELECT id,name,name_english,audiotype,url,q_order FROM audio ORDER BY q_order,name", null).use { c ->
                buildList {
                    while (c.moveToNext()) add(Reciter(c.int("id"), c.string("name"), c.string("name_english"), c.int("audiotype"), c.string("url"), c.int("q_order")))
                }
            }
        }
    }

    fun reciterSampleUrl(reciter: Reciter, surah: Int, ayah: Int): String {
        val base = reciter.baseUrl.trimEnd('/') + "/"
        val fileName = if (reciter.audioType == 0) "%03d%03d.mp3".format(Locale.US, surah.coerceIn(1, 114), ayah.coerceAtLeast(1))
            else "%03d.mp3".format(Locale.US, surah.coerceIn(1, 114))
        return base + fileName
    }

    suspend fun loadFarmuda(context: Context, query: String = ""): List<FarmudaItem> = withContext(Dispatchers.IO) {
        queryDb(context, "content/noor_tafsir_1.2.2/base/assets/farmuda.db", emptyList()) { db ->
            val term = if (query.isBlank()) "%" else "%" + query.trim() + "%"
            db.rawQuery(
                "SELECT _id,title,ar_desc,ku_desc,badini_title,badini,fav_time FROM farmuda WHERE title LIKE ? OR ar_desc LIKE ? OR ku_desc LIKE ? OR badini_title LIKE ? OR badini LIKE ? ORDER BY _id LIMIT 300",
                arrayOf(term, term, term, term, term)
            ).use { c ->
                buildList {
                    while (c.moveToNext()) add(FarmudaItem(c.int("_id"), c.string("title"), c.string("ar_desc"), c.string("ku_desc"), c.string("badini_title"), c.string("badini"), c.int("fav_time")))
                }
            }
        }
    }

    suspend fun loadAudioItems(context: Context): List<AudioItem> = withContext(Dispatchers.IO) {
        withContext(Dispatchers.Default) {
            val result = mutableListOf<AudioItem>()
            fun walk(path: String) {
                val children = try { context.assets.list(path).orEmpty() } catch (_: Exception) { emptyArray() }
                for (child in children) {
                    val full = if (path.isBlank()) child else "$path/$child"
                    val lower = child.lowercase(Locale.ROOT)
                    if (lower != "click.wav" && listOf(".mp3", ".ogg", ".wav", ".m4a", ".aac", ".flac", ".opus").any { lower.endsWith(it) }) {
                        result += AudioItem(full, child.substringBeforeLast('.').replace('_', ' ').replace('-', ' '), full.substringBeforeLast('/').substringAfterLast('/'))
                    } else if (children.isNotEmpty()) {
                        walk(full)
                    }
                }
            }
            walk("content")
            result.sortedBy { it.title.lowercase(Locale.ROOT) }
        }
    }

    suspend fun loadPdfPaths(context: Context): List<String> = withContext(Dispatchers.IO) {
        withContext(Dispatchers.Default) {
            val result = mutableListOf<String>()
            fun walk(path: String) {
                val children = try { context.assets.list(path).orEmpty() } catch (_: Exception) { emptyArray() }
                for (child in children) {
                    val full = if (path.isBlank()) child else "$path/$child"
                    if (child.lowercase(Locale.ROOT).endsWith(".pdf")) result += full
                    else if (children.isNotEmpty()) walk(full)
                }
            }
            walk("content")
            result.sorted()
        }
    }
}
