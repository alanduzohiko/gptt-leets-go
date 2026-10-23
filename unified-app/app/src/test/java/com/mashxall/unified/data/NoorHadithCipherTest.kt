package com.mashxall.unified.data

import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoorHadithCipherTest {
    private fun encryptLikeNoor(plainText: String): String {
        val cipher = Cipher.getInstance("AES")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(NoorHadithCipher.KEY.toByteArray(StandardCharsets.UTF_8), "AES")
        )
        return Base64.getEncoder().encodeToString(cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8)))
    }

    @Test
    fun decryptsTheSameAesBase64FormatAsNoor() {
        val original = "صحيح البخاري — حديث تجريبي"
        assertEquals(original, NoorHadithCipher.decrypt(encryptLikeNoor(original)))
    }

    @Test
    fun parsesOneOrMultipleBracketedQuranReferences() {
        assertEquals(
            listOf(2 to 255, 3 to 18, 114 to 6),
            NoorHadithCipher.parseVerseReferences("[2:255], [3:18], [114:6]")
        )
    }

    @Test
    fun ignoresInvalidQuranReferencesRatherThanCrashing() {
        assertEquals(
            listOf(2 to 255),
            NoorHadithCipher.parseVerseReferences("[2:255], [999:1], [2:0], corrupt")
        )
        assertTrue(NoorHadithCipher.parseVerseReferences(null).isEmpty())
    }

    @Test
    fun mapsEverySourceCollectionCodeToReadableArabicAndEnglishNames() {
        val expected = mapOf(
            "AbuDawud" to ("سنن أبي داود" to "Sunan Abu Dawud"),
            "Adab" to ("الأدب المفرد" to "Al-Adab Al-Mufrad"),
            "Bukhari" to ("صحيح البخاري" to "Sahih al-Bukhari"),
            "IbnMajah" to ("سنن ابن ماجه" to "Sunan Ibn Majah"),
            "Malik" to ("موطأ مالك" to "Muwatta Malik"),
            "Maram" to ("بلوغ المرام" to "Bulugh al-Maram"),
            "Muslim" to ("صحيح مسلم" to "Sahih Muslim"),
            "Nasai" to ("سنن النسائي" to "Sunan al-Nasa'i"),
            "Saliheen" to ("رياض الصالحين" to "Riyad as-Salihin"),
            "Tirmidhi" to ("جامع الترمذي" to "Jami at-Tirmidhi")
        )
        expected.forEach { (code, names) ->
            assertEquals("Arabic collection name for $code", names.first, NoorHadithCipher.collectionArabic(code))
            assertEquals("English collection name for $code", names.second, NoorHadithCipher.collectionEnglish(code))
        }
        assertEquals("", NoorHadithCipher.collectionArabic("UnlistedCollection"))
        assertEquals("", NoorHadithCipher.collectionEnglish("UnlistedCollection"))
        assertEquals("", NoorHadithCipher.collectionSorani("UnlistedCollection"))
        assertEquals("سەحیحی بوخاری", NoorHadithCipher.collectionSorani("Bukhari"))
    }

    @Test
    fun formatsMultipleRelatedHadithReferenceTokensWithoutLosingSourceInformation() {
        val formatted = NoorHadithCipher.formatReference(
            "DarusSalam 1:2:3, USC, in-book 4:5",
            "صحيح البخاري"
        )
        assertTrue(formatted.contains("دار السلام"))
        assertTrue(formatted.contains("بەرگی 1"))
        assertTrue(formatted.contains("پەرتووکى 2"))
        assertTrue(formatted.contains("حدیثی ژمارە 3"))
        assertTrue(formatted.contains("4 حدیثی ژمارە 5"))
        assertTrue(!formatted.contains("USC"))
    }

    @Test
    fun formatsRelatedReferencesInArabicAndEnglishWhenRequested() {
        val raw = "DarusSalam 1:2:3, USC, in-book 4:5"
        val arabic = NoorHadithCipher.formatReference(raw, "صحيح البخاري", "ar")
        val english = NoorHadithCipher.formatReference(raw, "Sahih al-Bukhari", "en")

        assertTrue(arabic.contains("المجلد 1"))
        assertTrue(arabic.contains("الكتاب 2"))
        assertTrue(arabic.contains("الحديث رقم 3"))
        assertTrue(arabic.contains("في الكتاب رقم"))
        assertTrue(english.contains("Vol. 1"))
        assertTrue(english.contains("Book 2"))
        assertTrue(english.contains("Hadith no. 3"))
        assertTrue(english.contains("In book"))
        assertTrue(!arabic.contains("بەرگی"))
        assertTrue(!english.contains("بەرگی"))
    }

    @Test
    fun encryptedGarbageIsNeverReturnedAsReadableText() {
        assertEquals("", NoorHadithCipher.decrypt("not a valid encrypted payload"))
    }
}
