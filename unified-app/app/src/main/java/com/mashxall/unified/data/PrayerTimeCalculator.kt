package com.mashxall.unified.data

import java.text.SimpleDateFormat
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan
import kotlin.math.roundToInt

enum class PrayerCalculationMethod(
    val id: String,
    val fajrAngle: Double,
    val ishaAngle: Double,
    val fixedIshaMinutes: Int = 0,
    val maghribOffsetMinutes: Int = 2
) {
    MWL("mwl", 18.0, 17.0),
    ISNA("isna", 15.0, 15.0),
    EGYPT("egypt", 19.5, 17.5),
    KARACHI("karachi", 18.0, 18.0),
    UMM_AL_QURA("umm_al_qura", 18.5, 0.0, 90);

    companion object {
        fun fromId(id: String?): PrayerCalculationMethod =
            entries.firstOrNull { it.id == id } ?: MWL
    }
}

enum class AsrCalculationSchool(val id: String, val shadowFactor: Double) {
    SHAFI("shafi", 1.0),
    HANAFI("hanafi", 2.0);

    companion object {
        fun fromId(id: String?): AsrCalculationSchool =
            entries.firstOrNull { it.id == id } ?: SHAFI
    }
}

/**
 * NOAA solar-position fallback for locations not covered by a source timetable.
 * The original city timetables are preferred for nearby cities when present.
 */
object PrayerTimeCalculator {
    private const val SUN_ALTITUDE = -0.833
    private const val DHUHR_SAFETY_MINUTES = 1.0

    fun distanceKm(latitude1: Double, longitude1: Double, latitude2: Double, longitude2: Double): Double {
        val dLat = Math.toRadians(latitude2 - latitude1)
        val dLon = Math.toRadians(longitude2 - longitude1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(latitude1)) * cos(Math.toRadians(latitude2)) *
            sin(dLon / 2) * sin(dLon / 2)
        return 6371.0088 * 2.0 * kotlin.math.asin(kotlin.math.sqrt(a.coerceIn(0.0, 1.0)))
    }

