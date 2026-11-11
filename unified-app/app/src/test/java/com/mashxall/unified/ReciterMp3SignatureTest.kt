package com.mashxall.unified

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReciterMp3SignatureTest {
    @Test
    fun acceptsId3MetadataHeader() {
        assertTrue(hasPlausibleMp3StreamSignature(byteArrayOf(0x49, 0x44, 0x33, 0x04, 0x00)))
    }

    @Test
    fun acceptsMpegAudioFrameSync() {
        assertTrue(hasPlausibleMp3StreamSignature(byteArrayOf(0xFF.toByte(), 0xFB.toByte(), 0x90.toByte(), 0x64)))
    }

    @Test
    fun rejectsLargeErrorResponseSignatures() {
        assertFalse(hasPlausibleMp3StreamSignature("<html><body>Not found</body></html>".toByteArray()))
        assertFalse(hasPlausibleMp3StreamSignature("""{"error":"file not found"}""".toByteArray()))
    }

    @Test
    fun rejectsShortHeaders() {
        assertFalse(hasPlausibleMp3StreamSignature(byteArrayOf(0x49, 0x44)))
    }
}