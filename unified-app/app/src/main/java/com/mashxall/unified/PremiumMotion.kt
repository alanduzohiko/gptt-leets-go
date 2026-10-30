package com.mashxall.unified

import android.animation.ValueAnimator
import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
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
    val context = LocalContext.current
    val motionAllowed = remember(context) { systemAllowsDecorativeMotion(context) }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (enabled && pressed) 0.942f else 1f,
        animationSpec = if (motionAllowed) spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ) else snap(),
        label = "Mashxall native press scale"
    )
    val haptics = LocalHapticFeedback.current

    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
        translationY = if (motionAllowed && enabled && pressed) 1.7.dp.toPx() else 0f
        rotationZ = if (motionAllowed && enabled && pressed) 0.28f else 0f
        shadowElevation = if (motionAllowed && enabled && pressed) 2.5.dp.toPx() else 0f
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
    index.coerceIn(0, 8) * 40L

internal fun systemAllowsDecorativeMotion(context: Context): Boolean {
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
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    return this.graphicsLayer {
        val fraction = progress.value.coerceIn(0f, 1f)
        alpha = fraction
        translationY = (1f - fraction) * 24.dp.toPx()
        scaleX = 0.94f + fraction * 0.06f
        scaleY = 0.94f + fraction * 0.06f
    }
}


/** Slow ambient light fields add depth without competing with reading content. */
@Composable
internal fun PremiumAmbientBackdrop(
    modifier: Modifier = Modifier,
    accent: Color,
    dark: Boolean
) {
    val context = LocalContext.current
    val motionAllowed = remember(context) { systemAllowsDecorativeMotion(context) }
    val drift = if (motionAllowed) {
        val transition = rememberInfiniteTransition(label = "Mashxall ambient drift")
        val x by transition.animateFloat(
            initialValue = -22f, targetValue = 24f,
            animationSpec = infiniteRepeatable(animation = tween(8200, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
            label = "Mashxall ambient horizontal drift"
        )
        val y by transition.animateFloat(
            initialValue = 16f, targetValue = -18f,
            animationSpec = infiniteRepeatable(animation = tween(10400, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
            label = "Mashxall ambient vertical drift"
        )
        x to y
    } else {
        0f to 0f
    }
    val glowAlpha = if (dark) 0.13f else 0.07f
    Box(modifier = modifier) {
        Box(
            Modifier.align(Alignment.TopEnd).offset(x = drift.first.dp + 42.dp, y = drift.second.dp - 62.dp)
                .size(250.dp).clip(CircleShape)
                .background(Brush.radialGradient(listOf(accent.copy(alpha = glowAlpha), Color.Transparent)))
        )
        Box(
            Modifier.align(Alignment.BottomStart).offset(x = -82.dp - drift.first.dp, y = 72.dp - drift.second.dp)
                .size(220.dp).clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFF3DA987).copy(alpha = if (dark) 0.12f else 0.055f), Color.Transparent)))
        )
        Box(
            Modifier.align(Alignment.CenterEnd).offset(x = 54.dp + drift.first.dp, y = -104.dp + drift.second.dp)
                .size(104.dp).clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFF6DA8C7).copy(alpha = if (dark) 0.055f else 0.035f), Color.Transparent)))
        )
    }
}

