package com.mashxall.unified

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.StatFs
import com.mashxall.unified.data.MushafPageVerseGeometry
import com.mashxall.unified.data.MushafPageVerse
import com.mashxall.unified.data.MushafWordBounds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.Locale
import java.util.zip.ZipFile

/**
 * The alternate Tajweed Mushaf is an optional, user-selected edition.
 * Its 604 full-resolution pages are deliberately NOT packaged in the APK.
 * The original graphic Mushaf and every existing Quran database remain untouched.
 */
internal object TajweedMushafRepository {
    private const val DROPBOX_DOWNLOAD_URL = "https://www.dropbox.com/scl/fi/km281o5a052wrmfb24y5n/Mashxall-Tajweed-Source-144MB.zip?rlkey=q8uyz0ifi12vo3wrfivmqz8kx&dl=1"
    private const val EXPECTED_ARCHIVE_BYTES = 144220538L
    private const val EXPECTED_ARCHIVE_SHA256 =
        "ba814deb2db28951feab1f6e1022153d948ae905131c373331d8edd653acbdc0"
    private const val EXPECTED_GLYPH_ROWS = 83881
    private const val EXPECTED_IMAGE_WIDTH = 1340
    private const val EXPECTED_IMAGE_HEIGHT = 1890
    private const val PAGE_COUNT = 604
    private const val TARGET_WIDTH = 1148
    private const val TARGET_HEIGHT = 1641
    internal const val PAGE_ARTWORK_SCALE = 0.70f
    private const val PACK_DIRECTORY = "tajweed-mushaf"
    private const val STAGING_DIRECTORY = "tajweed-mushaf.installing"
    private const val DATABASE_NAME = "ayahinfo_1280.db"
    private const val COMPLETE_MARKER = ".verified-pack"

    private val pageEntryPattern = Regex("""^tajweed/width_1280/page([0-9]{3})\.png$""")

    private class NonRetryableTajweedDownloadException(message: String) : IOException(message)

    fun isInstalled(context: Context): Boolean {
        val root = File(context.filesDir, PACK_DIRECTORY)
        val marker = File(root, COMPLETE_MARKER)
        val database = File(root, DATABASE_NAME)
        val pages = File(root, "width_1280")
        return marker.isFile &&
            marker.readText().trim() == EXPECTED_ARCHIVE_SHA256 &&
            database.isFile &&
            pages.isDirectory &&
            pages.listFiles()?.count { it.isFile && it.name.matches(Regex("""page[0-9]{3}\.png""")) } == PAGE_COUNT &&
            File(pages, "page001.png").isFile &&
            File(pages, "page604.png").isFile
    }

    fun pageFile(context: Context, page: Int): File? {
        if (page !in 1..PAGE_COUNT) return null
        val file = File(File(context.filesDir, PACK_DIRECTORY), "width_1280/page%03d.png".format(Locale.US, page))
        return file.takeIf { it.isFile && it.length() > 0L }
    }

    /** Download and validate only the pages plus the glyph map; leave the app's Quran data unchanged. */
    suspend fun install(
        context: Context,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit = { _, _ -> }
    ) = withContext(Dispatchers.IO) {
        if (isInstalled(context)) return@withContext

        val requiredBytes = EXPECTED_ARCHIVE_BYTES + 144157882L + 24L * 1024L * 1024L
        val availableBytes = StatFs(context.filesDir.absolutePath).availableBytes
        if (availableBytes < requiredBytes) {
            throw IOException("Not enough free storage. About 300 MB is needed temporarily to verify and install this optional Mushaf.")
        }

        val archive = File(context.cacheDir, "tajweed-mushaf-source.zip")
        val staging = File(context.filesDir, STAGING_DIRECTORY)
        val destination = File(context.filesDir, PACK_DIRECTORY)
        val backup = File(context.filesDir, "$PACK_DIRECTORY.previous")
        archive.delete()
        staging.deleteRecursively()
        backup.deleteRecursively()
        staging.mkdirs()

        try {
            downloadArchive(archive, onProgress)
            val actualSize = archive.length()
            check(actualSize == EXPECTED_ARCHIVE_BYTES) {
                "The Tajweed Mushaf download was incomplete ($actualSize bytes)."
            }
            check(sha256(archive) == EXPECTED_ARCHIVE_SHA256) {
                "The downloaded Tajweed Mushaf does not match the verified source file."
            }

            val imageDirectory = File(staging, "width_1280")
            check(imageDirectory.mkdirs() || imageDirectory.isDirectory)
            val databaseTarget = File(staging, DATABASE_NAME)
            val receivedPages = BooleanArray(PAGE_COUNT + 1)
            var pageCount = 0
            var databaseCopied = false

            ZipFile(archive).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (entry.isDirectory) continue
                    val pageMatch = pageEntryPattern.matchEntire(entry.name)
                    when {
                        pageMatch != null -> {
                            val page = pageMatch.groupValues[1].toIntOrNull()
                                ?: throw IOException("Invalid page number in the Tajweed Mushaf archive.")
                            if (page !in 1..PAGE_COUNT || receivedPages[page]) {
                                throw IOException("Duplicate or invalid Tajweed Mushaf page $page.")
                            }
                            val target = File(imageDirectory, "page%03d.png".format(Locale.US, page))
                            zip.getInputStream(entry).use { input ->
                                FileOutputStream(target).use { output -> input.copyTo(output, 128 * 1024) }
                            }
                            if (target.length() <= 8L) throw IOException("Tajweed Mushaf page $page is empty.")
                            receivedPages[page] = true
                            pageCount++
                        }
                        entry.name == "tajweed/databases/ayahinfo_1280.db" -> {
                            if (databaseCopied) throw IOException("Duplicate Tajweed glyph database.")
                            zip.getInputStream(entry).use { input ->
                                FileOutputStream(databaseTarget).use { output -> input.copyTo(output, 128 * 1024) }
                            }
                            databaseCopied = true
                        }
                    }
                }
            }

