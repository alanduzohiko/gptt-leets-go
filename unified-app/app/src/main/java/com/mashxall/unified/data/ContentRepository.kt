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
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Surah(val number: Int, val arabicName: String, val englishName: String, val ayahCount: Int)
data class Ayah(val surah: Int, val number: Int, val arabic: String, val translation: String, val page: Int = 0)
data class HadithItem(val id: Int, val arabic: String, val sorani: String, val english: String, val bookId: Int)
data class AzkarItem(val id: Int, val titleCkb: String, val titleAr: String, val titleEn: String, val arabic: String, val ckb: String, val english: String, val count: Int, val referenceCkb: String)
data class AllahName(val number: Int, val arabic: String, val ckb: String, val badini: String, val english: String)
data class City(val name: String, val latitude: Double, val longitude: Double)
data class PrayerRow(val city: String, val date: String, val fajr: String, val sunrise: String, val dhuhr: String, val asr: String, val maghrib: String, val isha: String)
data class TafsirBook(val id: String, val labelCkb: String, val labelAr: String, val labelEn: String, val assetPath: String, val language: String)
data class BookItem(val id: Int, val title: String, val writer: String, val description: String, val pages: Int, val url: String)
data class LexiconItem(val title: String, val content: String, val reference: String, val root: String)
data class WordItem(val position: Int, val root: String, val grammar: String, val arabicShape: String)
data class AudioItem(val assetPath: String, val title: String, val group: String)
data class Reciter(val id: Int, val nameArabic: String, val nameEnglish: String, val audioType: Int, val baseUrl: String, val order: Int)
data class FarmudaItem(val id: Int, val title: String, val arabic: String, val sorani: String, val badiniTitle: String, val badini: String, val preferredTime: Int)
data class ContentCounts(val databases: Int, val audioFiles: Int, val fonts: Int, val pdfs: Int, val assetFiles: Int)
private data class SalawatiAzkarText(val ckb: String, val english: String, val reference: String)
private data class KhairahPageMeta(
    val id: Int, val volume: String, val page: String, val offset: Int,
    val length: Int, val byteCount: Int, val sha256: String
)

private fun normalizeArabicKey(value: String): String = value.lowercase(Locale.ROOT).filterNot { ch ->
    ch in '\u064B'..'\u065F' || ch == '\u0670' || ch == '\u0640' || !ch.isLetterOrDigit()
}

object ContentRepository {
    const val QURAN_DB = "content/noor_tafsir_1.2.2/base/assets/quran.db"
    const val PRAYER_DB = "content/noor_tafsir_1.2.2/base/assets/prayertimes.db"
    const val HADITH_DB = "content/noor_tafsir_1.2.2/base/assets/db_hadith.db"
    const val WORD_DB = "content/noor_tafsir_1.2.2/base/assets/db_word_by_word.db"
    const val LEXICON_DB = "content/noor_tafsir_1.2.2/base/assets/db_words_lexicon.db"
    const val AZKAR_DB = "content/salawati_3.1.3/base/assets/database/muslim_db_v2.5.1.db"
    const val BOOKS_DB = "content/noor_tafsir_1.2.2/base/assets/tn_books.db"

