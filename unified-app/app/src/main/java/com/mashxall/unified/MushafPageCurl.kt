package com.mashxall.unified

import android.graphics.Bitmap
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.PI
import kotlin.math.sin

/**
 * Geometry for a single-sheet page curl.
 *
 * The moving crease is slightly diagonal, and the sheet is compressed through
 * a curved band instead of rotating as one flat card. The next page is already
 * visible underneath the clipped sheet.
 */
internal fun mushafCurlSeamX(
    widthPx: Float,
    yFraction: Float,
    direction: Float,
    progress: Float
): Float {
    if (!widthPx.isFinite() || widthPx <= 0f ||
        !yFraction.isFinite() || !direction.isFinite() || direction == 0f ||
        !progress.isFinite()
    ) return 0f
    val p = progress.coerceIn(0f, 1f)
    val wave = sin(PI * p).toFloat()
    val skew = widthPx * 0.62f * wave * (yFraction.coerceIn(0f, 1f) - 0.5f)
    val start = if (direction > 0f) widthPx * (1f - p) else widthPx * p
    return (start + if (direction > 0f) skew else -skew).coerceIn(0f, widthPx)
}

internal fun mushafCurlBandWidth(widthPx: Float, progress: Float): Float {
    if (!widthPx.isFinite() || widthPx <= 0f || !progress.isFinite()) return 0f
    val p = progress.coerceIn(0f, 1f)
    return widthPx * 0.28f * sin(PI * p).toFloat()
}

internal fun mushafCurlBoundaryX(
    widthPx: Float,
    yFraction: Float,
    direction: Float,
    progress: Float
): Float {
    val seam = mushafCurlSeamX(widthPx, yFraction, direction, progress)
    val band = mushafCurlBandWidth(widthPx, progress)
    val sign = if (direction > 0f) 1f else -1f
    return (seam + sign * band).coerceIn(0f, widthPx.coerceAtLeast(0f))
}

/** Bend the printed sheet toward its fold while keeping the crease endpoints stable. */
internal fun mushafCurlWarpX(
    sourceX: Float,
    seamX: Float,
    bandWidth: Float,
    direction: Float
): Float {
    if (!sourceX.isFinite() || !seamX.isFinite() ||
        !bandWidth.isFinite() || bandWidth <= 0f ||
        !direction.isFinite() || direction == 0f
    ) return sourceX
    val sign = if (direction > 0f) 1f else -1f
    val distance = (sourceX - seamX) * sign
    if (distance <= 0f) return sourceX
    val t = (distance / bandWidth).coerceIn(0f, 1f)
    val curved = t - 0.24f * sin(PI * t).toFloat()
    return seamX + sign * bandWidth * curved
}

