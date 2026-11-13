package com.mashxall.unified.data

import android.content.Context
import android.annotation.SuppressLint
import android.graphics.RectF
import android.util.Xml
import androidx.core.graphics.PathParser
import org.xmlpull.v1.XmlPullParser
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import java.util.Base64
import android.text.Html
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.File
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import org.tukaani.xz.XZInputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.time.LocalDate
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
data class NearbySalawatiCity(val city: City, val distanceKm: Double)

/** Select a named source location by exact geographic distance; this never supplies prayer times. */
internal fun nearestSalawatiCityCandidate(
    candidates: List<Pair<City, Double>>,
    maxDistanceKm: Double
): NearbySalawatiCity? {
    if (!maxDistanceKm.isFinite() || maxDistanceKm <= 0.0) return null
    return candidates.asSequence()
        .filter { (_, distance) -> distance.isFinite() && distance >= 0.0 && distance <= maxDistanceKm }
        .minByOrNull { it.second }
        ?.let { (city, distance) -> NearbySalawatiCity(city, distance) }
}
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
data class Reciter(
    val id: Int,
    val nameArabic: String,
    val nameEnglish: String,
    val audioType: Int,
    val baseUrl: String,
    val order: Int,
    /** Number of surahs confirmed by this source edition; incomplete editions must not be played past this count. */
    val surahTotal: Int = 114,
    /** Original source's recitation/riwayah label, when supplied. */
    val recitation: String = "",
    /** Stable upstream identifier used for attribution and diagnostics. */
    val sourceId: String = ""
)
data class OnlineAdhan(
    val id: String,
    val titleArabic: String,
    val titleEnglish: String,
    val category: String,
    val sourceFilename: String,
    val url: String,
    val sizeBytes: Long
)

/** Clean Sorani article HTML with the same source-marker rules used by Hadith display. */
internal fun cleanSoraniHadithSourceText(value: String): String =
    Html.fromHtml(
        stripHadithLatinSourceMarkers(value),
        Html.FROM_HTML_MODE_LEGACY
    )
        .toString()
        .replace('\u00a0', ' ')
        .replace(Regex("""\s+:"""), ":")
        .trim()

/** Remove source-database playback suffixes from the name shown to users. */
internal fun reciterDisplayName(value: String): String = value
    .replace(Regex("""\s*\((?:بێ بچڕان|gapless)\)""", RegexOption.IGNORE_CASE), "")
    .replace(Regex("""\s+"""), " ")
    .trim()
data class FarmudaItem(val id: Int, val title: String, val arabic: String, val sorani: String, val badiniTitle: String, val badini: String, val preferredTime: Int)
data class ContentCounts(val databases: Int, val audioFiles: Int, val fonts: Int, val pdfs: Int, val assetFiles: Int)

/** Stable daily rotation over the original Hadith database; returns a safe zero-based row offset. */
internal fun dailyHadithOffset(date: LocalDate, totalHadiths: Int): Int =
    if (totalHadiths <= 0) 0 else Math.floorMod(date.toEpochDay(), totalHadiths.toLong()).toInt()
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

/** Bounds extracted from one tagged word in the exact SVG page displayed by the Mushaf. */
internal data class MushafWordBounds(
    val surah: Int,
    val ayah: Int,
    val lineNumber: Int,
    val minX: Float,
    val maxX: Float,
    val minY: Float,
    val maxY: Float
)

/** Tight per-word regions for one ayah in the currently displayed SVG page. */
internal data class MushafPageVerseGeometry(
    val verse: MushafPageVerse,
    val rects: List<MushafPageRect>
)

/**
 * Keep the paths for each tagged SVG word as its own tight region within the ayah.
 * Different ayahs are never merged, even when they share a printed line.
 */
internal fun buildMushafPageVerseGeometry(words: List<MushafWordBounds>): List<MushafPageVerseGeometry> {
    val validWords = words.filter { word ->
        word.surah in 1..114 &&
            word.ayah in 1..286 &&
            word.lineNumber >= 0 &&
            word.minX.isFinite() && word.maxX.isFinite() &&
            word.minY.isFinite() && word.maxY.isFinite() &&
            word.minX >= 0f && word.minY >= 0f &&
            word.maxX <= 1148f && word.maxY <= 1641f &&
            word.maxX > word.minX && word.maxY > word.minY &&
            word.maxX - word.minX <= 1148f * 0.80f &&
            word.maxY - word.minY <= 1641f * 0.06f
    }

    // Keep each source word as its own selectable/highlightable shape. Merging
    // a whole printed line into one rectangle also paints the spaces between
    // words, which made the old highlight look larger than the actual ayah.
    return validWords.groupBy { MushafPageVerse(it.surah, it.ayah) }
        .map { (verse, verseWords) ->
            val wordRects = verseWords.map { word ->
                MushafPageRect(
                    glyphId = 0,
                    lineNumber = word.lineNumber,
                    minX = word.minX,
                    maxX = word.maxX,
                    minY = word.minY,
                    maxY = word.maxY
                )
            }.sortedWith(
                compareBy<MushafPageRect> { it.lineNumber }
                    .thenBy { it.minX }
                    .thenBy { it.minY }
            )
            MushafPageVerseGeometry(verse, wordRects)
        }
        .filter { it.rects.isNotEmpty() }
        .sortedWith(compareBy<MushafPageVerseGeometry> { it.verse.surah }.thenBy { it.verse.ayah })
}

/**
 * Build a continuous visible selection band for a verse while keeping the
 * underlying word rectangles untouched for precise hit testing. Wrapped verses
 * receive one continuous band per printed line.
 */
internal fun buildMushafVerseHighlightRects(wordRects: List<MushafPageRect>): List<MushafPageRect> {
    val valid = wordRects.filter { rect ->
        rect.lineNumber >= 0 &&
            rect.minX.isFinite() && rect.maxX.isFinite() &&
            rect.minY.isFinite() && rect.maxY.isFinite() &&
            rect.maxX > rect.minX && rect.maxY > rect.minY
    }
    return valid.groupBy { it.lineNumber }
        .toSortedMap()
        .map { (lineNumber, words) ->
            MushafPageRect(
                glyphId = 0,
                lineNumber = lineNumber,
                minX = words.minOf { it.minX },
                maxX = words.maxOf { it.maxX },
                minY = words.minOf { it.minY },
                maxY = words.maxOf { it.maxY }
            )
        }
}

/**
 * Resolve a tap against the displayed page's own vector-word bounds.
 * A small finger-sized tolerance makes the text easy to hit without selecting a
 * neighbouring verse or large blank areas around the printed text.
 */
internal fun findMushafPageVerseAtPoint(
    regions: List<MushafPageVerseGeometry>,
    x: Float,
    y: Float,
    tolerancePx: Float = 12f
): MushafPageVerse? {
    if (!x.isFinite() || !y.isFinite() || !tolerancePx.isFinite() || tolerancePx < 0f) return null

    return regions.asSequence()
        .flatMap { region -> region.rects.asSequence().map { rect -> region.verse to rect } }
        .map { (verse, rect) ->
            val dx = when {
                x < rect.minX -> rect.minX - x
                x > rect.maxX -> x - rect.maxX
                else -> 0f
            }
            val dy = when {
                y < rect.minY -> rect.minY - y
                y > rect.maxY -> y - rect.maxY
                else -> 0f
            }
            Triple(verse, rect, dx * dx + dy * dy)
        }
        .filter { (_, _, distanceSquared) -> distanceSquared <= tolerancePx * tolerancePx * 2f }
        .minByOrNull { (_, rect, distanceSquared) ->
            distanceSquared + (rect.maxX - rect.minX) * (rect.maxY - rect.minY) * 0.000001f
        }
        ?.first
}

/**
 * Read verse hit regions from the exact page SVG used for display.
 * The Noor page_data_line tables describe another page layout and are deliberately
 * not used here: their stored rectangles do not line up with this 604-page SVG edition.
 */
