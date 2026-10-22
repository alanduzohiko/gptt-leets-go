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
    fun mapsSourceCollectionCodesToHumanReadableNames() {
        assertEquals("صحيح البخاري", NoorHadithCipher.collectionArabic("Bukhari"))
        assertEquals("Sahih al-Bukhari", NoorHadithCipher.collectionEnglish("Bukhari"))
        assertEquals("", NoorHadithCipher.collectionArabic("UnlistedCollection"))
    }

    @Test
    fun encryptedGarbageIsNeverReturnedAsReadableText() {
        assertEquals("", NoorHadithCipher.decrypt("not a valid encrypted payload"))
    }
}
