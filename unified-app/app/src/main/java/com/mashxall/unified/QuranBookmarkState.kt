package com.mashxall.unified

internal data class QuranBookmarkToggleResult(
    val bookmarks: Set<String>,
    val isBookmarked: Boolean
)

internal fun quranBookmarkKey(surah: Int, ayah: Int): String = "$surah:$ayah"

/** Add a missing bookmark or remove an existing one without changing any other saved verse. */
internal fun toggleQuranBookmark(
    existing: Set<String>,
    surah: Int,
    ayah: Int
): QuranBookmarkToggleResult {
    require(surah in 1..114) { "Surah number must be between 1 and 114." }
    require(ayah >= 1) { "Ayah number must be positive." }
    val updated = existing.toMutableSet()
    val key = quranBookmarkKey(surah, ayah)
    val isBookmarked = if (updated.remove(key)) {
        false
    } else {
        updated.add(key)
        true
    }
    return QuranBookmarkToggleResult(updated.toSet(), isBookmarked)
}
