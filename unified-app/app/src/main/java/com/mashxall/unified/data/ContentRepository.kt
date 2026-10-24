package com.mashxall.unified.data

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import android.text.Html
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.IOException
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
data class Ayah(
    val surah: Int,
    val number: Int,
    val arabic: String,
    val translation: String,
    val page: Int = 0,
    val arabicWithoutHarakat: String = "",
    val note: String = "",
    val hasHadith: Boolean = false,
    val surahNameArabic: String = "",
    val surahNameEnglish: String = ""
)
data class AyahHadith(
    val id: Int,
    val collectionCode: String,
    val collectionArabic: String,
    val collectionEnglish: String,
    val collectionSorani: String,
    val reference: String,
    val referenceArabic: String,
    val referenceEnglish: String,
    val verseReference: String,
    val verseReferences: List<Pair<Int, Int>>,
    val arabic: String,
    val english: String,
    val kurdish: String
)
data class HadithCollection(
    val id: Int,
    val name: String,
    val nameArabic: String,
    val nameEnglish: String,
    val hadithCount: Int,
    val kind: Int,
    val sort: Int
)
data class HadithArticle(
    val id: Int,
    val titleCkb: String,
    val titleArabic: String,
    val titleEnglish: String,
    val bodyCkb: String,
    val bodyArabic: String,
    val bodyEnglish: String,
    val authorCkb: String = "",
    val authorArabic: String = "",
    val authorEnglish: String = "",
    val authorDetail: String = ""
)
data class HadithItem(
    val id: Int,
    val sort: Int,
    val sortInBook: String,
    val arabic: String,
    val sorani: String,
    val english: String,
    val bookId: Int,
    val bookName: String = "",
    val bookNameAr: String = "",
    val bookNameEn: String = "",
    val kitabName: String = "",
    val kitabNameAr: String = "",
    val kitabNameEn: String = "",
    val chapterName: String = "",
    val chapterNameAr: String = "",
    val chapterNameEn: String = "",
    val note: String = "",
    val footnote: String = ""
)
data class AzkarItem(
    val id: Int, val titleCkb: String, val titleAr: String, val titleEn: String,
    val arabic: String, val ckb: String, val english: String, val count: Int, val referenceCkb: String,
    val categoryId: Int = 0, val categoryCkb: String = "", val categoryArabic: String = "", val categoryEnglish: String = ""
)
data class AzkarCategory(val id: Int, val ckb: String, val arabic: String, val english: String)
data class TasbihPreset(val id: Int, val zikr: String, val targetCount: Int, val timeOfDay: Int, val meaningCkb: String)
data class AllahName(
    val number: Int,
    val arabic: String,
    val ckb: String,
    val badini: String,
    val english: String,
    val surahAyah: String = "",
    val highlightSection: String = "",
    val farsi: String = "",
    val extraText: String = ""
)
data class City(val name: String, val latitude: Double, val longitude: Double, val country: String = "")
data class PrayerRow(
    val city: String, val date: String, val fajr: String, val sunrise: String,
    val dhuhr: String, val asr: String, val maghrib: String, val isha: String,
    val source: String = "timetable", val latitude: Double? = null, val longitude: Double? = null
)
data class TafsirBook(val id: String, val labelCkb: String, val labelAr: String, val labelEn: String, val assetPath: String, val language: String)
data class BookItem(
    val id: Int,
    val title: String,
    val writer: String,
    val description: String,
    val pages: Int,
    val url: String,
    val branch: String = "",
    val uniqueText: String = "",
    val coverUrl: String = "",
    val bookColor: String = "",
    val textColor: String = "",
    val extraText: String = "",
    val extraNumber: Int = 0,
    val sourceFavoriteTime: String = ""
)
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
data class MushafPageVerse(val surah: Int, val ayah: Int)
data class MushafPageRect(
    val glyphId: Int,
    val lineNumber: Int,
    val minX: Float,
    val maxX: Float,
    val minY: Float,
    val maxY: Float
)

private data class MushafSvgMeta(
    val page: Int, val offset: Int, val compressedLength: Int,
    val uncompressedLength: Int, val sha256: String
)

private fun readableHadithText(value: String): String {
    val withoutHighlightMarkers = value.replace("<rr>", "").replace("</rr>", "")
    return Html.fromHtml(withoutHighlightMarkers, Html.FROM_HTML_MODE_LEGACY)
        .toString()
        .replace('\u00a0', ' ')
        .trim()
}

private fun normalizeArabicKey(value: String): String = value.lowercase(Locale.ROOT).filterNot { ch ->
    ch in '\u064B'..'\u065F' || ch == '\u0670' || ch == '\u0640' || !ch.isLetterOrDigit()
}

object ContentRepository {
    const val QURAN_DB = "content/noor_tafsir_1.2.2/base/assets/quran.db"
    const val PRAYER_DB = "content/noor_tafsir_1.2.2/base/assets/prayertimes.db"
    const val HADITH_DB = "content/noor_tafsir_1.2.2/base/assets/db_hadith.db"
    const val HADITH_OF_AYAH_DB = "content/noor_tafsir_1.2.2/base/assets/db_hadith_of_ayah.db"
    const val WORD_DB = "content/noor_tafsir_1.2.2/base/assets/db_word_by_word.db"
    const val TRANSLITERATION_DB = "content/noor_tafsir_1.2.2/base/assets/kurdish_transliteration.db"
    const val LEXICON_DB = "content/noor_tafsir_1.2.2/base/assets/db_words_lexicon.db"
    const val AZKAR_DB = "content/salawati_3.1.3/base/assets/database/muslim_db_v2.5.1.db"
    const val SALAWATI_DB = AZKAR_DB
    const val BOOKS_DB = "content/noor_tafsir_1.2.2/base/assets/tn_books.db"
    private const val TAJWEED_DB = "content/noor_tafsir_1.2.2/base/assets/ayah_info.db"
    private const val MUSHAF_SVG_BIN = "content/khairah_1.2/base/assets/mushaf/pages.bin"
    private const val MUSHAF_SVG_INDEX = "mushaf_svg_index.json"
    @Volatile private var cachedMushafIndex: Map<Int, MushafSvgMeta>? = null
    private data class AssetSignature(val sizeBytes: Long, val sha256: String)
    @Volatile private var cachedAssetSignatures: Map<String, AssetSignature>? = null

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

    /**
     * Content databases are copied into private storage so SQLite can open them read-only.
     * Validate the copied file against the immutable content_catalog.json each time we
     * open it. Checking only existence/length kept old databases across app updates and
     * caused valid Quran queries to fail with "no such table" on upgraded installations.
     */
    private fun contentSignatures(context: Context): Map<String, AssetSignature> {
        cachedAssetSignatures?.let { return it }
        synchronized(this) {
            cachedAssetSignatures?.let { return it }
            val result = buildMap<String, AssetSignature> {
                context.assets.open("content_catalog.json").bufferedReader(Charsets.UTF_8).use { reader ->
                    val root = JSONObject(reader.readText())
                    val rows = root.getJSONArray("files")
                    for (index in 0 until rows.length()) {
                        val row = rows.getJSONObject(index)
                        val assetPath = row.optString("asset_path")
                        val size = row.optLong("size_bytes", -1L)
                        val sha = row.optString("sha256")
                        if (assetPath.isNotBlank() && size >= 0L && sha.matches(Regex("[0-9a-fA-F]{64}"))) {
                            put(assetPath, AssetSignature(size, sha.lowercase(Locale.ROOT)))
                        }
                    }
                }
            }
            if (result.isEmpty()) throw IOException("Content asset catalog has no verifiable file entries")
            cachedAssetSignatures = result
            return result
        }
    }

    private fun sha256File(file: File): String {
        val hash = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered(256 * 1024).use { input ->
            val buffer = ByteArray(256 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                hash.update(buffer, 0, count)
            }
        }
        return hash.digest().joinToString("") { "%02x".format(it) }
    }