@Composable
internal fun MushafPageCurlOverlay(
    pageBitmap: Bitmap,
    progress: Float,
    direction: Float,
    darkMode: Boolean,
    sepiaMode: Boolean,
    modifier: Modifier = Modifier
) {
    val meshColumns = 36
    val meshRows = 48
    val vertices = remember(pageBitmap) {
        FloatArray((meshColumns + 1) * (meshRows + 1) * 2)
    }
    val pagePaint = remember(pageBitmap, darkMode, sepiaMode) {
        Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = when {
                darkMode -> ColorMatrixColorFilter(
                    floatArrayOf(
                        -1f, 0f, 0f, 0f, 255f,
                        0f, -1f, 0f, 0f, 255f,
                        0f, 0f, -1f, 0f, 255f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                sepiaMode -> ColorMatrixColorFilter(
                    floatArrayOf(
                        0.28106f, 0.55178f, 0.10716f, 0f, 15f,
                        0.25714f, 0.50482f, 0.09804f, 0f, 8f,
                        0.20631f, 0.40503f, 0.07866f, 0f, 2f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                else -> null
            }
        }
    }
    val shadingPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    }
    val shadowPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
    }
    val highlightPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
    }

    Canvas(modifier = modifier) {
        val width = pageBitmap.width.toFloat()
        val height = pageBitmap.height.toFloat()
        if (width <= 0f || height <= 0f || size.width <= 0f || size.height <= 0f) return@Canvas
        val fit = minOf(size.width / width, size.height / height)
        val left = (size.width - width * fit) / 2f
        val top = (size.height - height * fit) / 2f
        val p = progress.coerceIn(0f, 1f)
        val sign = if (direction > 0f) 1f else -1f
        val bandWidth = mushafCurlBandWidth(width, p)
        val wave = sin(PI * p).toFloat()
        val sampleCount = 72
        val clipPath = Path()
        val bandPath = Path()
        val seamPath = Path()
        val edgePath = Path()

        fun seamAt(y: Float): Float =
            mushafCurlSeamX(width, y / height, direction, p)

        fun boundaryAt(y: Float): Float =
            mushafCurlBoundaryX(width, y / height, direction, p)

        // Map a dense mesh so the Arabic page itself bends around the fold.
        for (row in 0..meshRows) {
            val sourceY = height * row / meshRows
            val yFraction = sourceY / height
            val seam = seamAt(sourceY)
            for (column in 0..meshColumns) {
                val sourceX = width * column / meshColumns
                val distance = (sourceX - seam) * sign
                val t = if (bandWidth > 0f) {
                    (distance / bandWidth).coerceIn(0f, 1f)
                } else {
                    0f
                }
                val curvedX = mushafCurlWarpX(sourceX, seam, bandWidth, direction)
                val depth = height * 0.04f * wave
                val curvedY = sourceY + depth * sin(PI * t).toFloat() *
                    sin(PI * yFraction).toFloat()
                val index = (row * (meshColumns + 1) + column) * 2
                vertices[index] = curvedX
                vertices[index + 1] = curvedY
            }
        }

        // The old page remains on one side of the moving, curved edge.
        // The new page is the image already rendered beneath this overlay.
        if (sign > 0f) {
            clipPath.moveTo(0f, 0f)
            clipPath.lineTo(boundaryAt(0f), 0f)
        } else {
            clipPath.moveTo(width, 0f)
            clipPath.lineTo(boundaryAt(0f), 0f)
        }
        for (index in 1..sampleCount) {
            val y = height * index / sampleCount
            clipPath.lineTo(boundaryAt(y), y)
        }
        if (sign > 0f) {
            clipPath.lineTo(0f, height)
        } else {
            clipPath.lineTo(width, height)
        }
        clipPath.close()

        seamPath.moveTo(seamAt(0f), 0f)
        edgePath.moveTo(boundaryAt(0f), 0f)
        bandPath.moveTo(seamAt(0f), 0f)
        bandPath.lineTo(boundaryAt(0f), 0f)
        for (index in 1..sampleCount) {
            val y = height * index / sampleCount
            seamPath.lineTo(seamAt(y), y)
            edgePath.lineTo(boundaryAt(y), y)
            bandPath.lineTo(boundaryAt(y), y)
        }
        for (index in sampleCount downTo 0) {
            val y = height * index / sampleCount
            bandPath.lineTo(seamAt(y), y)
        }
        bandPath.close()

        drawIntoCanvas { composeCanvas ->
            val canvas = composeCanvas.nativeCanvas
            val saveCount = canvas.save()
            canvas.translate(left, top)
            canvas.scale(fit, fit)

            canvas.save()
            canvas.clipPath(clipPath)
            canvas.drawBitmapMesh(
                pageBitmap,
                meshColumns,
                meshRows,
                vertices,
                0,
                null,
                0,
                pagePaint
            )
            canvas.restore()

            if (bandWidth > 1f && p > 0.005f && p < 0.995f) {
                val centerY = height * 0.5f
                val seamCenter = seamAt(centerY)
                val edgeCenter = boundaryAt(centerY)
                val shades = if (sign > 0f) {
                    intArrayOf(0x46000000, 0x76FFFFFF, 0x66000000)
                } else {
                    intArrayOf(0x66000000, 0x76FFFFFF, 0x46000000)
                }
                shadingPaint.shader = LinearGradient(
                    seamCenter,
                    0f,
                    edgeCenter,
                    0f,
                    shades,
                    floatArrayOf(0f, 0.52f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawPath(bandPath, shadingPaint)
                shadingPaint.shader = null

                // A broad, soft edge shadow and a narrow paper highlight give
                // the crease depth without the mirrored text of a card flip.
                shadowPaint.color = 0x26000000
                shadowPaint.strokeWidth = 42f
                canvas.drawPath(seamPath, shadowPaint)
                shadowPaint.color = 0x4A000000
                shadowPaint.strokeWidth = 16f
                canvas.drawPath(seamPath, shadowPaint)
                shadowPaint.color = 0x33000000
                shadowPaint.strokeWidth = 5f
                canvas.drawPath(edgePath, shadowPaint)

                highlightPaint.color = 0xCFFFFFFF.toInt()
                highlightPaint.strokeWidth = 2.4f
                canvas.drawPath(edgePath, highlightPaint)
                highlightPaint.color = 0x66FFFFFF
                highlightPaint.strokeWidth = 1.2f
                canvas.drawPath(seamPath, highlightPaint)
            }
            canvas.restoreToCount(saveCount)
        }
    }
}
