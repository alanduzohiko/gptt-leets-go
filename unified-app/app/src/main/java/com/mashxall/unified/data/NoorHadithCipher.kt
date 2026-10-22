package com.mashxall.unified.data

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * Noor's own cursor mapper decrypts arabic, english, reference and collection
 * using Cipher.getInstance("AES") and a 32-character key reconstructed from
 * the helper's integer/offset table. This mirrors that source format without
 * depending on or bundling any of Noor's decompiled code.
 */
internal object NoorHadithCipher {
    internal const val KEY = "c7fwxtzWmtNko9CdG8IlP9phi24ONllU"

    private const val BASE64_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

    private val collectionArabic = mapOf(
        "AbuDawud" to "سنن أبي داود",
        "Adab" to "الأدب المفرد",
        "Bukhari" to "صحيح البخاري",
        "IbnMajah" to "سنن ابن ماجه",
        "Malik" to "موطأ مالك",
        "Maram" to "بلوغ المرام",
        "Muslim" to "صحيح مسلم",
        "Nasai" to "سنن النسائي",
        "Saliheen" to "رياض الصالحين",
        "Tirmidhi" to "جامع الترمذي"
    )

    private val collectionEnglish = mapOf(
        "AbuDawud" to "Sunan Abu Dawud",
        "Adab" to "Al-Adab Al-Mufrad",
        "Bukhari" to "Sahih al-Bukhari",
        "IbnMajah" to "Sunan Ibn Majah",
        "Malik" to "Muwatta Malik",
        "Maram" to "Bulugh al-Maram",
        "Muslim" to "Sahih Muslim",
        "Nasai" to "Sunan al-Nasa'i",
        "Saliheen" to "Riyad as-Salihin",
        "Tirmidhi" to "Jami at-Tirmidhi"
    )

    fun decrypt(encoded: String?): String {
        if (encoded.isNullOrBlank()) return ""
        return try {
            val cipherText = decodeBase64(encoded)
            val cipher = Cipher.getInstance("AES")
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(KEY.toByteArray(StandardCharsets.UTF_8), "AES")
            )
            String(cipher.doFinal(cipherText), StandardCharsets.UTF_8)
        } catch (_: Exception) {
            // Never present encrypted/base64 bytes as religious text.
            ""
        }
    }

    fun collectionArabic(code: String): String = collectionArabic[code.trim()].orEmpty()
    fun collectionEnglish(code: String): String = collectionEnglish[code.trim()].orEmpty()

    fun parseVerseReferences(encodedReferences: String?): List<Pair<Int, Int>> {
        if (encodedReferences.isNullOrBlank()) return emptyList()
        val pattern = Regex("""\\[?\\s*(\\d{1,3})\\s*:\\s*(\\d{1,3})\\s*\\]?""")
        return pattern.findAll(encodedReferences).mapNotNull { match ->
            val surah = match.groupValues[1].toIntOrNull() ?: return@mapNotNull null
            val ayah = match.groupValues[2].toIntOrNull() ?: return@mapNotNull null
            if (surah in 1..114 && ayah in 1..286) surah to ayah else null
        }.distinct().toList()
    }

    private fun decodeBase64(input: String): ByteArray {
        val output = ByteArrayOutputStream(input.length * 3 / 4)
        var accumulator = 0
        var bits = 0
        var paddingStarted = false

        for (ch in input) {
            if (ch.isWhitespace()) continue
            if (ch == '=') {
                paddingStarted = true
                continue
            }
            if (paddingStarted) throw IllegalArgumentException("Invalid data after base64 padding")
            val value = if (ch.code < 128) BASE64_ALPHABET.indexOf(ch) else -1
            if (value < 0) throw IllegalArgumentException("Invalid base64 character")
            accumulator = (accumulator shl 6) or value
            bits += 6
            if (bits >= 8) {
                bits -= 8
                output.write((accumulator shr bits) and 0xFF)
            }
        }
        return output.toByteArray()
    }
}
