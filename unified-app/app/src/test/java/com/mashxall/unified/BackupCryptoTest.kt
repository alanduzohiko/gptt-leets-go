package com.mashxall.unified

import java.nio.charset.StandardCharsets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCryptoTest {
    private fun text(bytes: ByteArray): String = String(bytes, StandardCharsets.UTF_8)

    private fun replaceField(envelope: String, key: String, value: String): String {
        val prefix = "$key="
        var replaced = false
        val updated = envelope.lineSequence().map { line ->
            if (line.startsWith(prefix)) {
                replaced = true
                prefix + value
            } else line
        }.joinToString(separator = "\n", postfix = "\n")
        check(replaced) { "Missing envelope field: $key" }
        return updated
    }

    @Test
    fun encryptsAndRoundTripsWithoutExposingPersonalText() {
        val plainText = """{"format":"mashxall-backup","note":"private reading note"}"""
        val encrypted = BackupCrypto.encrypt(plainText, "a-long-passphrase".toCharArray())
        val visibleEnvelope = text(encrypted)

        assertTrue(visibleEnvelope.startsWith("format=${BackupCrypto.FORMAT}\n"))
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
        val envelope = text(encrypted)
        val ciphertext = envelope.lineSequence().first { it.startsWith("ciphertext_hex=") }.substringAfter('=')
        val replacement = if (ciphertext.last() == '0') '1' else '0'
        val tampered = replaceField(envelope, "ciphertext_hex", ciphertext.dropLast(1) + replacement)
        assertThrows(Exception::class.java) {
            BackupCrypto.decrypt(tampered.toByteArray(StandardCharsets.UTF_8), "correct horse battery".toCharArray())
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
        val tampered = replaceField(text(encrypted), "version", "2")
        assertThrows(IllegalArgumentException::class.java) {
            BackupCrypto.decrypt(tampered.toByteArray(StandardCharsets.UTF_8), "correct horse battery".toCharArray())
        }
    }
}
