package com.mashxall.unified

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleDriveConfirmationTest {
    @Test
    fun readsHiddenConfirmationTokenAndAllRequiredFormFields() {
        val html = """
            <html><body>
              <form id="download-form" action="https://drive.usercontent.google.com/download">
                <input type="hidden" name="id" value="shared-file-id">
                <input type="hidden" name="export" value="download">
                <input type="hidden" name="confirm" value="drive-token-123">
                <input type="hidden" name="authuser" value="0">
              </form>
            </body></html>
        """.trimIndent()

        val url = TajweedMushafRepository.googleDriveConfirmationUrl(
            html,
            "https://drive.google.com/uc?export=download&id=shared-file-id",
            emptyMap()
        )

        assertNotNull(url)
        assertTrue(url!!.contains("confirm=drive-token-123"))
        assertTrue(url.contains("id=shared-file-id"))
        assertTrue(url.contains("authuser=0"))
    }

    @Test
    fun usesDriveWarningCookieWhenConfirmationFormOmitsToken() {
        val html = """
            <html><body>
              <form id="download-form" action="/download">
                <input type="hidden" name="id" value="shared-file-id">
                <input type="hidden" name="export" value="download">
              </form>
            </body></html>
        """.trimIndent()

        val url = TajweedMushafRepository.googleDriveConfirmationUrl(
            html,
            "https://drive.google.com/uc?export=download&id=shared-file-id",
            mapOf("download_warning_123" to "cookie-token-456")
        )

        assertNotNull(url)
        assertTrue(url!!.contains("confirm=cookie-token-456"))
    }

    @Test
    fun doesNotTreatArbitraryHtmlAsAValidDownloadConfirmation() {
        val html = "<html><body>Sign in to continue</body></html>"
        val url = TajweedMushafRepository.googleDriveConfirmationUrl(
            html,
            "https://drive.google.com/uc?export=download&id=shared-file-id",
            emptyMap()
        )

        assertFalse(url != null)
    }
}