/** A reusable layered icon medallion. Continuous pulsing is opt-in and motion-aware. */
@Composable
internal fun PremiumIconMedallion(
    icon: ImageVector,
    tint: Color,
    size: Dp = 48.dp,
    pulse: Boolean = false,
    contentDescription: String? = null
) {
    val context = LocalContext.current
    val motionAllowed = remember(context) { systemAllowsDecorativeMotion(context) }
    val pulseScale = if (pulse && motionAllowed) {
        val transition = rememberInfiniteTransition(label = "Mashxall medallion pulse")
        val value by transition.animateFloat(
            initialValue = 0.955f, targetValue = 1.045f,
            animationSpec = infiniteRepeatable(animation = tween(1800, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
            label = "Mashxall medallion scale"
        )
        value
    } else {
        1f
    }
    Box(
        Modifier.size(size).graphicsLayer { scaleX = pulseScale; scaleY = pulseScale }
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(tint.copy(alpha = 0.24f), tint.copy(alpha = 0.075f), tint.copy(alpha = 0.025f))))
            .border(1.dp, tint.copy(alpha = 0.42f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.size(size * 0.78f).border(1.dp, tint.copy(alpha = 0.22f), CircleShape))
        Box(
            Modifier.size(size * 0.56f).clip(CircleShape).background(tint.copy(alpha = 0.07f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(size * 0.40f))
        }
    }
}


/** Selected-state control used across settings, audio filters, and reader options. */
@Composable
internal fun PremiumChoicePill(
    label: String,
    selected: Boolean,
    accent: Color,
    foreground: Color,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val motionAllowed = remember(context) { systemAllowsDecorativeMotion(context) }
    val fill by animateColorAsState(
        targetValue = when {
            !enabled -> foreground.copy(alpha = 0.025f)
            selected -> accent.copy(alpha = 0.17f)
            else -> foreground.copy(alpha = 0.035f)
        },
        animationSpec = if (motionAllowed) tween(230, easing = FastOutSlowInEasing) else snap(),
        label = "Mashxall choice pill fill"
    )
    val outline by animateColorAsState(
        targetValue = when {
            !enabled -> foreground.copy(alpha = 0.06f)
            selected -> accent.copy(alpha = 0.58f)
            else -> foreground.copy(alpha = 0.11f)
        },
        animationSpec = if (motionAllowed) tween(230, easing = FastOutSlowInEasing) else snap(),
        label = "Mashxall choice pill outline"
    )
    val labelColor by animateColorAsState(
        targetValue = if (enabled && selected) foreground else foreground.copy(alpha = if (enabled) 0.68f else 0.38f),
        animationSpec = if (motionAllowed) tween(230, easing = FastOutSlowInEasing) else snap(),
        label = "Mashxall choice pill label"
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.985f,
        animationSpec = if (motionAllowed) spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium) else snap(),
        label = "Mashxall choice pill response"
    )
    Row(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(15.dp))
            .background(Brush.horizontalGradient(listOf(fill, fill.copy(alpha = (fill.alpha * 0.58f).coerceAtLeast(0.02f)))))
            .border(1.dp, outline, RoundedCornerShape(15.dp))
            .premiumClickable(enabled = enabled, hapticFeedback = true, onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null) {
            Icon(
                leadingIcon,
                contentDescription = null,
                tint = if (enabled && selected) accent else foreground.copy(alpha = if (enabled) 0.68f else 0.30f),
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(label, color = labelColor, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, maxLines = 1)
        Spacer(Modifier.width(7.dp))
        Box(
            Modifier.size(7.dp).clip(CircleShape)
                .background(if (enabled && selected) accent else foreground.copy(alpha = if (enabled) 0.18f else 0.07f))
        )
    }
}


/** A live five-bar audio meter. Its animation exists only while a track is playing. */
@Composable
internal fun PremiumAudioWaveform(
    active: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val context = LocalContext.current
    val motionAllowed = remember(context) { systemAllowsDecorativeMotion(context) }
    if (active && motionAllowed) {
        PremiumAnimatedWaveformBars(accent = accent, modifier = modifier)
    } else {
        Row(
            modifier = modifier.width(34.dp).height(24.dp).semantics(mergeDescendants = true) {
                if (contentDescription != null) this.contentDescription = contentDescription
            },
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(7, 12, 17, 10, 6).forEachIndexed { index, barHeight ->
                Box(
                    Modifier.weight(1f).height(barHeight.dp).clip(CircleShape).background(accent.copy(alpha = if (active) 0.84f else 0.42f))
                )
            }
        }
    }
}

@Composable
private fun PremiumAnimatedWaveformBars(
    accent: Color,
    modifier: Modifier
) {
    val transition = rememberInfiniteTransition(label = "Mashxall active audio waveform")
    val h1 by transition.animateFloat(
        initialValue = 6f, targetValue = 18f,
        animationSpec = infiniteRepeatable(animation = tween(390, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "Audio wave bar one"
    )
    val h2 by transition.animateFloat(
        initialValue = 15f, targetValue = 7f,
        animationSpec = infiniteRepeatable(animation = tween(510, easing = FastOutSlowInEasing, delayMillis = 40), repeatMode = RepeatMode.Reverse),
        label = "Audio wave bar two"
    )
    val h3 by transition.animateFloat(
        initialValue = 8f, targetValue = 20f,
        animationSpec = infiniteRepeatable(animation = tween(460, easing = FastOutSlowInEasing, delayMillis = 90), repeatMode = RepeatMode.Reverse),
        label = "Audio wave bar three"
    )
    val h4 by transition.animateFloat(
        initialValue = 18f, targetValue = 6f,
        animationSpec = infiniteRepeatable(animation = tween(540, easing = FastOutSlowInEasing, delayMillis = 130), repeatMode = RepeatMode.Reverse),
        label = "Audio wave bar four"
    )
    val h5 by transition.animateFloat(
        initialValue = 7f, targetValue = 15f,
        animationSpec = infiniteRepeatable(animation = tween(430, easing = FastOutSlowInEasing, delayMillis = 180), repeatMode = RepeatMode.Reverse),
        label = "Audio wave bar five"
    )
    Row(
        modifier = modifier.width(34.dp).height(24.dp).semantics(mergeDescendants = true) {
            if (contentDescription != null) this.contentDescription = contentDescription
        },
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(h1, h2, h3, h4, h5).forEach { barHeight ->
            Box(
                Modifier.weight(1f).height(barHeight.dp).clip(CircleShape).background(accent)
            )
        }
    }
}


/** Shared, focus-reactive search field with a gentle icon turn and animated clear action. */
@Composable
internal fun PremiumSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    accent: Color,
    foreground: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val motionAllowed = remember(context) { systemAllowsDecorativeMotion(context) }
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val outline by animateColorAsState(
        targetValue = if (focused) accent.copy(alpha = 0.76f) else foreground.copy(alpha = 0.14f),
        animationSpec = if (motionAllowed) tween(230, easing = FastOutSlowInEasing) else snap(),
        label = "Mashxall search focus outline"
    )
    val focusScale by animateFloatAsState(
        targetValue = if (focused) 1.008f else 1f,
        animationSpec = if (motionAllowed) spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium) else snap(),
        label = "Mashxall search focus scale"
    )
    val iconRotation by animateFloatAsState(
        targetValue = if (focused) -12f else 0f,
        animationSpec = if (motionAllowed) spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium) else snap(),
        label = "Mashxall search icon response"
    )
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier.fillMaxWidth()
            .graphicsLayer { scaleX = focusScale; scaleY = focusScale }
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        accent.copy(alpha = if (focused) 0.095f else 0.035f),
                        foreground.copy(alpha = 0.018f)
                    )
                )
            )
            .border(1.dp, outline, shape)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            interactionSource = interactionSource,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, fontSize = 13.sp, color = foreground.copy(alpha = 0.48f)) },
            leadingIcon = {
                Icon(
                    Icons.Outlined.Search,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = iconRotation }
                )
            },
            trailingIcon = {
                AnimatedVisibility(
                    visible = value.isNotEmpty(),
                    enter = fadeIn(tween(120)) + scaleIn(initialScale = 0.65f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
                    exit = fadeOut(tween(90)) + scaleOut(targetScale = 0.65f, animationSpec = tween(90))
                ) {
                    IconButton(onClick = { onValueChange("") }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Clear search", tint = foreground.copy(alpha = 0.60f))
                    }
                }
            },
            shape = shape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                cursorColor = accent,
                focusedLeadingIconColor = accent,
                unfocusedLeadingIconColor = accent
            )
        )
    }
}


