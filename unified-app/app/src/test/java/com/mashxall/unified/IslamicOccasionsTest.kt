package com.mashxall.unified

import com.mashxall.unified.data.Surah
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IslamicOccasionsTest {
    @Test
    fun occasionsAreSortedAndNeverBeforeTheSelectedDay() {
        val today = LocalDate.of(2026, 11, 6)
        val occasions = upcomingIslamicOccasions(today, limit = 20)

        assertFalse(occasions.isEmpty())
        assertTrue(occasions.all { !it.date.isBefore(today) })
        assertEquals(occasions.map { it.date }.sorted(), occasions.map { it.date })
        assertTrue(occasions.any { it.key == "ramadan" })
        assertTrue(occasions.any { it.key == "eid_fitr" })
        assertTrue(occasions.any { it.key == "shawwal_six_days" })
        assertTrue(occasions.any { it.key == "dhul_hijjah_first_ten" })
        assertTrue(occasions.any { it.key == "tarwiyah" })
        assertTrue(occasions.any { it.key == "arafah" })
        assertTrue(occasions.any { it.key == "eid_adha" })
        assertTrue(occasions.any { it.key == "ashura" })
    }

    @Test
    fun occasionLimitIsRespectedAndZeroReturnsNoRows() {
        val today = LocalDate.of(2026, 11, 6)
        assertEquals(3, upcomingIslamicOccasions(today, limit = 3).size)
        assertTrue(upcomingIslamicOccasions(today, limit = 0).isEmpty())
    }

    @Test
    fun dailyAyahMapsAcrossSurahBoundariesAndAdvancesByDate() {
        val surahs = listOf(
            Surah(2, "البقرة", "Al-Baqarah", 3),
            Surah(1, "الفاتحة", "Al-Fatihah", 2)
        )

        assertEquals(DailyAyahReference(1, 1), dailyAyahReference(surahs, LocalDate.of(1970, 1, 1)))
        assertEquals(DailyAyahReference(1, 2), dailyAyahReference(surahs, LocalDate.of(1970, 1, 2)))
        assertEquals(DailyAyahReference(2, 1), dailyAyahReference(surahs, LocalDate.of(1970, 1, 3)))
        assertEquals(DailyAyahReference(2, 3), dailyAyahReference(surahs, LocalDate.of(1970, 1, 5)))
        assertEquals(DailyAyahReference(1, 1), dailyAyahReference(surahs, LocalDate.of(1970, 1, 6)))
    }

    @Test
    fun dailyAyahReturnsNullForMissingOrInvalidSurahCounts() {
        assertEquals(null, dailyAyahReference(emptyList(), LocalDate.now()))
        assertEquals(
            null,
            dailyAyahReference(listOf(Surah(115, "invalid", "Invalid", 7), Surah(1, "empty", "Empty", 0)), LocalDate.now())
        )
    }
}