@SuppressLint("RestrictedApi")
internal fun parseMushafPageVerseGeometry(svgSource: String): List<MushafPageVerseGeometry> {
    if (svgSource.isBlank()) return emptyList()

    data class CapturedWord(
        val depth: Int,
        val surah: Int,
        val ayah: Int,
        val lineNumber: Int,
        var minX: Float = Float.POSITIVE_INFINITY,
        var maxX: Float = Float.NEGATIVE_INFINITY,
        var minY: Float = Float.POSITIVE_INFINITY,
        var maxY: Float = Float.NEGATIVE_INFINITY
    )

    return try {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(
            ByteArrayInputStream(svgSource.removePrefix("\uFEFF").toByteArray(Charsets.UTF_8)),
            "UTF-8"
        )

        var viewBoxLeft = 0f
        var viewBoxTop = 0f
        var viewBoxWidth = 382.68f
        var viewBoxHeight = 547.09f
        var scale = 1f
        var offsetX = 0f
        var offsetY = 0f
        var capturedWord: CapturedWord? = null
        val words = mutableListOf<MushafWordBounds>()

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "svg" -> {
                        val viewBox = parser.getAttributeValue(null, "viewBox")
                        val values = viewBox?.trim()?.split(Regex("[,\\s]+"))?.mapNotNull { it.toFloatOrNull() }
                        if (values != null && values.size == 4 &&
                            values[2].isFinite() && values[3].isFinite() &&
                            values[2] > 0f && values[3] > 0f
                        ) {
                            viewBoxLeft = values[0]
                            viewBoxTop = values[1]
                            viewBoxWidth = values[2]
                            viewBoxHeight = values[3]
                        }
                        scale = minOf(1148f / viewBoxWidth, 1641f / viewBoxHeight)
                        offsetX = (1148f - viewBoxWidth * scale) * 0.5f
                        offsetY = (1641f - viewBoxHeight * scale) * 0.5f
                    }
                    "g" -> {
                        if (capturedWord == null && parser.getAttributeValue(null, "data-type") == "text") {
                            val surah = parser.getAttributeValue(null, "data-surah")?.toIntOrNull()
                            val ayah = parser.getAttributeValue(null, "data-aya")?.toIntOrNull()
                            val line = parser.getAttributeValue(null, "data-line-number")?.toIntOrNull()
                            if (surah != null && ayah != null && line != null) {
                                capturedWord = CapturedWord(parser.depth, surah, ayah, line)
                            }
                        }
                    }
                    "path" -> {
                        val word = capturedWord
                        val pathData = parser.getAttributeValue(null, "d")
                        if (word != null && !pathData.isNullOrBlank()) {
                            try {
                                val path = PathParser.createPathFromPathData(pathData)
                                val bounds = RectF()
                                path.computeBounds(bounds, true)
                                if (!bounds.isEmpty && bounds.left.isFinite() && bounds.right.isFinite() &&
                                    bounds.top.isFinite() && bounds.bottom.isFinite()
                                ) {
                                    val left = (bounds.left - viewBoxLeft) * scale + offsetX
                                    val right = (bounds.right - viewBoxLeft) * scale + offsetX
                                    val top = (bounds.top - viewBoxTop) * scale + offsetY
                                    val bottom = (bounds.bottom - viewBoxTop) * scale + offsetY
                                    word.minX = minOf(word.minX, left)
                                    word.maxX = maxOf(word.maxX, right)
                                    word.minY = minOf(word.minY, top)
                                    word.maxY = maxOf(word.maxY, bottom)
                                }
                            } catch (pathError: Exception) {
                                Log.w("MashxallMushaf", "Could not read one SVG word path", pathError)
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    val word = capturedWord
                    if (parser.name == "g" && word != null && parser.depth == word.depth) {
                        if (word.minX.isFinite() && word.maxX.isFinite() &&
                            word.minY.isFinite() && word.maxY.isFinite()
                        ) {
                            words += MushafWordBounds(
                                surah = word.surah,
                                ayah = word.ayah,
                                lineNumber = word.lineNumber,
                                minX = word.minX.coerceIn(0f, 1148f),
                                maxX = word.maxX.coerceIn(0f, 1148f),
                                minY = word.minY.coerceIn(0f, 1641f),
                                maxY = word.maxY.coerceIn(0f, 1641f)
                            )
                        }
                        capturedWord = null
                    }
                }
            }
            event = parser.next()
        }
        buildMushafPageVerseGeometry(words)
    } catch (error: Exception) {
        Log.e("MashxallMushaf", "Could not build verse regions from the displayed SVG page", error)
        emptyList()
    }
}

private data class MushafSvgMeta(
    val page: Int, val offset: Int, val compressedLength: Int,
    val uncompressedLength: Int, val sha256: String
)

/** Remove source-only letters and repair split source markup for display; never writes to SQLite. */
internal fun stripHadithLatinSourceMarkers(value: String): String {
    val repaired = value
        // Noor occasionally splits the HTML non-breaking-space entity with marker tags.
        .replace(Regex("""&n\s*<rr>b</rr>\s*<rr>s</rr>p;""", RegexOption.IGNORE_CASE), "&nbsp;")
        .replace("\\n", "\n")
        .replace(Regex("""<rr>\s*[A-Za-z]\s*</rr>""", RegexOption.IGNORE_CASE), "")
        .replace("<rr>", "")
        .replace("</rr>", "")
    // These single Latin letters are source annotations, not part of the Arabic narration.
    return repaired.replace(
        Regex("""(?<=[\u0600-\u06FF])\s+[A-Za-z](?=\s*[\u0600-\u06FF:：،,.;!?؟»«–—-]|$)"""),
        " "
    )
}

/** Normalize the original Arabic Hadith field without mutating its source row. */
internal fun cleanHadithArabicSourceText(value: String): String =
    Html.fromHtml(stripHadithLatinSourceMarkers(value), Html.FROM_HTML_MODE_LEGACY)
        .toString()
        .replace('\u00a0', ' ')
        .replace(Regex("""[ \t]+:"""), ":")
        .trim()

/** The Noor dictionary uses the original app's AES key and AES/ECB/PKCS5Padding format. */
internal object NoorLexiconCipher {
    internal const val KEY = "X8VKYizrHWxnUkUfEMGjk214eXU2Tjaa"

    fun decrypt(value: String): String {
        if (value.isBlank()) return ""
        if (!isLikelyEncryptedLexiconPayload(value)) return value
        return try {
            val cipher = Cipher.getInstance("AES")
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(KEY.toByteArray(Charsets.UTF_8), "AES")
            )
            String(
                cipher.doFinal(Base64.getDecoder().decode(value.filterNot(Char::isWhitespace))),
                Charsets.UTF_8
            )
        } catch (_: Exception) {
            // Match the original app: never show unreadable ciphertext as dictionary text.
            ""
        }
    }
}

/** Detect the original Base64 ciphertext only; the underlying database value is left untouched. */
internal fun isLikelyEncryptedLexiconPayload(value: String): Boolean {
    val compact = value.trim()
    if (compact.length < 64 || compact.length % 4 != 0 || compact.any(Char::isWhitespace)) return false
    if (!Regex("""[A-Za-z0-9+/]+={0,2}""").matches(compact)) return false
    val padding = compact.indexOf('=')
    return padding < 0 || (padding >= compact.length - 2 && compact.substring(padding).all { it == '=' })
}

private fun readableHadithText(value: String): String = cleanHadithArabicSourceText(value)

/**
 * Fold Quran text and user input consistently: vowel marks, Quranic annotation
 * marks, tatweel, punctuation and common Arabic/Persian letter variants do not
 * prevent a verse from matching what the reader typed.
 */
internal fun normalizeArabicSearchText(value: String): String {
    val folded = buildString(value.length) {
        value.lowercase(Locale.ROOT).forEach { raw ->
            val ch = when (raw) {
                'أ', 'إ', 'آ', 'ٱ' -> 'ا'
                'ى', 'ئ', 'ی' -> 'ي'
                'ؤ' -> 'و'
                'ة' -> 'ه'
                'ک' -> 'ك'
                in '٠'..'٩' -> ('0'.code + raw.code - '٠'.code).toChar()
                in '۰'..'۹' -> ('0'.code + raw.code - '۰'.code).toChar()
                else -> raw
            }
            if (
                ch in '\u0610'..'\u061A' ||
                ch in '\u064B'..'\u065F' ||
                ch == '\u0670' ||
                ch in '\u06D6'..'\u06ED' ||
                ch in '\u08D3'..'\u08FF' ||
                ch == '\u0640' ||
                ch == 'ء'
            ) return@forEach
            if (ch.isLetterOrDigit()) append(ch) else append(' ')
        }
    }
    return folded.trim().replace(Regex("\\s+"), " ")
}

/** Optional Quranic/Arabic marks may occur between adjacent letters, but not arbitrary text. */
private const val HADITH_SEARCH_MARKS_GLOB = "[\u0610-\u061A\u064B-\u065F\u0670\u06D6-\u06ED\u08D3-\u08FF\u0640]*"

