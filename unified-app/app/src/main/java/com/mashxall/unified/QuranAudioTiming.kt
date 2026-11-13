package com.mashxall.unified

import android.content.Context
import android.util.Log
import com.mashxall.unified.data.Reciter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

internal data class QuranAyahTimeRange(
    val ayah: Int,
    val startMs: Long,
    val endMs: Long
)

internal data class QuranTimingApiRow(
    val ayah: Int,
    val startMs: Long,
    val endMs: Long
)

internal data class QuranTimingRead(
    val id: Int,
    val folderUrl: String
)
internal fun normalizeQuranTimingFolder(value: String): String =
    value.trim()
        .replaceFirst(Regex("^http://", RegexOption.IGNORE_CASE), "https://")
        .trimEnd('/')
        .lowercase(Locale.ROOT)
        .replace("://www.mp3quran.net", "://mp3quran.net")

/**
 * MP3Quran has returned both a top-level JSON array and wrapped arrays
 * (for example { "value": [...] } or { "data": { "value": [...] } }).
 * Unwrap known response envelopes without treating a missing array as valid data.
 */
internal fun unwrapQuranApiArray(
    payload: String,
    wrapperKeys: List<String>
): JSONArray? {
    runCatching { JSONArray(payload) }.getOrNull()?.let { return it }
    val root = runCatching { JSONObject(payload) }.getOrNull() ?: return null

    fun findArray(value: Any?, depth: Int): JSONArray? {
        if (value is JSONArray) return value
        if (value !is JSONObject || depth <= 0) return null
        for (key in wrapperKeys) {
            val nested = value.opt(key)
            findArray(nested, depth - 1)?.let { return it }
        }
        return null
    }
    return findArray(root, 4)
}

private fun JSONObject.longValueOrNull(key: String): Long? =
    when (val value = opt(key)) {
        is Number -> value.toLong()
        is String -> value.trim().toLongOrNull()
        else -> null
    }

internal fun parseQuranTimingRows(payload: String): List<QuranTimingApiRow>? {
    val array = unwrapQuranApiArray(
        payload,
        listOf("value", "data", "ayat_timing", "timings", "result", "response", "payload")
    ) ?: return null
    return buildList {
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val ayah = item.optInt("ayah", -1)
            // The API may include ayah zero for the basmala; it is not a Quran verse.
            if (ayah <= 0) continue
            add(
                QuranTimingApiRow(
                    ayah = ayah,
                    startMs = item.longValueOrNull("start_time") ?: -1L,
                    endMs = item.longValueOrNull("end_time") ?: -1L
                )
            )
        }
    }
}

/**
 * Resolve a timing read to the exact selected full-surah audio edition.
 * A known moshaf/read ID must match exactly; only legacy readers without an
 * ID may fall back to a unique exact server-folder match.
 */
internal fun resolveQuranTimingRead(
    reads: List<QuranTimingRead>,
    readId: Int?,
    audioFolder: String
): QuranTimingRead? {
    if (readId != null) {
        // The second source_id component is MP3Quran's unique moshaf/read ID.
        // Use its official folder for both audio and timestamps; server mirrors
        // can legitimately differ from the URL stored in the bundled catalogue.
        return reads.firstOrNull { it.id == readId }
    }
    val normalizedAudioFolder = normalizeQuranTimingFolder(audioFolder)
    val folderMatches = reads.filter {
        normalizeQuranTimingFolder(it.folderUrl) == normalizedAudioFolder
    }
    return folderMatches.singleOrNull()
}

internal fun parseQuranTimingReads(payload: String): List<QuranTimingRead> {
    val array = unwrapQuranApiArray(
        payload,
        listOf("value", "reads", "data", "reciters", "result", "response", "payload")
    ) ?: return emptyList()
    return buildList {
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val id = item.optInt("id", -1)
            val folder = item.optString("folder_url")
                .ifBlank { item.optString("server") }
                .trim()
                // MP3Quran has historically returned HTTP folder URLs for some reads.
                // The matching audio host also supports HTTPS; normalize before using it.
                .replaceFirst(Regex("^http://", RegexOption.IGNORE_CASE), "https://")
            if (id > 0 && folder.startsWith("https://", ignoreCase = true)) {
                add(QuranTimingRead(id, folder))
            }
        }
    }.distinctBy { it.id }
}