/**
 * Editorial section masthead shared by the content-heavy areas. It gives each
 * destination a recognizable identity while preserving the same app-wide
 * motion language, palette response and reading-friendly hierarchy.
 */
@Composable
internal fun PremiumSectionHero(
    title: String,
    subtitle: String,
    icon: ImageVector,
    foreground: Color,
    accent: Color,
    eyebrow: String = "MASHXALL • DAILY COMPANION",
    metric: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val motionAllowed = remember(context) { systemAllowsDecorativeMotion(context) }
    AnimatedVisibility(
        visible = true,
        modifier = modifier.fillMaxWidth(),
        enter = if (motionAllowed) {
            fadeIn(animationSpec = tween(360, easing = FastOutSlowInEasing)) +
                scaleIn(initialScale = 0.965f, animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )) +
                slideInVertically(initialOffsetY = { it / 7 }, animationSpec = tween(360, easing = FastOutSlowInEasing))
        } else {
            fadeIn(animationSpec = tween(0))
        }
    ) {
        Box(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(25.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF0B3028),
                            Color(0xFF174D3C),
                            Color(0xFF102F29)
                        )
                    )
                )
                .border(1.dp, accent.copy(alpha = 0.34f), RoundedCornerShape(25.dp))
        ) {
            Box(
                Modifier.align(Alignment.TopEnd).offset(x = 44.dp, y = (-62).dp)
                    .size(166.dp).clip(CircleShape)
                    .background(Brush.radialGradient(listOf(accent.copy(alpha = 0.23f), Color.Transparent)))
            )
            Box(
                Modifier.align(Alignment.BottomStart).offset(x = (-42).dp, y = 38.dp)
                    .size(94.dp).clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Color(0xFF63B894).copy(alpha = 0.15f), Color.Transparent)))
            )
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        eyebrow,
                        color = accent.copy(alpha = 0.96f),
                        fontSize = 8.sp,
                        letterSpacing = 1.15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(7.dp))
                    Text(
                        title,
                        color = Color(0xFFFFF8E9),
                        fontSize = 21.sp,
                        lineHeight = 27.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        subtitle,
                        color = Color.White.copy(alpha = 0.74f),
                        fontSize = 10.sp,
                        lineHeight = 15.sp,
                        maxLines = 2
                    )
                    if (!metric.isNullOrBlank()) {
                        Spacer(Modifier.height(9.dp))
                        Row(
                            Modifier.clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.075f))
                                .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                                .padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(5.dp).clip(CircleShape).background(accent))
                            Spacer(Modifier.width(6.dp))
                            Text(metric, color = Color.White.copy(alpha = 0.92f), fontSize = 9.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                    }
                }
                Spacer(Modifier.width(11.dp))
                PremiumIconMedallion(
                    icon = icon,
                    tint = accent,
                    size = 62.dp,
                    pulse = true
                )
            }
        }
    }
}