    fun timeZoneForCountry(countryName: String, longitude: Double): ZoneId {
        val country = countryName.trim().lowercase(Locale.ROOT)
        val namedZone = when {
            listOf("iraq", "العراق", "عێراق").any { country.contains(it) } -> "Asia/Baghdad"
            listOf("saudi", "السعودية", "السعودية العربية").any { country.contains(it) } -> "Asia/Riyadh"
            listOf("kuwait", "الكويت").any { country.contains(it) } -> "Asia/Kuwait"
            listOf("qatar", "قطر").any { country.contains(it) } -> "Asia/Qatar"
            listOf("bahrain", "البحرين").any { country.contains(it) } -> "Asia/Bahrain"
            listOf("united arab emirates", "uae", "الإمارات", "الامارات").any { country.contains(it) } -> "Asia/Dubai"
            listOf("oman", "عمان", "سلطنة عمان").any { country.contains(it) } -> "Asia/Muscat"
            listOf("yemen", "اليمن").any { country.contains(it) } -> "Asia/Aden"
            listOf("jordan", "الأردن", "الاردن").any { country.contains(it) } -> "Asia/Amman"
            listOf("lebanon", "لبنان").any { country.contains(it) } -> "Asia/Beirut"
            listOf("palestine", "فلسطين").any { country.contains(it) } -> "Asia/Gaza"
            listOf("syria", "سوريا").any { country.contains(it) } -> "Asia/Damascus"
            listOf("turkey", "turkiye", "تركيا").any { country.contains(it) } -> "Europe/Istanbul"
            listOf("egypt", "مصر").any { country.contains(it) } -> "Africa/Cairo"
            listOf("libya", "ليبيا").any { country.contains(it) } -> "Africa/Tripoli"
            listOf("tunisia", "تونس").any { country.contains(it) } -> "Africa/Tunis"
            listOf("algeria", "الجزائر").any { country.contains(it) } -> "Africa/Algiers"
            listOf("morocco", "المغرب").any { country.contains(it) } -> "Africa/Casablanca"
            listOf("mauritania", "موريتانيا").any { country.contains(it) } -> "Africa/Nouakchott"
            listOf("sudan", "السودان").any { country.contains(it) } -> "Africa/Khartoum"
            listOf("somalia", "الصومال").any { country.contains(it) } -> "Africa/Mogadishu"
            listOf("djibouti", "جيبوتي").any { country.contains(it) } -> "Africa/Djibouti"
            listOf("comoros", "جزر القمر").any { country.contains(it) } -> "Indian/Comoro"
            listOf("iran", "إيران", "ايران").any { country.contains(it) } -> "Asia/Tehran"
            listOf("afghanistan", "أفغانستان").any { country.contains(it) } -> "Asia/Kabul"
            listOf("pakistan", "باكستان").any { country.contains(it) } -> "Asia/Karachi"
            listOf("bangladesh", "بنغلاديش").any { country.contains(it) } -> "Asia/Dhaka"
            listOf("india", "الهند").any { country.contains(it) } -> "Asia/Kolkata"
            listOf("malaysia", "ماليزيا").any { country.contains(it) } -> "Asia/Kuala_Lumpur"
            listOf("singapore", "سنغافورة").any { country.contains(it) } -> "Asia/Singapore"
            listOf("indonesia", "إندونيسيا").any { country.contains(it) } ->
                if (longitude >= 130.0) "Asia/Jayapura" else if (longitude >= 120.0) "Asia/Makassar" else "Asia/Jakarta"
            listOf("united kingdom", "britain", "uk", "المملكة المتحدة", "بريطانيا").any { country.contains(it) } -> "Europe/London"
            listOf("france", "فرنسا").any { country.contains(it) } -> "Europe/Paris"
            listOf("germany", "ألمانيا", "المانيا").any { country.contains(it) } -> "Europe/Berlin"
            else -> null
        }
        if (namedZone != null) {
            runCatching { ZoneId.of(namedZone) }.getOrNull()?.let { return it }
        }
        val approximateOffset = (longitude / 15.0).roundToInt().coerceIn(-12, 14)
        return ZoneId.ofOffset("UTC", ZoneOffset.ofHours(approximateOffset))
    }

