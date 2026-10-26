package com.mashxall.unified.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AudioInventoryTest {
    @Test
    fun buildsReadableAudioRowsFromTheOriginalAssetCataloguePaths() {
        assertEquals(
            AudioItem(
                assetPath = "content/salawati_3.1.3/base/assets/adhan/Adhan_al-Madina.mp3",
                title = "Adhan al Madina",
                group = "adhan"
            ),
            ContentRepository.audioItemFromAssetPath(
                "content/salawati_3.1.3/base/assets/adhan/Adhan_al-Madina.mp3"
            )
        )
        assertEquals(
            AudioItem(
                assetPath = "content/noor_tafsir_1.2.2/base/assets/reciters/Abdul_Basit.ogg",
                title = "Abdul Basit",
                group = "reciters"
            ),
            ContentRepository.audioItemFromAssetPath(
                "content/noor_tafsir_1.2.2/base/assets/reciters/Abdul_Basit.ogg"
            )
        )
    }

    @Test
    fun excludesNonAudioFilesAndTheSourceClickSound() {
        assertNull(ContentRepository.audioItemFromAssetPath("content/icons/cover.png"))
        assertNull(ContentRepository.audioItemFromAssetPath("content/audio/click.wav"))
        assertNull(ContentRepository.audioItemFromAssetPath("content/books/book.pdf"))
    }

    @Test
    fun recognizesAudioExtensionsWithoutCaseSensitivity() {
        assertEquals(
            AudioItem("content/audio/Adhan.WAV", "Adhan", "audio"),
            ContentRepository.audioItemFromAssetPath("content/audio/Adhan.WAV")
        )
    }
}
