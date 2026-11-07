package com.mashxall.unified

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TajweedDropboxDownloadTest {
    @Test
    fun sendsRangeOnlyToThePreviouslyVerifiedBinaryDownloadUrl() {
        val publicShareUrl =
            "https://www.dropbox.com/scl/fi/share-key/Mashxall-Tajweed-Source-144MB.zip?dl=1"
        val binaryUrl = "https://dl.dropboxusercontent.com/temp-download-token"

        assertFalse(shouldUseTajweedRange(publicShareUrl, binaryUrl, 70_000_000L))
        assertFalse(shouldUseTajweedRange(binaryUrl + "?changed=1", binaryUrl, 70_000_000L))
        assertTrue(shouldUseTajweedRange(binaryUrl, binaryUrl, 70_000_000L))
        assertFalse(shouldUseTajweedRange(binaryUrl, binaryUrl, 0L))
        assertFalse(shouldUseTajweedRange(publicShareUrl, null, 70_000_000L))
    }

    @Test
    fun retries416OnlyByRestartingAnExistingPartialDownloadOnce() {
        assertTrue(shouldRestartTajweedDownloadAfter416(70_000_000L, false))
        assertFalse(shouldRestartTajweedDownloadAfter416(0L, false))
        assertFalse(shouldRestartTajweedDownloadAfter416(70_000_000L, true))
    }

    @Test
    fun acceptsDropboxShareAndContentHosts() {
        assertTrue(isTrustedDropboxHost("dropbox.com"))
        assertTrue(isTrustedDropboxHost("www.dropbox.com"))
        assertTrue(isTrustedDropboxHost("dl.dropboxusercontent.com"))
        assertTrue(isTrustedDropboxHost("edge.dropboxusercontent.com"))
    }

    @Test
    fun rejectsLookalikeOrUnrelatedDownloadHosts() {
        assertFalse(isTrustedDropboxHost("dropbox.com.attacker.example"))
        assertFalse(isTrustedDropboxHost("evil-dropbox.com"))
        assertFalse(isTrustedDropboxHost("example.com"))
    }
}
