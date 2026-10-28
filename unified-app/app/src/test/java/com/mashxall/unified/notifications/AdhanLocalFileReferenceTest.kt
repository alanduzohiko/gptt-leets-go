package com.mashxall.unified.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

class AdhanLocalFileReferenceTest {
    @Test
    fun resolvesOnlyExistingLocalAudioReferences() {
        val file = File.createTempFile("mashxall-adhan-", ".mp3")
        try {
            file.writeBytes(ByteArray(1024) { 1 })
            assertEquals(file.absolutePath, resolveLocalAdhanFileReference("local:${file.absolutePath}")?.absolutePath)
            assertNull(resolveLocalAdhanFileReference("content/noor/adhan.mp3"))
        } finally {
            file.delete()
        }
    }

    @Test
    fun rejectsMissingAndIncompleteLocalFiles() {
        val missing = File(System.getProperty("java.io.tmpdir"), "mashxall-not-present.mp3")
        missing.delete()
        assertNull(resolveLocalAdhanFileReference("local:${missing.absolutePath}"))
        val tiny = File.createTempFile("mashxall-adhan-tiny-", ".mp3")
        try {
            tiny.writeBytes(byteArrayOf(1, 2, 3))
            assertNull(resolveLocalAdhanFileReference("local:${tiny.absolutePath}"))
        } finally {
            tiny.delete()
        }
    }
}