            check(pageCount == PAGE_COUNT && (1..PAGE_COUNT).all { receivedPages[it] }) {
                "The alternate Mushaf must contain all 604 pages; received $pageCount."
            }
            check(databaseCopied && databaseTarget.isFile && databaseTarget.length() == 5611520L) {
                "The Tajweed page-position database is missing or incomplete."
            }

            for (page in 1..PAGE_COUNT) {
                val image = File(imageDirectory, "page%03d.png".format(Locale.US, page))
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(image.absolutePath, options)
                check(options.outWidth == EXPECTED_IMAGE_WIDTH && options.outHeight == EXPECTED_IMAGE_HEIGHT) {
                    "Tajweed Mushaf page $page has unexpected dimensions ${options.outWidth}x${options.outHeight}."
                }
            }

            verifyGlyphDatabase(databaseTarget)
            File(staging, COMPLETE_MARKER).writeText(EXPECTED_ARCHIVE_SHA256)

            if (destination.exists() && !destination.renameTo(backup)) {
                throw IOException("Could not preserve the previous optional Mushaf files.")
            }
            if (!staging.renameTo(destination)) {
                if (backup.exists()) backup.renameTo(destination)
                throw IOException("Could not activate the verified Tajweed Mushaf files.")
            }
            backup.deleteRecursively()
        } catch (error: Exception) {
            staging.deleteRecursively()
            throw error
        } finally {
            archive.delete()
        }
        Unit
    }

    private fun downloadArchive(
        target: File,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit
    ) {
        val shareUrl = DROPBOX_DOWNLOAD_URL
        var requestUrl = shareUrl
        var lastError: Exception? = null
        var attempts = 0
        var binaryDownloadUrl: String? = null
        var restartedAfter416 = false

        while (attempts < 7) {
            var connection: HttpURLConnection? = null
            try {
                val existing = target.takeIf { it.isFile }?.length() ?: 0L
                val rangeStart = if (shouldUseTajweedRange(requestUrl, binaryDownloadUrl, existing)) {
                    existing
                } else {
                    0L
                }
                connection = openDropboxConnection(requestUrl, rangeStart, binaryDownloadUrl)
                val response = connection.responseCode

                if (response == 416) {
                    if (existing == EXPECTED_ARCHIVE_BYTES) return
                    if (shouldRestartTajweedDownloadAfter416(existing, restartedAfter416)) {
                        // Dropbox or its CDN rejected the stale byte range. Restart the public-link
                        // redirect handshake from byte zero, and never repeat a second 416.
                        target.delete()
                        restartedAfter416 = true
                        binaryDownloadUrl = null
                        requestUrl = shareUrl
                        continue
                    }
                    throw NonRetryableTajweedDownloadException(
                        "Dropbox rejected a clean download request with HTTP 416. " +
                            "The shared file link is not accepting the download request."
                    )
                }

                val contentType = connection.contentType.orEmpty().lowercase(Locale.ROOT)
                if (response !in 200..299 || contentType.contains("text/html")) {
                    val html = readDropboxResponse(connection)
                    if (response == 401 || response == 403 || response == 404 || response == 410 ||
                        contentType.contains("text/html")
                    ) {
                        // A shared-link or CDN URL may have expired. Refresh the public link rather
                        // than repeatedly requesting an expired CDN address with a partial Range.
                        target.delete()
                        binaryDownloadUrl = null
                        requestUrl = shareUrl
                    }
                    throw IOException(dropboxFailureReason(html, response))
                }

                val append = response == HttpURLConnection.HTTP_PARTIAL && rangeStart > 0L
                if (response != HttpURLConnection.HTTP_OK && response != HttpURLConnection.HTTP_PARTIAL) {
                    throw IOException("Dropbox returned HTTP " + response + ".")
                }

                // Remember only the exact final URL that served the binary ZIP. Resumed requests
                // may send Range to this URL, never to the public share page or its redirects.
                binaryDownloadUrl = connection.url.toString()
                requestUrl = binaryDownloadUrl.orEmpty()
                if (append) {
                    val startByte = Regex("""^bytes\s+(\d+)-""", RegexOption.IGNORE_CASE)
                        .find(connection.getHeaderField("Content-Range").orEmpty())
                        ?.groupValues?.getOrNull(1)?.toLongOrNull()
                    if (startByte != existing) {
                        target.delete()
                        throw IOException("Dropbox resumed from an unexpected byte position. Restarting safely.")
                    }
                } else if (existing > 0L) {
                    // The CDN ignored Range or changed its download URL: replace the partial copy.
                    target.delete()
                }

                val alreadyWritten = if (append) existing else 0L
                connection.inputStream.use { input ->
                    FileOutputStream(target, append).use { output ->
                        if (alreadyWritten == 0L) {
                            val header = ByteArray(4)
                            var read = 0
                            while (read < header.size) {
                                val count = input.read(header, read, header.size - read)
                                if (count < 0) throw IOException("Dropbox ended before sending a ZIP header.")
                                read += count
                            }
                            if (header[0] != 0x50.toByte() || header[1] != 0x4b.toByte()) {
                                target.delete()
                                throw IOException("Dropbox did not return the Tajweed Mushaf ZIP archive.")
                            }
                            output.write(header)
                        }

                        var downloaded = alreadyWritten + if (alreadyWritten == 0L) 4L else 0L
                        onProgress(downloaded.coerceAtMost(EXPECTED_ARCHIVE_BYTES), EXPECTED_ARCHIVE_BYTES)
                        val buffer = ByteArray(128 * 1024)
                        var lastUpdate = System.nanoTime()
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            output.write(buffer, 0, count)
                            downloaded += count
                            val now = System.nanoTime()
                            if (now - lastUpdate >= 250_000_000L) {
                                onProgress(downloaded.coerceAtMost(EXPECTED_ARCHIVE_BYTES), EXPECTED_ARCHIVE_BYTES)
                                lastUpdate = now
                            }
                        }
                        output.flush()
                    }
                }

                val size = target.length()
                onProgress(size.coerceAtMost(EXPECTED_ARCHIVE_BYTES), EXPECTED_ARCHIVE_BYTES)
                if (size == EXPECTED_ARCHIVE_BYTES) return
                if (size > EXPECTED_ARCHIVE_BYTES) {
                    target.delete()
                    throw IOException("Dropbox sent more data than the verified Tajweed Mushaf archive.")
                }
                throw IOException(
                    "The connection stopped at " + size + " of " + EXPECTED_ARCHIVE_BYTES +
                        " bytes. The Dropbox download will resume from this point."
                )
            } catch (error: Exception) {
                if (error is NonRetryableTajweedDownloadException) throw error
                lastError = error
                attempts++
                if (target.length() > EXPECTED_ARCHIVE_BYTES) target.delete()
                if (target.length() == 0L) {
                    requestUrl = shareUrl
                    binaryDownloadUrl = null
                }
                if (attempts >= 7) break
                try {
                    Thread.sleep((attempts * 900L).coerceAtMost(4_000L))
                } catch (interrupted: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw IOException("The Tajweed Mushaf download was cancelled.", interrupted)
                }
            } finally {
                connection?.disconnect()
            }
        }

        throw IOException(
            lastError?.message ?: "Could not download the Tajweed Mushaf from Dropbox.",
            lastError
        )
    }

    private fun openDropboxConnection(
        startUrl: String,
        rangeStart: Long,
        rangeUrl: String?
    ): HttpURLConnection {
        var url = URL(startUrl)
        repeat(10) {
            if (!url.protocol.equals("https", ignoreCase = true) || !isTrustedDropboxHost(url.host)) {
                throw IOException("Dropbox redirected the download to an untrusted host.")
            }

            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 30_000
                readTimeout = 90_000
                instanceFollowRedirects = false
                requestMethod = "GET"
                setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 11) AppleWebKit/537.36 Chrome/120.0 Mobile Safari/537.36"
                )
                setRequestProperty("Accept", "application/zip,application/octet-stream,*/*;q=0.8")
                setRequestProperty("Accept-Encoding", "identity")
                if (rangeStart > 0L && rangeUrl != null && url.toString() == rangeUrl) {
                    setRequestProperty("Range", "bytes=" + rangeStart + "-")
                }
            }

            val code = connection.responseCode
            if (code == 301 || code == 302 || code == 303 || code == 307 || code == 308) {
                val location = connection.getHeaderField("Location")
                    ?: throw IOException("Dropbox redirected without a download location.")
                val next = URL(url, location)
                if (!next.protocol.equals("https", ignoreCase = true) || !isTrustedDropboxHost(next.host)) {
                    connection.disconnect()
                    throw IOException("Dropbox redirected the download to an untrusted host.")
                }
                connection.disconnect()
                url = next
            } else {
                return connection
            }
        }
        throw IOException("Dropbox returned too many download redirects.")
    }

    private fun readDropboxResponse(connection: HttpURLConnection): String {
        val stream = runCatching { connection.inputStream }.getOrNull()
            ?: connection.errorStream
            ?: return ""
        return stream.use { input ->
            val bytes = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8 * 1024)
            var total = 0
            while (total < 64 * 1024) {
                val count = input.read(buffer, 0, minOf(buffer.size, 64 * 1024 - total))
                if (count < 0) break
                bytes.write(buffer, 0, count)
                total += count
            }
            bytes.toString(Charsets.UTF_8.name())
        }
    }

    private fun dropboxFailureReason(html: String, response: Int): String {
        val plain = html
            .replace(Regex("""(?is)<(script|style)\b.*?</\1>"""), " ")
            .replace(Regex("""(?is)<[^>]+>"""), " ")
            .replace("&nbsp;", " ", ignoreCase = true)
            .replace(Regex("""\s+"""), " ")
            .trim()
            .take(260)
        return when {
            response == 429 || plain.contains("too many requests", ignoreCase = true) ->
                "Dropbox temporarily limited downloads for this file. Please try again later."
            response == 401 || response == 403 || plain.contains("sign in", ignoreCase = true) ||
                plain.contains("access denied", ignoreCase = true) ->
                "Dropbox denied access. The shared link must allow anyone with the link to download the file."
            response == 404 || response == 410 ->
                "The public Dropbox link is missing, expired or has been revoked."
            plain.isNotBlank() ->
                "Dropbox returned a page instead of the Tajweed Mushaf ZIP: " + plain
            else -> "Dropbox returned HTTP " + response + " without the Tajweed Mushaf ZIP."
        }
    }

    private fun verifyGlyphDatabase(file: File) {
        val database = SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        database.use { db ->
            db.rawQuery("PRAGMA integrity_check", null).use { cursor ->
                if (!cursor.moveToFirst() || cursor.getString(0) != "ok") {
                    throw IOException("The Tajweed page-position database failed its integrity check.")
                }
            }
            db.rawQuery(
                "SELECT COUNT(*), MIN(page_number), MAX(page_number), COUNT(DISTINCT page_number) FROM glyphs",
                null
            ).use { cursor ->
                if (!cursor.moveToFirst() ||
                    cursor.getInt(0) != EXPECTED_GLYPH_ROWS ||
                    cursor.getInt(1) != 1 ||
                    cursor.getInt(2) != PAGE_COUNT ||
                    cursor.getInt(3) != PAGE_COUNT
                ) {
                    throw IOException("The Tajweed page-position database does not cover all 604 pages.")
                }
            }
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(1024 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }

    suspend fun loadRenderedPage(context: Context, page: Int): TajweedMushafRenderedPage? =
        withContext(Dispatchers.IO) {
            val file = pageFile(context, page) ?: return@withContext null
            val source = BitmapFactory.decodeFile(file.absolutePath) ?: return@withContext null
            val bitmap = try {
                renderIntoPageViewport(source, TARGET_WIDTH, TARGET_HEIGHT, page)
            } finally {
                source.recycle()
            }
            TajweedMushafRenderedPage(bitmap, loadPageGeometry(context, page))
        }

    suspend fun renderPageBitmap(context: Context, page: Int, width: Int, height: Int): Bitmap? =
        withContext(Dispatchers.IO) {
            val file = pageFile(context, page) ?: return@withContext null
            val source = BitmapFactory.decodeFile(file.absolutePath) ?: return@withContext null
            try {
                renderIntoPageViewport(source, width, height, page)
            } finally {
                source.recycle()
            }
        }

    suspend fun loadPageForVerse(context: Context, surah: Int, ayah: Int): Int? =
        withContext(Dispatchers.IO) {
            if (surah !in 1..114 || ayah !in 1..286 || !isInstalled(context)) return@withContext null
            val file = File(context.filesDir, "$PACK_DIRECTORY/$DATABASE_NAME")
            val database = SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            database.use { db ->
                db.rawQuery(
                    "SELECT MIN(page_number) FROM glyphs WHERE sura_number=? AND ayah_number=?",
                    arrayOf(surah.toString(), ayah.toString())
                ).use { cursor ->
                    if (cursor.moveToFirst()) cursor.getInt(0).takeIf { it in 1..PAGE_COUNT } else null
                }
            }
        }

    private fun loadPageGeometry(context: Context, page: Int): List<MushafPageVerseGeometry> {
        if (page !in 1..PAGE_COUNT || !isInstalled(context)) return emptyList()
        val databaseFile = File(context.filesDir, "$PACK_DIRECTORY/$DATABASE_NAME")
        val database = SQLiteDatabase.openDatabase(databaseFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        val scale = minOf(TARGET_WIDTH.toFloat() / EXPECTED_IMAGE_WIDTH, TARGET_HEIGHT.toFloat() / EXPECTED_IMAGE_HEIGHT) * PAGE_ARTWORK_SCALE
        val offsetX = (TARGET_WIDTH - EXPECTED_IMAGE_WIDTH * scale) * 0.5f
        val offsetY = (TARGET_HEIGHT - EXPECTED_IMAGE_HEIGHT * scale) * 0.5f
        val words = mutableListOf<MushafWordBounds>()
        database.use { db ->
            db.rawQuery(
                """
                SELECT sura_number, ayah_number, line_number, min_x, max_x, min_y, max_y
                FROM glyphs
                WHERE page_number=?
                ORDER BY line_number, position
                """.trimIndent(),
                arrayOf(page.toString())
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    words += tajweedGlyphBoundsToPageSpace(
                        surah = cursor.getInt(0),
                        ayah = cursor.getInt(1),
                        lineNumber = cursor.getInt(2),
                        minX = cursor.getFloat(3),
                        maxX = cursor.getFloat(4),
                        minY = cursor.getFloat(5),
                        maxY = cursor.getFloat(6),
                        scale = scale,
                        offsetX = offsetX,
                        offsetY = offsetY
                    )
                }
            }
        }
        return buildTajweedPageVerseGeometry(words)
    }

    private fun renderIntoPageViewport(source: Bitmap, width: Int, height: Int, page: Int): Bitmap {
        val target = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val scale = minOf(width.toFloat() / source.width, height.toFloat() / source.height) * PAGE_ARTWORK_SCALE
        val targetWidth = source.width * scale
        val targetHeight = source.height * scale
        val destination = RectF(
            (width - targetWidth) * 0.5f,
            (height - targetHeight) * 0.5f,
            (width + targetWidth) * 0.5f,
            (height + targetHeight) * 0.5f
        )
        Canvas(target).apply {
            drawColor(Color.WHITE)
            drawBitmap(source, null, destination, Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG))

            // The Tajweed PNG artwork has no printed folio number. Add its page
            // number to the rendered bitmap (not to the source ZIP or databases)
            // so it sits on the paper and follows zoom/curl just like the original.
            val numberPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
                color = Color.BLACK
                textAlign = Paint.Align.CENTER
                textSize = 30f * (height.toFloat() / TARGET_HEIGHT).coerceAtLeast(0.5f)
                typeface = Typeface.create("serif", Typeface.NORMAL)
            }
            drawText(
                tajweedPageNumberLabel(page),
                width * 0.5f,
                height * 0.935f,
                numberPaint
            )
            val juzPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
                color = Color.BLACK
                textAlign = Paint.Align.RIGHT
                textSize = 23f * (height.toFloat() / TARGET_HEIGHT).coerceAtLeast(0.5f)
                typeface = Typeface.create("serif", Typeface.BOLD)
            }
            drawText(
                "الجزء ${tajweedPageNumberLabel(mushafJuzForPage(page))}",
                width * 0.84f,
                height * 0.935f,
                juzPaint
            )
        }
        return target
    }
}

