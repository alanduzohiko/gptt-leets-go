package com.mashxall.unified

import java.nio.charset.StandardCharsets
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCryptoTest {
    @Test
    fun encryptsAndRoundTripsWithoutExposingPersonalText() {
        val plainText = """{"format":"mashxall-backup","note":"private reading note"}"""
        val encrypted = BackupCrypto.encrypt(plainText, "a-long-passphrase".toCharArray())
        val visibleEnvelope = String(encrypted, StandardCharsets.UTF_8)

        assertTrue(visibleEnvelope.contains(BackupCrypto.FORMAT))
        assertFalse(visibleEnvelope.contains("private reading note"))
        assertEquals(plainText, BackupCrypto.decrypt(encrypted, "a-long-passphrase".toCharArray()))
    }

    @Test
    fun rejectsWrongPassphraseUsingAuthenticatedEncryption() {
        val encrypted = BackupCrypto.encrypt("private contents", "correct horse battery".toCharArray())
        assertThrows(Exception::class.java) {
            BackupCrypto.decrypt(encrypted, "wrong horse battery".toCharArray())
        }
    }

    @Test
    fun rejectsTamperedCiphertext() {
        val encrypted = BackupCrypto.encrypt("private contents", "correct horse battery".toCharArray())
        val envelope = JSONObject(String(encrypted, StandardCharsets.UTF_8))
        val ciphertext = envelope.getString("ciphertext_hex")
        val replacement = if (ciphertext.last() == '0') '1' else '0'
        envelope.put("ciphertext_hex", ciphertext.dropLast(1) + replacement)
        assertThrows(Exception::class.java) {
            BackupCrypto.decrypt(envelope.toString().toByteArray(StandardCharsets.UTF_8), "correct horse battery".toCharArray())
        }
    }

    @Test
    fun rejectsShortPassphrases() {
        assertThrows(IllegalArgumentException::class.java) {
            BackupCrypto.encrypt("private contents", "short".toCharArray())
        }
    }

    @Test
    fun rejectsUnsupportedEnvelopeVersions() {
        val encrypted = BackupCrypto.encrypt("private contents", "correct horse battery".toCharArray())
        val envelope = JSONObject(String(encrypted, StandardCharsets.UTF_8)).put("version", 2)
        assertThrows(IllegalArgumentException::class.java) {
            BackupCrypto.decrypt(envelope.toString().toByteArray(StandardCharsets.UTF_8), "correct horse battery".toCharArray())
        }
    }
}
