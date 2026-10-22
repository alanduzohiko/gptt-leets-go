package com.mashxall.unified.data

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrayerTimeCalculatorTest {
    private val date: Date = Date.from(
        LocalDate.of(2026, 10, 22)
            .atTime(12, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
    )

    private fun minutes(clock: String): Int {
        val parsed = LocalTime.parse(clock)
        return parsed.hour * 60 + parsed.minute
    }

    @Test
    fun calculatedPrayerTimesAreValidAndChronologicalForSulaymaniyah() {
        val row = PrayerTimeCalculator.calculate(
            date = date,
            latitude = 35.56,
            longitude = 45.43,
            method = PrayerCalculationMethod.MWL,
            asrSchool = AsrCalculationSchool.SHAFI
        )

        val values = listOf(
            row.fajr,
            row.sunrise,
            row.dhuhr,
            row.asr,
            row.maghrib,
            row.isha
        ).map(::minutes)

        assertEquals(6, values.size)
        assertTrue("Fajr must precede sunrise", values[0] < values[1])
        assertTrue("Sunrise must precede Dhuhr", values[1] < values[2])
        assertTrue("Dhuhr must precede Asr", values[2] < values[3])
        assertTrue("Asr must precede Maghrib", values[3] < values[4])
        assertTrue("Maghrib must precede Isha", values[4] < values[5])
        assertEquals("calculated:mwl:shafi", row.source)
    }

    @Test
    fun hanafiAsrIsLaterThanStandardAsrAtSulaymaniyah() {
        val standard = PrayerTimeCalculator.calculate(
            date, 35.56, 45.43, PrayerCalculationMethod.MWL, AsrCalculationSchool.SHAFI
        )
        val hanafi = PrayerTimeCalculator.calculate(
            date, 35.56, 45.43, PrayerCalculationMethod.MWL, AsrCalculationSchool.HANAFI
        )

        assertTrue(minutes(hanafi.asr) > minutes(standard.asr))
    }

    @Test
    fun ummAlQuraUsesItsConfiguredFixedIshaInterval() {
        val row = PrayerTimeCalculator.calculate(
            date, 35.56, 45.43, PrayerCalculationMethod.UMM_AL_QURA, AsrCalculationSchool.SHAFI
        )
        val difference = (minutes(row.isha) - minutes(row.maghrib) + 1440) % 1440
        assertEquals(90, difference)
        assertEquals("calculated:umm_al_qura:shafi", row.source)
    }

    @Test
    fun invalidCoordinatesAreRejectedInsteadOfReturningMisleadingTimes() {
        try {
            PrayerTimeCalculator.calculate(date, 91.0, 45.43)
            throw AssertionError("Latitude outside -90..90 should be rejected")
        } catch (_: IllegalArgumentException) {
            // Expected: never silently substitute an unrelated city.
        }
    }

    @Test
    fun calculationMethodAndSchoolIdsHaveStableFallbacks() {
        assertEquals(PrayerCalculationMethod.MWL, PrayerCalculationMethod.fromId("unknown"))
        assertEquals(PrayerCalculationMethod.ISNA, PrayerCalculationMethod.fromId("isna"))
        assertEquals(AsrCalculationSchool.SHAFI, AsrCalculationSchool.fromId(null))
        assertEquals(AsrCalculationSchool.HANAFI, AsrCalculationSchool.fromId("hanafi"))
    }

    @Test
    fun distanceToSameCoordinateIsZero() {
        assertEquals(0.0, PrayerTimeCalculator.distanceKm(35.56, 45.43, 35.56, 45.43), 0.000001)
    }
}