internal fun tajweedPageNumberLabel(page: Int): String {
    if (page !in 1..604) return ""
    val digits = "٠١٢٣٤٥٦٧٨٩"
    return page.toString().map { digit ->
        digits[digit.digitToInt()]
    }.joinToString("")
}

internal fun shouldUseTajweedRange(
    requestUrl: String,
    verifiedBinaryUrl: String?,
    existingBytes: Long
): Boolean = existingBytes > 0L && verifiedBinaryUrl != null && requestUrl == verifiedBinaryUrl

internal fun shouldRestartTajweedDownloadAfter416(
    existingBytes: Long,
    alreadyRestarted: Boolean
): Boolean = existingBytes > 0L && !alreadyRestarted

internal fun isTrustedDropboxHost(host: String): Boolean {
    val value = host.trim().trimEnd('.').lowercase(Locale.ROOT)
    return value == "dropbox.com" ||
        value.endsWith(".dropbox.com") ||
        value == "dropboxusercontent.com" ||
        value.endsWith(".dropboxusercontent.com")
}

internal data class TajweedMushafRenderedPage(
    val bitmap: Bitmap,
    val geometry: List<MushafPageVerseGeometry>
)

internal fun tajweedGlyphBoundsToPageSpace(
    surah: Int,
    ayah: Int,
    lineNumber: Int,
    minX: Float,
    maxX: Float,
    minY: Float,
    maxY: Float,
    scale: Float = minOf(1148f / 1340f, 1641f / 1890f) * TajweedMushafRepository.PAGE_ARTWORK_SCALE,
    offsetX: Float = (1148f - 1340f * minOf(1148f / 1340f, 1641f / 1890f) * TajweedMushafRepository.PAGE_ARTWORK_SCALE) * 0.5f,
    offsetY: Float = (1641f - 1890f * minOf(1148f / 1340f, 1641f / 1890f) * TajweedMushafRepository.PAGE_ARTWORK_SCALE) * 0.5f
): MushafWordBounds = MushafWordBounds(
    surah = surah,
    ayah = ayah,
    lineNumber = lineNumber,
    minX = minX * scale + offsetX,
    maxX = maxX * scale + offsetX,
    minY = minY * scale + offsetY,
    maxY = maxY * scale + offsetY
)