/**
 * A restrained orbit around the Qibla compass. The orbit rotates independently
 * from the sensor-driven needle so it adds depth without altering direction data.
 */
@Composable
internal fun PremiumCompassOrbit(
    tint: Color,
    size: Dp = 232.dp
) {
    val context = LocalContext.current
    val motionAllowed = remember(context) { systemAllowsDecorativeMotion(context) }
    if (motionAllowed) {
        PremiumAnimatedCompassOrbit(tint = tint, size = size)
    } else {
        PremiumCompassOrbitFrame(tint = tint, size = size, rotation = 0f)
    }
}

@Composable
private fun PremiumAnimatedCompassOrbit(tint: Color, size: Dp) {
    val transition = rememberInfiniteTransition(label = "Qibla orbit rotation")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Qibla orbit slow rotation"
    )
    PremiumCompassOrbitFrame(tint = tint, size = size, rotation = rotation)
}

@Composable
private fun PremiumCompassOrbitFrame(tint: Color, size: Dp, rotation: Float) {
    Box(
        Modifier.size(size).graphicsLayer { rotationZ = rotation }
    ) {
        Box(
            Modifier.fillMaxSize()
                .border(1.dp, tint.copy(alpha = 0.20f), CircleShape)
        )
        Box(
            Modifier.align(Alignment.TopCenter).padding(top = 2.dp)
                .size(9.dp).clip(CircleShape)
                .background(tint.copy(alpha = 0.92f))
        )
        Box(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 3.dp)
                .size(5.dp).clip(CircleShape)
                .background(tint.copy(alpha = 0.50f))
        )
        Box(
            Modifier.align(Alignment.CenterStart).padding(start = 3.dp)
                .size(5.dp).clip(CircleShape)
                .background(tint.copy(alpha = 0.34f))
        )
        Box(
            Modifier.align(Alignment.CenterEnd).padding(end = 3.dp)
                .size(5.dp).clip(CircleShape)
                .background(tint.copy(alpha = 0.34f))
        )
        Box(
            Modifier.align(Alignment.TopStart).padding(start = size * 0.19f, top = size * 0.19f)
                .size(3.dp).clip(CircleShape)
                .background(tint.copy(alpha = 0.42f))
        )
        Box(
            Modifier.align(Alignment.BottomEnd).padding(end = size * 0.19f, bottom = size * 0.19f)
                .size(3.dp).clip(CircleShape)
                .background(tint.copy(alpha = 0.42f))
        )
    }
}
