package com.mashxall.unified

import com.mashxall.unified.data.PrayerRow
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PrayerCountdownTest {
    private fun row(
        fajr: String = "04:51",
        sunrise: String = "06:12",
        dhuhr: String = "11:49",
        asr: String = "14:49",
        maghrib: String = "17:21",
        isha: String = "18:36"
    ) = PrayerRow(
        city = "Sulaymaniyah",
        date = "10-29",
        fajr = fajr,
        sunrise = sunrise,
        dhuhr = dhuhr,
        asr = asr,
        maghrib = maghrib,
        isha = isha,
        source = "salawati:Sulaymaniyah, Iraq"
    )

    private fun localTime(day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.OCTOBER)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    @Test
    fun resolvesFajrAsTheNextPrayerBeforeDawn() {
        val target = nextSourcePrayerTarget(row(), row(fajr = "04:42"), localTime(29, 4, 0))
        assertEquals("fajr", target?.key)
        assertEquals(localTime(29, 4, 51), target?.timeMillis)
    }

    @Test
    fun skipsSunriseBecauseItIsNotAnAdhanPrayer() {
        val target = nextSourcePrayerTarget(row(), row(), localTime(29, 7, 0))
        assertEquals("dhuhr", target?.key)
        assertEquals(localTime(29, 11, 49), target?.timeMillis)
    }

    @Test
    fun afterIshaRequiresAndUsesTomorrowSalawatiFajr() {
        val target = nextSourcePrayerTarget(row(), row(fajr = "04:42"), localTime(29, 20, 0))
        assertEquals("fajr", target?.key)
        assertEquals(localTime(30, 4, 42), target?.timeMillis)
    }

    @Test
    fun doesNotGuessTomorrowFajrWhenTomorrowSourceIsMissing() {
        assertNull(nextSourcePrayerTarget(row(), null, localTime(29, 20, 0)))
    }

    @Test
    fun refusesInvalidSourceRowsRatherThanInventingAClock() {
        assertNull(nextSourcePrayerTarget(row(fajr = "25:70"), row(), localTime(29, 3, 0)))
        assertNull(nextSourcePrayerTarget(null, row(), localTime(29, 3, 0)))
    }

    @Test
    fun rejectsNoorRowsEvenIfTheirClockValuesLookValid() {
        assertNull(nextSourcePrayerTarget(row().copy(source = "noor-base:Sulaymaniyah"), row(), localTime(29, 3, 0)))
        assertNull(nextSourcePrayerTarget(row(), row().copy(source = "noor-base:Sulaymaniyah"), localTime(29, 20, 0)))
    }
}
