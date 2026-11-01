package com.mashxall.unified

/**
 * Resolve a horizontal swipe into a PDF page index. A sufficiently long left
 * swipe advances, a right swipe returns, and malformed/boundary gestures do nothing.
 */
internal fun pdfSwipeTarget(
    pageIndex: Int,
    pageCount: Int,
    horizontalDragPx: Float,
    thresholdPx: Float = 72f
): Int? {
    if (pageCount <= 0 || pageIndex !in 0 until pageCount) return null
    if (!horizontalDragPx.isFinite() || !thresholdPx.isFinite() || thresholdPx <= 0f) return null
    return when {
        horizontalDragPx < -thresholdPx && pageIndex < pageCount - 1 -> pageIndex + 1
        horizontalDragPx > thresholdPx && pageIndex > 0 -> pageIndex - 1
        else -> null
    }
}