    private fun openDb(context: Context, assetPath: String): SQLiteDatabase {
        val signatures = contentSignatures(context)
        val expected = signatures[assetPath]
            ?: throw IOException("Content catalog has no SHA-256 entry for database: $assetPath")
        val databaseDir = File(context.filesDir, "content-databases").apply { mkdirs() }
        val cacheId = digest(assetPath)
        val file = File(databaseDir, "$cacheId.db")
        val signatureFile = File(databaseDir, "$cacheId.sha256")
        synchronized(file.absolutePath.intern()) {
            var needsCopy = !file.isFile || file.length() != expected.sizeBytes
            if (!needsCopy) {
                val recordedHash = runCatching { signatureFile.readText(Charsets.UTF_8).trim() }.getOrNull()
                if (recordedHash == expected.sha256) {
                    // This exact private file was previously verified against this
                    // version's packaged content catalog.
                    needsCopy = false
                } else {
                    val actualHash = runCatching { sha256File(file) }.getOrNull()
                    if (actualHash == expected.sha256) {
                        signatureFile.writeText(expected.sha256, Charsets.UTF_8)
                        needsCopy = false
                    } else {
                        Log.w(
                            "MashxallData",
                            "Cached DB failed source SHA-256; refreshing $assetPath (cached=${actualHash ?: "unreadable"}, expected=${expected.sha256})"
                        )
                        needsCopy = true
                    }
                }
            }

            if (needsCopy) {
                val temp = File(databaseDir, "$cacheId.db.tmp")
                if (temp.exists()) temp.delete()
                context.assets.open(assetPath).use { input ->
                    temp.outputStream().buffered(256 * 1024).use { output ->
                        input.copyTo(output, 256 * 1024)
                    }
                }
                val actualSize = temp.length()
                val actualHash = sha256File(temp)
                if (actualSize != expected.sizeBytes || actualHash != expected.sha256) {
                    temp.delete()
                    throw IOException(
                        "Packaged database integrity mismatch for $assetPath: " +
                            "size=$actualSize/${expected.sizeBytes}, sha256=$actualHash/${expected.sha256}"
                    )
                }
                if (file.exists() && !file.delete()) {
                    temp.delete()
                    throw IOException("Could not replace stale cached database for $assetPath")
                }
                if (!temp.renameTo(file)) {
                    temp.copyTo(file, overwrite = true)
                    temp.delete()
                }
                signatureFile.writeText(expected.sha256, Charsets.UTF_8)
                Log.i("MashxallData", "Verified and cached database $assetPath ($actualSize bytes)")
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

    suspend fun loadMushafPageVerses(context: Context, page: Int): List<MushafPageVerse> = withContext(Dispatchers.IO) {
        val targetPage = page.coerceIn(1, 604)
        queryDb(context, TAJWEED_DB, emptyList()) { db ->
            db.rawQuery(
                """
                SELECT DISTINCT surah_id,verse_id
                FROM (
                    SELECT page_number,surah_id,verse_id
                    FROM v2_tajweed_page_data_line
                    WHERE page_number=?
                    UNION
                    SELECT page_number,surah_id,verse_id
                    FROM page_data_line
                    WHERE page_number=?
                )
                WHERE surah_id BETWEEN 1 AND 114 AND verse_id BETWEEN 1 AND 286
                ORDER BY surah_id,verse_id
                """.trimIndent(),
                arrayOf(targetPage.toString(), targetPage.toString())
            ).use { c ->
                buildList {
                    while (c.moveToNext()) add(MushafPageVerse(c.int("surah_id"), c.int("verse_id")))
                }
            }
        }
    }

    /**
     * Original Noor page geometry for a selected verse. This powers the same
     * translucent verse highlight used by the source Mushaf reader. Coordinates
     * are in the original page SVG coordinate space, not screen pixels.
     */
    suspend fun loadMushafVerseRects(
        context: Context,
        page: Int,
        surah: Int,
        ayah: Int
    ): List<MushafPageRect> = withContext(Dispatchers.IO) {
        val targetPage = page.coerceIn(1, 604)
        fun readRects(table: String): List<MushafPageRect> {
            val safeTable = if (table == "v2_tajweed_page_data_line") table else "page_data_line"
            return queryDb(context, TAJWEED_DB, emptyList()) { db ->
                db.rawQuery(
                    """
                    SELECT glyph_id,line_number,min_x,max_x,min_y,max_y
                    FROM $safeTable
                    WHERE page_number=? AND surah_id=? AND verse_id=?
                    ORDER BY line_number,min_x
                    """.trimIndent(),
                    arrayOf(targetPage.toString(), surah.toString(), ayah.toString())
                ).use { c ->
                    buildList {
                        while (c.moveToNext()) {
                            val left = c.getFloat(c.getColumnIndexOrThrow("min_x"))
                            val right = c.getFloat(c.getColumnIndexOrThrow("max_x"))
                            val top = c.getFloat(c.getColumnIndexOrThrow("min_y"))
                            val bottom = c.getFloat(c.getColumnIndexOrThrow("max_y"))
                            if (right > left && bottom > top) {
                                add(MushafPageRect(
                                    c.getInt(c.getColumnIndexOrThrow("glyph_id")),
                                    c.getInt(c.getColumnIndexOrThrow("line_number")),
                                    left, right, top, bottom
                                ))
                            }
                        }
                    }
                }
            }
        }
        // Prefer the detailed Tajweed glyph geometry; the legacy table is only
        // a fallback for pages/verses not represented in the v2 table.
        val tajweedRects = readRects("v2_tajweed_page_data_line")
        if (tajweedRects.isNotEmpty()) tajweedRects else readRects("page_data_line")
    }

    /**
     * Resolve a page touch against the original Tajweed word bounds first;
     * fall back to the source line bounds for gaps or legacy pages.
     */
    suspend fun findMushafVerseAtPoint(
        context: Context,
        page: Int,
        x: Float,
        y: Float
    ): MushafPageVerse? = withContext(Dispatchers.IO) {
        val targetPage = page.coerceIn(1, 604)
        fun findIn(table: String): MushafPageVerse? {
            val safeTable = if (table == "v2_tajweed_page_data_line") table else "page_data_line"
            return queryDb(context, TAJWEED_DB, null as MushafPageVerse?) { db ->
                db.rawQuery(
                    """
                    SELECT surah_id,verse_id
                    FROM $safeTable
                    WHERE page_number=?
                      AND min_x<=? AND max_x>=?
                      AND min_y<=? AND max_y>=?
                      AND surah_id BETWEEN 1 AND 114
                      AND verse_id BETWEEN 1 AND 286
                    ORDER BY ((max_x-min_x)*(max_y-min_y)) ASC,glyph_id ASC
                    LIMIT 1
                    """.trimIndent(),
                    arrayOf(
                        targetPage.toString(), x.toString(), x.toString(),
                        y.toString(), y.toString()
                    )
                ).use { c ->
                    if (c.moveToFirst()) MushafPageVerse(c.getInt(0), c.getInt(1)) else null
                }
            }
        }
        findIn("v2_tajweed_page_data_line") ?: findIn("page_data_line")
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

    private fun translationKeyFor(language: String, translationKey: String?): String =
        translationKey ?: when (language) {
            "en" -> "sahih"
            "ar" -> "none"
            else -> "puxta"
        }

    private fun translationColumnFor(selectedTranslation: String): String = when (selectedTranslation) {
        "raman" -> "translation_raman"
        "asan" -> "translation_asan"
        "sanahi" -> "translation_sanahi"
        "sahih", "none", "translit" -> ""
        else -> "translation_puxta"
    }

    private suspend fun allTranslationRows(
        context: Context,
        key: String,
        selectedSurah: Int? = null
    ): Map<Pair<Int, Int>, String> {
        val path = when (key) {
            "sahih" -> tafsirBooks.first { it.id == "sahih" }.assetPath
            "translit" -> TRANSLITERATION_DB
            else -> return emptyMap()
        }
        return queryDb(context, path, emptyMap<Pair<Int, Int>, String>()) { db ->
            val sql = if (selectedSurah == null) {
                "SELECT surah_id,verse_id,text FROM quran"
            } else {
                "SELECT surah_id,verse_id,text FROM quran WHERE surah_id=?"
            }
            val args = selectedSurah?.let { arrayOf(it.toString()) }
            db.rawQuery(sql, args).use { c ->
                buildMap {
                    while (c.moveToNext()) {
                        val rawText = c.string("text")
                        val text = if (key == "translit") {
                            Html.fromHtml(rawText, Html.FROM_HTML_MODE_LEGACY).toString().trim()
                        } else rawText
                        put(c.int("surah_id") to c.int("verse_id"), text)
                    }
                }
            }
        }
    }

    private fun verseTranslation(
        key: String,
        verseKey: Pair<Int, Int>,
        inlineTranslation: String,
        separateTranslations: Map<Pair<Int, Int>, String>
    ): String = when (key) {
        "sahih", "translit" -> separateTranslations[verseKey].orEmpty()
        "none" -> ""
        else -> inlineTranslation
    }

    private fun Cursor.toAyah(
        key: String,
        translations: Map<Pair<Int, Int>, String>
    ): Ayah {
        val surah = int("surah_id")
        val number = int("verse_id")
        val verseKey = surah to number
        return Ayah(
            surah = surah,
            number = number,
            arabic = string("quran_arabic"),
            translation = verseTranslation(key, verseKey, string("translation_text"), translations),
            page = int("page"),
            arabicWithoutHarakat = string("quran_withoutharkat"),
            note = string("note"),
            hasHadith = int("has_hadith") != 0,
            surahNameArabic = string("surah_name_arabic"),
            surahNameEnglish = string("surah_name_english")
        )
    }

    private suspend fun readQuranRows(
        context: Context,
        key: String,
        query: String?,
        global: Boolean,
        selectedSurah: Int? = null
    ): List<Ayah> = withContext(Dispatchers.IO) {
        val translationColumn = translationColumnFor(key)
        val translationSelect = if (translationColumn.isBlank()) "'' AS translation_text" else "q.$translationColumn AS translation_text"
        val from = if (global) {
            """
            FROM quran q
            LEFT JOIN surah_name sn ON sn.surah_no=q.surah_id
            """.trimIndent()
        } else {
            "FROM quran q LEFT JOIN surah_name sn ON sn.surah_no=q.surah_id"
        }
        val where = if (selectedSurah != null && !global) "WHERE q.surah_id=?" else ""
        val sql = """
            SELECT q.surah_id,q.verse_id,q.quran_arabic,q.quran_withoutharkat,
                   $translationSelect,q.page,q.has_hadith,q.note,
                   COALESCE(sn.name_arabic,'') AS surah_name_arabic,
                   COALESCE(sn.name_english,'') AS surah_name_english
            $from
            $where
            ORDER BY q.surah_id,q.verse_id
        """.trimIndent()
        val external = allTranslationRows(context, key, selectedSurah.takeIf { !global })
        val queryArgs = if (selectedSurah != null && !global) arrayOf(selectedSurah.toString()) else null
        queryDb(context, QURAN_DB, emptyList()) { db ->
            db.rawQuery(sql, queryArgs).use { c ->
                buildList {
                    while (c.moveToNext()) {
                        val item = c.toAyah(key, external)
                        if (query == null || query.isBlank()) add(item)
                        else {
                            val term = query.trim()
                            val folded = normalizeArabicKey(term)
                            val arabicFolded = normalizeArabicKey(item.arabicWithoutHarakat.ifBlank { item.arabic })
                            val idMatch = term == "${item.surah}:${item.number}" ||
                                term == "${item.surah}:${item.number}".replace(":", " ")
                            val containsMatch = item.arabic.contains(term, ignoreCase = true) ||
                                item.arabicWithoutHarakat.contains(term, ignoreCase = true) ||
                                item.translation.contains(term, ignoreCase = true) ||
                                item.surahNameArabic.contains(term, ignoreCase = true) ||
                                item.surahNameEnglish.contains(term, ignoreCase = true) ||
                                (folded.isNotBlank() && arabicFolded.contains(folded, ignoreCase = true))
                            if (idMatch || containsMatch) add(item)
                        }
                    }
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
        val key = translationKeyFor(language, translationKey)
        val rows = readQuranRows(context, key, null, global = false, selectedSurah = surah)
        if (query.isBlank()) rows else rows.filter { item ->
            val term = query.trim()
            item.arabic.contains(term, true) ||
                item.arabicWithoutHarakat.contains(term, true) ||
                item.translation.contains(term, true) ||
                item.number.toString() == term
        }
    }

    suspend fun searchAyahs(
        context: Context,
        query: String,
        language: String,
        translationKey: String? = null
    ): List<Ayah> = readQuranRows(
        context,
        translationKeyFor(language, translationKey),
        query.trim(),
        global = true
    )

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
                    if (c.moveToFirst()) {
                        Html.fromHtml(c.string("text"), Html.FROM_HTML_MODE_LEGACY)
                            .toString()
                            .replace('\u00a0', ' ')
                            .trim()
                    } else ""
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
    private fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }

    private val khairahTafsirPageTextCache = object : LinkedHashMap<String, String>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean =
            size > 48
    }

    private fun readAssetByteRange(
        context: Context,
        assetPath: String,
        offset: Int,
        length: Int
    ): ByteArray? {
        if (offset < 0 || length <= 0) return null
        return try {
            context.assets.open(assetPath).use { input ->
                var remaining = offset.toLong()
                while (remaining > 0L) {
                    val skipped = input.skip(remaining)
                    if (skipped > 0L) {
                        remaining -= skipped
                    } else {
                        if (input.read() < 0) return null
                        remaining--
                    }
                }
                val bytes = ByteArray(length)
                var readTotal = 0
                while (readTotal < bytes.size) {
                    val read = input.read(bytes, readTotal, bytes.size - readTotal)
                    if (read < 0) return null
                    readTotal += read
                }
                bytes
            }
        } catch (error: Exception) {
            Log.w("MashxallData", "Could not read $assetPath range ($offset, $length)", error)
            null
        }
    }

    private fun decodeKhairahTafsirPage(
        context: Context,
        book: TafsirBook,
        binaryPath: String,
        meta: KhairahPageMeta
    ): String {
        val cacheKey = "${book.id}:${meta.id}:${meta.sha256}"
        synchronized(khairahTafsirPageTextCache) {
            khairahTafsirPageTextCache[cacheKey]?.let { return it }
        }
        if (meta.offset < 0 || meta.length <= 0) return ""
        val compressed = readAssetByteRange(context, binaryPath, meta.offset, meta.length) ?: return ""
        val raw = try {
            val inflater = Inflater(true)
            try {
                InflaterInputStream(
                    ByteArrayInputStream(compressed),
                    inflater,
                    16 * 1024
                ).use { input -> input.readBytes() }
            } finally {
                inflater.end()
            }
        } catch (error: Exception) {
            Log.w("MashxallData", "Could not inflate ${book.id} page ${meta.id}", error)
            return ""
        }
        if (raw.size != meta.byteCount || sha256Hex(raw) != meta.sha256) {
            Log.w("MashxallData", "Page integrity check failed for ${book.id}, page ${meta.id}")
            return ""
        }
        val decoded = try {
            val array = JSONArray(String(raw, Charsets.UTF_8))
            val pageText = StringBuilder()
            for (index in 0 until array.length()) {
                val paragraph = array.optJSONObject(index) ?: continue
                val sourceText = paragraph.optString("text", "")
                if (sourceText.isBlank()) continue
                val plain = Html.fromHtml(sourceText, Html.FROM_HTML_MODE_LEGACY).toString().trim()
                if (plain.isNotBlank()) {
                    if (pageText.isNotEmpty()) pageText.append("\n\n")
                    pageText.append(plain)
                }
            }
            pageText.toString().trim()
        } catch (error: Exception) {
            Log.w("MashxallData", "Could not parse ${book.id} page ${meta.id}", error)
            return ""
        }
        synchronized(khairahTafsirPageTextCache) {
            khairahTafsirPageTextCache[cacheKey] = decoded
        }
        return decoded
    }

    /**
     * Khairah indexes raw-DEFLATE page streams in SQLite. Read only the requested
     * compressed byte ranges and cache a bounded number of already verified page
     * texts. This avoids loading the entire, potentially large .bin asset every
     * time a reader advances one verse.
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
        val out = StringBuilder()
        for (meta in metas) {
            val pageText = decodeKhairahTafsirPage(context, book, binaryPath, meta)
            if (pageText.isBlank()) continue
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
        return out.toString().trim()
    }

    suspend fun loadHadithsForAyah(
        context: Context,
        surah: Int,
        ayah: Int
    ): List<AyahHadith> = withContext(Dispatchers.IO) {
        queryDb(context, HADITH_OF_AYAH_DB, emptyList<AyahHadith>()) { db ->
            db.rawQuery(
                """
                SELECT id,reference,english,collection,verse_reference,arabic,kurdish
                FROM hadith_on_ayah
                ORDER BY id
                """.trimIndent(), null
            ).use { c ->
                buildList {
                    while (c.moveToNext()) {
                        val verseReference = c.string("verse_reference")
                        val verseReferences = NoorHadithCipher.parseVerseReferences(verseReference)
                        if ((surah to ayah) !in verseReferences) continue

                        val collectionCode = NoorHadithCipher.decrypt(c.string("collection"))
                        val collectionArabic = NoorHadithCipher.collectionArabic(collectionCode)
                        val collectionEnglish = NoorHadithCipher.collectionEnglish(collectionCode)
                        val collectionSorani = NoorHadithCipher.collectionSorani(collectionCode)
                        val rawReference = NoorHadithCipher.decrypt(c.string("reference"))
                        val reference = NoorHadithCipher.formatReference(rawReference, collectionSorani, "ckb")
                        val referenceArabic = NoorHadithCipher.formatReference(rawReference, collectionArabic, "ar")
                        val referenceEnglish = NoorHadithCipher.formatReference(rawReference, collectionEnglish, "en")
                        add(
                            AyahHadith(
                                id = c.int("id"),
                                collectionCode = collectionCode,
                                collectionArabic = collectionArabic,
                                collectionEnglish = collectionEnglish,
                                collectionSorani = collectionSorani,
                                reference = reference,
                                referenceArabic = referenceArabic,
                                referenceEnglish = referenceEnglish,
                                verseReference = verseReference,
                                verseReferences = verseReferences,
                                arabic = readableHadithText(NoorHadithCipher.decrypt(c.string("arabic"))),
                                english = readableHadithText(NoorHadithCipher.decrypt(c.string("english"))),
                                kurdish = readableHadithText(c.string("kurdish"))
                            )
                        )
                    }
                }
            }
        }
    }

    suspend fun loadHadithArticles(context: Context, query: String = ""): List<HadithArticle> = withContext(Dispatchers.IO) {
        queryDb(context, HADITH_DB, emptyList()) { db ->
            fun columns(table: String): Set<String> =
                db.rawQuery("PRAGMA table_info($table)", null).use { c ->
                    buildSet { while (c.moveToNext()) add(c.string("name")) }
                }
            val articleColumns = columns("articles")
            val authorColumns = columns("author")
            fun findColumn(columns: Set<String>, vararg candidates: String): String? {
                val lower = columns.associateBy { it.lowercase(Locale.ROOT) }
                return candidates.firstNotNullOfOrNull { lower[it.lowercase(Locale.ROOT)] }
            }

            // Inspect the bundled source schema instead of assuming every source build
            // uses the same field names for article-author relationships.
            val articleAuthorId = findColumn(articleColumns, "author_id", "authorid", "author")
            val authorPrimaryKey = findColumn(authorColumns, "id", "_id", "author_id", "authorid")
            val authorCkbColumn = findColumn(authorColumns, "name_ckb", "name_ku", "name_sorani", "name", "title", "author_name")
            val authorArabicColumn = findColumn(authorColumns, "name_ar", "arabic_name", "nameArabic", "author_name_ar", "name")
            val authorEnglishColumn = findColumn(authorColumns, "name_en", "english_name", "nameEnglish", "author_name_en", "name")
            val directAuthorColumn = findColumn(articleColumns, "author_name", "author", "author_text")
            val canJoin = articleAuthorId != null && authorPrimaryKey != null &&
                authorCkbColumn != null && authorArabicColumn != null && authorEnglishColumn != null

            val join = if (canJoin) "LEFT JOIN author auth ON auth.${authorPrimaryKey} = a.${articleAuthorId}" else ""
            val ckbExpr = when {
                canJoin -> "COALESCE(auth.${authorCkbColumn},'')"
                directAuthorColumn != null -> "COALESCE(a.${directAuthorColumn},'')"
                else -> "''"
            }
            val arabicExpr = if (canJoin) "COALESCE(auth.${authorArabicColumn},'')" else ckbExpr
            val englishExpr = if (canJoin) "COALESCE(auth.${authorEnglishColumn},'')" else ckbExpr
            val detailColumn = findColumn(authorColumns, "biography", "bio", "description", "details", "about")
            val detailExpr = if (canJoin && detailColumn != null) "COALESCE(auth.${detailColumn},'')" else "''"

            val filter = if (query.isBlank()) "" else """
                WHERE a.description LIKE ? OR a.description_text_ar LIKE ? OR a.description_text_en LIKE ?
                   OR a.text LIKE ? OR a.text_ar LIKE ? OR a.text_en LIKE ?
                   OR $ckbExpr LIKE ? OR $arabicExpr LIKE ? OR $englishExpr LIKE ?
            """.trimIndent()
            val term = "%${query.trim()}%"
            val args = if (query.isBlank()) null else arrayOf(term, term, term, term, term, term, term, term, term)
            db.rawQuery(
                """
                SELECT a.id,a.description,a.description_text_ar,a.description_text_en,
                       a.text,a.text_ar,a.text_en,
                       $ckbExpr AS author_ckb,
                       $arabicExpr AS author_arabic,
                       $englishExpr AS author_english,
                       $detailExpr AS author_detail
                FROM articles a
                $join
                $filter
                ORDER BY a.id
                """.trimIndent(), args
            ).use { c ->
                buildList {
                    while (c.moveToNext()) {
                        fun plain(html: String): String = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
                            .toString().replace('\u00a0', ' ').trim()
                        add(
                            HadithArticle(
                                id = c.int("id"),
                                titleCkb = plain(c.string("description")),
                                titleArabic = plain(c.string("description_text_ar")),
                                titleEnglish = plain(c.string("description_text_en")),
                                bodyCkb = plain(c.string("text")),
                                bodyArabic = plain(c.string("text_ar")),
                                bodyEnglish = plain(c.string("text_en")),
                                authorCkb = plain(c.string("author_ckb")),
                                authorArabic = plain(c.string("author_arabic")),
                                authorEnglish = plain(c.string("author_english")),
                                authorDetail = plain(c.string("author_detail"))
                            )
                        )
                    }
                }
            }
        }
    }

    suspend fun loadHadithCollections(context: Context): List<HadithCollection> = withContext(Dispatchers.IO) {
        queryDb(context, HADITH_DB, emptyList()) { db ->
            db.rawQuery(
                "SELECT id,name,name_ar,name_en,count,type,sort FROM book ORDER BY sort,name",
                null
            ).use { c ->
                buildList {
                    while (c.moveToNext()) add(
                        HadithCollection(
                            id = c.int("id"),
                            name = c.string("name"),
                            nameArabic = c.string("name_ar"),
                            nameEnglish = c.string("name_en"),
                            hadithCount = c.int("count"),
                            kind = c.int("type"),
                            sort = c.int("sort")
                        )
                    )
                }
            }
        }
    }

    private fun hadithSearchFilter(query: String, bookId: Int? = null, hadithIds: Set<Int>? = null): String {
        val conditions = buildList {
            if (bookId != null) add("h.book_id=?")
            if (hadithIds != null) {
                val safeIds = hadithIds.asSequence().filter { it > 0 }.distinct().sorted().toList()
                if (safeIds.isEmpty()) add("0=1")
                else add("h.id IN (" + safeIds.joinToString(",") + ")")
            }
            if (query.isNotBlank()) {
                add(
                    """
                    (
                        h.text LIKE ? OR h.text_ar LIKE ? OR h.text_en LIKE ?
                        OR b.name LIKE ? OR b.name_ar LIKE ? OR b.name_en LIKE ?
                        OR k.title LIKE ? OR k.title_ar LIKE ? OR k.title_en LIKE ?
                        OR ch.title LIKE ? OR ch.title_ar LIKE ? OR ch.title_en LIKE ?
                        OR h.note LIKE ? OR h.footnote LIKE ?
                    )
                    """.trimIndent()
                )
            }
        }
        return if (conditions.isEmpty()) "" else "WHERE " + conditions.joinToString(" AND ")
    }

    private fun hadithSearchArgs(query: String, bookId: Int? = null): Array<String> =
        buildList {
            if (bookId != null) add(bookId.toString())
            if (query.isNotBlank()) repeat(14) { add("%${query.trim()}%") }
        }.toTypedArray()

    suspend fun loadHadithCount(
        context: Context,
        query: String = "",
        bookId: Int? = null,
        hadithIds: Set<Int>? = null
    ): Int = withContext(Dispatchers.IO) {
        queryDb(context, HADITH_DB, 0) { db ->
            val filter = hadithSearchFilter(query, bookId, hadithIds)
            val args = hadithSearchArgs(query, bookId).takeIf { it.isNotEmpty() }
            db.rawQuery(
                """
                SELECT COUNT(DISTINCT h.id)
                FROM hadiths h
                LEFT JOIN book b ON b.id=h.book_id
                LEFT JOIN kitab k ON k.id=h.kitab_id
                LEFT JOIN chapters ch ON ch.id=h.chapter_id
                $filter
                """.trimIndent(), args
            ).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }
        }
    }

    suspend fun loadHadiths(
        context: Context,
        query: String = "",
        offset: Int = 0,
        limit: Int = 100,
        bookId: Int? = null,
        hadithIds: Set<Int>? = null
    ): List<HadithItem> = withContext(Dispatchers.IO) {
        if (hadithIds != null && hadithIds.none { it > 0 }) return@withContext emptyList()
        queryDb(context, HADITH_DB, emptyList()) { db ->
            val filter = hadithSearchFilter(query, bookId, hadithIds)
            val baseArgs = hadithSearchArgs(query, bookId)
            val args = baseArgs + arrayOf(limit.coerceIn(1, 250).toString(), offset.coerceAtLeast(0).toString())
            val sql = """
                SELECT h.id,h.sort,h.sort_in_book,h.text_ar,h.text,h.text_en,h.book_id,h.note,h.footnote,
                       COALESCE(b.name,'') AS book_name,
                       COALESCE(b.name_ar,'') AS book_name_ar,
                       COALESCE(b.name_en,'') AS book_name_en,
                       COALESCE(k.title,'') AS kitab_name,
                       COALESCE(k.title_ar,'') AS kitab_name_ar,
                       COALESCE(k.title_en,'') AS kitab_name_en,
                       COALESCE(ch.title,'') AS chapter_name,
                       COALESCE(ch.title_ar,'') AS chapter_name_ar,
                       COALESCE(ch.title_en,'') AS chapter_name_en
                FROM hadiths h
                LEFT JOIN book b ON b.id=h.book_id
                LEFT JOIN kitab k ON k.id=h.kitab_id
                LEFT JOIN chapters ch ON ch.id=h.chapter_id
                $filter
                ORDER BY h.book_id,h.sort,h.id
                LIMIT ? OFFSET ?
            """.trimIndent()
            db.rawQuery(sql, args).use { c ->
                buildList {
                    while (c.moveToNext()) add(
                        HadithItem(
                            id = c.int("id"),
                            sort = c.int("sort"),
                            sortInBook = c.string("sort_in_book"),
                            arabic = c.string("text_ar"),
                            sorani = c.string("text"),
                            english = c.string("text_en"),
                            bookId = c.int("book_id"),
                            bookName = c.string("book_name"),
                            bookNameAr = c.string("book_name_ar"),
                            bookNameEn = c.string("book_name_en"),
                            kitabName = c.string("kitab_name"),
                            kitabNameAr = c.string("kitab_name_ar"),
                            kitabNameEn = c.string("kitab_name_en"),
                            chapterName = c.string("chapter_name"),
                            chapterNameAr = c.string("chapter_name_ar"),
                            chapterNameEn = c.string("chapter_name_en"),
                            note = c.string("note"),
                            footnote = c.string("footnote")
                        )
                    )
                }
            }
        }
    }

    private const val SALAWATI_ID_OFFSET = 1_000_000

    private suspend fun loadSalawatiAzkarItems(context: Context, query: String): List<AzkarItem> =
        withContext(Dispatchers.IO) {
            val visibleCategories = loadAzkarCategories(context)
            fun categoryIdFor(sourceId: Int, ckb: String, arabic: String, english: String): Int {
                val labels = listOf(ckb, arabic, english)
                    .filter { it.isNotBlank() }
                    .map { normalizeArabicKey(it) }
                    .filter { it.isNotBlank() }
                val match = visibleCategories.firstOrNull { category ->
                    listOf(category.ckb, category.arabic, category.english)
                        .filter { it.isNotBlank() }
                        .map { normalizeArabicKey(it) }
                        .any { it.isNotBlank() && it in labels }
                }
                return match?.id ?: (sourceId + SALAWATI_ID_OFFSET)
            }
            queryDb(context, AZKAR_DB, emptyList()) { db ->
                val term = if (query.isBlank()) "%" else "%${query.trim()}%"
                val sql = """
                    SELECT it._id AS item_id,
                           COALESCE((SELECT t.chapter_name FROM azkar_chapter_translation t WHERE t.chapter_id=ch._id AND t.language='ckb' LIMIT 1),'') AS title_ckb,
                           COALESCE((SELECT t.chapter_name FROM azkar_chapter_translation t WHERE t.chapter_id=ch._id AND t.language='ar' LIMIT 1),'') AS title_ar,
                           COALESCE((SELECT t.chapter_name FROM azkar_chapter_translation t WHERE t.chapter_id=ch._id AND t.language='en' LIMIT 1),'') AS title_en,
                           it.item AS arabic,
                           COALESCE((SELECT t.item_translation FROM azkar_item_translation t WHERE t.item_id=it._id AND t.language='ckb' LIMIT 1),'') AS ckb_text,
                           COALESCE((SELECT t.item_translation FROM azkar_item_translation t WHERE t.item_id=it._id AND t.language='en' LIMIT 1),'') AS en_text,
                           COALESCE((SELECT r.reference FROM azkar_reference ref JOIN azkar_reference_translation r ON r.reference_id=ref._id WHERE ref.item_id=it._id AND r.language='ckb' LIMIT 1),'') AS ref_ckb,
                           COALESCE(cat._id,0) AS category_id,
                           COALESCE((SELECT t.category_name FROM azkar_category_translation t WHERE t.category_id=cat._id AND t.language='ckb' LIMIT 1),'') AS category_ckb,
                           COALESCE((SELECT t.category_name FROM azkar_category_translation t WHERE t.category_id=cat._id AND t.language='ar' LIMIT 1),'') AS category_ar,
                           COALESCE((SELECT t.category_name FROM azkar_category_translation t WHERE t.category_id=cat._id AND t.language='en' LIMIT 1),'') AS category_en
                    FROM azkar_item it
                    LEFT JOIN azkar_chapter ch ON ch._id=it.chapter_id
                    LEFT JOIN azkar_category cat ON cat._id=ch.category_id
                    WHERE it.item LIKE ?
                       OR EXISTS(SELECT 1 FROM azkar_item_translation t WHERE t.item_id=it._id AND t.item_translation LIKE ?)
                       OR EXISTS(SELECT 1 FROM azkar_chapter_translation t WHERE t.chapter_id=ch._id AND t.chapter_name LIKE ?)
                       OR EXISTS(SELECT 1 FROM azkar_category_translation t WHERE t.category_id=cat._id AND t.category_name LIKE ?)
                    ORDER BY cat._id,ch._id,it._id
                """.trimIndent()
                db.rawQuery(sql, arrayOf(term, term, term, term)).use { c ->
                    buildList {
                        while (c.moveToNext()) {
                            val categoryCkb = c.string("category_ckb")
                            val categoryArabic = c.string("category_ar")
                            val categoryEnglish = c.string("category_en")
                            add(
                                AzkarItem(
                                    c.int("item_id") + SALAWATI_ID_OFFSET,
                                    c.string("title_ckb"),
                                    c.string("title_ar"),
                                    c.string("title_en"),
                                    c.string("arabic"),
                                    c.string("ckb_text"),
                                    c.string("en_text"),
                                    1,
                                    c.string("ref_ckb"),
                                    categoryIdFor(c.int("category_id"), categoryCkb, categoryArabic, categoryEnglish),
                                    categoryCkb,
                                    categoryArabic,
                                    categoryEnglish
                                )
                            )
                        }
                    }
                }
            }
        }

    suspend fun loadAzkarCategories(context: Context): List<AzkarCategory> = withContext(Dispatchers.IO) {
        val noor = queryDb(context, PRAYER_DB, emptyList<AzkarCategory>()) { db ->
            db.rawQuery("SELECT _id,ckb,ar,en FROM azkar_categories ORDER BY _id", null).use { c ->
                buildList { while (c.moveToNext()) add(AzkarCategory(c.int("_id"), c.string("ckb"), c.string("ar"), c.string("en"))) }
            }
        }
        val salawati = queryDb(context, AZKAR_DB, emptyList<AzkarCategory>()) { db ->
            db.rawQuery(
                """
                SELECT category._id AS id,
                       COALESCE((SELECT t.category_name FROM azkar_category_translation t WHERE t.category_id=category._id AND t.language='ckb' LIMIT 1),'') AS ckb,
                       COALESCE((SELECT t.category_name FROM azkar_category_translation t WHERE t.category_id=category._id AND t.language='ar' LIMIT 1),'') AS ar,
                       COALESCE((SELECT t.category_name FROM azkar_category_translation t WHERE t.category_id=category._id AND t.language='en' LIMIT 1),'') AS en
                FROM azkar_category category ORDER BY category._id
                """.trimIndent(), null
            ).use { c ->
                buildList { while (c.moveToNext()) add(AzkarCategory(c.int("id") + SALAWATI_ID_OFFSET, c.string("ckb"), c.string("ar"), c.string("en"))) }
            }
        }
        val seenLabels = noor.flatMap { listOf(it.ckb, it.arabic, it.english) }
            .filter { it.isNotBlank() }
            .map { normalizeArabicKey(it) }
            .toMutableSet()
        val extra = salawati.filter { item ->
            val labels = listOf(item.ckb, item.arabic, item.english).filter { it.isNotBlank() }.map { normalizeArabicKey(it) }
            labels.none { it.isNotBlank() && it in seenLabels }.also { unique ->
                if (unique) seenLabels.addAll(labels.filter { it.isNotBlank() })
            }
        }
        (noor + extra).distinctBy { it.id }
    }

    suspend fun loadAzkar(context: Context, language: String, query: String = ""): List<AzkarItem> = withContext(Dispatchers.IO) {
        val primary = queryDb(context, PRAYER_DB, emptyList<AzkarItem>()) { db ->
            val sql = """
                SELECT it._id AS item_id, ch.ckb AS title_ckb, ch.ar AS title_ar, ch.en AS title_en,
                       it.ar AS arabic, it.ckb AS ckb_text, it.en AS en_text, it.counter AS item_count,
                       (SELECT refs.ckb FROM azkar_references refs WHERE refs.chapter_id=it.chapter_id ORDER BY refs._id LIMIT 1) AS ref_ckb,
                       COALESCE(cat._id,0) AS category_id,
                       COALESCE(cat.ckb,'') AS category_ckb,
                       COALESCE(cat.ar,'') AS category_ar,
                       COALESCE(cat.en,'') AS category_en
                FROM azkar_items it
                LEFT JOIN azkar_chapters ch ON ch._id=it.chapter_id
                LEFT JOIN azkar_categories cat ON cat._id=ch.category_id
                WHERE it.ckb LIKE ? OR it.ar LIKE ? OR it.en LIKE ? OR ch.ckb LIKE ? OR ch.ar LIKE ? OR ch.en LIKE ?
                   OR cat.ckb LIKE ? OR cat.ar LIKE ? OR cat.en LIKE ?
                ORDER BY cat._id,ch._id,it._id
            """.trimIndent()
            val term = if (query.isBlank()) "%" else "%${query.trim()}%"
            db.rawQuery(sql, arrayOf(term, term, term, term, term, term, term, term, term)).use { c ->
                buildList {
                    while (c.moveToNext()) add(
                        AzkarItem(
                            c.int("item_id"), c.string("title_ckb"), c.string("title_ar"), c.string("title_en"),
                            c.string("arabic"), c.string("ckb_text"), c.string("en_text"), c.int("item_count"), c.string("ref_ckb"),
                            c.int("category_id"), c.string("category_ckb"), c.string("category_ar"), c.string("category_en")
                        )
                    )
                }
            }
        }

        val translations = queryDb(context, AZKAR_DB, emptyMap<String, SalawatiAzkarText>()) { db ->
            db.rawQuery(
                """
                SELECT it.item AS arabic,
                       (SELECT t.item_translation FROM azkar_item_translation t WHERE t.item_id=it._id AND t.language='ckb' LIMIT 1) AS ckb_text,
                       (SELECT t.item_translation FROM azkar_item_translation t WHERE t.item_id=it._id AND t.language='en' LIMIT 1) AS en_text,
                       (SELECT r.reference FROM azkar_reference ref JOIN azkar_reference_translation r ON r.reference_id=ref._id WHERE ref.item_id=it._id AND r.language='ckb' LIMIT 1) AS ref_ckb
                FROM azkar_item it
                """.trimIndent(), null
            ).use { c ->
                buildMap {
                    while (c.moveToNext()) {
                        val key = normalizeArabicKey(c.string("arabic"))
                        if (key.isNotBlank()) put(key, SalawatiAzkarText(c.string("ckb_text"), c.string("en_text"), c.string("ref_ckb")))
                    }
                }
            }
        }
        val enrichedNoor = primary.map { item ->
            val extra = translations[normalizeArabicKey(item.arabic)]
            if (extra == null) item else item.copy(
                ckb = extra.ckb.ifBlank { item.ckb },
                english = extra.english.ifBlank { item.english },
                referenceCkb = extra.reference.ifBlank { item.referenceCkb }
            )
        }
        val salawatiItems = loadSalawatiAzkarItems(context, query)
        // The same Arabic remembrance can legitimately appear in multiple chapters
        // (for example, a morning and an evening section). De-duplicate only the
        // same Arabic text in the same normalized category and chapter, not by text
        // alone, so we never silently discard a distinct source context.
        fun identity(item: AzkarItem): String = listOf(
            normalizeArabicKey(item.arabic),
            item.categoryId.toString(),
            normalizeArabicKey(item.titleCkb.ifBlank { item.titleAr.ifBlank { item.titleEn } })
        ).joinToString("|")
        val seenEntries = enrichedNoor.map(::identity).toMutableSet()
        val uniqueSalawati = salawatiItems.filter { item ->
            val key = identity(item)
            key == "||" || seenEntries.add(key)
        }
        (enrichedNoor + uniqueSalawati).distinctBy { it.id }
    }

    suspend fun loadTasbihPresets(context: Context): List<TasbihPreset> = withContext(Dispatchers.IO) {
        queryDb(context, PRAYER_DB, emptyList()) { db ->
            db.rawQuery("SELECT id,zikr,count,time,meaning FROM tasbih ORDER BY id", null).use { c ->
                buildList {
                    while (c.moveToNext()) add(
                        TasbihPreset(c.int("id"), c.string("zikr"), c.int("count"), c.int("time"), c.string("meaning"))
                    )
                }
            }
        }
    }

    suspend fun loadNames(context: Context): List<AllahName> = withContext(Dispatchers.IO) {
        val noorNames = queryDb(context, PRAYER_DB, emptyList<AllahName>()) { db ->
            db.rawQuery(
                "SELECT _id,arabic,surah_ayah,highlight_section,sorani,badini,english,farsi,nu FROM allah_names ORDER BY _id",
                null
            ).use { c ->
                buildList {
                    while (c.moveToNext()) add(
                        AllahName(
                            c.int("_id") + 1,
                            c.string("arabic"),
                            c.string("sorani"),
                            c.string("badini"),
                            c.string("english"),
                            c.string("surah_ayah"),
                            c.string("highlight_section"),
                            c.string("farsi"),
                            c.string("nu")
                        )
                    )
                }
            }
        }
        val salawatiNames = queryDb(context, AZKAR_DB, emptyList<AllahName>()) { db ->
            db.rawQuery(
                """
                SELECT n._id AS id,n.name AS arabic,
                       COALESCE((SELECT t.name FROM name_translation t WHERE t.name_id=n._id AND t.language='ckb' LIMIT 1),'') AS ckb,
                       COALESCE((SELECT t.name FROM name_translation t WHERE t.name_id=n._id AND t.language='en' LIMIT 1),'') AS english,
                       COALESCE((SELECT t.name FROM name_translation t WHERE t.name_id=n._id AND t.language='ar' LIMIT 1),'') AS arabic_translation
                FROM name n
                ORDER BY n._id
                """.trimIndent(), null
            ).use { c ->
                buildList {
                    while (c.moveToNext()) add(
                        AllahName(
                            number = c.int("id"),
                            arabic = c.string("arabic").ifBlank { c.string("arabic_translation") },
                            ckb = c.string("ckb"),
                            badini = "",
                            english = c.string("english"),
                            extraText = c.string("arabic_translation")
                        )
                    )
                }
            }
        }

        val combined = linkedMapOf<String, AllahName>()
        noorNames.forEach { name ->
            val key = normalizeArabicKey(name.arabic).ifBlank { "noor:${name.number}" }
            combined[key] = name
        }
        salawatiNames.forEach { name ->
            val key = normalizeArabicKey(name.arabic).ifBlank { "salawati:${name.number}" }
            val existing = combined[key]
            if (existing == null) {
                combined[key] = name.copy(number = combined.size + 1)
            } else {
                combined[key] = existing.copy(
                    ckb = existing.ckb.ifBlank { name.ckb },
                    english = existing.english.ifBlank { name.english },
                    extraText = existing.extraText.ifBlank { name.extraText }
                )
            }
        }
        combined.values.mapIndexed { index, name -> name.copy(number = index + 1) }
    }

    /**
     * Search the city catalogue on demand instead of loading all 166k Salawati
     * locations into Compose state. Results preserve the country so same-name
     * cities in different countries remain selectable.
     */
    suspend fun searchCities(context: Context, query: String = "", limit: Int = 220): List<City> = withContext(Dispatchers.IO) {
        val term = query.trim()
        val pageLimit = limit.coerceIn(20, 300)
        val pattern = "%$term%"
        val noor = queryDb(context, PRAYER_DB, emptyList<City>()) { db ->
            val sql = if (term.isBlank()) {
                """
                SELECT city,lat,lon
                FROM cities
                ORDER BY CASE
                    WHEN lower(city) LIKE '%sulaymaniyah%' OR lower(city) LIKE '%sulaimani%' OR lower(city) LIKE '%halabja%' THEN 0
                    WHEN lower(city) LIKE '%erbil%' OR lower(city) LIKE '%kirkuk%' OR lower(city) LIKE '%baghdad%' THEN 1
                    WHEN lower(city) LIKE '%duhok%' OR lower(city) LIKE '%mosul%' THEN 2
                    ELSE 3 END, city
                LIMIT ?
                """.trimIndent()
            } else {
                """
                SELECT city,lat,lon FROM cities
                WHERE city LIKE ?
                ORDER BY CASE WHEN lower(city)=lower(?) THEN 0 WHEN lower(city) LIKE lower(?) THEN 1 ELSE 2 END,city
                LIMIT ?
                """.trimIndent()
            }
            val args = if (term.isBlank()) arrayOf(pageLimit.toString())
                else arrayOf(pattern, term, "$term%", pageLimit.toString())
            db.rawQuery(sql, args).use { c ->
                buildList {
                    while (c.moveToNext()) add(City(c.string("city"), c.double("lat"), c.double("lon"), ""))
                }
            }
        }
        val salawati = queryDb(context, SALAWATI_DB, emptyList<City>()) { db ->
            val where = if (term.isBlank()) {
                """
                (
                    lower(country.name) LIKE '%iraq%' OR lower(country.name) LIKE '%العراق%'
                    OR lower(l.name) LIKE '%sulaymaniyah%' OR lower(l.name) LIKE '%sulaimani%'
                    OR lower(l.name) LIKE '%halabja%' OR lower(l.name) LIKE '%erbil%'
                    OR lower(l.name) LIKE '%hawler%' OR lower(l.name) LIKE '%kirkuk%'
                    OR lower(l.name) LIKE '%baghdad%' OR lower(l.name) LIKE '%duhok%'
                    OR lower(l.name) LIKE '%mosul%' OR lower(l.name) LIKE '%basra%'
                    OR lower(l.name) LIKE '%najaf%' OR lower(l.name) LIKE '%karbala%'
                )
                """.trimIndent()
            } else "(l.name LIKE ? OR country.name LIKE ?)"
            val sql = """
                SELECT l.name,l.latitude,l.longitude,COALESCE(country.name,'') AS country_name
                FROM location l
                JOIN prayer_time pt ON pt.location_id=l._id
                LEFT JOIN country ON country._id=l.country_id
                WHERE $where
                GROUP BY l._id
                ORDER BY
                    CASE
                        WHEN lower(country.name) LIKE '%iraq%' OR lower(country.name) LIKE '%العراق%' THEN 0
                        WHEN lower(l.name) LIKE '%sulaymaniyah%' OR lower(l.name) LIKE '%sulaimani%' THEN 1
                        ELSE 2
                    END,
                    CASE WHEN lower(l.name)=lower(?) THEN 0 WHEN lower(l.name) LIKE lower(?) THEN 1 ELSE 2 END,
                    country_name,l.name
                LIMIT ?
            """.trimIndent()
            val args = if (term.isBlank()) {
                arrayOf("", "", pageLimit.toString())
            } else {
                arrayOf(pattern, pattern, term, "$term%", pageLimit.toString())
            }
            db.rawQuery(sql, args).use { c ->
                buildList {
                    while (c.moveToNext()) add(
                        City(c.string("name"), c.double("latitude"), c.double("longitude"), c.string("country_name"))
                    )
                }
            }
        }
        (noor + salawati)
            .distinctBy { "${it.name.trim().lowercase(Locale.ROOT)}|${it.country.trim().lowercase(Locale.ROOT)}" }
            .let { results ->
                if (term.isBlank()) results
                else results.sortedWith(
                    compareBy<City> {
                        when {
                            it.name.equals(term, true) -> 0
                            it.name.startsWith(term, true) -> 1
                            it.country.equals(term, true) -> 2
                            else -> 3
                        }
                    }.thenBy { it.country.lowercase(Locale.ROOT) }.thenBy { it.name.lowercase(Locale.ROOT) }
                )
            }
    }

    suspend fun findCityByName(context: Context, name: String, countryName: String = ""): City? = withContext(Dispatchers.IO) {
        val noor = queryDb(context, PRAYER_DB, null as City?) { db ->
            db.rawQuery(
                "SELECT city,lat,lon FROM cities WHERE lower(city)=lower(?) LIMIT 1",
                arrayOf(name)
            ).use { c ->
                if (c.moveToFirst()) City(c.string("city"), c.double("lat"), c.double("lon")) else null
            }
        }
        if (noor != null && countryName.isBlank()) return@withContext noor
        queryDb(context, SALAWATI_DB, noor) { db ->
            val sql = if (countryName.isBlank()) {
                """
                SELECT l.name,l.latitude,l.longitude,COALESCE(country.name,'') AS country_name
                FROM location l LEFT JOIN country ON country._id=l.country_id
                WHERE lower(l.name)=lower(?)
                ORDER BY CASE WHEN lower(country.name) LIKE '%iraq%' OR lower(country.name) LIKE '%العراق%' THEN 0 ELSE 1 END
                LIMIT 1
                """.trimIndent()
            } else {
                """
                SELECT l.name,l.latitude,l.longitude,COALESCE(country.name,'') AS country_name
                FROM location l LEFT JOIN country ON country._id=l.country_id
                WHERE lower(l.name)=lower(?) AND lower(country.name)=lower(?)
                LIMIT 1
                """.trimIndent()
            }
            val args = if (countryName.isBlank()) arrayOf(name) else arrayOf(name, countryName)
            db.rawQuery(sql, args).use { c ->
                if (c.moveToFirst()) City(c.string("name"), c.double("latitude"), c.double("longitude"), c.string("country_name")) else noor
            }
        }
    }

    /** Limited compatibility helper for callers that only need common city choices. */
    suspend fun loadCities(context: Context): List<City> = searchCities(context, "", limit = 250)

    internal fun addMinutesToPrayerTime(value: String, offsetMinutes: Int): String {
        if (offsetMinutes == 0) return value
        val parts = value.trim().split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: return value
        val minute = parts.getOrNull(1)?.take(2)?.toIntOrNull() ?: return value
        if (hour !in 0..23 || minute !in 0..59) return value
        val total = (hour * 60 + minute + offsetMinutes).mod(24 * 60)
        return "%02d:%02d".format(Locale.US, total / 60, total % 60)
    }

    /**
     * Noor's original extra table defines cities whose time comes from a
     * reference city (c_base) plus six explicit minute offsets in prayer order:
     * Fajr, sunrise, Dhuhr, Asr, Maghrib, Isha. Used only when there is no
     * direct timetable for the requested city in either source database.
     */
    private suspend fun loadNoorPrayerTimesWithCorrection(
        context: Context,
        requestedCity: String,
        date: Date
    ): PrayerRow? = withContext(Dispatchers.IO) {
        val md = SimpleDateFormat("MM-dd", Locale.US).format(date)
        queryDb(context, PRAYER_DB, null as PrayerRow?) { db ->
            val correction = db.rawQuery(
                """
                SELECT city,name,c_base,correction
                FROM extra
                WHERE lower(city)=lower(?) OR lower(name)=lower(?)
                ORDER BY CASE WHEN lower(city)=lower(?) THEN 0 ELSE 1 END,c_order
                LIMIT 1
                """.trimIndent(),
                arrayOf(requestedCity, requestedCity, requestedCity)
            ).use { c ->
                if (c.moveToFirst()) {
                    arrayOf(c.string("city"), c.string("name"), c.string("c_base"), c.string("correction"))
                } else null
            } ?: return@queryDb null

            val canonicalCity = correction[0].ifBlank { requestedCity }
            val baseCity = correction[2].ifBlank { canonicalCity }
            val minuteValues = correction[3]
                .split(",")
                .mapNotNull { it.trim().toIntOrNull() }
                .take(6)
            val minuteOffsets = List(6) { minuteValues.getOrElse(it) { 0 } }

            val base = db.rawQuery(
                """
                SELECT city,date,fajr,sunrise,dhuhr,asr,maghrib,isha
                FROM staticprayertimes
                WHERE lower(city)=lower(?) AND date=?
                LIMIT 1
                """.trimIndent(),
                arrayOf(baseCity, md)
            ).use { c ->
                if (c.moveToFirst()) PrayerRow(
                    city = c.string("city"),
                    date = c.string("date"),
                    fajr = c.string("fajr"),
                    sunrise = c.string("sunrise"),
                    dhuhr = c.string("dhuhr"),
                    asr = c.string("asr"),
                    maghrib = c.string("maghrib"),
                    isha = c.string("isha"),
                    source = "noor-base:${c.string("city")}"
                ) else null
            } ?: return@queryDb null

            base.copy(
                city = canonicalCity,
                fajr = addMinutesToPrayerTime(base.fajr, minuteOffsets[0]),
                sunrise = addMinutesToPrayerTime(base.sunrise, minuteOffsets[1]),
                dhuhr = addMinutesToPrayerTime(base.dhuhr, minuteOffsets[2]),
                asr = addMinutesToPrayerTime(base.asr, minuteOffsets[3]),
                maghrib = addMinutesToPrayerTime(base.maghrib, minuteOffsets[4]),
                isha = addMinutesToPrayerTime(base.isha, minuteOffsets[5]),
                source = buildString {
                    append("noor-correction:")
                    append(canonicalCity)
                    append(" (")
                    append(baseCity)
                    if (minuteOffsets.any { it != 0 }) {
                        append("; ")
                        append(minuteOffsets.joinToString("/") { if (it > 0) "+$it" else it.toString() })
                        append(" min")
                    }
                    append(")")
                }
            )
        }
    }

    /**
     * Prefer the selected city's Salawati source timetable when it exists.
     * Noor remains the verified regional fallback and supplies its source-linked
     * correction table for Iraq/local cities without a direct Salawati row.
     */
    private val prayerClockPattern = Regex("""^(?:[01]\d|2[0-3]):[0-5]\d$""")

    internal fun hasValidPrayerTimes(row: PrayerRow?): Boolean =
        row != null && listOf(row.fajr, row.sunrise, row.dhuhr, row.asr, row.maghrib, row.isha)
            .all { prayerClockPattern.matches(it.trim()) }

    internal fun preferredSourcePrayerTimes(salawati: PrayerRow?, noor: PrayerRow?): PrayerRow? =
        salawati?.takeIf(::hasValidPrayerTimes) ?: noor?.takeIf(::hasValidPrayerTimes)

    /**
     * GPS can find Noor and Salawati rows at identical coordinates. Keep physical
     * distance as the first criterion, but prefer Salawati when distances tie;
     * otherwise a stable combined-list order can silently undo the city-source
     * preference.
     */
    internal fun orderPrayerCandidates(candidates: List<Pair<PrayerRow, Double>>): List<Pair<PrayerRow, Double>> =
        candidates.sortedWith(
            compareBy<Pair<PrayerRow, Double>> { it.second }
                .thenBy {
                    when {
                        it.first.source.startsWith("salawati", ignoreCase = true) -> 0
                        it.first.source.startsWith("noor", ignoreCase = true) -> 1
                        else -> 2
                    }
                }
        )

    suspend fun loadPrayerTimes(
        context: Context,
        city: String,
        date: Date = Date(),
        countryName: String = ""
    ): PrayerRow? = withContext(Dispatchers.IO) {
        val md = SimpleDateFormat("MM-dd", Locale.US).format(date)
        val country = countryName.trim()
        val useNoorRegionalData = country.isBlank() ||
            country.contains("iraq", ignoreCase = true) ||
            country.contains("العراق", ignoreCase = true)

        // Salawati is the first-choice city database: users can select the exact
        // country/location record from its 166k-location catalogue, including
        // the original fixed and dependent timetable metadata.
        val salawati = loadSalawatiPrayerTimesForCity(context, city, country, date)
        val noor = if (useNoorRegionalData) {
            queryDb(context, PRAYER_DB, null as PrayerRow?) { db ->
                db.rawQuery(
                    "SELECT city,date,fajr,sunrise,dhuhr,asr,maghrib,isha FROM staticprayertimes WHERE lower(city)=lower(?) AND date=? LIMIT 1",
                    arrayOf(city, md)
                ).use { c ->
                    if (c.moveToFirst()) PrayerRow(
                        c.string("city"), c.string("date"), c.string("fajr"), c.string("sunrise"),
                        c.string("dhuhr"), c.string("asr"), c.string("maghrib"), c.string("isha"),
                        source = "noor:${c.string("city")}"
                    ) else null
                }
            }
        } else null

        preferredSourcePrayerTimes(salawati, noor)?.let { return@withContext it }
        if (useNoorRegionalData) {
            loadNoorPrayerTimesWithCorrection(context, city, date).takeIf(::hasValidPrayerTimes)
        } else null
    }

    /**
     * Use Salawati's original city timetable. Several named localities explicitly
     * reference a base city's prayer schedule via location.prayer_dependent_id.
     * Resolve that relation instead of silently substituting solar estimates.
     */
    private suspend fun loadSalawatiPrayerTimesForCity(
        context: Context,
        cityName: String,
        countryName: String,
        date: Date
    ): PrayerRow? = withContext(Dispatchers.IO) {
        val md = SimpleDateFormat("MM-dd", Locale.US).format(date)
        queryDb(context, SALAWATI_DB, null as PrayerRow?) { db ->
            db.rawQuery(
                """
                SELECT requested.name AS city,
                       pt.date,pt.fajr,pt.sunrise,pt.dhuhr,pt.asr,pt.maghrib,pt.isha,
                       requested.latitude AS lat,requested.longitude AS lon,
                       country.name AS country,
                       COALESCE(requested.has_fixed_prayer_time,0) AS has_fixed,
                       COALESCE(requested.prayer_dependent_id,0) AS dependent_id,
                       COALESCE(base.name,'') AS base_city
                FROM location requested
                JOIN country ON country._id=requested.country_id
                LEFT JOIN location base ON base._id=requested.prayer_dependent_id
                LEFT JOIN prayer_time pt
                  ON pt.location_id = CASE
                       WHEN COALESCE(requested.has_fixed_prayer_time,0)=1 THEN requested._id
                       WHEN COALESCE(requested.prayer_dependent_id,0)>0 THEN requested.prayer_dependent_id
                       ELSE requested._id
                     END
                 AND pt.date=?
                WHERE lower(requested.name)=lower(?)
                  AND (?='' OR lower(country.name)=lower(?))
                ORDER BY
                    CASE WHEN COALESCE(requested.has_fixed_prayer_time,0)=1 THEN 0
                         WHEN COALESCE(requested.prayer_dependent_id,0)>0 THEN 1
                         ELSE 2 END,
                    CASE WHEN lower(country.name) LIKE '%iraq%' OR lower(country.name) LIKE '%العراق%' THEN 0 ELSE 1 END,
                    requested._id
                LIMIT 1
                """.trimIndent(),
                arrayOf(md, cityName, countryName, countryName)
            ).use { c ->
                if (!c.moveToFirst()) return@use null
                val fajr = c.string("fajr")
                val sunrise = c.string("sunrise")
                val dhuhr = c.string("dhuhr")
                val asr = c.string("asr")
                val maghrib = c.string("maghrib")
                val isha = c.string("isha")
                if (fajr.isBlank() || dhuhr.isBlank() || isha.isBlank()) return@use null
                val dependency = c.int("dependent_id")
                val baseCity = c.string("base_city")
                val source = if (dependency > 0 && baseCity.isNotBlank()) {
                    "salawati-dependent:${c.string("city")} → $baseCity, ${c.string("country")}"
                } else {
                    "salawati:${c.string("city")}, ${c.string("country")}"
                }
                PrayerRow(
                    city = c.string("city"),
                    date = c.string("date"),
                    fajr = fajr,
                    sunrise = sunrise,
                    dhuhr = dhuhr,
                    asr = asr,
                    maghrib = maghrib,
                    isha = isha,
                    source = source,
                    latitude = c.double("lat"),
                    longitude = c.double("lon")
                )
            }
        }
    }

    private suspend fun loadNearbyTimetableRows(
        context: Context,
        latitude: Double,
        longitude: Double,
        date: Date,
        radiusKm: Double
    ): List<PrayerRow> = withContext(Dispatchers.IO) {
        val monthDay = SimpleDateFormat("MM-dd", Locale.US).format(date)
        val latDelta = radiusKm / 110.6
        val lonDelta = radiusKm / (111.32 * kotlin.math.cos(Math.toRadians(latitude)).let { kotlin.math.abs(it).coerceAtLeast(0.15) })
        val minLat = (latitude - latDelta).coerceAtLeast(-90.0)
        val maxLat = (latitude + latDelta).coerceAtMost(90.0)
        val minLon = (longitude - lonDelta).coerceAtLeast(-180.0)
        val maxLon = (longitude + lonDelta).coerceAtMost(180.0)
        val noorRows = queryDb(context, PRAYER_DB, emptyList<PrayerRow>()) { db ->
            db.rawQuery(
                """
                SELECT c.city,pt.date,pt.fajr,pt.sunrise,pt.dhuhr,pt.asr,pt.maghrib,pt.isha,c.lat,c.lon
                FROM staticprayertimes pt
                JOIN cities c ON lower(c.city)=lower(pt.city)
                WHERE pt.date=?
                  AND c.lat BETWEEN ? AND ?
                  AND c.lon BETWEEN ? AND ?
                ORDER BY ((c.lat-?)*(c.lat-?)) + ((c.lon-?)*(c.lon-?))
                LIMIT 120
                """.trimIndent(),
                arrayOf(
                    monthDay, minLat.toString(), maxLat.toString(), minLon.toString(), maxLon.toString(),
                    latitude.toString(), latitude.toString(), longitude.toString(), longitude.toString()
                )
            ).use { c ->
                buildList {
                    while (c.moveToNext()) add(
                        PrayerRow(
                            city=c.string("city"), date=c.string("date"), fajr=c.string("fajr"),
                            sunrise=c.string("sunrise"), dhuhr=c.string("dhuhr"), asr=c.string("asr"),
                            maghrib=c.string("maghrib"), isha=c.string("isha"),
                            source="noor:${c.string("city")}", latitude=c.double("lat"), longitude=c.double("lon")
                        )
                    )
                }
            }
        }
        val salawatiRows = queryDb(context, SALAWATI_DB, emptyList<PrayerRow>()) { db ->
            db.rawQuery(
                """
                SELECT l.name AS city,pt.date,pt.fajr,pt.sunrise,pt.dhuhr,pt.asr,pt.maghrib,pt.isha,
                       l.latitude AS lat,l.longitude AS lon,COALESCE(country.name,'') AS country_name
                FROM prayer_time pt
                JOIN location l ON l._id=pt.location_id
                LEFT JOIN country ON country._id=l.country_id
                WHERE pt.date=?
                  AND l.latitude BETWEEN ? AND ?
                  AND l.longitude BETWEEN ? AND ?
                ORDER BY ((l.latitude-?)*(l.latitude-?)) + ((l.longitude-?)*(l.longitude-?))
                LIMIT 120
                """.trimIndent(),
                arrayOf(
                    monthDay, minLat.toString(), maxLat.toString(), minLon.toString(), maxLon.toString(),
                    latitude.toString(), latitude.toString(), longitude.toString(), longitude.toString()
                )
            ).use { c ->
                buildList {
                    while (c.moveToNext()) add(
                        PrayerRow(
                            city=c.string("city"), date=c.string("date"), fajr=c.string("fajr"),
                            sunrise=c.string("sunrise"), dhuhr=c.string("dhuhr"), asr=c.string("asr"),
                            maghrib=c.string("maghrib"), isha=c.string("isha"),
                            source="salawati:${c.string("city")}, ${c.string("country_name")}",
                            latitude=c.double("lat"), longitude=c.double("lon")
                        )
                    )
                }
            }
        }
        val dependentRows = queryDb(context, SALAWATI_DB, emptyList<PrayerRow>()) { db ->
            db.rawQuery(
                """
                SELECT dep.name AS city,pt.date,pt.fajr,pt.sunrise,pt.dhuhr,pt.asr,pt.maghrib,pt.isha,
                       dep.latitude AS lat,dep.longitude AS lon,
                       COALESCE(country.name,'') AS country_name,
                       COALESCE(base.name,'') AS base_city
                FROM location dep
                JOIN location base ON base._id=dep.prayer_dependent_id
                JOIN prayer_time pt ON pt.location_id=base._id
                LEFT JOIN country ON country._id=dep.country_id
                WHERE pt.date=?
                  AND dep.prayer_dependent_id IS NOT NULL
                  AND COALESCE(dep.has_fixed_prayer_time,0)=0
                  AND dep.latitude BETWEEN ? AND ?
                  AND dep.longitude BETWEEN ? AND ?
                  AND NOT EXISTS (
                      SELECT 1 FROM prayer_time own_pt
                      WHERE own_pt.location_id=dep._id AND own_pt.date=pt.date
                  )
                ORDER BY ((dep.latitude-?)*(dep.latitude-?)) + ((dep.longitude-?)*(dep.longitude-?))
                LIMIT 120
                """.trimIndent(),
                arrayOf(
                    monthDay, minLat.toString(), maxLat.toString(), minLon.toString(), maxLon.toString(),
                    latitude.toString(), latitude.toString(), longitude.toString(), longitude.toString()
                )
            ).use { c ->
                buildList {
                    while (c.moveToNext()) add(
                        PrayerRow(
                            city = c.string("city"),
                            date = c.string("date"),
                            fajr = c.string("fajr"),
                            sunrise = c.string("sunrise"),
                            dhuhr = c.string("dhuhr"),
                            asr = c.string("asr"),
                            maghrib = c.string("maghrib"),
                            isha = c.string("isha"),
                            source = "salawati-dependent:${c.string("city")} → ${c.string("base_city")}, ${c.string("country_name")}",
                            latitude = c.double("lat"),
                            longitude = c.double("lon")
                        )
                    )
                }
            }
        }
        noorRows + salawatiRows + dependentRows
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
        val candidates = loadNearbyTimetableRows(context, latitude, longitude, date, staticRadiusKm)
            .filter(::hasValidPrayerTimes)
            .mapNotNull { row ->
                val rowLat = row.latitude ?: return@mapNotNull null
                val rowLon = row.longitude ?: return@mapNotNull null
                val distance = PrayerTimeCalculator.distanceKm(latitude, longitude, rowLat, rowLon)
                if (distance <= staticRadiusKm) row to distance else null
            }
            .sortedBy { it.second }
        val nearest = orderPrayerCandidates(candidates).firstOrNull()
        if (nearest != null) {
            nearest.first.copy(
                source = "${nearest.first.source} • ${"%.1f".format(Locale.US, nearest.second)}km",
                latitude = latitude,
                longitude = longitude
            )
        } else {
            PrayerTimeCalculator.calculate(date, latitude, longitude, method, asrSchool)
        }
    }

    suspend fun loadBooks(context: Context, query: String = ""): List<BookItem> = withContext(Dispatchers.IO) {
        queryDb(context, BOOKS_DB, emptyList()) { db ->
            val like = if (query.isBlank()) "%" else "%${query.trim()}%"
            db.rawQuery(
                """
                SELECT id,title,writer,description,page_number,url,
                       COALESCE(branch,'') AS branch,
                       COALESCE(unique_text,'') AS unique_text,
                       COALESCE(url_cover,'') AS url_cover,
                       COALESCE(book_color,'') AS book_color,
                       COALESCE(text_color,'') AS text_color,
                       COALESCE(extra_text,'') AS extra_text,
                       COALESCE(extra_number,0) AS extra_number,
                       COALESCE(fav_time,'') AS fav_time
                FROM book_data
                WHERE title LIKE ? OR writer LIKE ? OR description LIKE ?
                   OR unique_text LIKE ? OR extra_text LIKE ? OR branch LIKE ?
                ORDER BY sorter,title
                """.trimIndent(),
                arrayOf(like, like, like, like, like, like)
            ).use { c ->
                buildList {
                    while (c.moveToNext()) add(
                        BookItem(
                            id = c.int("id"),
                            title = c.string("title"),
                            writer = c.string("writer"),
                            description = c.string("description"),
                            pages = c.int("page_number"),
                            url = c.string("url"),
                            branch = c.string("branch"),
                            uniqueText = c.string("unique_text"),
                            coverUrl = c.string("url_cover"),
                            bookColor = c.string("book_color"),
                            textColor = c.string("text_color"),
                            extraText = c.string("extra_text"),
                            extraNumber = c.int("extra_number"),
                            sourceFavoriteTime = c.string("fav_time")
                        )
                    )
                }
            }
        }
    }

    private fun lexiconFilter(query: String): String =
        if (query.isBlank()) "" else "WHERE w.title LIKE ? OR w.content LIKE ? OR w.root LIKE ? OR r.reference_name LIKE ?"

    private fun lexiconArgs(query: String): Array<String> =
        Array(4) { "%${query.trim()}%" }

    suspend fun loadLexiconCount(context: Context, query: String = ""): Int = withContext(Dispatchers.IO) {
        queryDb(context, LEXICON_DB, 0) { db ->
            val filter = lexiconFilter(query)
            val args = if (query.isBlank()) null else lexiconArgs(query)
            db.rawQuery(
                """
                SELECT COUNT(*)
                FROM word_list w
                LEFT JOIN "references" r ON r.id=w.reference
                $filter
                """.trimIndent(), args
            ).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }
        }
    }

    suspend fun searchLexicon(
        context: Context,
        query: String,
        offset: Int = 0,
        limit: Int = 100
    ): List<LexiconItem> = withContext(Dispatchers.IO) {
        queryDb(context, LEXICON_DB, emptyList()) { db ->
            val filter = lexiconFilter(query)
            val baseArgs = if (query.isBlank()) emptyArray() else lexiconArgs(query)
            val args = baseArgs + arrayOf(limit.coerceIn(1, 300).toString(), offset.coerceAtLeast(0).toString())
            db.rawQuery(
                """
                SELECT w.title,w.content,r.reference_name,w.root
                FROM word_list w
                LEFT JOIN "references" r ON r.id=w.reference
                $filter
                ORDER BY w.title,w.root
                LIMIT ? OFFSET ?
                """.trimIndent(), args
            ).use { c ->
                buildList {
                    while (c.moveToNext()) {
                        add(LexiconItem(c.string("title"), c.string("content"), c.string("reference_name"), c.string("root")))
                    }
                }
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
                "SELECT _id,title,ar_desc,ku_desc,badini_title,badini,fav_time FROM farmuda WHERE title LIKE ? OR ar_desc LIKE ? OR ku_desc LIKE ? OR badini_title LIKE ? OR badini LIKE ? ORDER BY _id",
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