    fun calculate(
        date: Date,
        latitude: Double,
        longitude: Double,
        method: PrayerCalculationMethod = PrayerCalculationMethod.MWL,
        asrSchool: AsrCalculationSchool = AsrCalculationSchool.SHAFI,
        timeZone: ZoneId = ZoneId.systemDefault()
    ): PrayerRow {
        require(latitude in -90.0..90.0) { "Latitude must be between -90 and 90 degrees" }
        require(longitude in -180.0..180.0) { "Longitude must be between -180 and 180 degrees" }

        val zone = timeZone
        val localDate = date.toInstant().atZone(zone).toLocalDate()
        val localNoon = localDate.atTime(12, 0).atZone(zone)
        val julianDay = localNoon.toInstant().toEpochMilli() / 86_400_000.0 + 2_440_587.5
        val century = (julianDay - 2_451_545.0) / 36_525.0

        val meanLongitude = normalizeDegrees(280.46646 + century * (36000.76983 + century * 0.0003032))
        val meanAnomaly = 357.52911 + century * (35999.05029 - 0.0001537 * century)
        val eccentricity = 0.016708634 - century * (0.000042037 + 0.0000001267 * century)
        val anomalyRadians = Math.toRadians(meanAnomaly)
        val equationOfCenter =
            sin(anomalyRadians) * (1.914602 - century * (0.004817 + 0.000014 * century)) +
            sin(2.0 * anomalyRadians) * (0.019993 - 0.000101 * century) +
            sin(3.0 * anomalyRadians) * 0.000289
        val omega = 125.04 - 1934.136 * century
        val apparentLongitude = meanLongitude + equationOfCenter - 0.00569 -
            0.00478 * sin(Math.toRadians(omega))
        val meanObliquity = 23.0 + (26.0 + (
            21.448 - century * (46.815 + century * (0.00059 - century * 0.001813))
        ) / 60.0) / 60.0
        val obliquity = meanObliquity + 0.00256 * cos(Math.toRadians(omega))
        val declination = asin(sin(Math.toRadians(obliquity)) * sin(Math.toRadians(apparentLongitude)))
        val yBase = tan(Math.toRadians(obliquity) / 2.0)
        val y = yBase * yBase
        val L0 = Math.toRadians(meanLongitude)
        val eotRadians =
            y * sin(2.0 * L0) -
            2.0 * eccentricity * sin(anomalyRadians) +
            4.0 * eccentricity * y * sin(anomalyRadians) * cos(2.0 * L0) -
            0.5 * y * y * sin(4.0 * L0) -
            1.25 * eccentricity * eccentricity * sin(2.0 * anomalyRadians)
        val equationOfTimeMinutes = Math.toDegrees(eotRadians) * 4.0
        val timeZoneHours = localNoon.offset.totalSeconds / 3600.0
        val solarNoon = 12.0 - longitude / 15.0 - equationOfTimeMinutes / 60.0 + timeZoneHours

        fun hourAngleForAltitude(altitudeDegrees: Double): Double {
            val latitudeRadians = Math.toRadians(latitude)
            val altitudeRadians = Math.toRadians(altitudeDegrees)
            val cosHourAngle = (
                sin(altitudeRadians) - sin(latitudeRadians) * sin(declination)
            ) / (cos(latitudeRadians) * cos(declination))
            return Math.toDegrees(acos(cosHourAngle.coerceIn(-1.0, 1.0))) / 15.0
        }

        val fajr = solarNoon - hourAngleForAltitude(-method.fajrAngle)
        val sunrise = solarNoon - hourAngleForAltitude(SUN_ALTITUDE)
        val dhuhr = solarNoon + DHUHR_SAFETY_MINUTES / 60.0
        val sunset = solarNoon + hourAngleForAltitude(SUN_ALTITUDE)
        val maghrib = sunset + method.maghribOffsetMinutes / 60.0
        val declinationDegrees = Math.toDegrees(declination)
        val latitudeDeclinationDifference = kotlin.math.abs(latitude - declinationDegrees)
        val asrAltitude = Math.toDegrees(
            atan(1.0 / (asrSchool.shadowFactor + tan(Math.toRadians(latitudeDeclinationDifference))))
        )
        val asr = solarNoon + hourAngleForAltitude(asrAltitude)
        val isha = if (method.fixedIshaMinutes > 0) {
            maghrib + method.fixedIshaMinutes / 60.0
        } else {
            solarNoon + hourAngleForAltitude(-method.ishaAngle)
        }

        val dateFormatter = SimpleDateFormat("MM-dd", Locale.US).apply {
            this.timeZone = TimeZone.getTimeZone(zone)
        }
        fun clock(hour: Double): String {
            val rounded = (hour * 60.0).roundToInt().mod(1440)
            val calendar = Calendar.getInstance(TimeZone.getTimeZone(zone), Locale.US)
            calendar.time = Date.from(localDate.atStartOfDay(zone).toInstant())
            calendar.set(Calendar.HOUR_OF_DAY, rounded / 60)
            calendar.set(Calendar.MINUTE, rounded % 60)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            return SimpleDateFormat("HH:mm", Locale.US).apply {
                this.timeZone = TimeZone.getTimeZone(zone)
            }.format(calendar.time)
        }

        return PrayerRow(
            city = "GPS",
            date = dateFormatter.format(date),
            fajr = clock(fajr),
            sunrise = clock(sunrise),
            dhuhr = clock(dhuhr),
            asr = clock(asr),
            maghrib = clock(maghrib),
            isha = clock(isha),
            source = "calculated:${method.id}:${asrSchool.id}",
            latitude = latitude,
            longitude = longitude
        )
    }

    private fun normalizeDegrees(value: Double): Double {
        var degrees = value % 360.0
        if (degrees < 0.0) degrees += 360.0
        return degrees
    }
}
