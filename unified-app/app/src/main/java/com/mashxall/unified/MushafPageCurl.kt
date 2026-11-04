package com.mashxall.unified

import android.graphics.Bitmap
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Convert horizontal finger travel to a continuous turn amount. */
internal fun mushafCurlDragProgress(horizontalDragPx: Float, viewportWidthPx: Float): Float {
    if (!horizontalDragPx.isFinite() || !viewportWidthPx.isFinite() || viewportWidthPx <= 0f) return 0f
    return (kotlin.math.abs(horizontalDragPx) / viewportWidthPx).coerceIn(0f, 0.985f)
}

private const val MUSHAF_CURL_RADIUS_FRACTION = 0.095f

/**
 * The next-page gesture sweeps a moving fold across one paper leaf. The reverse
 * gesture mirrors the sweep. The fold line follows the initial finger height,
 * reproducing the diagonal, twisted curl visible in the reference clip.
 */
internal fun mushafCurlFoldPositionFraction(
    progress: Float,
    direction: Float,
    yFraction: Float,
    anchorYFraction: Float = 0.5f
): Float {
    if (!progress.isFinite() || !direction.isFinite() || direction == 0f ||
        !yFraction.isFinite() || !anchorYFraction.isFinite()
    ) return if (direction < 0f) 1f else 0f

    val p = progress.coerceIn(0f, 1f)
    val base = if (direction < 0f) 1f - p else p
    val bend = sin(PI * p).toFloat()
    val turnSign = if (direction < 0f) 1f else -1f
    val diagonal = (anchorYFraction.coerceIn(0f, 1f) - yFraction.coerceIn(0f, 1f)) *
        0.26f * bend * turnSign
    return (base + diagonal).coerceIn(-0.26f, 1.26f)
}

/**
 * Local paper angle around the moving cylindrical fold.
 *
 * Values before the fold stay flat; the next 180 degrees form the visible curl;
 * points beyond the curl are the turned-over reverse of the same paper leaf.
 */
internal fun mushafCurlSurfaceAngle(
    progress: Float,
    direction: Float,
    xFraction: Float,
    yFraction: Float,
    anchorYFraction: Float = 0.5f
): Float {
    if (!progress.isFinite() || !direction.isFinite() || direction == 0f ||
        !xFraction.isFinite() || !yFraction.isFinite() || !anchorYFraction.isFinite()
    ) return 0f

    val fold = mushafCurlFoldPositionFraction(progress, direction, yFraction, anchorYFraction)
    val distance = if (direction < 0f) xFraction - fold else fold - xFraction
    return (distance / MUSHAF_CURL_RADIUS_FRACTION).coerceIn(0f, PI.toFloat())
}

/** Project one paper point over a 180-degree cylindrical curl. */
internal fun mushafCurlProjectedX(
    widthPx: Float,
    sourceX: Float,
    progress: Float,
    direction: Float,
    xFraction: Float = if (widthPx > 0f) sourceX / widthPx else 0f,
    yFraction: Float = 0.5f,
    anchorYFraction: Float = 0.5f
): Float {
    if (!widthPx.isFinite() || widthPx <= 0f || !sourceX.isFinite() ||
        !progress.isFinite() || !direction.isFinite() || direction == 0f
    ) return sourceX

    val p = progress.coerceIn(0f, 1f)
    val foldX = mushafCurlFoldPositionFraction(p, direction, yFraction, anchorYFraction) * widthPx
    val distance = if (direction < 0f) sourceX - foldX else foldX - sourceX
    if (distance <= 0f) return sourceX

    val radius = maxOf(widthPx * MUSHAF_CURL_RADIUS_FRACTION, 20f)
    val arcLength = PI.toFloat() * radius
    val angle = (distance / radius).coerceIn(0f, PI.toFloat())
    val travelSign = direction
    val wrappedX = if (distance <= arcLength) {
        foldX + travelSign * radius * sin(angle).toFloat()
    } else {
        foldX + direction * (distance - arcLength)
    }

    val depth = if (distance <= arcLength) radius * (1f - cos(angle).toFloat()) else 0f
    val cameraDistance = widthPx * 3.2f
    val perspective = (cameraDistance / (cameraDistance - depth))
        .coerceIn(0.82f, 1.24f)
    val edgeTwist = travelSign * radius * 0.045f * sin(angle).toFloat() *
        (yFraction.coerceIn(0f, 1f) - 0.5f)
    return foldX + (wrappedX - foldX) * perspective + edgeTwist
}