internal data class QuranSurahAudioTiming(
    val readId: Int,
    val audioUrl: String,
    val ayahs: List<QuranAyahTimeRange>
) {
    val durationMs: Long
        get() = ayahs.lastOrNull()?.endMs ?: 0L

    fun startMsForAyah(ayah: Int): Long {
        if (ayahs.isEmpty()) return 0L
        return ayahs.getOrNull(ayah.coerceIn(1, ayahs.size) - 1)?.startMs ?: 0L
    }
}

/**
 * Accept only a complete, ordered API timeline for the exact ayah count.
 * The MP3 itself remains a single continuous surah recording.
 */
internal fun validateQuranAyahTimings(
    rows: List<QuranTimingApiRow>,
    expectedAyahCount: Int
): List<QuranAyahTimeRange>? {
    if (expectedAyahCount < 1 || rows.size != expectedAyahCount) return null
    val result = ArrayList<QuranAyahTimeRange>(expectedAyahCount)
    var previousStart = -1L
    rows.forEachIndexed { index, row ->
        // The official MP3Quran timing feed may have tiny overlapping
        // transition windows. It only guarantees monotonically increasing
        // ayah starts; requiring end(i) <= start(i+1) rejects valid reads.
        if (row.ayah != index + 1 ||
            row.startMs < 0L ||
            row.endMs <= row.startMs ||
            row.startMs < previousStart
        ) return null
        result += QuranAyahTimeRange(row.ayah, row.startMs, row.endMs)
        previousStart = row.startMs
    }
    return result
}

/** Resolve the ayah containing the real Media3 playback position (milliseconds). */
internal fun quranAyahAtPositionMs(
    ayahs: List<QuranAyahTimeRange>,
    positionMs: Long
): Int? {
    if (ayahs.isEmpty()) return null
    var low = 0
    var high = ayahs.lastIndex
    var answer = 0
    while (low <= high) {
        val middle = (low + high) ushr 1
        if (ayahs[middle].startMs <= positionMs) {
            answer = middle
            low = middle + 1
        } else {
            high = middle - 1
        }
    }
    return ayahs[answer].ayah
}

/** Prefer provider-verified duration over Media3's bitrate-derived estimate. */
internal fun quranPlaybackDurationMs(
    playerDurationMs: Long,
    verifiedSurahDurationMs: Long?
): Long = verifiedSurahDurationMs?.takeIf { it > 0L }
    ?: playerDurationMs.takeIf { it > 0L }
    ?: 0L

/** True only when Media3 reports a position close to the requested seek destination. */
internal fun quranSeekPositionReached(
    targetMs: Long,
    actualMs: Long,
    toleranceMs: Long = 150L
): Boolean {
    if (targetMs < 0L || actualMs < 0L) return false
    return kotlin.math.abs(targetMs - actualMs) <= toleranceMs.coerceAtLeast(0L)
}

/**
 * Fetches genuine verse boundaries from MP3Quran's ayat_timing API.
 * The timing read ID is matched to the chosen MP3Quran edition; the returned
 * folder URL is used for the MP3 as well, keeping the audio and timeline paired.
 * Timing responses are cached in the app cache directory, not in Quran content DBs.
 */
internal object QuranAudioTimingRepository {
    private const val API_BASE = "https://www.mp3quran.net/api/v3"
    private const val READS_TTL_MS = 24L * 60L * 60L * 1000L
    private const val TIMING_TTL_MS = 30L * 24L * 60L * 60L * 1000L

