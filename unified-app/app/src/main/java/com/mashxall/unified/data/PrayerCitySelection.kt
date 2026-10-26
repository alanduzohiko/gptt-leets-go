package com.mashxall.unified.data

/**
 * Return only a deliberately chosen named city for a location-less notification
 * fallback. In particular, never silently select Erbil when first-run GPS and
 * manual city selection have not succeeded.
 */
internal fun explicitPrayerCityOrNull(savedName: String?, explicitlySelected: Boolean): String? {
    val city = savedName?.trim().orEmpty()
    if (city.isBlank()) return null
    if (city.equals("Erbil", ignoreCase = true) && !explicitlySelected) return null
    return city
}
