package com.mashxall.unified

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuranAudioTimingTest {
    private val completeRows = listOf(
        QuranTimingApiRow(1, 0L, 3_200L),
        QuranTimingApiRow(2, 3_200L, 9_100L),
        QuranTimingApiRow(3, 9_100L, 13_600L),
        QuranTimingApiRow(4, 13_600L, 18_000L)
    )

    @Test
    fun acceptsCompleteSequentialProviderTimestamps() {
        val timings = validateQuranAyahTimings(completeRows, expectedAyahCount = 4)
        assertEquals(4, timings?.size)
        assertEquals(3_200L, timings?.get(1)?.startMs)
        assertEquals(18_000L, timings?.lastOrNull()?.endMs)
    }

    @Test
    fun rejectsMissingDuplicateOutOfOrderAndInvalidVerseTimes() {
        assertNull(validateQuranAyahTimings(completeRows.dropLast(1), 4))
        assertNull(validateQuranAyahTimings(completeRows.mapIndexed { index, row ->
            if (index == 2) row.copy(ayah = 2) else row
        }, 4))
        assertNull(validateQuranAyahTimings(completeRows.mapIndexed { index, row ->
            if (index == 1) row.copy(startMs = 9_101L, endMs = 10_000L) else row
        }, 4))
        assertNull(validateQuranAyahTimings(completeRows.mapIndexed { index, row ->
            if (index == 1) row.copy(startMs = -1L) else row
        }, 4))
        assertNull(validateQuranAyahTimings(completeRows.mapIndexed { index, row ->
            if (index == 1) row.copy(endMs = row.startMs) else row
        }, 4))
    }


    @Test
    fun parsesTopLevelAndWrappedMp3QuranTimingArrays() {
        val wrapped = """{"value":[
            {"ayah":0,"start_time":0,"end_time":2731},
            {"ayah":1,"start_time":2731,"end_time":5720},
            {"ayah":2,"start_time":"5720","end_time":"9100"}
        ]}"""
        val rows = parseQuranTimingRows(wrapped)
        assertEquals(
            listOf(
                QuranTimingApiRow(1, 2_731L, 5_720L),
                QuranTimingApiRow(2, 5_720L, 9_100L)
            ),
            rows
        )
        assertEquals(2, parseQuranTimingRows("""{"data":{"value":[
            {"ayah":1,"start_time":0,"end_time":1000},
            {"ayah":2,"start_time":1000,"end_time":2000}
        ]}}""")?.size)
    }

    @Test
    fun parsesWrappedTimingReadListsAndNormalizesHttpFolders() {
        val reads = parseQuranTimingReads("""{"data":{"value":[
            {"id":53,"folder_url":"http://server7.mp3quran.net/basit/"},
            {"id":118,"folder_url":"https://server13.mp3quran.net/husr/"}
        ]}}""")
        assertEquals(2, reads.size)
        assertEquals(53, reads[0].id)
        assertEquals("https://server7.mp3quran.net/basit/", reads[0].folderUrl)
        assertEquals("https://server13.mp3quran.net/husr/", reads[1].folderUrl)
    }

    @Test
    fun rejectsUnknownTimingResponseEnvelopeInsteadOfPretendingItContainsData() {
        assertNull(parseQuranTimingRows("""{"status":"ok","message":"no timing array"}"""))
        assertTrue(parseQuranTimingReads("""{"status":"ok"}""").isEmpty())
    }

    @Test
    fun acceptsSmallOverlapsBetweenOfficialTransitionRanges() {
        val rows = completeRows.mapIndexed { index, row ->
            if (index == 1) row.copy(startMs = 3_100L, endMs = 9_200L) else row
        }
        val timings = validateQuranAyahTimings(rows, 4)
        assertEquals(4, timings?.size)
        assertEquals(2, timings?.get(1)?.ayah)
    }

    @Test
    fun selectedReadIdWinsWhenItsOfficialServerDiffersFromTheCatalogueMirror() {
        val reads = listOf(
            QuranTimingRead(53, "https://server7.mp3quran.net/basit/"),
            QuranTimingRead(51, "https://server7.mp3quran.net/basit/Almusshaf-Al-Mojawwad/")
        )
        assertEquals(
            51,
            resolveQuranTimingRead(reads, 51, "https://another-mirror.example/basit/")?.id
        )
    }

    @Test
    fun verifiedTimingDurationOverridesAnApproximatePlayerDuration() {
        assertEquals(18_000L, quranPlaybackDurationMs(19_200L, 18_000L))
        assertEquals(19_200L, quranPlaybackDurationMs(19_200L, null))
        assertEquals(18_000L, quranPlaybackDurationMs(-1L, 18_000L))
        assertEquals(0L, quranPlaybackDurationMs(-1L, null))
    }

    @Test
    fun aSeekIsNotConfirmedUntilThePlayerReachesItsRequestedPosition() {
        assertFalse(quranSeekPositionReached(42_000L, 0L))
        assertFalse(quranSeekPositionReached(42_000L, 40_000L))
        assertTrue(quranSeekPositionReached(42_000L, 42_100L))
        assertFalse(quranSeekPositionReached(42_000L, -1L))
    }

    @Test
    fun explicitMedia3SeekEventConfirmsEvenAnApproximateMp3FrameDestination() {
        assertTrue(quranSeekCompletionConfirmed(42_000L, 41_100L, isSeekDiscontinuity = true))
        assertTrue(quranSeekCompletionConfirmed(42_000L, 40_800L, isSeekDiscontinuity = false))
        assertFalse(quranSeekCompletionConfirmed(42_000L, 38_000L, isSeekDiscontinuity = false))
    }

    @Test
    fun resolvesAReadOnlyWhenItsIdAndFolderBelongToTheSelectedMoshaf() {
        val reads = listOf(
            QuranTimingRead(53, "https://server7.mp3quran.net/basit/"),
            QuranTimingRead(51, "https://server7.mp3quran.net/basit/Almusshaf-Al-Mojawwad/")
        )
        assertEquals(
            53,
            resolveQuranTimingRead(reads, 53, "https://server7.mp3quran.net/basit/")?.id
        )
        assertEquals(
            51,
            resolveQuranTimingRead(reads, 51, "https://server7.mp3quran.net/basit/")?.id
        )
        assertEquals(
            53,
            resolveQuranTimingRead(reads, null, "https://server7.mp3quran.net/basit/")?.id
        )
        val ambiguousFolderReads = reads + QuranTimingRead(54, "https://server7.mp3quran.net/basit/")
        assertNull(resolveQuranTimingRead(ambiguousFolderReads, null, "https://server7.mp3quran.net/basit/"))
        // A stale legacy read ID may recover only from a unique exact folder.
        assertEquals(
            53,
            resolveQuranTimingRead(reads, 999, "https://server7.mp3quran.net/basit/")?.id
        )
        assertNull(
            resolveQuranTimingRead(
                ambiguousFolderReads,
                999,
                "https://server7.mp3quran.net/basit/"
            )
        )
        assertEquals(
            51,
            resolveQuranTimingRead(reads, null, "https://server7.mp3quran.net/basit/Almusshaf-Al-Mojawwad/")?.id
        )
    }

    @Test
    fun activeAyahFollowsTheActualMediaPositionAcrossVerseBoundaries() {
        val timings = validateQuranAyahTimings(completeRows, 4)!!
        assertEquals(1, quranAyahAtPositionMs(timings, 0L))
        assertEquals(1, quranAyahAtPositionMs(timings, 3_199L))
        assertEquals(2, quranAyahAtPositionMs(timings, 3_200L))
        assertEquals(2, quranAyahAtPositionMs(timings, 9_099L))
        assertEquals(3, quranAyahAtPositionMs(timings, 9_100L))
        assertEquals(4, quranAyahAtPositionMs(timings, 18_000L))
        assertNull(quranAyahAtPositionMs(emptyList(), 1_000L))
    }

    @Test
    fun selectedAyahUsesItsExactProviderStartTimeInTheContinuousFile() {
        val timings = validateQuranAyahTimings(completeRows, 4)!!
        val audio = QuranSurahAudioTiming(
            readId = 123,
            audioUrl = "https://cdn.mp3quran.net/example/001.mp3",
            ayahs = timings
        )
        assertEquals(0L, audio.startMsForAyah(1))
        assertEquals(9_100L, audio.startMsForAyah(3))
        assertEquals(18_000L, audio.durationMs)
    }

    @Test
    fun onlyAValidatedFullSurahTimelineEnablesExactMidSurahStart() {
        val withoutTimeline = quranPlaybackStartPlan(
            audioType = 1,
            requestedAyah = 43,
            ayahCount = 114
        )
        assertFalse(withoutTimeline.isSupported)

        val withTimeline = quranPlaybackStartPlan(
            audioType = 1,
            requestedAyah = 43,
            ayahCount = 114,
            hasAyahTimings = true
        )
        assertTrue(withTimeline.isSupported)
        assertFalse(withTimeline.usesAyahClips)
        assertEquals(43, withTimeline.startingAyah)
    }
}