/** Vertical perspective and corner twist keep the curled sheet from looking flat. */
internal fun mushafCurlProjectedY(
    widthPx: Float,
    heightPx: Float,
    sourceX: Float,
    sourceY: Float,
    progress: Float,
    direction: Float,
    anchorYFraction: Float = 0.5f
): Float {
    if (!widthPx.isFinite() || widthPx <= 0f || !heightPx.isFinite() || heightPx <= 0f ||
        !sourceX.isFinite() || !sourceY.isFinite() || !progress.isFinite() ||
        !direction.isFinite() || direction == 0f
    ) return sourceY

    val xFraction = (sourceX / widthPx).coerceIn(0f, 1f)
    val yFraction = (sourceY / heightPx).coerceIn(0f, 1f)
    val p = progress.coerceIn(0f, 1f)
    val fold = mushafCurlFoldPositionFraction(p, direction, yFraction, anchorYFraction)
    val distance = if (direction < 0f) {
        sourceX - fold * widthPx
    } else {
        fold * widthPx - sourceX
    }
    val radius = maxOf(widthPx * MUSHAF_CURL_RADIUS_FRACTION, 20f)
    val angle = (distance / radius).coerceIn(0f, PI.toFloat())
    val arcLength = PI.toFloat() * radius
    val depth = if (distance in 0f..arcLength) radius * (1f - cos(angle).toFloat()) else 0f
    val cameraDistance = widthPx * 3.2f
    val perspective = (cameraDistance / (cameraDistance - depth))
        .coerceIn(0.82f, 1.24f)
    val verticalTwist = direction * radius * 0.22f * sin(angle).toFloat() *
        (yFraction - anchorYFraction.coerceIn(0f, 1f))
    val verticalBow = heightPx * 0.012f * sin(angle).toFloat() *
        (xFraction - fold)
    return heightPx * 0.5f + (sourceY - heightPx * 0.5f) * perspective +
        verticalTwist + verticalBow
}

/**
 * Source X coordinate where the two printed faces meet visually. On the curl
 * cylinder this is the 90-degree point, where the sheet is edge-on.
 */
internal fun mushafCurlFaceBoundaryX(
    widthPx: Float,
    heightPx: Float,
    yFraction: Float,
    progress: Float,
    direction: Float,
    anchorYFraction: Float = 0.5f
): Float {
    if (!widthPx.isFinite() || widthPx <= 0f || !heightPx.isFinite() || heightPx <= 0f ||
        !yFraction.isFinite() || !progress.isFinite() || !direction.isFinite() || direction == 0f
    ) return 0f

    val p = progress.coerceIn(0f, 1f)
    val leftAngle = mushafCurlSurfaceAngle(p, direction, 0f, yFraction, anchorYFraction)
    val rightAngle = mushafCurlSurfaceAngle(p, direction, 1f, yFraction, anchorYFraction)
    val halfTurn = PI.toFloat() * 0.5f
    if (maxOf(leftAngle, rightAngle) <= halfTurn) {
        return if (direction < 0f) widthPx else 0f
    }
    if (minOf(leftAngle, rightAngle) >= halfTurn) {
        return if (direction < 0f) 0f else widthPx
    }

    var low = 0f
    var high = 1f
    val increasing = rightAngle > leftAngle
    repeat(24) {
        val middle = (low + high) * 0.5f
        val angle = mushafCurlSurfaceAngle(p, direction, middle, yFraction, anchorYFraction)
        if ((angle < halfTurn) == increasing) low = middle else high = middle
    }
    return ((low + high) * 0.5f) * widthPx
}