    suspend fun load(
        context: Context,
        reciter: Reciter,
        surah: Int,
        expectedAyahCount: Int
    ): QuranSurahAudioTiming? = withContext(Dispatchers.IO) {
        if (reciter.audioType == 0 ||
            surah !in 1..114 ||
            surah > reciter.surahTotal ||
            expectedAyahCount < 1
        ) return@withContext null

        try {
            val readId = reciter.sourceId.substringAfterLast('-')
                .toIntOrNull()
                ?: reciter.id.takeIf { it >= 1_000_000 }?.minus(1_000_000)
            val readsPayload = cachedApiText(
                context = context,
                fileName = "mp3quran_timing_reads.json",
                url = "$API_BASE/ayat_timing/reads",
                ttlMs = READS_TTL_MS,
                isValid = { parseQuranTimingReads(it).isNotEmpty() }
            ) ?: return@withContext null
            val reads = parseQuranTimingReads(readsPayload)
            val normalizedBase = normalizeFolder(reciter.baseUrl)
            // The second component of our MP3Quran source_id is the concrete
            // moshaf/read id. Match it first and require the folder to agree;
            // a reciter may have several editions sharing a server hostname.
            val read = resolveQuranTimingRead(reads, readId, normalizedBase) ?: run {
                    Log.w(
                        "MashxallQuranTiming",
                        "No timing read matches reciter ${reciter.id} folder '${reciter.baseUrl}' (sourceId=${reciter.sourceId}, reads=${reads.size})"
                    )
                    return@withContext null
                }

            val timingPayload = cachedApiText(
                context = context,
                fileName = "mp3quran_timing_${read.id}_${surah}.json",
                url = "$API_BASE/ayat_timing?surah=$surah&read=${read.id}",
                ttlMs = TIMING_TTL_MS,
                isValid = { payload ->
                    val parsed = parseQuranTimingRows(payload)
                    parsed != null && validateQuranAyahTimings(parsed, expectedAyahCount) != null
                }
            ) ?: return@withContext null
            val rows = parseQuranTimingRows(timingPayload) ?: run {
                Log.w(
                    "MashxallQuranTiming",
                    "Timing response did not contain a recognized ayah array (read=${read.id}, surah=$surah)"
                )
                return@withContext null
            }
            val ayahs = validateQuranAyahTimings(rows, expectedAyahCount) ?: run {
                Log.w(
                    "MashxallQuranTiming",
                    "Timing rows were incomplete or invalid (read=${read.id}, surah=$surah, rows=${rows.size}, expected=$expectedAyahCount)"
                )
                return@withContext null
            }
            val folder = read.folderUrl.trim().trimEnd('/') + "/"
            if (!folder.startsWith("https://", ignoreCase = true)) return@withContext null
            val audioUrl = folder + String.format(Locale.US, "%03d.mp3", surah)
            QuranSurahAudioTiming(read.id, audioUrl, ayahs)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.w("MashxallQuranTiming", "Could not load exact verse timing for reciter ${reciter.id}, surah $surah", error)
            null
        }
    }

    private fun cachedApiText(
        context: Context,
        fileName: String,
        url: String,
        ttlMs: Long,
        isValid: (String) -> Boolean
    ): String? {
        val directory = File(context.cacheDir, "quran-audio-timing")
        val cacheFile = File(directory, fileName)
        val cached = runCatching {
            cacheFile.takeIf { it.isFile && it.length() in 1..2_000_000L }?.readText(Charsets.UTF_8)
        }.getOrNull()
        if (!cached.isNullOrBlank() && isValid(cached) &&
            System.currentTimeMillis() - cacheFile.lastModified() < ttlMs
        ) {
            return cached
        }

        // Earlier builds cached any non-empty HTTP body, including an error or
        // an unsupported JSON envelope. Discard invalid entries and fetch again.
        if (!cached.isNullOrBlank() && !runCatching { isValid(cached) }.getOrDefault(false)) {
            runCatching { cacheFile.delete() }
        }
        val fresh = runCatching { fetchText(url) }.getOrNull()
        if (!fresh.isNullOrBlank() && runCatching { isValid(fresh) }.getOrDefault(false)) {
            runCatching {
                directory.mkdirs()
                val temporary = File(directory, fileName + ".tmp")
                temporary.writeText(fresh, Charsets.UTF_8)
                if (cacheFile.exists()) cacheFile.delete()
                if (!temporary.renameTo(cacheFile)) {
                    temporary.copyTo(cacheFile, overwrite = true)
                    temporary.delete()
                }
            }.onFailure {
                Log.w("MashxallQuranTiming", "Could not cache verified Quran timing response", it)
            }
            return fresh
        }
        if (!fresh.isNullOrBlank()) {
            Log.w("MashxallQuranTiming", "Rejected invalid timing API response from $url")
        }
        // Only return the old body if it still validates. Never reuse malformed cached data.
        return cached?.takeIf { runCatching { isValid(it) }.getOrDefault(false) }
    }

    private fun fetchText(url: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12_000
            readTimeout = 20_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Mobile Safari/537.36"
            )
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) throw java.io.IOException("MP3Quran timing API returned HTTP $code")
            if (connection.contentLengthLong > 2_000_000L) {
                throw java.io.IOException("MP3Quran timing response exceeded the cache limit")
            }
            return connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun normalizeFolder(value: String): String =
        normalizeQuranTimingFolder(value)
}
