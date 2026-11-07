package com.mashxall.unified

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
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
    private const val DRIVE_FILE_ID = "1gbRdMP2AZ_LGJ4sZIfWLxx2BkGyQ9B4B"
    private const val EXPECTED_ARCHIVE_BYTES = 144220538L
    private const val EXPECTED_ARCHIVE_SHA256 =
        "ba814deb2db28951feab1f6e1022153d948ae905131c373331d8edd653acbdc0"
    private const val EXPECTED_GLYPH_ROWS = 83881
    private const val EXPECTED_IMAGE_WIDTH = 1340
    private const val EXPECTED_IMAGE_HEIGHT = 1890
    private const val PAGE_COUNT = 604
    private const val TARGET_WIDTH = 1148
    private const val TARGET_HEIGHT = 1641
    private const val PACK_DIRECTORY = "tajweed-mushaf"
    private const val STAGING_DIRECTORY = "tajweed-mushaf.installing"
    private const val DATABASE_NAME = "ayahinfo_1280.db"
    private const val COMPLETE_MARKER = ".verified-pack"

    private val pageEntryPattern = Regex("""^tajweed/width_1280/page([0-9]{3})\.png$""")

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
        val endpoints = listOf(
            "https://drive.google.com/uc?export=download&id=$DRIVE_FILE_ID",
            "https://drive.usercontent.google.com/download?id=$DRIVE_FILE_ID&export=download"
        )
        val cookies = linkedMapOf<String, String>()
        var requestUrl = endpoints.first()
        var lastError: Exception? = null
        var attempts = 0
        var confirmations = 0
        var binaryDownloadUrl: String? = null
        var restartedAfter416 = false

        while (attempts < 7) {
            var connection: HttpURLConnection? = null
            try {
                val existing = target.takeIf { it.isFile }?.length() ?: 0L
                val rangeStart = if (shouldUseDriveRange(requestUrl, binaryDownloadUrl, existing)) existing else 0L
                connection = openDriveConnection(requestUrl, cookies, rangeStart, binaryDownloadUrl)
                val response = connection.responseCode

                if (response == 416) {
                    if (existing == EXPECTED_ARCHIVE_BYTES) return
                    if (shouldRestartDriveDownloadAfter416(existing, restartedAfter416)) {
                        // A stale/unsupported Range request must never be repeated.
                        // Restart the Google Drive confirmation handshake from byte zero.
                        target.delete()
                        restartedAfter416 = true
                        binaryDownloadUrl = null
                        requestUrl = endpoints.first()
                        cookies.clear()
                        confirmations = 0
                        continue
                    }
                    throw IOException(
                        "Google Drive rejected a clean download request with HTTP 416. " +
                            "The shared Drive file or its download endpoint is not accepting this request."
                    )
                }

                val contentType = connection.contentType.orEmpty().lowercase(Locale.ROOT)
                if (contentType.contains("text/html") || response !in 200..299) {
                    val html = readDriveHtml(connection)
                    val confirmation = googleDriveConfirmationUrl(html, connection.url.toString(), cookies)
                    if (confirmation != null && confirmations < 5) {
                        confirmations++
                        requestUrl = confirmation
                        // Confirmation pages are handshake requests, never byte-range targets.
                        binaryDownloadUrl = null
                        continue
                    }
                    throw IOException(googleDriveFailureReason(html, response))
                }

                val append = response == HttpURLConnection.HTTP_PARTIAL && rangeStart > 0L
                if (response != HttpURLConnection.HTTP_OK && response != HttpURLConnection.HTTP_PARTIAL) {
                    throw IOException("Google Drive returned HTTP " + response + ".")
                }
                // Save the final URL that actually returned the ZIP. Only this URL may
                // receive a Range header on a later retry; redirects and confirmation
                // forms are always requested from byte zero.
                binaryDownloadUrl = connection.url.toString()
                requestUrl = binaryDownloadUrl.orEmpty()
                if (append) {
                    val start = Regex("""^bytes\s+(\d+)-""", RegexOption.IGNORE_CASE)
                        .find(connection.getHeaderField("Content-Range").orEmpty())
                        ?.groupValues?.getOrNull(1)?.toLongOrNull()
                    if (start != null && start != existing) {
                        target.delete()
                        throw IOException("Google Drive resumed from an unexpected byte position. Restarting safely.")
                    }
                } else if (existing > 0L) {
                    // The server ignored Range and returned the whole file: replace the partial copy.
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
                                if (count < 0) throw IOException("Google Drive ended before sending a ZIP header.")
                                read += count
                            }
                            if (header[0] != 0x50.toByte() || header[1] != 0x4b.toByte()) {
                                target.delete()
                                throw IOException("Google Drive did not return the Tajweed Mushaf ZIP archive.")
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
                    throw IOException("Google Drive sent more data than the verified Tajweed Mushaf archive.")
                }
                throw IOException(
                    "The connection stopped at " + size + " of " + EXPECTED_ARCHIVE_BYTES +
                        " bytes. The download will resume from this point."
                )
            } catch (error: Exception) {
                lastError = error
                attempts++
                if (target.length() > EXPECTED_ARCHIVE_BYTES) target.delete()
                if (attempts >= 7) break

                // Keep both the partial file and its confirmed URL when resuming.
                if (target.length() == 0L && requestUrl in endpoints) {
                    requestUrl = endpoints[attempts % endpoints.size]
                }
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
            lastError?.message ?: "Could not download the Tajweed Mushaf from Google Drive.",
            lastError
        )
    }

    private fun openDriveConnection(
        startUrl: String,
        cookies: MutableMap<String, String>,
        rangeStart: Long,
        rangeUrl: String?
    ): HttpURLConnection {
        var url = URL(startUrl)
        repeat(10) {
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 30_000
                readTimeout = 90_000
                instanceFollowRedirects = false
                requestMethod = "GET"
                setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 11) AppleWebKit/537.36 Chrome/120.0 Mobile Safari/537.36"
                )
                setRequestProperty("Accept", "application/zip,application/octet-stream,text/html;q=0.9,*/*;q=0.8")
                setRequestProperty("Accept-Encoding", "identity")
                if (cookies.isNotEmpty() && isGoogleDriveHost(url.host)) {
                    setRequestProperty("Cookie", cookies.entries.joinToString("; ") { entry ->
                        entry.key + "=" + entry.value
                    })
                }
                if (rangeStart > 0L && rangeUrl != null && url.toString() == rangeUrl) {
                    setRequestProperty("Range", "bytes=" + rangeStart + "-")
                }
            }

            val code = connection.responseCode
            connection.headerFields.entries
                .filter { it.key?.equals("Set-Cookie", ignoreCase = true) == true }
                .flatMap { it.value.orEmpty() }
                .forEach { header ->
                    val pair = header.substringBefore(';').trim()
                    val equals = pair.indexOf('=')
                    if (equals > 0) {
                        cookies[pair.substring(0, equals).trim()] = pair.substring(equals + 1).trim()
                    }
                }

            if (code in 300..399) {
                val location = connection.getHeaderField("Location")
                    ?: throw IOException("Google Drive redirected without a download location.")
                val next = URL(url, location)
                if (!isGoogleDriveHost(next.host)) {
                    connection.disconnect()
                    throw IOException("Google Drive redirected the download to an untrusted host.")
                }
                connection.disconnect()
                url = next
            } else {
                return connection
            }
        }
        throw IOException("Google Drive returned too many redirects.")
    }

    private fun isGoogleDriveHost(host: String): Boolean {
        val value = host.lowercase(Locale.ROOT)
        return value == "google.com" ||
            value.endsWith(".google.com") ||
            value == "googleusercontent.com" ||
            value.endsWith(".googleusercontent.com")
    }

    private fun readDriveHtml(connection: HttpURLConnection): String {
        val stream = runCatching { connection.inputStream }.getOrNull()
            ?: connection.errorStream
            ?: return ""
        return stream.use { input ->
            val bytes = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(16 * 1024)
            var total = 0
            while (total < 2 * 1024 * 1024) {
                val count = input.read(buffer, 0, minOf(buffer.size, 2 * 1024 * 1024 - total))
                if (count < 0) break
                bytes.write(buffer, 0, count)
                total += count
            }
            bytes.toString(Charsets.UTF_8.name())
        }
    }

    internal fun googleDriveConfirmationUrl(
        html: String,
        currentUrl: String,
        cookies: Map<String, String>
    ): String? {
        if (html.isBlank()) return null
        val cookieToken = cookies.entries.firstOrNull {
            it.key.startsWith("download_warning", ignoreCase = true)
        }?.value

        val forms = Regex(
            """<form\b([^>]*)>(.*?)</form>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        val inputs = Regex(
            """<input\b([^>]*)>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        for (form in forms.findAll(html)) {
            val attributes = parseHtmlAttributes(form.groupValues[1])
            val parameters = linkedMapOf<String, String>()
            for (input in inputs.findAll(form.groupValues[2])) {
                val fields = parseHtmlAttributes(input.groupValues[1])
                val name = fields["name"] ?: continue
                val type = fields["type"].orEmpty()
                if (type.equals("hidden", true) || name in setOf("id", "export", "confirm", "uuid", "authuser")) {
                    parameters[name] = fields["value"].orEmpty()
                }
            }

            if (parameters["confirm"].isNullOrBlank() && !cookieToken.isNullOrBlank()) {
                parameters["confirm"] = cookieToken
            }
            if (parameters["confirm"].isNullOrBlank()) continue

            parameters.putIfAbsent("id", DRIVE_FILE_ID)
            parameters.putIfAbsent("export", "download")
            val action = htmlUnescape(attributes["action"].orEmpty()).ifBlank { currentUrl }
            val resolved = runCatching { URL(URL(currentUrl), action).toString() }.getOrNull() ?: continue
            return appendGoogleQueryParameters(resolved, parameters)
        }

        // Some Drive warning pages provide a "Download anyway" link instead of a form.
        val anchors = Regex(
            """<a\b([^>]*)>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        for (anchor in anchors.findAll(html)) {
            val attributes = parseHtmlAttributes(anchor.groupValues[1])
            val href = htmlUnescape(attributes["href"].orEmpty())
            if (href.contains("confirm=", ignoreCase = true)) {
                return runCatching { URL(URL(currentUrl), href).toString() }.getOrNull()
            }
        }

        if (!cookieToken.isNullOrBlank()) {
            return appendGoogleQueryParameters(
                "https://drive.google.com/uc",
                linkedMapOf("export" to "download", "id" to DRIVE_FILE_ID, "confirm" to cookieToken)
            )
        }
        return null
    }

    private fun parseHtmlAttributes(source: String): Map<String, String> {
        val pattern = Regex("""([A-Za-z_:][A-Za-z0-9_.:-]*)\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s>]+))""")
        return buildMap {
            for (match in pattern.findAll(source)) {
                val value = match.groups[2]?.value
                    ?: match.groups[3]?.value
                    ?: match.groups[4]?.value.orEmpty()
                put(match.groupValues[1].lowercase(Locale.ROOT), htmlUnescape(value))
            }
        }
    }

    private fun htmlUnescape(value: String): String = value
        .replace("&amp;", "&", ignoreCase = true)
        .replace("&quot;", "\"", ignoreCase = true)
        .replace("&#39;", "'", ignoreCase = true)
        .replace("&#x27;", "'", ignoreCase = true)
        .replace("&#61;", "=", ignoreCase = true)
        .replace("&#x3d;", "=", ignoreCase = true)
        .replace("\\u003d", "=", ignoreCase = true)
        .replace("\\u0026", "&", ignoreCase = true)

    private fun appendGoogleQueryParameters(url: String, updates: Map<String, String>): String {
        val clean = url.substringBefore('#')
        val fragment = url.substringAfter('#', "").takeIf { url.contains('#') }?.let { "#$it" }.orEmpty()
        val path = clean.substringBefore('?')
        val query = clean.substringAfter('?', "")
        val values = linkedMapOf<String, String>()
        if (query.isNotBlank()) {
            query.split('&').filter { it.isNotBlank() }.forEach { part ->
                val rawKey = part.substringBefore('=')
                val rawValue = part.substringAfter('=', "")
                val key = runCatching { java.net.URLDecoder.decode(rawKey, "UTF-8") }.getOrDefault(rawKey)
                val value = runCatching { java.net.URLDecoder.decode(rawValue, "UTF-8") }.getOrDefault(rawValue)
                values[key] = value
            }
        }
        updates.forEach { (key, value) -> values[key] = value }
        val encoded = values.entries.joinToString("&") { entry ->
            java.net.URLEncoder.encode(entry.key, "UTF-8") + "=" +
                java.net.URLEncoder.encode(entry.value, "UTF-8")
        }
        return "$path?$encoded$fragment"
    }

    private fun googleDriveFailureReason(html: String, response: Int): String {
        val plain = html
            .replace(Regex("""(?is)<(script|style)\b.*?</\1>"""), " ")
            .replace(Regex("""(?is)<[^>]+>"""), " ")
            .replace("&nbsp;", " ", ignoreCase = true)
            .replace(Regex("""\s+"""), " ")
            .trim()
            .take(260)
        return when {
            plain.contains("quota", ignoreCase = true) ->
                "Google Drive temporarily limited downloads for this file. Please try again later."
            plain.contains("sign in", ignoreCase = true) || plain.contains("access denied", ignoreCase = true) ->
                "Google Drive requires file access. The shared link must allow anyone with the link to download it."
            plain.isNotBlank() ->
                "Google Drive returned a confirmation page that could not be completed: " + plain
            else -> "Google Drive returned HTTP " + response + " without a usable download confirmation."
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
                renderIntoPageViewport(source, TARGET_WIDTH, TARGET_HEIGHT)
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
                renderIntoPageViewport(source, width, height)
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
        val scale = minOf(TARGET_WIDTH.toFloat() / EXPECTED_IMAGE_WIDTH, TARGET_HEIGHT.toFloat() / EXPECTED_IMAGE_HEIGHT)
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

    private fun renderIntoPageViewport(source: Bitmap, width: Int, height: Int): Bitmap {
        val target = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val scale = minOf(width.toFloat() / source.width, height.toFloat() / source.height)
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
        }
        return target
    }
}

internal fun shouldUseDriveRange(
    requestUrl: String,
    verifiedBinaryUrl: String?,
    existingBytes: Long
): Boolean = existingBytes > 0L && verifiedBinaryUrl != null && requestUrl == verifiedBinaryUrl

internal fun shouldRestartDriveDownloadAfter416(
    existingBytes: Long,
    alreadyRestarted: Boolean
): Boolean = existingBytes > 0L && !alreadyRestarted

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
    scale: Float = minOf(1148f / 1340f, 1641f / 1890f),
    offsetX: Float = (1148f - 1340f * minOf(1148f / 1340f, 1641f / 1890f)) * 0.5f,
    offsetY: Float = (1641f - 1890f * minOf(1148f / 1340f, 1641f / 1890f)) * 0.5f
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