/** A search token must match as a continuous word, allowing only Arabic marks between letters. */
internal fun hadithSearchGlobPattern(query: String): String {
    val letters = normalizeArabicSearchText(query).filterNot(Char::isWhitespace)
    if (letters.isBlank()) return ""
    return buildString {
        append('*')
        letters.forEachIndexed { index, letter ->
            append(
                when (letter) {
                    'ا' -> "[اأإآٱ]"
                    'ي' -> "[يىئ]"
                    'و' -> "[وؤ]"
                    'ه' -> "[هة]"
                    'ك' -> "[كک]"
                    else -> letter.toString()
                }
            )
            if (index < letters.lastIndex) append(HADITH_SEARCH_MARKS_GLOB)
        }
        append('*')
    }
}

/** Each query word must be present, in any order, within one displayed Hadith text field. */
internal fun hadithSearchGlobPatterns(query: String): List<String> =
    normalizeArabicSearchText(query)
        .split(' ')
        .filter(String::isNotBlank)
        .distinct()
        .map(::hadithSearchGlobPattern)

private fun normalizeArabicKey(value: String): String = value.lowercase(Locale.ROOT).filterNot { ch ->
    ch in '\u064B'..'\u065F' || ch == '\u0670' || ch == '\u0640' || !ch.isLetterOrDigit()
}

/** Search every displayed field of an Adhkar item with Arabic/Sorani-friendly normalization. */
internal fun normalizeAzkarSearchText(value: String): String = buildString {
    value.lowercase(Locale.ROOT).forEach { original ->
        if (original in '\u064B'..'\u065F' || original == '\u0670' || original == '\u0640') {
            return@forEach
        }
        val ch = when (original) {
            'أ', 'إ', 'آ', 'ٱ' -> 'ا'
            'ى', 'ئ', 'ی' -> 'ي'
            'ؤ' -> 'و'
            'ک' -> 'ك'
            else -> original
        }
        if (ch.isLetterOrDigit()) {
            append(ch)
        } else if (isNotEmpty() && last() != ' ') {
            append(' ')
        }
    }
}.trim().replace(Regex("\\s+"), " ")

internal fun azkarSearchMatches(query: String, item: AzkarItem): Boolean {
    val normalizedQuery = normalizeAzkarSearchText(query)
    if (normalizedQuery.isBlank()) return true
    val searchableText = normalizeAzkarSearchText(
        listOf(
            item.titleCkb, item.titleAr, item.titleEn,
            item.arabic, item.ckb, item.english, item.referenceCkb,
            item.categoryCkb, item.categoryArabic, item.categoryEnglish
        ).joinToString(" ")
    )
    val tokens = normalizedQuery.split(' ').filter(String::isNotBlank)
    return tokens.all(searchableText::contains) ||
        searchableText.replace(" ", "").contains(normalizedQuery.replace(" ", ""))
}

internal fun quranSearchMatches(
    query: String,
    arabic: String,
    arabicWithoutHarakat: String,
    translation: String,
    surahNameArabic: String,
    surahNameEnglish: String,
    surah: Int,
    ayah: Int
): Boolean {
    val rawQuery = query.trim()
    if (rawQuery.isBlank()) return false
    val verseKey = "$surah:$ayah"
    if (rawQuery == verseKey || rawQuery == verseKey.replace(":", " ")) return true

    val normalizedQuery = normalizeArabicSearchText(rawQuery)
    if (normalizedQuery.isBlank()) return false
    val sources = listOf(arabic, arabicWithoutHarakat, translation, surahNameArabic, surahNameEnglish)
        .map(::normalizeArabicSearchText)
        .filter(String::isNotBlank)
    if (sources.isEmpty()) return false

    val compactQuery = normalizedQuery.replace(" ", "")
    if (compactQuery.length >= 2 && sources.any { it.replace(" ", "").contains(compactQuery) }) {
        return true
    }

    // A query typed with slightly different spacing or punctuation can still
    // match when all meaningful words occur in the verse or its translation.
    val terms = normalizedQuery.split(' ').filter { it.length >= 2 }
    return terms.isNotEmpty() && terms.all { term -> sources.any { it.contains(term) } }
}


private data class KurdistanCityAlias(
    val names: List<String>,
    val sourceTerms: List<String>,
    val sorani: String,
    val arabic: String,
    val english: String
)

private fun normalizeKurdistanCityText(value: String): String = value
    .lowercase(Locale.ROOT)
    .replace("ـ", "")
    .replace("\u200c", "")
    .replace("\u200e", "")
    .replace("\u200f", "")
    .replace("أ", "ا")
    .replace("إ", "ا")
    .replace("آ", "ا")
    .replace("ٱ", "ا")
    .replace("ى", "ي")
    .replace("ة", "ه")
    .replace("ک", "ك")
    .replace("ی", "ي")
    .replace("ە", "ه")
    .replace("ێ", "ي")
    .replace("ۆ", "و")
    .replace("ڕ", "ر")
    .replace("ڵ", "ل")
    .filter { it.isLetterOrDigit() }

/** Search Sorani and Arabic spellings against the original Latin Salawati keys. */
private val kurdistanCityAliases = listOf(
    KurdistanCityAlias(listOf("Erbil", "Hawler", "Hewler", "هەولێر", "هولێر", "هولير", "أربيل", "اربيل", "اربیل"), listOf("erbil", "hawler", "hewler"), "هەولێر", "أربيل", "Erbil"),
    KurdistanCityAlias(listOf("Sulaymaniyah", "Sulaimaniyah", "Sulaimani", "Slemani", "Silêmanî", "سلێمانی", "سليمانية", "السليمانية", "سليماني"), listOf("sulaymaniyah", "sulaimaniyah", "sulaimani", "slemani", "silemani"), "سلێمانی", "السليمانية", "Sulaymaniyah"),
    KurdistanCityAlias(listOf("Duhok", "Dohuk", "دهۆک", "دهوك", "دهوک"), listOf("duhok", "dohuk"), "دهۆک", "دهوك", "Duhok"),
    KurdistanCityAlias(listOf("Halabja", "Halabjah", "هەڵەبجە", "حلبجة", "حلبجه"), listOf("halabja", "halabjah"), "هەڵەبجە", "حلبجة", "Halabja"),
    KurdistanCityAlias(listOf("Zakho", "Zaxo", "زاخۆ", "زاخو"), listOf("zakho", "zaxo"), "زاخۆ", "زاخو", "Zakho"),
    KurdistanCityAlias(listOf("Ranya", "Rania", "ڕانیە", "رانية", "رانيه"), listOf("ranya", "rania"), "ڕانیە", "رانية", "Ranya"),
    KurdistanCityAlias(listOf("Chamchamal", "چەمچەماڵ", "جمجمال"), listOf("chamchamal"), "چەمچەماڵ", "جمجمال", "Chamchamal"),
    KurdistanCityAlias(listOf("Koya", "Koysinjaq", "کۆیە", "كويسنجق", "كوية"), listOf("koya", "koysinjaq"), "کۆیە", "كوية", "Koya"),
    KurdistanCityAlias(listOf("Shaqlawa", "شەقڵاوە", "شقلاوة"), listOf("shaqlawa"), "شەقڵاوە", "شقلاوة", "Shaqlawa"),
    KurdistanCityAlias(listOf("Soran", "سۆران", "سوران"), listOf("soran"), "سۆران", "سوران", "Soran"),
    KurdistanCityAlias(listOf("Akre", "Aqra", "ئاکرێ", "عقرة"), listOf("akre", "aqra"), "ئاکرێ", "عقرة", "Akre"),
    KurdistanCityAlias(listOf("Kirkuk", "کەرکووک", "كركوك"), listOf("kirkuk"), "کەرکووک", "كركوك", "Kirkuk"),
    KurdistanCityAlias(listOf("Dukan", "دوکان", "دوكان"), listOf("dukan"), "دوکان", "دوكان", "Dukan"),
    KurdistanCityAlias(listOf("Qaladiza", "Qaladze", "قلادزێ", "قلادزه", "قلعة دزة"), listOf("qaladiza", "qaladze"), "قلادزێ", "قلعة دزة", "Qaladiza"),
    KurdistanCityAlias(listOf("Penjwen", "پێنجوێن", "بنجوين"), listOf("penjwen"), "پێنجوێن", "بنجوين", "Penjwen"),
    KurdistanCityAlias(listOf("Darbandikhan", "دەربەندیخان", "دربنديخان"), listOf("darbandikhan"), "دەربەندیخان", "دربنديخان", "Darbandikhan"),
    KurdistanCityAlias(listOf("Kalar", "کەلار", "كلار"), listOf("kalar"), "کەلار", "كلار", "Kalar"),
    KurdistanCityAlias(listOf("Kifri", "کفری", "كفري"), listOf("kifri"), "کفری", "كفري", "Kifri"),
    KurdistanCityAlias(listOf("Khanaqin", "خانەقین", "خانقين"), listOf("khanaqin"), "خانەقین", "خانقين", "Khanaqin"),
    KurdistanCityAlias(listOf("Makhmur", "مەخمور", "مخمور"), listOf("makhmur"), "مەخمور", "مخمور", "Makhmur"),
    KurdistanCityAlias(listOf("Bardarash", "بەردەڕەش", "بردرش"), listOf("bardarash"), "بەردەڕەش", "بردرش", "Bardarash"),
    KurdistanCityAlias(listOf("Harir", "هەریر", "حرير"), listOf("harir"), "هەریر", "حرير", "Harir"),
    KurdistanCityAlias(listOf("Barzan", "بارزان"), listOf("barzan"), "بارزان", "بارزان", "Barzan"),
    KurdistanCityAlias(listOf("Mergasor", "میرگەسۆر", "ميركسور"), listOf("mergasor", "margasor"), "میرگەسۆر", "ميركسور", "Mergasor"),
    KurdistanCityAlias(listOf("Sidakan", "سیدەکان", "سيدكان"), listOf("sidakan"), "سیدەکان", "سيدكان", "Sidakan"),
    KurdistanCityAlias(listOf("Sharbazher", "شەربەژێر", "شهربازير"), listOf("sharbazher"), "شەربەژێر", "شهربازير", "Sharbazher")
)

