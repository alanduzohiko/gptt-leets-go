package com.mashxall.unified

import com.mashxall.unified.data.AudioItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ImportedAdhanAudioTest {
    @Test
    fun savesAndRestoresAnImportedTrackRecord() {
        val file = File.createTempFile("mashxall-imported-adhan-", ".mp3")
        try {
            file.writeBytes(ByteArray(2048) { 7 })
            val record = importedAdhanAssetRecord(file.absolutePath, "Madinah Adhan")
            val restored = importedAdhanAudioItem(record) { it == file.absolutePath && file.isFile }
            assertNotNull(restored)
            assertEquals("local:${file.absolutePath}", restored!!.assetPath)
            assertEquals("Madinah Adhan", restored.title)
        } finally {
            file.delete()
        }
    }

    @Test
    fun customLocalAudioCanBeSelectedOnlyWhenItExistsAndIsAvailable() {
        val file = File.createTempFile("mashxall-custom-adhan-", ".mp3")
        try {
            file.writeBytes(ByteArray(2048) { 1 })
            val item = AudioItem("local:${file.absolutePath}", "Custom Muezzin", "Muezzin imported from this phone")
            assertTrue(hasSelectableAdhanAsset(item.assetPath, listOf(item)))
            assertFalse(hasSelectableAdhanAsset("local:/missing/adhan.mp3", listOf(item)))
        } finally {
            file.delete()
        }
    }

    @Test
    fun rejectsMalformedOrMissingImportedTrackRecords() {
        assertNull(importedAdhanAudioItem("broken", { true }))
        assertNull(importedAdhanAudioItem("/missing/adhan.mp3\tMissing", { false }))
        assertFalse(importedAdhanAssetRecord("/tmp/adhan.mp3", "Custom\tname").contains("Custom\tname"))
    }
}
