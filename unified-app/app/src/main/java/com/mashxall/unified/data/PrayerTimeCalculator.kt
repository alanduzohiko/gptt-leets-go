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
            listOf("italy", "إيطاليا", "ايطاليا").any { country.contains(it) } -> "Europe/Rome"
            listOf("spain", "إسبانيا", "اسبانيا").any { country.contains(it) } -> if (longitude < -10.0) "Atlantic/Canary" else "Europe/Madrid"
            listOf("portugal", "البرتغال").any { country.contains(it) } -> if (longitude < -15.0) "Atlantic/Madeira" else "Europe/Lisbon"
            listOf("ireland", "أيرلندا", "ايرلندا").any { country.contains(it) } -> "Europe/Dublin"
            listOf("netherlands", "هولندا").any { country.contains(it) } -> "Europe/Amsterdam"
            listOf("belgium", "بلجيكا").any { country.contains(it) } -> "Europe/Brussels"
            listOf("switzerland", "سويسرا").any { country.contains(it) } -> "Europe/Zurich"
            listOf("austria", "النمسا").any { country.contains(it) } -> "Europe/Vienna"
            listOf("norway", "النرويج").any { country.contains(it) } -> "Europe/Oslo"
            listOf("sweden", "السويد").any { country.contains(it) } -> "Europe/Stockholm"
            listOf("finland", "فنلندا").any { country.contains(it) } -> "Europe/Helsinki"
            listOf("denmark", "الدنمارك").any { country.contains(it) } -> "Europe/Copenhagen"
            listOf("poland", "بولندا").any { country.contains(it) } -> "Europe/Warsaw"
            listOf("greece", "اليونان").any { country.contains(it) } -> "Europe/Athens"
            listOf("ukraine", "أوكرانيا", "اوكرانيا").any { country.contains(it) } -> "Europe/Kyiv"
            listOf("romania", "رومانيا").any { country.contains(it) } -> "Europe/Bucharest"
            listOf("bulgaria", "بلغاريا").any { country.contains(it) } -> "Europe/Sofia"
            listOf("czech", "التشيك").any { country.contains(it) } -> "Europe/Prague"
            listOf("russia", "روسيا").any { country.contains(it) } -> when {
                longitude < 30.0 -> "Europe/Moscow"
                longitude < 60.0 -> "Asia/Yekaterinburg"
                longitude < 90.0 -> "Asia/Omsk"
                longitude < 110.0 -> "Asia/Krasnoyarsk"
                longitude < 130.0 -> "Asia/Irkutsk"
                longitude < 150.0 -> "Asia/Yakutsk"
                longitude < 170.0 -> "Asia/Vladivostok"
                else -> "Asia/Magadan"
            }
            listOf("united states", "united states of america", "usa", "الولايات المتحدة").any { country.contains(it) } -> when {
                longitude < -150.0 -> "Pacific/Honolulu"
                longitude < -130.0 -> "America/Anchorage"
                longitude < -114.0 -> "America/Los_Angeles"
                longitude < -101.0 -> "America/Denver"
                longitude < -86.0 -> "America/Chicago"
                else -> "America/New_York"
            }
            listOf("canada", "كندا").any { country.contains(it) } -> when {
                longitude < -120.0 -> "America/Vancouver"
                longitude < -102.0 -> "America/Edmonton"
                longitude < -90.0 -> "America/Winnipeg"
                longitude < -68.0 -> "America/Toronto"
                else -> "America/Halifax"
            }
            listOf("mexico", "المكسيك").any { country.contains(it) } -> when {
                longitude < -112.0 -> "America/Tijuana"
                longitude < -104.0 -> "America/Chihuahua"
                longitude < -90.0 -> "America/Mexico_City"
                else -> "America/Cancun"
            }
            listOf("brazil", "البرازيل").any { country.contains(it) } -> when {
                longitude < -60.0 -> "America/Manaus"
                longitude < -45.0 -> "America/Cuiaba"
                longitude < -35.0 -> "America/Sao_Paulo"
                else -> "America/Noronha"
            }
            listOf("argentina", "الأرجنتين", "الارجنتين").any { country.contains(it) } -> "America/Argentina/Buenos_Aires"
            listOf("chile", "تشيلي").any { country.contains(it) } -> if (longitude < -100.0) "Pacific/Easter" else "America/Santiago"
            listOf("peru", "بيرو").any { country.contains(it) } -> "America/Lima"
            listOf("colombia", "كولومبيا").any { country.contains(it) } -> "America/Bogota"
            listOf("south africa", "جنوب أفريقيا", "جنوب افريقيا").any { country.contains(it) } -> "Africa/Johannesburg"
            listOf("nigeria", "نيجيريا").any { country.contains(it) } -> "Africa/Lagos"
            listOf("ghana", "غانا").any { country.contains(it) } -> "Africa/Accra"
            listOf("kenya", "كينيا").any { country.contains(it) } -> "Africa/Nairobi"
            listOf("ethiopia", "إثيوبيا", "اثيوبيا").any { country.contains(it) } -> "Africa/Addis_Ababa"
            listOf("uganda", "أوغندا", "اوغندا").any { country.contains(it) } -> "Africa/Kampala"
            listOf("tanzania", "تنزانيا").any { country.contains(it) } -> "Africa/Dar_es_Salaam"
            listOf("senegal", "السنغال").any { country.contains(it) } -> "Africa/Dakar"
            listOf("ivory coast", "côte d'ivoire", "ساحل العاج").any { country.contains(it) } -> "Africa/Abidjan"
            listOf("uzbekistan", "أوزبكستان", "اوزبكستان").any { country.contains(it) } -> "Asia/Tashkent"
            listOf("kazakhstan", "كازاخستان").any { country.contains(it) } -> if (longitude < 60.0) "Asia/Aqtau" else if (longitude < 75.0) "Asia/Aqtobe" else "Asia/Almaty"
            listOf("azerbaijan", "أذربيجان", "اذربيجان").any { country.contains(it) } -> "Asia/Baku"
            listOf("georgia", "جورجيا").any { country.contains(it) } -> "Asia/Tbilisi"
            listOf("armenia", "أرمينيا", "ارمينيا").any { country.contains(it) } -> "Asia/Yerevan"
            listOf("nepal", "نيبال").any { country.contains(it) } -> "Asia/Kathmandu"
            listOf("bhutan", "بوتان").any { country.contains(it) } -> "Asia/Thimphu"
            listOf("sri lanka", "سريلانكا", "سري لانكا").any { country.contains(it) } -> "Asia/Colombo"
            listOf("myanmar", "burma", "ميانمار").any { country.contains(it) } -> "Asia/Yangon"
            listOf("thailand", "تايلاند").any { country.contains(it) } -> "Asia/Bangkok"
            listOf("vietnam", "فيتنام").any { country.contains(it) } -> "Asia/Ho_Chi_Minh"
            listOf("china", "الصين").any { country.contains(it) } -> "Asia/Shanghai"
            listOf("japan", "اليابان").any { country.contains(it) } -> "Asia/Tokyo"
            listOf("south korea", "korea, republic", "كوريا الجنوبية").any { country.contains(it) } -> "Asia/Seoul"
            listOf("philippines", "الفلبين").any { country.contains(it) } -> "Asia/Manila"
            listOf("australia", "أستراليا", "استراليا").any { country.contains(it) } -> when {
                longitude < 129.0 -> "Australia/Perth"
                longitude < 133.0 -> "Australia/Darwin"
                longitude < 141.0 -> "Australia/Adelaide"
                else -> "Australia/Sydney"
            }
            listOf("new zealand", "نيوزيلندا").any { country.contains(it) } -> "Pacific/Auckland"
            listOf("fiji", "فيجي").any { country.contains(it) } -> "Pacific/Fiji"
            else -> null
        }
        if (namedZone != null) {
            runCatching { ZoneId.of(namedZone) }.getOrNull()?.let { return it }
        }
        val approximateOffsetMinutes = ((longitude / 3.75).roundToInt() * 15).coerceIn(-720, 840)
        return ZoneId.ofOffset("UTC", ZoneOffset.ofTotalSeconds(approximateOffsetMinutes * 60))
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
