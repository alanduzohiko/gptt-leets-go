package com.mashxall.unified.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class DailyHadithRotationTest {
    @Test
    fun dailyHadithOffsetIsStableForTheSameDate() {
        val date = LocalDate.of(2026, 11, 6)

        assertEquals(dailyHadithOffset(date, 125), dailyHadithOffset(date, 125))
    }

    @Test
    fun dailyHadithAdvancesByOneAndWrapsAtTheEnd() {
        val firstDay = LocalDate.of(1970, 1, 1)
        val nextDay = firstDay.plusDays(1)

        assertEquals(0, dailyHadithOffset(firstDay, 5))
        assertEquals(1, dailyHadithOffset(nextDay, 5))
        assertEquals(0, dailyHadithOffset(firstDay.plusDays(5), 5))
    }

    @Test
    fun emptyHadithDatabaseHasSafeOffset() {
        val today = LocalDate.of(2026, 11, 6)

        assertEquals(0, dailyHadithOffset(today, 0))
        assertEquals(0, dailyHadithOffset(today, -10))
    }
}
