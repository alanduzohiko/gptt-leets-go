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
import kotlin.math.abs
import kotlin.math.sin

/** Live finger travel becomes the page curl amount, capped just before the edge. */
internal fun mushafCurlDragProgress(horizontalDragPx: Float, viewportWidthPx: Float): Float {
    if (!horizontalDragPx.isFinite() || !viewportWidthPx.isFinite() || viewportWidthPx <= 0f) return 0f
    return (abs(horizontalDragPx) / viewportWidthPx).coerceIn(0f, 0.985f)
}

/** Diagonal fold line that travels across the page with the user's drag. */
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
    return (start + if (direction > 0f) -skew else skew).coerceIn(0f, widthPx)
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

/** Compress both printed faces toward the rolling edge, revealing the next leaf underneath. */
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
    backPageBitmap: Bitmap,
    progress: Float,
    direction: Float,
    darkMode: Boolean,
    sepiaMode: Boolean,
    modifier: Modifier = Modifier
) {
    val meshColumns = 40
    val meshRows = 56
    val meshFloatCount = (meshColumns + 1) * (meshRows + 1) * 2
    val meshes = remember(pageBitmap, backPageBitmap) {
        FloatArray(meshFloatCount) to FloatArray(meshFloatCount)
    }
    val frontVertices = meshes.first
    val backVertices = meshes.second

    val frontPaint = remember(pageBitmap, darkMode, sepiaMode) {
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
    val backPaint = remember(backPageBitmap, darkMode, sepiaMode) {
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
    val foldPaint = remember {
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
        if (width <= 0f || height <= 0f || size.width <= 0f || size.height <= 0f ||
            !direction.isFinite() || direction == 0f || !progress.isFinite()
        ) return@Canvas

        val fit = minOf(size.width / width, size.height / height)
        val left = (size.width - width * fit) / 2f
        val top = (size.height - height * fit) / 2f
        val p = progress.coerceIn(0f, 1f)
        val forward = direction < 0f
        val bandWidth = mushafCurlBandWidth(width, p)
        val curlDepth = height * 0.052f * sin(PI * p).toFloat()
        val samples = 96

        fun seamAt(y: Float) = mushafCurlSeamX(width, y / height, direction, p)
        fun boundaryAt(y: Float) = mushafCurlBoundaryX(width, y / height, direction, p)

        val frontPath = Path()
        val backPath = Path()
        val foldPath = Path()
        val seamPath = Path()
        val boundaryPath = Path()

        if (forward) {
            frontPath.moveTo(seamAt(0f), 0f)
            frontPath.lineTo(width, 0f)
            frontPath.lineTo(width, height)
            for (i in samples downTo 0) {
                val y = height * i / samples
                frontPath.lineTo(seamAt(y), y)
            }

            backPath.moveTo(0f, 0f)
            for (i in 0..samples) {
                val y = height * i / samples
                backPath.lineTo(boundaryAt(y), y)
            }
            backPath.lineTo(0f, height)
        } else {
            frontPath.moveTo(0f, 0f)
            frontPath.lineTo(seamAt(0f), 0f)
            for (i in 0..samples) {
                val y = height * i / samples
                frontPath.lineTo(seamAt(y), y)
            }
            frontPath.lineTo(0f, height)

            backPath.moveTo(boundaryAt(0f), 0f)
            backPath.lineTo(width, 0f)
            backPath.lineTo(width, height)
            for (i in samples downTo 0) {
                val y = height * i / samples
                backPath.lineTo(boundaryAt(y), y)
            }
        }
        frontPath.close()
        backPath.close()

        foldPath.moveTo(boundaryAt(0f), 0f)
        foldPath.lineTo(seamAt(0f), 0f)
        for (i in 1..samples) {
            val y = height * i / samples
            foldPath.lineTo(seamAt(y), y)
        }
        for (i in samples downTo 0) {
            val y = height * i / samples
            foldPath.lineTo(boundaryAt(y), y)
        }
        foldPath.close()

        seamPath.moveTo(seamAt(0f), 0f)
        boundaryPath.moveTo(boundaryAt(0f), 0f)
        for (i in 1..samples) {
            val y = height * i / samples
            seamPath.lineTo(seamAt(y), y)
            boundaryPath.lineTo(boundaryAt(y), y)
        }

        // Make the printed front and reverse faces bend toward the fold from
        // opposite sides. The under-page remains a separate, visible layer.
        for (row in 0..meshRows) {
            val sourceY = height * row / meshRows
            val yFraction = sourceY / height
            val seam = seamAt(sourceY)
            for (column in 0..meshColumns) {
                val sourceX = width * column / meshColumns
                val frontX = mushafCurlWarpX(sourceX, seam, bandWidth, -direction)
                val backX = mushafCurlWarpX(sourceX, seam, bandWidth, direction)
                val t = if (bandWidth > 0f) (abs(sourceX - seam) / bandWidth).coerceIn(0f, 1f) else 0f
                val lift = curlDepth * sin(PI * t).toFloat() * sin(PI * yFraction).toFloat()
                val index = (row * (meshColumns + 1) + column) * 2
                frontVertices[index] = frontX
                frontVertices[index + 1] = sourceY + lift
                backVertices[index] = backX
                backVertices[index + 1] = sourceY + lift
            }
        }

        drawIntoCanvas { composeCanvas ->
            val canvas = composeCanvas.nativeCanvas
            val saved = canvas.save()
            canvas.translate(left, top)
            canvas.scale(fit, fit)

            // The reverse side is real page content, not a blank white card.
            canvas.save()
            canvas.clipPath(backPath)
            canvas.drawBitmapMesh(backPageBitmap, meshColumns, meshRows, backVertices, 0, null, 0, backPaint)
            canvas.restore()

            canvas.save()
            canvas.clipPath(frontPath)
            canvas.drawBitmapMesh(pageBitmap, meshColumns, meshRows, frontVertices, 0, null, 0, frontPaint)
            canvas.restore()

            if (bandWidth > 1f && p > 0.003f && p < 0.997f) {
                val centerY = height * 0.5f
                val boundaryX = boundaryAt(centerY)
                val seamX = seamAt(centerY)
                val colors = if (forward) {
                    intArrayOf(0x3A000000, 0x28FFFFFF, 0x52000000)
                } else {
                    intArrayOf(0x52000000, 0x28FFFFFF, 0x3A000000)
                }
                foldPaint.shader = LinearGradient(
                    boundaryX, 0f, seamX, 0f,
                    colors, floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP
                )
                canvas.drawPath(foldPath, foldPaint)
                foldPaint.shader = null

                shadowPaint.color = 0x31000000
                shadowPaint.strokeWidth = 40f
                canvas.drawPath(boundaryPath, shadowPaint)
                shadowPaint.color = 0x4A000000
                shadowPaint.strokeWidth = 13f
                canvas.drawPath(seamPath, shadowPaint)
                highlightPaint.color = 0xDFFFFFFF.toInt()
                highlightPaint.strokeWidth = 2.8f
                canvas.drawPath(boundaryPath, highlightPaint)
                highlightPaint.color = 0x78FFFFFF
                highlightPaint.strokeWidth = 1.4f
                canvas.drawPath(seamPath, highlightPaint)
            }
            canvas.restoreToCount(saved)
        }
    }
}