/**
 * The source glyph database uses taller line boxes than the SVG edition.
 * Keep the hit areas tied to this edition's own 1340x1890 coordinate map;
 * its maximum recorded glyph height is 155 source pixels (about 133 page pixels).
 */
internal fun buildTajweedPageVerseGeometry(
    words: List<MushafWordBounds>
): List<MushafPageVerseGeometry> {
    return words.asSequence()
        .filter { word ->
            word.surah in 1..114 &&
                word.ayah in 1..286 &&
                word.lineNumber >= 0 &&
                word.minX.isFinite() && word.maxX.isFinite() &&
                word.minY.isFinite() && word.maxY.isFinite() &&
                word.minX >= 0f && word.minY >= 0f &&
                word.maxX <= 1148f && word.maxY <= 1641f &&
                word.maxX > word.minX && word.maxY > word.minY &&
                word.maxX - word.minX <= 1148f * 0.80f &&
                word.maxY - word.minY <= 1641f * 0.09f
        }
        .groupBy { MushafPageVerse(it.surah, it.ayah) }
        .map { (verse, verseWords) ->
            val rects = verseWords.map { word ->
                com.mashxall.unified.data.MushafPageRect(
                    glyphId = 0,
                    lineNumber = word.lineNumber,
                    minX = word.minX,
                    maxX = word.maxX,
                    minY = word.minY,
                    maxY = word.maxY
                )
            }.sortedWith(
                compareBy<com.mashxall.unified.data.MushafPageRect> { it.lineNumber }
                    .thenBy { it.minX }
                    .thenBy { it.minY }
            )
            MushafPageVerseGeometry(verse, rects)
        }
        .filter { it.rects.isNotEmpty() }
        .sortedWith(compareBy<MushafPageVerseGeometry> { it.verse.surah }.thenBy { it.verse.ayah })
}
