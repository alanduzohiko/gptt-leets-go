package com.mashxall.unified

import android.animation.ValueAnimator
import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Shared native touch response for tappable surfaces throughout Mashxall.
 *
 * The spring scale is deliberately small so cards feel tactile without shifting
 * neighbouring content. Material's current indication supplies the ripple, and
 * haptics can be opted out when a screen already provides richer task-specific
 * feedback (for example, Quran navigation or Tasbih counting).
 */
internal fun Modifier.premiumClickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    hapticFeedback: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (enabled && pressed) 0.955f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "Mashxall native press scale"
    )
    val haptics = LocalHapticFeedback.current

    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }.clickable(
        interactionSource = interactionSource,
        indication = LocalIndication.current,
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role
    ) {
        if (hapticFeedback) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onClick()
    }
}


/**
 * A lightweight shimmer placeholder shown only while source data is loading.
 * It contains no placeholder text or pretend content; its motion stops when the
 * loading state leaves composition.
 */
@Composable
internal fun PremiumLoadingSkeleton(
    modifier: Modifier = Modifier,
    accent: Color
) {
    val context = LocalContext.current
    val motionAllowed = remember(context) { systemAllowsDecorativeMotion(context) }
    val shimmerOffset = if (motionAllowed) {
        val transition = rememberInfiniteTransition(label = "Mashxall loading shimmer")
        val animatedOffset by transition.animateFloat(
            initialValue = -240f,
            targetValue = 720f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1350, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "Mashxall shimmer position"
        )
        animatedOffset
    } else {
        // No running animation when Android's animator scale is disabled.
        -240f
    }
    val shimmer = Brush.linearGradient(
        colors = listOf(
            accent.copy(alpha = 0.045f),
            accent.copy(alpha = 0.20f),
            accent.copy(alpha = 0.045f)
        ),
        start = Offset(shimmerOffset, 0f),
        end = Offset(shimmerOffset + 240f, 90f)
    )

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(3) { index ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(accent.copy(alpha = 0.035f), RoundedCornerShape(18.dp))
                    .border(1.dp, accent.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                    .padding(14.dp)
            ) {
                Box(
                    Modifier.fillMaxWidth(if (index == 0) 0.62f else 0.76f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(shimmer)
                )
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier.fillMaxWidth(0.92f).height(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(shimmer)
                )
                Spacer(Modifier.height(7.dp))
                Box(
                    Modifier.fillMaxWidth(if (index == 2) 0.54f else 0.71f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(shimmer)
                )
            }
        }
    }
}


/** Keep list entry motion brief, capped, and disabled with Android's motion scale. */
internal fun staggerDelayMillis(index: Int): Long =
    index.coerceIn(0, 8) * 28L

private fun systemAllowsDecorativeMotion(context: Context): Boolean {
    val animatorEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        ValueAnimator.areAnimatorsEnabled()
    } else {
        true
    }
    val durationScale = runCatching {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )
    }.getOrDefault(1f)
    return animatorEnabled && durationScale > 0f
}

/**
 * Entrance motion for real content rows: fade, a restrained upward settle and
 * a small spring scale. Stable keys make the animation follow the item rather
 * than its position when search results are reordered.
 */
@Composable
internal fun Modifier.staggeredEntrance(
    itemKey: Any,
    index: Int,
    enabled: Boolean = true
): Modifier {
    val context = LocalContext.current
    val motionAllowed = remember(context) { systemAllowsDecorativeMotion(context) }
    val progress = remember(itemKey) {
        Animatable(if (motionAllowed && enabled) 0f else 1f)
    }

    LaunchedEffect(itemKey, index, motionAllowed, enabled) {
        if (!motionAllowed || !enabled) {
            progress.snapTo(1f)
        } else {
            progress.snapTo(0f)
            delay(staggerDelayMillis(index))
            progress.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    return this.graphicsLayer {
        val fraction = progress.value.coerceIn(0f, 1f)
        alpha = fraction
        translationY = (1f - fraction) * 18.dp.toPx()
        scaleX = 0.965f + fraction * 0.035f
        scaleY = 0.965f + fraction * 0.035f
    }
}