    val tafsirBooks = listOf(
        TafsirBook("roshn", "ڕۆشن", "تفسير ڕۆشن", "Roshn Kurdish Tafsir", "content/noor_tafsir_1.2.2/base/assets/tafseeri_roshn.db", "ckb"),
        TafsirBook("hazhar", "هەژار", "تفسير هەژار", "Hazhar Kurdish Tafsir", "content/noor_tafsir_1.2.2/base/assets/tafseeri_hazhar.db", "ckb"),
        TafsirBook("maisar", "ميسەر", "التيسير", "Muyassar Tafsir", "content/noor_tafsir_1.2.2/base/assets/tafseer_maisar.db", "ckb"),
        TafsirBook("sahih", "Sahih International", "Sahih International", "Sahih International", "content/noor_tafsir_1.2.2/base/assets/tafseer_sahih_international.db", "en"),
        TafsirBook("noor", "تفسير نور", "تفسیر نور", "Noor Tafsir", "content/noor_tafsir_1.2.2/base/assets/tafseeri_noor.db", "fa"),
        TafsirBook("hawrami", "هەورامی", "التفسير الهورامي", "Hawrami Tafsir", "content/noor_tafsir_1.2.2/base/assets/tafseeri_hawrami.db", "ckb"),
        TafsirBook("runahi", "ڕووناهی بادینی", "التفسير الباديني", "Runahi Badini Tafsir", "content/noor_tafsir_1.2.2/base/assets/tafseeri_runahi_badini.db", "bad"),
        TafsirBook("zhian", "ژیان بادینی", "تفسير ژيان", "Zhian Badini Tafsir", "content/noor_tafsir_1.2.2/base/assets/tafseeri_zhian_badini.db", "bad"),
        TafsirBook("saadi", "تفسير السعدي", "تفسير السعدي", "Saadi Tafsir", "content/noor_tafsir_1.2.2/base/assets/tafseeri_saadi.db", "ckb"),
        TafsirBook("mukhtasar", "مختصر", "المختصر", "Mukhtasar Tafsir", "content/noor_tafsir_1.2.2/base/assets/tafseeri_mukhtasar.db", "ar"),
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

    suspend fun loadSurahs(context: Context): List<Surah> = withContext(Dispatchers.IO) {
        queryDb(context, QURAN_DB, emptyList()) { db ->
            db.rawQuery("SELECT surah_no,name_arabic,name_english,ayah_number FROM surah_name ORDER BY surah_no", null).use { c ->
                buildList { while (c.moveToNext()) add(Surah(c.int("surah_no"), c.string("name_arabic"), c.string("name_english"), c.int("ayah_number"))) }
            }
        }
    }

    suspend fun loadAyahs(context: Context, surah: Int, language: String, query: String = ""): List<Ayah> = withContext(Dispatchers.IO) {
        // English uses the dedicated Sahih International translation DB. Arabic
        // mode presents the Arabic Qur'an text without mislabelling a tafsir as
        // a translation. Sorani uses Noor's concise Kurdish translation.
        val translationBook = if (language == "en") "sahih" else ""
        val extraTranslations = if (translationBook.isNotEmpty()) {
            queryDb(context, tafsirBooks.first { it.id == translationBook }.assetPath, emptyMap<Int, String>()) { db ->
                db.rawQuery("SELECT verse_id,text FROM quran WHERE surah_id=?", arrayOf(surah.toString())).use { c ->
                    buildMap { while (c.moveToNext()) put(c.int("verse_id"), c.string("text")) }
                }
            }
        } else emptyMap()
        queryDb(context, QURAN_DB, emptyList()) { db ->
            val sql = "SELECT surah_id,verse_id,quran_arabic,translation_puxta,page FROM quran WHERE surah_id=? ORDER BY verse_id"
            db.rawQuery(sql, arrayOf(surah.toString())).use { c ->
                buildList {
                    while (c.moveToNext()) {
                        val num = c.int("verse_id")
                        val arabic = c.string("quran_arabic")
                        val translation = when (language) {
                            "en" -> extraTranslations[num].orEmpty()
                            "ar" -> ""
                            else -> c.string("translation_puxta")
                        }
                        if (query.isBlank() || arabic.contains(query, true) || translation.contains(query, true) || num.toString() == query) {
                            add(Ayah(surah, num, arabic, translation, c.int("page")))
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
        queryDb(context, book.assetPath, "") { db ->
            val tables = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null).use { c ->
                buildSet { while (c.moveToNext()) add(c.getString(0)) }
            }
            if ("quran" !in tables) return@queryDb ""
            val cols = db.rawQuery("PRAGMA table_info(quran)", null).use { c ->
                buildSet { while (c.moveToNext()) add(c.getString(1)) }
            }
            if (!setOf("surah_id", "verse_id", "text").all { it in cols }) return@queryDb ""
            db.rawQuery("SELECT text FROM quran WHERE surah_id=? AND verse_id=? LIMIT 1", arrayOf(surah.toString(), ayah.toString())).use { c ->
                if (c.moveToFirst()) c.string("text") else ""
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
        out.toString().trim()
    }

    private fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    suspend fun loadHadiths(context: Context, query: String = ""): List<HadithItem> = withContext(Dispatchers.IO) {
        queryDb(context, HADITH_DB, emptyList()) { db ->
            val sql = if (query.isBlank()) {
                "SELECT id,text_ar,text,text_en,book_id FROM hadiths ORDER BY id DESC LIMIT 100"
            } else {
                "SELECT id,text_ar,text,text_en,book_id FROM hadiths WHERE text LIKE ? OR text_ar LIKE ? OR text_en LIKE ? ORDER BY id DESC LIMIT 100"
            }
            val args = if (query.isBlank()) null else arrayOf("%$query%", "%$query%", "%$query%")
            db.rawQuery(sql, args).use { c ->
                buildList { while (c.moveToNext()) add(HadithItem(c.int("id"), c.string("text_ar"), c.string("text"), c.string("text_en"), c.int("book_id"))) }
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
        queryDb(context, PRAYER_DB, emptyList()) { db ->
            db.rawQuery("SELECT city,lat,lon FROM cities ORDER BY city", null).use { c ->
                buildList { while (c.moveToNext()) add(City(c.string("city"), c.double("lat"), c.double("lon"))) }
            }
        }
    }

    suspend fun loadPrayerTimes(context: Context, city: String, date: Date = Date()): PrayerRow? = withContext(Dispatchers.IO) {
        val md = SimpleDateFormat("MM-dd", Locale.US).format(date)
        queryDb(context, PRAYER_DB, null) { db ->
            db.rawQuery("SELECT city,date,fajr,sunrise,dhuhr,asr,maghrib,isha FROM staticprayertimes WHERE lower(city)=lower(?) AND date=? LIMIT 1", arrayOf(city, md)).use { c ->
                if (c.moveToFirst()) PrayerRow(c.string("city"), c.string("date"), c.string("fajr"), c.string("sunrise"), c.string("dhuhr"), c.string("asr"), c.string("maghrib"), c.string("isha")) else null
            }
        }
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

    suspend fun loadWordAnalysis(context: Context, surah: Int, ayah: Int): List<WordItem> = withContext(Dispatchers.IO) {
        queryDb(context, WORD_DB, emptyList()) { db ->
            db.rawQuery("SELECT position,root,irab,arabic1 FROM quran_corpus WHERE surah_id=? AND verse_id=? ORDER BY position", arrayOf(surah.toString(), ayah.toString())).use { c ->
                buildList { while (c.moveToNext()) add(WordItem(c.int("position"), c.string("root"), c.string("irab"), c.string("arabic1"))) }
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