private val kurdistanRegionSearchNames = setOf(
    "کوردستان", "کوردستانی عێراق", "هەرێمی کوردستان", "هەرێمی کوردستانی عێراق",
    "اقلیم کوردستان", "اقلیم کوردستان العراق", "إقليم كردستان", "اقليم كردستان",
    "إقليم كردستان العراق", "اقليم كردستان العراق", "Kurdistan", "Kurdistan Region",
    "Iraqi Kurdistan"
).map(::normalizeKurdistanCityText).toSet()

internal fun isKurdistanRegionSearchQuery(query: String): Boolean =
    normalizeKurdistanCityText(query) in kurdistanRegionSearchNames

internal fun expandKurdistanCitySearchTerms(query: String): List<String> {
    val trimmed = query.trim()
    if (trimmed.isBlank()) return emptyList()
    val needle = normalizeKurdistanCityText(trimmed)
    if (needle.length < 3) return listOf(trimmed)
    val matches = kurdistanCityAliases.filter { alias ->
        alias.names.any { name ->
            val normalized = normalizeKurdistanCityText(name)
            normalized == needle || normalized.startsWith(needle)
        }
    }
    if (matches.isEmpty()) return listOf(trimmed)
    return (listOf(trimmed) + matches.flatMap { it.sourceTerms }).distinct()
}

internal fun citySearchMatchesQuery(query: String, sourceCityName: String, countryName: String): Boolean {
    if (query.isBlank() || isKurdistanRegionSearchQuery(query)) return true
    val normalizedQuery = normalizeKurdistanCityText(query)
    val terms = expandKurdistanCitySearchTerms(query)
    return terms.any { term ->
        sourceCityName.contains(term, ignoreCase = true) ||
            countryName.contains(term, ignoreCase = true)
    } || normalizeKurdistanCityText(sourceCityName).contains(normalizedQuery) ||
        normalizeKurdistanCityText(countryName).contains(normalizedQuery)
}

internal fun localizedKurdistanCityName(sourceCityName: String, languageKey: String): String {
    val normalized = normalizeKurdistanCityText(sourceCityName)
    val alias = kurdistanCityAliases.firstOrNull { item ->
        item.names.any { normalizeKurdistanCityText(it) == normalized } ||
            item.sourceTerms.any { normalizeKurdistanCityText(it) == normalized }
    } ?: return sourceCityName
    return when (languageKey.lowercase(Locale.ROOT)) {
        "ckb" -> alias.sorani
        "ar" -> alias.arabic
        else -> alias.english
    }
}

