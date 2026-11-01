package com.mashxall.unified.data

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Geographic helpers for selecting the nearest original Salawati timetable.
 *
 * Prayer times themselves must come from the bundled source timetable. This
 * utility deliberately contains no astronomical prayer-time calculation.
 */
object PrayerLocationMath {
    fun distanceKm(latitude1: Double, longitude1: Double, latitude2: Double, longitude2: Double): Double {
        val dLat = Math.toRadians(latitude2 - latitude1)
        val dLon = Math.toRadians(longitude2 - longitude1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(latitude1)) * cos(Math.toRadians(latitude2)) *
            sin(dLon / 2) * sin(dLon / 2)
        return 6371.0088 * 2.0 * asin(sqrt(a.coerceIn(0.0, 1.0)))
    }
}