@Composable
internal fun MushafPageCurlOverlay(
    pageBitmap: Bitmap,
    backPageBitmap: Bitmap,
    progress: Float,
    direction: Float,
    anchorYFraction: Float = 0.5f,
    darkMode: Boolean,
    sepiaMode: Boolean,
    modifier: Modifier = Modifier
) {
    val meshColumns = 48
    val meshRows = 64
    val vertexCount = (meshColumns + 1) * (meshRows + 1) * 2
    val frontVertices = remember(pageBitmap, backPageBitmap) { FloatArray(vertexCount) }
    val backVertices = remember(pageBitmap, backPageBitmap) { FloatArray(vertexCount) }

    // The reverse print is mirrored in texture space. The projected paper mesh
    // also turns over, so the reader sees normally oriented Arabic on the back.
    val mirroredBackBitmap = remember(backPageBitmap) {
        val matrix = Matrix().apply {
            setScale(-1f, 1f)
            postTranslate(backPageBitmap.width.toFloat(), 0f)
        }
        Bitmap.createBitmap(
            backPageBitmap,
            0,
            0,
            backPageBitmap.width,
            backPageBitmap.height,
            matrix,
            true
        )
    }
    DisposableEffect(mirroredBackBitmap) {
        onDispose {
            if (!mirroredBackBitmap.isRecycled) mirroredBackBitmap.recycle()
        }
    }

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
    val backPaint = remember(mirroredBackBitmap, darkMode, sepiaMode) {
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
    val edgePaint = remember {
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
    val paths = remember(pageBitmap, backPageBitmap) {
        arrayOf(Path(), Path(), Path(), Path())
    }

    Canvas(modifier = modifier) {
        val width = pageBitmap.width.toFloat()
        val height = pageBitmap.height.toFloat()
        if (width <= 0f || height <= 0f || size.width <= 0f || size.height <= 0f ||
            !direction.isFinite() || direction == 0f || !progress.isFinite()
        ) return@Canvas

        val fit = minOf(size.width / width, size.height / height)
        val left = (size.width - width * fit) * 0.5f
        val top = (size.height - height * fit) * 0.5f
        val p = progress.coerceIn(0f, 1f)
        val backFaceOnRight = direction < 0f
        val samples = 96

        fun projectX(x: Float, y: Float): Float =
            mushafCurlProjectedX(
                width, x, p, direction,
                xFraction = x / width,
                yFraction = y / height,
                anchorYFraction = anchorYFraction
            )

        fun projectY(x: Float, y: Float): Float =
            mushafCurlProjectedY(width, height, x, y, p, direction, anchorYFraction)

        // Build a perspective mesh of the whole leaf. Each vertex bends in both
        // X and Y; its angle is solved from the slow finger-led progress.
        for (row in 0..meshRows) {
            val sourceY = height * row / meshRows
            for (column in 0..meshColumns) {
                val sourceX = width * column / meshColumns
                val index = (row * (meshColumns + 1) + column) * 2
                val projectedX = projectX(sourceX, sourceY)
                val projectedY = projectY(sourceX, sourceY)
                // Mesh vertices and clip paths share screen-pixel coordinates.
                frontVertices[index] = left + projectedX * fit
                frontVertices[index + 1] = top + projectedY * fit
                backVertices[index] = left + projectedX * fit
                backVertices[index + 1] = top + projectedY * fit
            }
        }

        val frontPath = paths[0].apply { reset() }
        val backPath = paths[1].apply { reset() }
        val creasePath = paths[2].apply { reset() }
        val silhouettePath = paths[3].apply { reset() }

        fun boundaryAt(y: Float): Float =
            mushafCurlFaceBoundaryX(width, height, y / height, p, direction, anchorYFraction)

        fun point(x: Float, y: Float) {
            val px = left + projectX(x, y) * fit
            val py = top + projectY(x, y) * fit
            silhouettePath.lineTo(px, py)
        }

        // The two visible faces exactly share the projected fold curve.
        // Their outer edges are sampled from the same mesh so the clip masks
        // do not cut across curved corners with straight, artificial chords.
        if (backFaceOnRight) {
            // Advancing: front is left of the fold, reverse is right of it.
            frontPath.moveTo(left + projectX(0f, 0f) * fit, top + projectY(0f, 0f) * fit)
            frontPath.lineTo(
                left + projectX(boundaryAt(0f), 0f) * fit,
                top + projectY(boundaryAt(0f), 0f) * fit
            )
            backPath.moveTo(
                left + projectX(boundaryAt(0f), 0f) * fit,
                top + projectY(boundaryAt(0f), 0f) * fit
            )
            backPath.lineTo(left + projectX(width, 0f) * fit, top + projectY(width, 0f) * fit)

            for (i in 1..samples) {
                val y = height * i / samples
                val boundary = boundaryAt(y)
                frontPath.lineTo(left + projectX(boundary, y) * fit, top + projectY(boundary, y) * fit)
                backPath.lineTo(left + projectX(width, y) * fit, top + projectY(width, y) * fit)
            }
            frontPath.lineTo(left + projectX(0f, height) * fit, top + projectY(0f, height) * fit)
            backPath.lineTo(
                left + projectX(boundaryAt(height), height) * fit,
                top + projectY(boundaryAt(height), height) * fit
            )
            for (i in samples - 1 downTo 0) {
                val y = height * i / samples
                frontPath.lineTo(left + projectX(0f, y) * fit, top + projectY(0f, y) * fit)
                backPath.lineTo(
                    left + projectX(boundaryAt(y), y) * fit,
                    top + projectY(boundaryAt(y), y) * fit
                )
            }
        } else {
            // Returning: front is right of the fold, reverse is left of it.
            frontPath.moveTo(
                left + projectX(boundaryAt(0f), 0f) * fit,
                top + projectY(boundaryAt(0f), 0f) * fit
            )
            frontPath.lineTo(left + projectX(width, 0f) * fit, top + projectY(width, 0f) * fit)
            backPath.moveTo(left + projectX(0f, 0f) * fit, top + projectY(0f, 0f) * fit)
            backPath.lineTo(
                left + projectX(boundaryAt(0f), 0f) * fit,
                top + projectY(boundaryAt(0f), 0f) * fit
            )

            for (i in 1..samples) {
                val y = height * i / samples
                val boundary = boundaryAt(y)
                frontPath.lineTo(left + projectX(width, y) * fit, top + projectY(width, y) * fit)
                backPath.lineTo(left + projectX(boundary, y) * fit, top + projectY(boundary, y) * fit)
            }
            frontPath.lineTo(
                left + projectX(boundaryAt(height), height) * fit,
                top + projectY(boundaryAt(height), height) * fit
            )
            backPath.lineTo(left + projectX(0f, height) * fit, top + projectY(0f, height) * fit)
            for (i in samples - 1 downTo 0) {
                val y = height * i / samples
                frontPath.lineTo(
                    left + projectX(boundaryAt(y), y) * fit,
                    top + projectY(boundaryAt(y), y) * fit
                )
                backPath.lineTo(left + projectX(0f, y) * fit, top + projectY(0f, y) * fit)
            }
        }
        frontPath.close()
        backPath.close()

        // The outer edge and fold line are traced through the same projected mesh
        // so their shadows stay attached to the moving sheet.
        silhouettePath.moveTo(left + projectX(0f, 0f) * fit, top + projectY(0f, 0f) * fit)
        for (i in 1..meshRows) {
            val y = height * i / meshRows
            point(0f, y)
        }
        for (i in 1..meshColumns) {
            val x = width * i / meshColumns
            point(x, height)
        }
        for (i in meshRows downTo 0) {
            val y = height * i / meshRows
            point(width, y)
        }
        for (i in meshColumns downTo 0) {
            val x = width * i / meshColumns
            point(x, 0f)
        }
        silhouettePath.close()

        creasePath.moveTo(
            left + projectX(boundaryAt(0f), 0f) * fit,
            top + projectY(boundaryAt(0f), 0f) * fit
        )
        for (i in 1..samples) {
            val y = height * i / samples
            creasePath.lineTo(
                left + projectX(boundaryAt(y), y) * fit,
                top + projectY(boundaryAt(y), y) * fit
            )
        }

        drawIntoCanvas { composeCanvas ->
            val canvas = composeCanvas.nativeCanvas
            val saved = canvas.save()

            // Contact shadow and thin paper edge behind both printed faces.
            shadowPaint.color = 0x50000000
            shadowPaint.strokeWidth = 30f * fit
            canvas.drawPath(silhouettePath, shadowPaint)
            edgePaint.color = 0x9A82745F.toInt()
            edgePaint.strokeWidth = 2.1f * fit
            canvas.drawPath(silhouettePath, edgePaint)

            // Draw the unturned paper first. The curled reverse then travels over it,
            // just as a lifted physical leaf passes across the page beneath.
            canvas.save()
            canvas.clipPath(frontPath)
            canvas.drawBitmapMesh(
                pageBitmap, meshColumns, meshRows,
                frontVertices, 0, null, 0, frontPaint
            )
            canvas.restore()

            canvas.save()
            canvas.clipPath(backPath)
            canvas.drawBitmapMesh(
                mirroredBackBitmap, meshColumns, meshRows,
                backVertices, 0, null, 0, backPaint
            )
            canvas.restore()

            val wave = sin(PI * p).toFloat()
            if (wave > 0.01f) {
                // A soft broad shadow, a deep crease, and a narrow paper highlight
                // reproduce the edge lighting seen in the reference frames.
                shadowPaint.color = 0x44000000
                shadowPaint.strokeWidth = 46f * fit * wave
                canvas.drawPath(creasePath, shadowPaint)

                shadowPaint.color = 0x72000000
                shadowPaint.strokeWidth = 12f * fit * wave
                canvas.drawPath(creasePath, shadowPaint)

                val centerY = height * 0.5f
                val creaseX = projectX(boundaryAt(centerY), centerY) * fit + left
                foldPaint.shader = LinearGradient(
                    creaseX - 38f * fit,
                    top,
                    creaseX + 38f * fit,
                    top,
                    intArrayOf(0x10000000, 0x58FFFFFF, 0x5A000000),
                    floatArrayOf(0f, 0.48f, 1f),
                    Shader.TileMode.CLAMP
                )
                val foldBand = Path()
                foldBand.moveTo(
                    left + projectX((boundaryAt(0f) - if (direction > 0f) 52f else -52f).coerceIn(0f, width), 0f) * fit,
                    top + projectY((boundaryAt(0f) - if (forward) 52f else -52f).coerceIn(0f, width), 0f) * fit
                )
                for (i in 1..samples) {
                    val y = height * i / samples
                    val x = (boundaryAt(y) - if (direction > 0f) 52f else -52f).coerceIn(0f, width)
                    foldBand.lineTo(left + projectX(x, y) * fit, top + projectY(x, y) * fit)
                }
                for (i in samples downTo 0) {
                    val y = height * i / samples
                    val x = boundaryAt(y)
                    foldBand.lineTo(left + projectX(x, y) * fit, top + projectY(x, y) * fit)
                }
                foldBand.close()
                canvas.drawPath(foldBand, foldPaint)
                foldPaint.shader = null

                highlightPaint.color = 0xE8FFFFFF.toInt()
                highlightPaint.strokeWidth = 2.4f * fit
                canvas.drawPath(creasePath, highlightPaint)
                highlightPaint.color = 0x7AFFFFFF
                highlightPaint.strokeWidth = 1.1f * fit
                canvas.drawPath(silhouettePath, highlightPaint)
            }

            canvas.restoreToCount(saved)
        }
    }
}