object ContentRepository {
    const val QURAN_DB = "content/noor_tafsir_1.2.2/base/assets/quran.db"
    // Used for Noor's worship/name source data, not as a prayer-times fallback.
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
        QuranTranslation("raman", "ڕامان", "كردي (رەمان)", "Sorani — Raman"),
        QuranTranslation("asan", "ئاسان", "كردي (ئاسان)", "Sorani — Asan"),
        QuranTranslation("sanahi", "بادینی", "كردي باديني", "Badini — Sanahi"),
        QuranTranslation("sahih", "Sahih International", "Sahih International", "English — Sahih International"),
        QuranTranslation("translit", "ئایەتەکان بە کوردی", "النطق الكردي بالحروف اللاتينية", "Kurdish transliteration"),
        QuranTranslation("none", "بێ وەرگێڕان", "النص العربي فقط", "Arabic text only")
    )

    val tafsirBooks = listOf(
        TafsirBook("roshn", "ڕۆشن", "تفسير ڕۆشن", "Roshn Kurdish Tafsir", "content/noor_tafsir_1.2.2/base/assets/tafseeri_roshn.db", "ckb"),
        TafsirBook("hazhar", "هەژار", "تفسير هەژار", "Hazhar Kurdish Tafsir", "content/noor_tafsir_1.2.2/base/assets/tafseeri_hazhar.db", "ckb"),
        TafsirBook("maisar", "ئاسان", "التيسير", "Muyassar Tafsir", "content/noor_tafsir_1.2.2/base/assets/tafseer_maisar.db", "ckb"),
        TafsirBook("sahih", "Sahih International", "Sahih International", "Sahih International", "content/noor_tafsir_1.2.2/base/assets/tafseer_sahih_international.db", "en"),
        TafsirBook("runahi", "ڕووناهی بادینی", "التفسير الباديني", "Runahi Badini Tafsir", "content/noor_tafsir_1.2.2/base/assets/tafseeri_runahi_badini.db", "bad"),
        TafsirBook("zhian", "ژیان بادینی", "تفسير ژيان", "Zhian Badini Tafsir", "content/noor_tafsir_1.2.2/base/assets/tafseeri_zhian_badini.db", "bad"),
        TafsirBook("saadi", "تفسير السعدي", "تفسير السعدي", "Saadi Tafsir", "content/noor_tafsir_1.2.2/base/assets/tafseeri_saadi.db", "ar"),
        TafsirBook("mukhtasar", "مختصر", "المختصر", "Mukhtasar Tafsir", "content/noor_tafsir_1.2.2/base/assets/tafseeri_mukhtasar.db", "ar"),
        TafsirBook("rebar", "ڕێبەر", "تفسير ريبار", "Rebar Tafsir (Sorani)", "content/noor_tafsir_1.2.2/base/assets/tafseer_rebar.db", "ckb"),
        TafsirBook("khairah_ibn_kathir", "ابن كثير", "تفسير ابن كثير", "Ibn Kathir (Khairah)", "content/khairah_1.2/base/assets/tafsir/ibn-kathir.sqlite", "ar"),
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
     * Resolve the printed Mushaf page for one playback verse using the original
     * Quran database page column. Playback uses this to follow the recitation
     * across page boundaries without scanning all 604 SVG pages.
     */
    suspend fun loadMushafPageForVerse(context: Context, surah: Int, ayah: Int): Int? = withContext(Dispatchers.IO) {
        if (surah !in 1..114 || ayah !in 1..286) return@withContext null
        val page = queryDb(context, QURAN_DB, 0) { db ->
            db.rawQuery(
                "SELECT page FROM quran WHERE surah_id=? AND verse_id=? LIMIT 1",
                arrayOf(surah.toString(), ayah.toString())
            ).use { cursor ->
                if (cursor.moveToFirst()) cursor.int("page") else 0
            }
        }
        page.takeIf { it in 1..604 }
    }

    /**
     * Legacy Noor rectangles are retained for auditing only. The active Mushaf
     * reader derives hit regions from the displayed SVG via parseMushafPageVerseGeometry,
     * because these database rectangles belong to a different page layout.
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
     * Legacy database hit-testing retained for reference only. The active page
     * reader must use findMushafPageVerseAtPoint on the displayed SVG geometry.
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
                        else if (
                            quranSearchMatches(
                                query = query,
                                arabic = item.arabic,
                                arabicWithoutHarakat = item.arabicWithoutHarakat,
                                translation = item.translation,
                                surahNameArabic = item.surahNameArabic,
                                surahNameEnglish = item.surahNameEnglish,
                                surah = item.surah,
                                ayah = item.number
                            )
                        ) {
                            add(item)
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

    /**
     * Fetch the AlQuranCloud color-coded Tajweed text for a single surah.
     * The full tagged dataset is deliberately not bundled in the APK. Once a
     * surah is fetched successfully, its JSON is cached in app-private storage
     * for subsequent offline reading.
     */
    suspend fun loadTajweedAyahs(
        context: Context,
        surah: Int,
        expectedAyahCount: Int
    ): Map<Int, String> = withContext(Dispatchers.IO) {
        if (surah !in 1..114 || expectedAyahCount !in 1..286) return@withContext emptyMap()
        val cacheFile = File(
            File(context.filesDir, "tajweed-colored"),
            "surah-%03d.json".format(Locale.US, surah)
        )

        fun decode(raw: String): Map<Int, String>? = runCatching {
            val root = JSONObject(raw)
            if (root.optInt("code", 200) != 200) return@runCatching null
            val data = root.optJSONObject("data") ?: return@runCatching null
            val rows = data.optJSONArray("ayahs") ?: return@runCatching null
            val values = sortedMapOf<Int, String>()
            for (index in 0 until rows.length()) {
                val row = rows.optJSONObject(index) ?: continue
                val number = row.optInt("numberInSurah", 0)
                val taggedText = row.optString("text", "").trim()
                if (number in 1..expectedAyahCount && taggedText.isNotBlank()) {
                    values[number] = taggedText
                }
            }
            if (values.size != expectedAyahCount ||
                values.keys != (1..expectedAyahCount).toSet()
            ) null else values
        }.getOrNull()

        val cached = runCatching {
            if (cacheFile.isFile && cacheFile.length() in 32L..5L * 1024L * 1024L) {
                decode(cacheFile.readText(Charsets.UTF_8))
            } else null
        }.getOrNull()
        if (cached != null) return@withContext cached

        var connection: HttpURLConnection? = null
        try {
            connection = (URL("https://api.alquran.cloud/v1/surah/$surah/quran-tajweed")
                .openConnection() as HttpURLConnection).apply {
                connectTimeout = 12_000
                readTimeout = 25_000
                instanceFollowRedirects = true
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "Mashxall Android")
            }
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                Log.w("MashxallTajweed", "Tajweed API returned HTTP $responseCode for surah $surah")
                return@withContext cached ?: emptyMap()
            }
            if (connection.contentLengthLong > 5L * 1024L * 1024L) {
                Log.w("MashxallTajweed", "Tajweed response exceeded the 5 MiB limit for surah $surah")
                return@withContext cached ?: emptyMap()
            }
            val raw = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            if (raw.toByteArray(Charsets.UTF_8).size > 5 * 1024 * 1024) {
                Log.w("MashxallTajweed", "Tajweed response exceeded the 5 MiB limit for surah $surah")
                return@withContext cached ?: emptyMap()
            }
            val decoded = decode(raw)
            if (decoded == null) {
                Log.w("MashxallTajweed", "Tajweed response was incomplete or malformed for surah $surah")
                return@withContext cached ?: emptyMap()
            }
            runCatching {
                cacheFile.parentFile?.mkdirs()
                val temporary = File(cacheFile.parentFile, cacheFile.name + ".part")
                temporary.writeText(raw, Charsets.UTF_8)
                if (cacheFile.exists()) cacheFile.delete()
                if (!temporary.renameTo(cacheFile)) {
                    temporary.copyTo(cacheFile, overwrite = true)
                    temporary.delete()
                }
            }.onFailure { error ->
                Log.w("MashxallTajweed", "Could not cache Tajweed surah $surah", error)
            }
            decoded
        } catch (error: Exception) {
            if (error is kotlinx.coroutines.CancellationException) throw error
            Log.w("MashxallTajweed", "Could not load Tajweed surah $surah", error)
            cached ?: emptyMap()
        } finally {
            connection?.disconnect()
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

            val articleSearchColumns = listOf(
                "COALESCE(a.description,'')",
                "COALESCE(a.description_text_ar,'')",
                "COALESCE(a.description_text_en,'')",
                "COALESCE(a.text,'')",
                "COALESCE(a.text_ar,'')",
                "COALESCE(a.text_en,'')",
                ckbExpr,
                arabicExpr,
                englishExpr,
                detailExpr,
                "CAST(a.id AS TEXT)"
            )
            val articleSearchText = articleSearchColumns.joinToString(" || ' ' || ")
            val articleSearchPatterns = hadithSearchGlobPatterns(query)
            val filter = when {
                query.isBlank() -> ""
                articleSearchPatterns.isEmpty() -> "WHERE 0=1"
                else -> "WHERE " + articleSearchPatterns.joinToString(" AND ") {
                    "LOWER(($articleSearchText)) GLOB ?"
                }
            }
            val args = articleSearchPatterns.toTypedArray().takeIf { it.isNotEmpty() }
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
                        fun plainCkb(html: String): String = cleanSoraniHadithSourceText(html)
                        add(
                            HadithArticle(
                                id = c.int("id"),
                                titleCkb = plainCkb(c.string("description")),
                                titleArabic = plain(c.string("description_text_ar")),
                                titleEnglish = plain(c.string("description_text_en")),
                                bodyCkb = plainCkb(c.string("text")),
                                bodyArabic = plain(c.string("text_ar")),
                                bodyEnglish = plain(c.string("text_en")),
                                authorCkb = plainCkb(c.string("author_ckb")),
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
                """
                SELECT b.id,b.name,b.name_ar,b.name_en,
                       COUNT(DISTINCT h.id) AS actual_count,b.type,b.sort
                FROM book b
                LEFT JOIN hadiths h ON h.book_id=b.id
                GROUP BY b.id,b.name,b.name_ar,b.name_en,b.type,b.sort
                HAVING COUNT(DISTINCT h.id)>0
                ORDER BY b.sort,b.name
                """.trimIndent(),
                null
            ).use { c ->
                buildList {
                    while (c.moveToNext()) add(
                        HadithCollection(
                            id = c.int("id"),
                            name = c.string("name"),
                            nameArabic = c.string("name_ar"),
                            nameEnglish = c.string("name_en"),
                            hadithCount = c.int("actual_count"),
                            kind = c.int("type"),
                            sort = c.int("sort")
                        )
                    )
                }
            }
        }
    }

    private fun hadithSearchFilter(query: String, bookId: Int? = null, hadithIds: Set<Int>? = null): String {
        // Search the actual Hadith wording only. Matching book names, chapter titles,
        // notes or numeric IDs used to introduce unrelated narrations into the results.
        val contentColumns = listOf("h.text", "h.text_ar", "h.text_en")
        val searchPatterns = hadithSearchGlobPatterns(query)
        val conditions = buildList {
            if (bookId != null) add("h.book_id=?")
            if (hadithIds != null) {
                val safeIds = hadithIds.asSequence().filter { it > 0 }.distinct().sorted().toList()
                if (safeIds.isEmpty()) add("0=1")
                else add("h.id IN (" + safeIds.joinToString(",") + ")")
            }
            if (query.isNotBlank()) {
                if (searchPatterns.isEmpty()) {
                    add("0=1")
                } else {
                    // All words must match within the same language's Hadith text,
                    // while the user may type the words in any order.
                    val perLanguageMatches = contentColumns.map { column ->
                        val tokenConditions = searchPatterns.map {
                            "LOWER(COALESCE($column,'')) GLOB ?"
                        }
                        "(" + tokenConditions.joinToString(" AND ") + ")"
                    }
                    add("(" + perLanguageMatches.joinToString(" OR ") + ")")
                }
            }
        }
        return if (conditions.isEmpty()) "" else "WHERE " + conditions.joinToString(" AND ")
    }

    private fun hadithSearchArgs(query: String, bookId: Int? = null): Array<String> =
        buildList {
            if (bookId != null) add(bookId.toString())
            val patterns = hadithSearchGlobPatterns(query)
            if (patterns.isNotEmpty()) repeat(3) { addAll(patterns) }
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


    /**
     * Select one source-backed Hadith for the local calendar day. Only records that
     * include both Arabic and the database's original Sorani translation are shown;
     * this never fabricates or machine-translates missing text.
     */
    suspend fun loadDailyHadith(context: Context, date: LocalDate): HadithItem? = withContext(Dispatchers.IO) {
        val total = loadHadithCount(context)
        if (total <= 0) return@withContext null

        val offset = dailyHadithOffset(date, total)
        val primary = loadHadiths(context, offset = offset, limit = 120)
        fun readableCandidate(rows: List<HadithItem>): HadithItem? =
            rows.firstNotNullOfOrNull { row ->
                val arabic = cleanHadithArabicSourceText(row.arabic)
                val sorani = cleanHadithArabicSourceText(row.sorani)
                if (arabic.isBlank() || sorani.isBlank()) null
                else row.copy(arabic = arabic, sorani = sorani)
            }

        readableCandidate(primary)?.let { return@withContext it }
        if (offset == 0) return@withContext null

        val wrapped = loadHadiths(context, offset = 0, limit = minOf(250, offset))
        readableCandidate(wrapped)
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
            // Search is applied after translations from both source databases are merged.
            // Loading the full source rows here prevents translated Sorani/English text
            // from being missed just because the original database lacks that translation.
            val term = "%"
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
        val salawatiItems = loadSalawatiAzkarItems(context, "")
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
        val combined = (enrichedNoor + uniqueSalawati).distinctBy { it.id }
        if (query.isBlank()) combined else combined.filter { azkarSearchMatches(query, it) }
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
    /**
     * Search Salawati's full location catalogue. The prayer screen must use the
     * same source the user requested; Noor's unrelated city list is not merged
     * into prayer-location choices. Dependent locations are searchable too.
     */
    suspend fun searchCities(context: Context, query: String = "", limit: Int = 220): List<City> = withContext(Dispatchers.IO) {
        val requestedTerm = query.trim()
        // Searching for Kurdistan shows the curated local city list; Sorani and
        // Arabic city names expand to the original source-database spellings.
        val term = if (isKurdistanRegionSearchQuery(requestedTerm)) "" else requestedTerm
        val searchTerms = expandKurdistanCitySearchTerms(term)
        val pageLimit = limit.coerceIn(20, 300)
        val pattern = "%$term%"
        val where = if (term.isBlank()) {
            // First-open results deliberately prioritise Iraqi Kurdistan instead
            // of filling the limited picker with unrelated Iraqi cities. Typing
            // a search still searches the full Salawati location catalogue.
            """
            (
                (lower(country.name) LIKE '%iraq%' OR lower(country.name) LIKE '%العراق%')
                AND (
                    lower(l.name) LIKE '%sulaymaniyah%' OR lower(l.name) LIKE '%sulaimani%'
                    OR lower(l.name) LIKE '%silêmanî%' OR lower(l.name) LIKE '%slêmanî%'
                    OR lower(l.name) LIKE '%halabja%' OR lower(l.name) LIKE '%erbil%'
                    OR lower(l.name) LIKE '%hawler%' OR lower(l.name) LIKE '%duhok%'
                    OR lower(l.name) LIKE '%dohuk%' OR lower(l.name) LIKE '%zaxo%'
                    OR lower(l.name) LIKE '%zakho%' OR lower(l.name) LIKE '%rania%'
                    OR lower(l.name) LIKE '%ranya%' OR lower(l.name) LIKE '%chamchamal%'
                    OR lower(l.name) LIKE '%koya%' OR lower(l.name) LIKE '%koysinjaq%'
                    OR lower(l.name) LIKE '%shaqlawa%' OR lower(l.name) LIKE '%soran%'
                    OR lower(l.name) LIKE '%akre%' OR lower(l.name) LIKE '%aqra%'
                    OR lower(l.name) LIKE '%qaradagh%' OR lower(l.name) LIKE '%karadagh%'
                    OR lower(l.name) LIKE '%darbandikhan%' OR lower(l.name) LIKE '%penjwen%'
                    OR lower(l.name) LIKE '%sidakan%' OR lower(l.name) LIKE '%kalak%'
                    OR lower(l.name) LIKE '%dukan%' OR lower(l.name) LIKE '%qaladze%'
                    OR lower(l.name) LIKE '%qaladiza%' OR lower(l.name) LIKE '%sharbazher%'
                    OR lower(l.name) LIKE '%harir%' OR lower(l.name) LIKE '%barzan%'
                    OR lower(l.name) LIKE '%barderash%' OR lower(l.name) LIKE '%kirkuk%'
                )
            )
            """.trimIndent()
        } else {
            val cityClauses = searchTerms.joinToString(" OR ") { "lower(l.name) LIKE lower(?)" }
            "($cityClauses OR lower(country.name) LIKE lower(?))"
        }
        queryDb(context, SALAWATI_DB, emptyList<City>()) { db ->
            val sql = """
                SELECT l.name AS city,l.latitude AS lat,l.longitude AS lon,
                       COALESCE(country.name,'') AS country_name
                FROM location l
                LEFT JOIN country ON country._id=l.country_id
                WHERE $where
                  AND EXISTS (
                    SELECT 1 FROM prayer_time available
                    WHERE available.location_id = CASE
                        WHEN COALESCE(l.has_fixed_prayer_time,0)=1 THEN l._id
                        WHEN COALESCE(l.prayer_dependent_id,0)>0 THEN l.prayer_dependent_id
                        ELSE l._id
                    END
                  )
                GROUP BY l._id
                ORDER BY
                    CASE WHEN lower(country.name) LIKE '%iraq%' OR lower(country.name) LIKE '%العراق%' THEN 0 ELSE 1 END,
                    CASE WHEN lower(l.name)=lower(?) THEN 0 WHEN lower(l.name) LIKE lower(?) THEN 1 ELSE 2 END,
                    country_name,l.name
                LIMIT ?
            """.trimIndent()
            val args = if (term.isBlank()) {
                arrayOf("", "", pageLimit.toString())
            } else {
                (searchTerms.map { "%$it%" } + listOf(pattern, term, "$term%", pageLimit.toString())).toTypedArray()
            }
            db.rawQuery(sql, args).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(City(cursor.string("city"), cursor.double("lat"), cursor.double("lon"), cursor.string("country_name")))
                    }
                }
            }
        }
    }

    suspend fun findCityByName(context: Context, name: String, countryName: String = ""): City? = withContext(Dispatchers.IO) {
        queryDb(context, SALAWATI_DB, null as City?) { db ->
            val sql = """
                SELECT l.name AS city,l.latitude AS lat,l.longitude AS lon,
                       COALESCE(country.name,'') AS country_name
                FROM location l
                LEFT JOIN country ON country._id=l.country_id
                WHERE lower(l.name)=lower(?)
                  AND (?='' OR lower(country.name)=lower(?))
                  AND EXISTS (
                    SELECT 1 FROM prayer_time available
                    WHERE available.location_id = CASE
                        WHEN COALESCE(l.has_fixed_prayer_time,0)=1 THEN l._id
                        WHEN COALESCE(l.prayer_dependent_id,0)>0 THEN l.prayer_dependent_id
                        ELSE l._id
                    END
                  )
                ORDER BY
                    CASE WHEN lower(country.name) LIKE '%iraq%' OR lower(country.name) LIKE '%العراق%' THEN 0 ELSE 1 END,
                    l._id
                LIMIT 1
            """.trimIndent()
            db.rawQuery(sql, arrayOf(name, countryName, countryName)).use { cursor ->
                if (cursor.moveToFirst()) {
                    City(cursor.string("city"), cursor.double("lat"), cursor.double("lon"), cursor.string("country_name"))
                } else null
            }
        }
    }

    /** Return the nearest named Salawati locality for display, even if no prayer schedule exists there. */
    suspend fun findNearestSalawatiCityAtLocation(
        context: Context,
        latitude: Double,
        longitude: Double,
        maxDistanceKm: Double = 30.0
    ): NearbySalawatiCity? = withContext(Dispatchers.IO) {
        if (!latitude.isFinite() || latitude !in -90.0..90.0 ||
            !longitude.isFinite() || longitude !in -180.0..180.0 ||
            !maxDistanceKm.isFinite() || maxDistanceKm <= 0.0
        ) return@withContext null

        val latDelta = maxDistanceKm / 110.6
        val longitudeCos = kotlin.math.abs(kotlin.math.cos(Math.toRadians(latitude))).coerceAtLeast(0.15)
        val lonDelta = maxDistanceKm / (111.32 * longitudeCos)
        val minLat = (latitude - latDelta).coerceAtLeast(-90.0)
        val maxLat = (latitude + latDelta).coerceAtMost(90.0)
        val minLon = (longitude - lonDelta).coerceAtLeast(-180.0)
        val maxLon = (longitude + lonDelta).coerceAtMost(180.0)

        val candidates = queryDb(context, SALAWATI_DB, emptyList<City>()) { db ->
            db.rawQuery(
                """
                SELECT l.name AS city,
                       l.latitude AS lat,
                       l.longitude AS lon,
                       COALESCE(country.name,'') AS country_name
                FROM location l
                LEFT JOIN country ON country._id=l.country_id
                WHERE l.latitude BETWEEN ? AND ?
                  AND l.longitude BETWEEN ? AND ?
                  AND l.name IS NOT NULL AND trim(l.name) <> ''
                ORDER BY ((l.latitude-?)*(l.latitude-?)) + ((l.longitude-?)*(l.longitude-?))
                LIMIT 1000
                """.trimIndent(),
                arrayOf(
                    minLat.toString(), maxLat.toString(), minLon.toString(), maxLon.toString(),
                    latitude.toString(), latitude.toString(), longitude.toString(), longitude.toString()
                )
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(City(
                            name = cursor.string("city"),
                            latitude = cursor.double("lat"),
                            longitude = cursor.double("lon"),
                            country = cursor.string("country_name")
                        ))
                    }
                }
            }
        }

        val measured = candidates.map { city ->
            city to PrayerLocationMath.distanceKm(latitude, longitude, city.latitude, city.longitude)
        }
        nearestSalawatiCityCandidate(measured, maxDistanceKm)
    }

    /** Load the same Salawati-backed catalogue for non-search city pickers. */
    suspend fun loadCities(context: Context): List<City> = searchCities(context, "", limit = 250)


    // Timetable resolution below is Salawati-only. Noor's database remains
    // available for its non-timetable Quran/worship data elsewhere in the app.
    /**
     * Salawati locations do not all own a timetable. Fixed rows override dependency links;
     * otherwise a dependent locality must inherit the exact source row its relation names.
     * City lookup and GPS lookup must share this mapping.
     */
    internal fun prayerScheduleLocationId(locationId: Int, hasFixedPrayerTime: Boolean, dependentLocationId: Int): Int =
        when {
            hasFixedPrayerTime -> locationId
            dependentLocationId > 0 -> dependentLocationId
            else -> locationId
        }

    internal fun hasInheritedPrayerSchedule(hasFixedPrayerTime: Boolean, dependentLocationId: Int): Boolean =
        !hasFixedPrayerTime && dependentLocationId > 0

    internal fun prayerScheduleLocationSql(alias: String): String {
        require(alias == "l" || alias == "requested") { "Unsupported Salawati SQL location alias." }
        return """
            CASE
                WHEN COALESCE($alias.has_fixed_prayer_time,0)=1 THEN $alias._id
                WHEN COALESCE($alias.prayer_dependent_id,0)>0 THEN $alias.prayer_dependent_id
                ELSE $alias._id
            END
        """.trimIndent()
    }

    private val prayerClockPattern = Regex("""^(?:[01]\d|2[0-3]):[0-5]\d$""")

    internal fun hasValidPrayerTimes(row: PrayerRow?): Boolean =
        row != null && listOf(row.fajr, row.sunrise, row.dhuhr, row.asr, row.maghrib, row.isha)
            .all { prayerClockPattern.matches(it.trim()) }

    /** The prayer feature accepts only a valid timetable from Salawati. */
    internal fun salawatiOnlyPrayerTimes(row: PrayerRow?): PrayerRow? =
        row?.takeIf { it.source.startsWith("salawati", ignoreCase = true) && hasValidPrayerTimes(it) }


    /**
     * Select the closest original Salawati timetable first. When a directly
     * scheduled locality and a dependent locality have the same coordinates,
     * prefer the direct row so the dependent-city fallback cannot win by query order.
     */
    internal fun orderPrayerCandidates(candidates: List<Pair<PrayerRow, Double>>): List<Pair<PrayerRow, Double>> =
        candidates.sortedWith(
            compareBy<Pair<PrayerRow, Double>> { it.second }
                .thenBy { if (it.first.source.startsWith("salawati-dependent", ignoreCase = true)) 1 else 0 }
                .thenBy { it.first.city.lowercase(Locale.ROOT) }
        )

    suspend fun loadPrayerTimes(
        context: Context,
        city: String,
        date: Date = Date(),
        countryName: String = ""
    ): PrayerRow? = withContext(Dispatchers.IO) {
        // Use only the selected city and dependent-city rules stored by Salawati.
        salawatiOnlyPrayerTimes(
            loadSalawatiPrayerTimesForCity(context, city, countryName.trim(), date)
        )
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
                   ON pt.location_id = ${prayerScheduleLocationSql("requested")}
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
                val hasFixedSchedule = c.int("has_fixed") == 1
                val dependency = c.int("dependent_id")
                val baseCity = c.string("base_city")
                val source = if (hasInheritedPrayerSchedule(hasFixedSchedule, dependency) && baseCity.isNotBlank()) {
                    "salawati-dependent:${c.string("city")} → $baseCity, ${c.string("country")}"
                } else {
                    "salawati:${c.string("city")}, ${c.string("country")}"
                }
                salawatiOnlyPrayerTimes(
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

        // Join each nearby locality to the prayer_time row it actually uses.
        // A direct JOIN to l._id incorrectly returns a locality's own row even
        // when Salawati says it inherits another city's timetable.
        queryDb(context, SALAWATI_DB, emptyList<PrayerRow>()) { db ->
            db.rawQuery(
                """
                SELECT l.name AS city,pt.date,pt.fajr,pt.sunrise,pt.dhuhr,pt.asr,pt.maghrib,pt.isha,
                       l.latitude AS lat,l.longitude AS lon,
                       COALESCE(country.name,'') AS country_name,
                       COALESCE(l.has_fixed_prayer_time,0) AS has_fixed,
                       COALESCE(l.prayer_dependent_id,0) AS dependent_id,
                       COALESCE(base.name,'') AS base_city
                FROM location l
                JOIN prayer_time pt ON pt.location_id = ${prayerScheduleLocationSql("l")}
                LEFT JOIN location base ON base._id=l.prayer_dependent_id
                LEFT JOIN country ON country._id=l.country_id
                WHERE pt.date=?
                  AND l.latitude BETWEEN ? AND ?
                  AND l.longitude BETWEEN ? AND ?
                ORDER BY ((l.latitude-?)*(l.latitude-?)) + ((l.longitude-?)*(l.longitude-?))
                LIMIT 600
                """.trimIndent(),
                arrayOf(
                    monthDay, minLat.toString(), maxLat.toString(), minLon.toString(), maxLon.toString(),
                    latitude.toString(), latitude.toString(), longitude.toString(), longitude.toString()
                )
            ).use { c ->
                buildList {
                    while (c.moveToNext()) {
                        val city = c.string("city")
                        val countryName = c.string("country_name")
                        val hasFixed = c.int("has_fixed") == 1
                        val dependentId = c.int("dependent_id")
                        val baseCity = c.string("base_city")
                        val source = if (hasInheritedPrayerSchedule(hasFixed, dependentId)) {
                            "salawati-dependent:$city → ${baseCity.ifBlank { "source location" }}, $countryName"
                        } else {
                            "salawati:$city, $countryName"
                        }
                        add(
                            PrayerRow(
                                city = city,
                                date = c.string("date"),
                                fajr = c.string("fajr"),
                                sunrise = c.string("sunrise"),
                                dhuhr = c.string("dhuhr"),
                                asr = c.string("asr"),
                                maghrib = c.string("maghrib"),
                                isha = c.string("isha"),
                                source = source,
                                latitude = c.double("lat"),
                                longitude = c.double("lon")
                            )
                        )
                    }
                }
            }
        }
    }

    /**
     * Prefer an original source timetable very close to the GPS fix. If none exists
     * within 7 km, retry within 30 km and expose the selected source-city distance in
     * the UI. No source times are recalculated or altered.
     */
    internal fun nearestValidSalawatiCandidate(
        candidates: List<Pair<PrayerRow, Double>>,
        maxDistanceKm: Double
    ): Pair<PrayerRow, Double>? {
        if (!maxDistanceKm.isFinite() || maxDistanceKm <= 0.0) return null
        val valid = candidates.mapNotNull { (row, distance) ->
            val validRow = salawatiOnlyPrayerTimes(row) ?: return@mapNotNull null
            if (!distance.isFinite() || distance < 0.0 || distance > maxDistanceKm) return@mapNotNull null
            validRow to distance
        }
        return orderPrayerCandidates(valid).firstOrNull()
    }

    suspend fun loadPrayerTimesAtLocation(
        context: Context,
        latitude: Double,
        longitude: Double,
        date: Date = Date(),
        staticRadiusKm: Double = 7.0,
        fallbackRadiusKm: Double = 30.0
    ): PrayerRow? = withContext(Dispatchers.IO) {
        if (!latitude.isFinite() || latitude !in -90.0..90.0 ||
            !longitude.isFinite() || longitude !in -180.0..180.0 ||
            !staticRadiusKm.isFinite() || staticRadiusKm <= 0.0
        ) return@withContext null

        suspend fun findCandidates(radiusKm: Double): List<Pair<PrayerRow, Double>> =
            loadNearbyTimetableRows(context, latitude, longitude, date, radiusKm).mapNotNull { row ->
                val validRow = salawatiOnlyPrayerTimes(row) ?: return@mapNotNull null
                val rowLat = validRow.latitude ?: return@mapNotNull null
                val rowLon = validRow.longitude ?: return@mapNotNull null
                val distance = PrayerLocationMath.distanceKm(latitude, longitude, rowLat, rowLon)
                if (distance.isFinite() && distance >= 0.0 && distance <= radiusKm) validRow to distance else null
            }

        val nearest = nearestValidSalawatiCandidate(
            findCandidates(staticRadiusKm), staticRadiusKm
        ) ?: if (fallbackRadiusKm.isFinite() && fallbackRadiusKm > staticRadiusKm) {
            nearestValidSalawatiCandidate(findCandidates(fallbackRadiusKm), fallbackRadiusKm)
        } else null

        if (nearest != null) {
            nearest.first.copy(
                source = "${nearest.first.source} • ${"%.1f".format(Locale.US, nearest.second)}km",
                latitude = latitude,
                longitude = longitude
            )
        } else {
            // No valid source timetable exists within the fallback radius.
            // Do not calculate prayer times or substitute another database's schedule.
            null
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
                        add(
                            LexiconItem(
                                c.string("title"),
                                NoorLexiconCipher.decrypt(c.string("content")),
                                c.string("reference_name"),
                                c.string("root")
                            )
                        )
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
        val originalReciters = queryDb(context, QURAN_DB, emptyList()) { db ->
            db.rawQuery("SELECT id,name,name_english,audiotype,url,q_order FROM audio ORDER BY q_order,name", null).use { c ->
                buildList {
                    while (c.moveToNext()) add(
                        Reciter(
                            id = c.int("id"),
                            nameArabic = reciterDisplayName(c.string("name")),
                            nameEnglish = reciterDisplayName(c.string("name_english")),
                            audioType = c.int("audiotype"),
                            baseUrl = c.string("url"),
                            order = c.int("q_order")
                        )
                    )
                }
            }
        }
        // Add ten separately verified EveryAyah sets that provide one file per ayah.
        // The pinned catalogue audit checked all 6,236 canonical files in each folder;
        // no audio is bundled in Mashxall, and every track streams from EveryAyah.
        val everyAyahReciters = runCatching {
            val rows = context.assets.open("everyayah_verse_reciters.json")
                .bufferedReader(Charsets.UTF_8).use { JSONObject(it.readText()).getJSONArray("records") }
            buildList {
                for (index in 0 until rows.length()) {
                    val row = rows.getJSONObject(index)
                    val folder = row.optString("folder").trim().trim('/')
                    val englishName = reciterDisplayName(row.optString("name"))
                    val arabicName = reciterDisplayName(row.optString("name_ar"))
                    val sourceId = row.optString("id")
                    val audioType = row.optInt("audio_type", -1)
                    val surahTotal = row.optInt("surah_total", 0)
                    val verifiedCount = row.optInt("verified_canonical_ayahs", 0)
                    val missingCount = row.optInt("missing_canonical_ayahs", -1)
                    if (
                        folder.isBlank() || englishName.isBlank() || arabicName.isBlank() ||
                        sourceId.isBlank() || audioType != 0 || surahTotal != 114 ||
                        verifiedCount != 6236 || missingCount != 0 ||
                        !row.optString("url_template").startsWith("https://everyayah.com/data/$folder/")
                    ) continue
                    add(
                        Reciter(
                            id = 2_000_000 + index,
                            nameArabic = arabicName,
                            nameEnglish = englishName,
                            audioType = 0,
                            baseUrl = "https://everyayah.com/data/$folder/",
                            order = 2_000 + index,
                            surahTotal = surahTotal,
                            recitation = "Hafs • EveryAyah • ${row.optString("bitrate")}",
                            sourceId = "everyayah:$folder"
                        )
                    )
                }
            }
        }.onFailure { error ->
            Log.w("MashxallAudio", "Could not load the verified EveryAyah verse-reciter catalogue", error)
        }.getOrDefault(emptyList())

        // The Quran database remains the source for verse-by-verse reciters.
        // Add source-attributed MP3Quran full-surah/edition catalog entries without
        // bundling third-party audio files in the APK. Numeric IDs are offset so
        // they cannot collide with the source database's own IDs.
        val catalogReciters = runCatching {
            val rows = context.assets.open("mp3quran_reciters.json")
                .bufferedReader(Charsets.UTF_8).use { JSONObject(it.readText()).getJSONArray("records") }
            buildList {
                for (index in 0 until rows.length()) {
                    val row = rows.getJSONObject(index)
                    val sourceId = row.optString("source_id")
                    val upstreamId = row.optInt(
                        "numeric_id",
                        sourceId.substringAfterLast('-').toIntOrNull() ?: (index + 1)
                    )
                    val baseUrl = row.optString("server").trim()
                    val englishName = reciterDisplayName(row.optString("name"))
                    if (upstreamId < 1 || englishName.isBlank() || !baseUrl.startsWith("https://")) continue
                    add(
                        Reciter(
                            id = 1_000_000 + upstreamId,
                            nameArabic = reciterDisplayName(row.optString("name_ar").ifBlank { englishName }),
                            nameEnglish = englishName,
                            audioType = 1,
                            baseUrl = baseUrl.trimEnd('/') + "/",
                            order = 10_000 + index,
                            surahTotal = row.optInt("surah_total", 0).coerceIn(0, 114),
                            recitation = row.optString("recitation"),
                            sourceId = sourceId
                        )
                    )
                }
            }
        }.onFailure { error ->
            Log.w("MashxallAudio", "Could not load the attributed online reciter catalog", error)
        }.getOrDefault(emptyList())
        (originalReciters + everyAyahReciters + catalogReciters)
            .distinctBy { it.id }
            .sortedWith(compareBy<Reciter> { it.order }.thenBy { it.nameEnglish.lowercase(Locale.ROOT) })
    }

    suspend fun loadOnlineAdhan(context: Context): List<OnlineAdhan> = withContext(Dispatchers.IO) {
        runCatching {
            val rows = context.assets.open("adhan_catalog.json")
                .bufferedReader(Charsets.UTF_8).use { JSONObject(it.readText()).getJSONArray("records") }
            buildList {
                for (index in 0 until rows.length()) {
                    val row = rows.getJSONObject(index)
                    val id = row.optString("id")
                    val url = row.optString("url")
                    if (id.isBlank() || !url.startsWith("https://")) continue
                    add(
                        OnlineAdhan(
                            id = id,
                            titleArabic = row.optString("title_ar"),
                            titleEnglish = row.optString("title_en"),
                            category = row.optString("category"),
                            sourceFilename = row.optString("source_filename"),
                            url = url,
                            sizeBytes = row.optLong("size_bytes", 0L).coerceAtLeast(0L)
                        )
                    )
                }
            }
        }.onFailure { error ->
            Log.w("MashxallAudio", "Could not load the source-linked adhan catalog", error)
        }.getOrDefault(emptyList())
    }

    /**
     * Dedicated curated muezzin catalogue for the public audio library.
     * The legacy loadOnlineAdhan() remains unchanged for existing prayer alerts.
     */
    suspend fun loadOnlineMuezzin(context: Context): List<OnlineAdhan> = withContext(Dispatchers.IO) {
        runCatching {
            val rows = context.assets.open("muezzin_catalog.json")
                .bufferedReader(Charsets.UTF_8).use { JSONObject(it.readText()).getJSONArray("records") }
            buildList {
                for (index in 0 until rows.length()) {
                    val row = rows.getJSONObject(index)
                    val id = row.optString("id")
                    val url = row.optString("url")
                    if (id.isBlank() || !url.startsWith("https://") ||
                        row.optString("category") != "muezzin"
                    ) continue
                    add(
                        OnlineAdhan(
                            id = id,
                            titleArabic = row.optString("title_ar"),
                            titleEnglish = row.optString("title_en"),
                            category = "muezzin",
                            sourceFilename = row.optString("source_filename"),
                            url = url,
                            sizeBytes = row.optLong("size_bytes", 0L).coerceAtLeast(0L)
                        )
                    )
                }
            }
        }.onFailure { error ->
            Log.w("MashxallAudio", "Could not load the curated muezzin catalogue", error)
        }.getOrDefault(emptyList())
    }

    internal fun secureReciterStreamBaseUrl(value: String): String {
        val trimmed = value.trim()
        return if (trimmed.startsWith("http://", ignoreCase = true)) {
            "https://" + trimmed.substringAfter("://")
        } else {
            trimmed
        }
    }

    fun reciterSampleUrl(reciter: Reciter, surah: Int, ayah: Int): String {
        val base = secureReciterStreamBaseUrl(reciter.baseUrl).trimEnd('/') + "/"
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
