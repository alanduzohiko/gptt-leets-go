package com.mashxall.unified

import android.animation.ValueAnimator
import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ceil
import kotlin.math.sin
import kotlin.math.roundToInt

/**
 * User-controlled motion and touch preferences shared by every native screen.
 * The values are loaded from the app's private preferences by MashxallApp and
 * are observable Compose state so visible screens react immediately to changes.
 */
internal object MashxallMotionPreferences {
    private val reduceMotionState = mutableStateOf(false)
    var reduceMotion: Boolean
        get() = reduceMotionState.value
        set(value) { reduceMotionState.value = value }

    private val hapticsEnabledState = mutableStateOf(true)
    var hapticsEnabled: Boolean
        get() = hapticsEnabledState.value
        set(value) { hapticsEnabledState.value = value }
}

/** Decorative motion is allowed only when both Android and the user allow it. */
internal fun shouldAllowMashxallMotion(
    systemAnimationsEnabled: Boolean,
    durationScale: Float,
    reduceMotion: Boolean
): Boolean = systemAnimationsEnabled && durationScale.isFinite() && durationScale > 0f && !reduceMotion

/** Keep the pure preference decision independently testable. */
internal fun shouldPerformMashxallHaptic(hapticsEnabled: Boolean): Boolean = hapticsEnabled

/** Route every haptic through one preference-aware gate. Android still applies device-level settings. */
internal fun HapticFeedback.performMashxallHaptic(type: HapticFeedbackType) {
    if (shouldPerformMashxallHaptic(MashxallMotionPreferences.hapticsEnabled)) performHapticFeedback(type)
}

/**
 * Shared native touch response for tappable surfaces throughout Mashxall.
 *
 * The spring scale is deliberately small so cards feel tactile without shifting
 * neighbouring content. Material's current indication supplies the ripple, and
 * haptics can be opted out when a screen already provides richer task-specific
 * feedback (for example, Quran navigation or Tasbih counting).
 */
internal fun premiumPressTargetScale(
    enabled: Boolean,
    pressed: Boolean,
    pressedScale: Float = 0.968f
): Float {
    val safeScale = if (pressedScale.isFinite()) pressedScale.coerceIn(0.90f, 1f) else 0.968f
    return if (enabled && pressed) safeScale else 1f
}

/**
 * Native controls respond according to their role: tabs use a shorter, quieter
 * motion, while primary buttons get a slightly stronger press acknowledgement.
 */
internal fun premiumRolePressScale(role: Role?, baseScale: Float = 0.968f): Float {
    val safeBase = if (baseScale.isFinite()) baseScale.coerceIn(0.90f, 1f) else 0.968f
    return when (role) {
        Role.Button -> minOf(safeBase, 0.932f)
        Role.Tab -> minOf(safeBase, 0.952f)
        else -> minOf(safeBase, 0.962f)
    }
}

/** A tiny role-specific tilt makes button, tab and card presses feel distinct. */
internal fun premiumRolePressRotationDegrees(role: Role?): Float = when (role) {
    Role.Button -> -0.62f
    Role.Tab -> 0.24f
    else -> 0.38f
}

/** Each control gets a slightly different press tilt from its own tactile depth. */
internal fun premiumControlPressRotationDegrees(role: Role?, pressedScale: Float = 0.968f): Float {
    val safeScale = if (pressedScale.isFinite()) pressedScale.coerceIn(0.90f, 1f) else 0.968f
    val depthDelta = (0.968f - safeScale).coerceIn(-0.032f, 0.068f)
    return (premiumRolePressRotationDegrees(role) + depthDelta * 2.2f).coerceIn(-0.9f, 0.55f)
}

/** Role-specific edge glow remains restrained on tabs and stronger on primary actions. */
internal fun premiumRolePressGlowAlpha(role: Role?): Float = when (role) {
    Role.Button -> 0.30f
    Role.Tab -> 0.18f
    else -> 0.23f
}

@OptIn(ExperimentalFoundationApi::class)
internal fun Modifier.premiumClickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    onLongClickLabel: String? = null,
    role: Role? = null,
    hapticFeedback: Boolean = true,
    pressedScale: Float = 0.968f,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier = composed {
    val context = LocalContext.current
    val motionAllowed = remember(context, MashxallMotionPreferences.reduceMotion) { systemAllowsDecorativeMotion(context) }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val roleScale = premiumRolePressScale(role, pressedScale)
    val scale by animateFloatAsState(
        targetValue = premiumPressTargetScale(enabled, pressed, roleScale),
        animationSpec = if (motionAllowed) spring(
            dampingRatio = if (role == Role.Button) Spring.DampingRatioMediumBouncy else Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ) else snap(),
        label = "Mashxall native press scale"
    )
    val haptics = LocalHapticFeedback.current
    val pressAccent = MaterialTheme.colorScheme.primary
    val pressGlow by animateFloatAsState(
        targetValue = if (motionAllowed && enabled && pressed) 1f else 0f,
        animationSpec = if (motionAllowed) tween(120, easing = FastOutSlowInEasing) else snap(),
        label = "Mashxall touch edge glow"
    )

    val touchTarget = this.drawWithContent {
        drawContent()
        if (motionAllowed && enabled && pressGlow > 0f) {
            val glow = premiumRolePressGlowAlpha(role) * pressGlow
            val corner = CornerRadius((size.minDimension * 0.20f).coerceAtLeast(8.dp.toPx()))
            drawRoundRect(
                color = pressAccent.copy(alpha = glow * 0.045f),
                cornerRadius = corner
            )
            drawRoundRect(
                color = pressAccent.copy(alpha = glow),
                cornerRadius = corner,
                style = Stroke(width = (1.1f + 1.5f * pressGlow).dp.toPx())
            )
            drawRoundRect(
                color = Color.White.copy(alpha = glow * 0.16f),
                cornerRadius = corner,
                style = Stroke(width = 0.65.dp.toPx())
            )
            // A short moving glint makes the press acknowledgement visible,
            // while its start and edge glow differ by control role.
            val glintX = size.width * (0.14f + 0.72f * pressGlow)
            drawLine(
                color = Color.White.copy(alpha = glow * 0.22f),
                start = Offset((glintX - 4.dp.toPx()).coerceAtLeast(4.dp.toPx()), 1.5.dp.toPx()),
                end = Offset((glintX + 4.dp.toPx()).coerceAtMost(size.width - 4.dp.toPx()), 1.5.dp.toPx()),
                strokeWidth = 1.1.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawCircle(
                color = pressAccent.copy(alpha = glow * 0.62f),
                radius = 1.45.dp.toPx(),
                center = Offset(glintX, 2.dp.toPx())
            )
        }
    }.graphicsLayer {
        scaleX = scale
        scaleY = scale
        translationY = if (motionAllowed && enabled && pressed) 1.15.dp.toPx() else 0f
        rotationZ = if (motionAllowed && enabled && pressed) premiumControlPressRotationDegrees(role, pressedScale) else 0f
        shadowElevation = if (motionAllowed && enabled && pressed) {
            if (role == Role.Button) 7.dp.toPx() else 4.5.dp.toPx()
        } else 0f
    }
    if (onLongClick != null) {
        touchTarget.combinedClickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            enabled = enabled,
            onClickLabel = onClickLabel,
            onLongClickLabel = onLongClickLabel,
            role = role,
            onLongClick = {
                if (hapticFeedback) haptics.performMashxallHaptic(HapticFeedbackType.LongPress)
                onLongClick()
            },
            onClick = {
                if (hapticFeedback) haptics.performMashxallHaptic(HapticFeedbackType.TextHandleMove)
                onClick()
            }
        )
    } else {
        touchTarget.clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role
        ) {
            if (hapticFeedback) haptics.performMashxallHaptic(HapticFeedbackType.TextHandleMove)
            onClick()
        }
    }
}


/**
 * Switch wrapper that adds a soft press acknowledgement while retaining
 * Material's native thumb morph, track animation, semantics and touch target.
 */
@Composable
internal fun PremiumSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val context = LocalContext.current
    val motionAllowed = remember(context, MashxallMotionPreferences.reduceMotion) {
        systemAllowsDecorativeMotion(context)
    }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (enabled && pressed) 1.075f else 1f,
        animationSpec = if (motionAllowed) spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ) else snap(),
        label = "Mashxall switch touch response"
    )
    val haptics = LocalHapticFeedback.current
    Switch(
        checked = checked,
        onCheckedChange = { next ->
            haptics.performMashxallHaptic(HapticFeedbackType.TextHandleMove)
            onCheckedChange(next)
        },
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale },
        enabled = enabled,
        interactionSource = interactionSource
    )
}

/**
 * Dialog and inline text actions with the same tactile response as Mashxall's
 * larger controls: spring press, native Material ripple and preference-aware haptic.
 */
@Composable
internal fun PremiumTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    content: @Composable RowScope.() -> Unit
) {
    val context = LocalContext.current
    val motionAllowed = remember(context, MashxallMotionPreferences.reduceMotion) {
        systemAllowsDecorativeMotion(context)
    }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (enabled && pressed) 0.94f else 1f,
        animationSpec = if (motionAllowed) spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ) else snap(),
        label = "Mashxall text button touch spring"
    )
    val haptics = LocalHapticFeedback.current
    TextButton(
        onClick = {
            haptics.performMashxallHaptic(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale },
        enabled = enabled,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content
    )
}

/**
 * Material outlined actions retain their native semantics and ripple while
 * receiving the same brief spring response and preference-aware haptic as
 * Mashxall's custom action surfaces.
 */
@Composable
internal fun PremiumOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
    content: @Composable RowScope.() -> Unit
) {
    val context = LocalContext.current
    val motionAllowed = remember(context, MashxallMotionPreferences.reduceMotion) {
        systemAllowsDecorativeMotion(context)
    }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (enabled && pressed) 0.945f else 1f,
        animationSpec = if (motionAllowed) spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ) else snap(),
        label = "Mashxall outlined action press"
    )
    val haptics = LocalHapticFeedback.current
    OutlinedButton(
        onClick = {
            haptics.performMashxallHaptic(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale },
        enabled = enabled,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content
    )
}

/**
 * Mashxall's shared indeterminate ring. It uses a restrained two-layer track,
 * a moving rounded arc and one terminal jewel. With Reduce Motion enabled the
 * same status remains visible as a static open ring instead of spinning.
 */
@Composable
internal fun PremiumLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color,
    strokeWidth: Dp = 3.dp
) {
    val context = LocalContext.current
    val motionAllowed = remember(context, MashxallMotionPreferences.reduceMotion) {
        systemAllowsDecorativeMotion(context)
    }
    val rotation = if (motionAllowed) {
        val transition = rememberInfiniteTransition(label = "Mashxall loading ring")
        val angle by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(980, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "Mashxall loading ring rotation"
        )
        angle
    } else 0f

    Canvas(
        modifier = modifier.semantics {
            progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
        }
    ) {
        val diameter = minOf(size.width, size.height)
        if (diameter <= 0f) return@Canvas
        val stroke = strokeWidth.toPx().coerceAtLeast(1.dp.toPx())
        val arcDiameter = (diameter - stroke).coerceAtLeast(0f)
        if (arcDiameter <= 0f) return@Canvas
        val arcSize = Size(arcDiameter, arcDiameter)
        val topLeft = Offset(
            (size.width - arcDiameter) / 2f,
            (size.height - arcDiameter) / 2f
        )
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = arcDiameter / 2f
        drawArc(
            color = color.copy(alpha = 0.13f),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke)
        )
        drawArc(
            color = color,
            startAngle = rotation - 96f,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        val endAngle = Math.toRadians((rotation + 174f).toDouble())
        drawCircle(
            color = color.copy(alpha = 0.92f),
            radius = stroke * 0.88f,
            center = Offset(
                center.x + (kotlin.math.cos(endAngle) * radius).toFloat(),
                center.y + (kotlin.math.sin(endAngle) * radius).toFloat()
            )
        )
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
    val motionAllowed = remember(context, MashxallMotionPreferences.reduceMotion) { systemAllowsDecorativeMotion(context) }
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


/**
 * Page-shaped loading state for the graphic Mushaf. The layout echoes a real
 * page without inventing readable Quran text while the SVG is being decoded.
 */
@Composable
internal fun PremiumMushafPageSkeleton(
    modifier: Modifier = Modifier,
    accent: Color,
    darkMode: Boolean,
    sepiaMode: Boolean
) {
    val context = LocalContext.current
    val motionAllowed = remember(context, MashxallMotionPreferences.reduceMotion) { systemAllowsDecorativeMotion(context) }
    val shimmerOffset = if (motionAllowed) {
        val transition = rememberInfiniteTransition(label = "Mushaf page loading shimmer")
        val value by transition.animateFloat(
            initialValue = -360f,
            targetValue = 760f,
            animationSpec = infiniteRepeatable(
                animation = tween(1550, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "Mushaf page shimmer position"
        )
        value
    } else {
        -360f
    }
    val paper = when {
        darkMode -> Color(0xFF171E1B)
        sepiaMode -> Color(0xFFF0E2C4)
        else -> Color(0xFFFFFEF8)
    }
    val ink = if (darkMode) Color(0xFF91A79B) else Color(0xFF718477)
    val shimmer = Brush.linearGradient(
        colors = listOf(
            ink.copy(alpha = 0.045f),
            accent.copy(alpha = 0.20f),
            ink.copy(alpha = 0.045f)
        ),
        start = Offset(shimmerOffset, 0f),
        end = Offset(shimmerOffset + 240f, 90f)
    )
    Column(
        modifier = modifier.fillMaxSize()
            .clip(RoundedCornerShape(10.dp))
            .background(paper)
            .border(1.dp, accent.copy(alpha = 0.34f), RoundedCornerShape(10.dp))
            .padding(horizontal = 17.dp, vertical = 19.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        Spacer(Modifier.height(2.dp))
        Box(
            Modifier.fillMaxWidth(0.26f).height(7.dp)
                .clip(RoundedCornerShape(5.dp)).background(shimmer)
        )
        Box(Modifier.size(34.dp).border(1.dp, accent.copy(alpha = 0.22f), CircleShape)) {
            Box(
                Modifier.align(Alignment.Center).size(20.dp)
                    .border(1.dp, accent.copy(alpha = 0.18f), CircleShape)
            )
        }
        repeat(3) { row ->
            Box(
                Modifier.fillMaxWidth(if (row == 1) 0.94f else 0.88f)
                    .height(6.dp).clip(RoundedCornerShape(5.dp))
                    .background(shimmer)
            )
        }
        Spacer(Modifier.height(4.dp))
        repeat(13) { index ->
            Box(
                Modifier.fillMaxWidth(
                    when (index % 4) {
                        0 -> 0.94f
                        1 -> 0.86f
                        2 -> 0.98f
                        else -> 0.78f
                    }
                ).height(if (index % 5 == 0) 7.dp else 5.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmer)
            )
        }
        Spacer(Modifier.weight(1f))
        Box(
            Modifier.fillMaxWidth(0.20f).height(5.dp)
                .clip(RoundedCornerShape(4.dp)).background(shimmer)
        )
    }
}


internal fun shouldAnimateContentSize(motionAllowed: Boolean, enabled: Boolean = true): Boolean =
    motionAllowed && enabled

/** Animate expanding/collapsing content only when the system allows motion. */
internal fun Modifier.premiumAnimateContentSize(enabled: Boolean = true): Modifier = composed {
    val context = LocalContext.current
    val motionAllowed = remember(context, MashxallMotionPreferences.reduceMotion) { systemAllowsDecorativeMotion(context) }
    if (shouldAnimateContentSize(motionAllowed, enabled)) {
        this.animateContentSize(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    } else {
        this
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
    return shouldAllowMashxallMotion(animatorEnabled, durationScale, MashxallMotionPreferences.reduceMotion)
}

internal fun normalizeAmbientBackdropStyle(value: String?): String = when (value) {
    "ambient", "orbit", "ornament", "off" -> value ?: "ambient"
    else -> "ambient"
}

internal fun normalizeAmbientBackdropIntensity(value: Float): Float =
    if (value.isFinite()) value.coerceIn(0.20f, 1f) else 0.82f

internal fun normalizeAmbientBackdropSpeed(value: Float): Float =
    if (value.isFinite()) value.coerceIn(0.45f, 1.45f) else 0.72f

internal fun shouldShowAmbientBackdrop(style: String): Boolean =
    normalizeAmbientBackdropStyle(style) != "off"

internal fun shouldAnimateAmbientBackdrop(
    screenKey: String,
    motionAllowed: Boolean,
    style: String = "ambient"
): Boolean {
    val safeStyle = normalizeAmbientBackdropStyle(style)
    return motionAllowed &&
        (safeStyle == "ambient" || safeStyle == "orbit") &&
        screenKey != "immersive_mushaf"
}

internal fun ambientBackdropIntensityForScreen(screenKey: String, intensity: Float): Float {
    val safe = normalizeAmbientBackdropIntensity(intensity)
    val readerFactor = when (screenKey) {
        "quran" -> 0.72f
        "tafsir" -> 0.66f
        else -> 1f
    }
    return safe * readerFactor
}

/**
 * The edge ornament is drawn above screen content, so it remains visible when
 * opaque cards cover the ambient background. The immersive Mushaf receives
 * only a tiny static edge treatment to keep the page text completely clear.
 */
internal fun premiumScreenOrnamentIntensity(
    screenKey: String,
    intensity: Float,
    immersiveMushaf: Boolean = false
): Float {
    val safe = normalizeAmbientBackdropIntensity(intensity)
    val factor = when {
        immersiveMushaf -> 0.16f
        screenKey == "quran" -> 0.72f
        screenKey == "tafsir" -> 0.66f
        else -> 0.78f
    }
    return (safe * factor).coerceIn(0.02f, 0.82f)
}

internal fun ambientMotionDurationMillis(baseMillis: Int, speed: Float): Int =
    (baseMillis.coerceAtLeast(1_000) / normalizeAmbientBackdropSpeed(speed))
        .roundToInt()
        .coerceIn(1_000, 120_000)

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
    val motionAllowed = remember(context, MashxallMotionPreferences.reduceMotion) { systemAllowsDecorativeMotion(context) }
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
        translationY = (1f - fraction) * 30.dp.toPx()
        translationX = (1f - fraction) * 4.dp.toPx()
        rotationZ = (1f - fraction) * 0.9f
        scaleX = 0.92f + fraction * 0.08f
        scaleY = 0.92f + fraction * 0.08f
    }
}



/**
 * Returns one of the 16 alternating outer/inner vertices of an eight-point
 * Islamic rosette. Invalid radii collapse safely instead of creating NaN
 * drawing coordinates.
 */
internal fun premiumOrnamentVertex(
    index: Int,
    outerRadius: Float,
    innerRadius: Float
): Offset {
    val safeOuter = outerRadius.takeIf { it.isFinite() && it > 0f } ?: 0f
    if (safeOuter <= 0f) return Offset.Zero
    val safeInner = innerRadius.takeIf { it.isFinite() && it > 0f }
        ?.coerceAtMost(safeOuter) ?: 0f
    val wrappedIndex = ((index % 16) + 16) % 16
    val angle = -PI / 2.0 + wrappedIndex * PI / 8.0
    val radius = if (wrappedIndex % 2 == 0) safeOuter else safeInner
    return Offset((cos(angle) * radius).toFloat(), (sin(angle) * radius).toFloat())
}

/**
 * A quiet, native-drawn Mashxall rosette motif. It is deliberately static:
 * geometric detail adds identity without running another animation loop or
 * competing with Quran pages and other reading surfaces.
 */
@Composable
internal fun PremiumIslamicOrnament(
    modifier: Modifier = Modifier,
    accent: Color,
    dark: Boolean
) {
    Canvas(modifier = modifier) {
        if (size.minDimension <= 0f) return@Canvas
        val baseRadius = 54.dp.toPx()
        val lineWidth = 0.8.dp.toPx()
        val alpha = if (dark) 0.075f else 0.047f
        val anchors = listOf(
            Triple(0.105f, 0.205f, 1.0f),
            Triple(0.895f, 0.785f, 0.82f)
        )
        anchors.forEach { (fractionX, fractionY, scale) ->
            val center = Offset(size.width * fractionX, size.height * fractionY)
            val outer = baseRadius * scale
            val inner = outer * 0.47f
            val rosette = Path()
            repeat(16) { index ->
                val point = premiumOrnamentVertex(index, outer, inner)
                val x = center.x + point.x
                val y = center.y + point.y
                if (index == 0) rosette.moveTo(x, y) else rosette.lineTo(x, y)
            }
            rosette.close()
            drawPath(
                path = rosette,
                color = accent.copy(alpha = alpha),
                style = Stroke(width = lineWidth)
            )

            val innerRosette = Path()
            repeat(16) { index ->
                val point = premiumOrnamentVertex(index, outer * 0.48f, inner * 0.48f)
                val x = center.x + point.x
                val y = center.y + point.y
                if (index == 0) innerRosette.moveTo(x, y) else innerRosette.lineTo(x, y)
            }
            innerRosette.close()
            drawPath(
                path = innerRosette,
                color = accent.copy(alpha = alpha * 0.78f),
                style = Stroke(width = lineWidth * 0.8f)
            )
            drawCircle(
                color = accent.copy(alpha = alpha * 0.72f),
                radius = lineWidth * 1.8f,
                center = center
            )
        }
    }
}

/**
 * One-shot radial feedback for a successful user action. It is event-driven,
 * short-lived, and fully disabled when Android's animator scale is disabled.
 */
@Composable
internal fun PremiumTapBurst(
    eventKey: Int,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val motionAllowed = remember(context, MashxallMotionPreferences.reduceMotion) { systemAllowsDecorativeMotion(context) }
    val progress = remember { Animatable(1f) }
    LaunchedEffect(eventKey, motionAllowed) {
        if (motionAllowed && eventKey > 0) {
            progress.snapTo(0f)
            progress.animateTo(
                1f,
                animationSpec = tween(480, easing = FastOutSlowInEasing)
            )
        } else {
            progress.snapTo(1f)
        }
    }
    Canvas(modifier = modifier) {
        if (!motionAllowed || eventKey <= 0 || progress.value >= 1f || size.minDimension <= 0f) {
            return@Canvas
        }
        val phase = progress.value.coerceIn(0f, 1f)
        val remaining = 1f - phase
        val center = Offset(size.width / 2f, size.height / 2f)
        val reach = size.minDimension * 0.45f
        drawCircle(
            color = accent.copy(alpha = 0.34f * remaining),
            radius = reach * (0.22f + phase * 0.78f),
            center = center,
            style = Stroke(width = (2.6f * remaining + 0.7f).dp.toPx())
        )
        drawCircle(
            color = accent.copy(alpha = 0.19f * remaining),
            radius = reach * (0.06f + phase * 0.54f),
            center = center,
            style = Stroke(width = (1.7f * remaining + 0.5f).dp.toPx())
        )
        repeat(8) { index ->
            val angle = -PI / 2.0 + index * PI / 4.0
            val distance = reach * (0.24f + phase * 0.70f)
            val particle = Offset(
                center.x + (cos(angle) * distance).toFloat(),
                center.y + (sin(angle) * distance).toFloat()
            )
            drawCircle(
                color = accent.copy(alpha = 0.72f * remaining),
                radius = (size.minDimension * 0.014f * remaining).coerceAtLeast(0.7.dp.toPx()),
                center = particle
            )
        }
    }
}

/** Slow ambient light fields add depth without competing with reading content. */
private data class AmbientBackdropMotion(
    val driftX: Float = 0f,
    val driftY: Float = 0f,
    val orbitRotation: Float = 0f,
    val shimmerAlpha: Float = 0.04f
)

/**
 * Shared atmospheric background used by every destination. One transition
 * drives all ambient values to keep the animation inexpensive on mid-range
 * Android devices. Reader screens pass a lower intensity to protect legibility.
 */
@Composable
internal fun PremiumAmbientBackdrop(
    modifier: Modifier = Modifier,
    accent: Color,
    dark: Boolean,
    animated: Boolean = true,
    style: String = "ambient",
    intensity: Float = 0.82f,
    speed: Float = 0.72f
) {
    val context = LocalContext.current
    val motionAllowed = remember(context, MashxallMotionPreferences.reduceMotion) {
        systemAllowsDecorativeMotion(context)
    }
    val safeStyle = normalizeAmbientBackdropStyle(style)
    val safeIntensity = normalizeAmbientBackdropIntensity(intensity)
    val safeSpeed = normalizeAmbientBackdropSpeed(speed)
    val runMotion = animated && motionAllowed && (safeStyle == "ambient" || safeStyle == "orbit")
    val motion = if (runMotion) {
        val transition = rememberInfiniteTransition(label = "Mashxall shared ambient atmosphere")
        val driftX by transition.animateFloat(
            initialValue = if (safeStyle == "ambient") -18f else 0f,
            targetValue = if (safeStyle == "ambient") 20f else 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(ambientMotionDurationMillis(9_200, safeSpeed), easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "Mashxall ambient horizontal drift"
        )
        val driftY by transition.animateFloat(
            initialValue = if (safeStyle == "ambient") 13f else 0f,
            targetValue = if (safeStyle == "ambient") -15f else 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(ambientMotionDurationMillis(11_400, safeSpeed), easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "Mashxall ambient vertical drift"
        )
        val orbit by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(ambientMotionDurationMillis(48_000, safeSpeed), easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "Mashxall ambient orbital rotation"
        )
        val shimmer by transition.animateFloat(
            initialValue = 0.045f * safeIntensity,
            targetValue = 0.105f * safeIntensity,
            animationSpec = infiniteRepeatable(
                animation = tween(ambientMotionDurationMillis(6_400, safeSpeed), easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "Mashxall ambient breathing light"
        )
        AmbientBackdropMotion(driftX, driftY, orbit, shimmer)
    } else {
        AmbientBackdropMotion(
            shimmerAlpha = (if (dark) 0.065f else 0.032f) * safeIntensity
        )
    }
    val glowAlpha = (if (dark) 0.205f else 0.105f) * safeIntensity
    val secondaryAlpha = (if (dark) 0.165f else 0.082f) * safeIntensity
    val coolAlpha = (if (dark) 0.092f else 0.045f) * safeIntensity

    Box(modifier = modifier) {
        PremiumIslamicOrnament(
            Modifier.fillMaxSize(),
            accent = accent.copy(alpha = safeIntensity),
            dark = dark
        )
        Box(
            Modifier.align(Alignment.TopEnd)
                .offset(x = motion.driftX.dp + 42.dp, y = motion.driftY.dp - 62.dp)
                .size(250.dp).clip(CircleShape)
                .background(Brush.radialGradient(listOf(accent.copy(alpha = glowAlpha), Color.Transparent)))
        )
        Box(
            Modifier.align(Alignment.BottomStart)
                .offset(x = -82.dp - motion.driftX.dp, y = 72.dp - motion.driftY.dp)
                .size(220.dp).clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFF3DA987).copy(alpha = secondaryAlpha), Color.Transparent)))
        )
        Box(
            Modifier.align(Alignment.CenterEnd)
                .offset(x = 54.dp + motion.driftX.dp, y = -104.dp + motion.driftY.dp)
                .size(104.dp).clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFF6DA8C7).copy(alpha = coolAlpha), Color.Transparent)))
        )
        if (safeStyle != "ornament") {
            // The orbital accent is deliberately behind content, never over text.
            Box(
                Modifier.align(Alignment.BottomEnd).offset(x = 28.dp, y = 34.dp)
                    .size(118.dp).graphicsLayer { rotationZ = motion.orbitRotation }
            ) {
                Box(Modifier.fillMaxSize().border(0.95.dp, accent.copy(alpha = 0.22f * safeIntensity), CircleShape))
                Box(
                    Modifier.align(Alignment.TopCenter).offset(y = (-2).dp)
                        .size(6.dp).clip(CircleShape).background(accent.copy(alpha = 0.54f * safeIntensity))
                )
                Box(
                    Modifier.align(Alignment.CenterEnd).offset(x = 2.dp)
                        .size(4.dp).clip(CircleShape).background(Color(0xFF8FD4B1).copy(alpha = 0.54f * safeIntensity))
                )
            }
        }
        if (safeStyle == "ambient") {
            Box(
                Modifier.align(Alignment.Center).offset(x = 82.dp, y = 42.dp)
                    .size(146.dp).graphicsLayer { alpha = motion.shimmerAlpha }
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(accent.copy(alpha = 0.78f), Color.Transparent)))
            )
        }
    }
}

/**
 * Visible screen-edge arabesques. Unlike the ambient glow, this layer is drawn
 * over the page margins so cards cannot hide all of the app's decoration.
 * Motion stays near the edges and never crosses the reading column.
 */
@Composable
internal fun PremiumScreenOrnamentOverlay(
    modifier: Modifier = Modifier,
    accent: Color,
    intensity: Float,
    screenKey: String,
    animated: Boolean,
    immersiveMushaf: Boolean = false
) {
    val context = LocalContext.current
    val systemMotionAllowed = remember(context, MashxallMotionPreferences.reduceMotion) {
        systemAllowsDecorativeMotion(context)
    }
    val motionAllowed = animated && systemMotionAllowed
    val safeIntensity = if (intensity.isFinite()) intensity.coerceIn(0f, 1f) else 0.2f
    val (drift, orbit, shimmer) = if (motionAllowed) {
        val transition = rememberInfiniteTransition(label = "Mashxall screen edge ornaments")
        val driftValue by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(7_600, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "Mashxall edge ornament drift"
        )
        val orbitValue by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(38_000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "Mashxall edge ornament orbit"
        )
        val shimmerValue by transition.animateFloat(
            initialValue = 0.38f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2_500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "Mashxall edge ornament shimmer"
        )
        Triple(driftValue, orbitValue, shimmerValue)
    } else {
        Triple(0f, 0f, 0.66f)
    }
    val alphaBase = if (immersiveMushaf) {
        0.045f + safeIntensity * 0.12f
    } else {
        0.23f + safeIntensity * 0.55f
    }
    val phaseSeed = (screenKey.hashCode() and 0xFF).toFloat() / 255f

    Canvas(modifier = modifier) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas
        val sideInset = if (immersiveMushaf) 5.5.dp.toPx() else 9.dp.toPx()
        val radius = if (immersiveMushaf) 7.5.dp.toPx() else 21.dp.toPx()
        val thinStroke = if (immersiveMushaf) 0.55.dp.toPx() else 1.15.dp.toPx()
        val driftRadians = drift * (2.0 * PI)
        val seedRadians = phaseSeed * (2.0 * PI)
        val driftDistance = if (immersiveMushaf) 1.8 else 8.0
        val verticalDrift = (sin(driftRadians + seedRadians) * driftDistance).toFloat() * density
        val topEdgeY = if (immersiveMushaf) 2.5.dp.toPx() else 5.dp.toPx()
        val bottomEdgeY = size.height - topEdgeY
        val ribbonTravel = if (motionAllowed) drift else phaseSeed
        val ribbonX = size.width * (0.12f + 0.76f * ribbonTravel.coerceIn(0f, 1f))
        val ribbonRadius = if (immersiveMushaf) 0.8.dp.toPx() else 2.25.dp.toPx()

        listOf(topEdgeY, bottomEdgeY).forEachIndexed { index, edgeY ->
            drawLine(
                color = accent.copy(alpha = alphaBase * 0.27f),
                start = Offset(if (index == 0) 18.dp.toPx() else 26.dp.toPx(), edgeY),
                end = Offset(size.width - if (index == 0) 26.dp.toPx() else 18.dp.toPx(), edgeY),
                strokeWidth = if (immersiveMushaf) 0.45.dp.toPx() else 0.8.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawCircle(
                color = accent.copy(alpha = alphaBase * (0.52f + shimmer * 0.32f)),
                radius = ribbonRadius,
                center = Offset(ribbonX, edgeY)
            )
            drawCircle(
                color = accent.copy(alpha = alphaBase * 0.18f),
                radius = ribbonRadius * (if (immersiveMushaf) 1.8f else 2.7f),
                center = Offset(ribbonX, edgeY)
            )
        }

        repeat(2) { side ->
            val edgeX = if (side == 0) sideInset else size.width - sideInset
            val sidePhase = if (side == 0) 0f else 137f
            drawLine(
                color = accent.copy(alpha = alphaBase * 0.25f),
                start = Offset(edgeX, size.height * 0.12f),
                end = Offset(edgeX, size.height * 0.88f),
                strokeWidth = thinStroke,
                cap = StrokeCap.Round
            )

            repeat(3) { index ->
                val wave = sin(driftRadians + seedRadians + index * 1.45 + side * PI).toFloat()
                val centerY = size.height * (0.19f + index * 0.305f) +
                    verticalDrift + wave * (if (immersiveMushaf) 1.5.dp.toPx() else 6.dp.toPx())
                val center = Offset(edgeX, centerY)
                val ringSize = Size(radius * 2f, radius * 2f)
                val ringTopLeft = Offset(center.x - radius, center.y - radius)
                val sweep = if (immersiveMushaf) 92f else 142f
                drawArc(
                    color = accent.copy(alpha = alphaBase * (0.48f + shimmer * 0.18f)),
                    startAngle = orbit + sidePhase + index * 61f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = ringTopLeft,
                    size = ringSize,
                    style = Stroke(width = thinStroke)
                )
                drawArc(
                    color = accent.copy(alpha = alphaBase * 0.24f),
                    startAngle = orbit + sidePhase + index * 61f + 180f,
                    sweepAngle = sweep * 0.58f,
                    useCenter = false,
                    topLeft = ringTopLeft,
                    size = ringSize,
                    style = Stroke(width = thinStroke * 0.72f)
                )
                val jewelAngle = Math.toRadians((orbit + sidePhase + index * 61f + sweep).toDouble())
                drawCircle(
                    color = accent.copy(alpha = alphaBase * (0.72f + shimmer * 0.20f)),
                    radius = if (immersiveMushaf) 1.0.dp.toPx() else 1.8.dp.toPx(),
                    center = Offset(
                        center.x + cos(jewelAngle).toFloat() * radius,
                        center.y + sin(jewelAngle).toFloat() * radius
                    )
                )
                drawCircle(
                    color = accent.copy(alpha = alphaBase * 0.42f),
                    radius = if (immersiveMushaf) 0.65.dp.toPx() else 1.1.dp.toPx(),
                    center = Offset(center.x, center.y)
                )
            }

            // A small eight-point rosette is anchored to the outer margin.
            val rosetteCenter = Offset(
                edgeX,
                size.height * 0.51f - verticalDrift * (if (side == 0) 1f else -1f)
            )
            val outerRadius = if (immersiveMushaf) 4.8.dp.toPx() else 12.dp.toPx()
            val rosettePath = Path()
            for (vertex in 0 until 16) {
                val point = premiumOrnamentVertex(
                    vertex,
                    outerRadius,
                    outerRadius * 0.48f
                )
                val x = rosetteCenter.x + point.x
                val y = rosetteCenter.y + point.y
                if (vertex == 0) rosettePath.moveTo(x, y) else rosettePath.lineTo(x, y)
            }
            rosettePath.close()
            drawPath(
                path = rosettePath,
                color = accent.copy(alpha = alphaBase * 0.55f),
                style = Stroke(width = thinStroke)
            )
            drawCircle(
                color = accent.copy(alpha = alphaBase * 0.76f),
                radius = if (immersiveMushaf) 0.9.dp.toPx() else 1.5.dp.toPx(),
                center = rosetteCenter
            )
        }
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
    val motionAllowed = remember(context, MashxallMotionPreferences.reduceMotion) { systemAllowsDecorativeMotion(context) }
    val pulseMotion = if (pulse && motionAllowed) {
        val transition = rememberInfiniteTransition(label = "Mashxall medallion pulse")
        val scale by transition.animateFloat(
            initialValue = 0.955f, targetValue = 1.045f,
            animationSpec = infiniteRepeatable(animation = tween(1800, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
            label = "Mashxall medallion scale"
        )
        val orbit by transition.animateFloat(
            initialValue = 0f, targetValue = 360f,
            animationSpec = infiniteRepeatable(animation = tween(9800, easing = LinearEasing), repeatMode = RepeatMode.Restart),
            label = "Mashxall medallion orbit"
        )
        scale to orbit
    } else {
        1f to 0f
    }
    Box(
        Modifier.size(size).graphicsLayer { scaleX = pulseMotion.first; scaleY = pulseMotion.first }
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(tint.copy(alpha = 0.24f), tint.copy(alpha = 0.075f), tint.copy(alpha = 0.025f))))
            .border(1.dp, tint.copy(alpha = 0.42f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.size(size * 0.78f).border(1.dp, tint.copy(alpha = 0.22f), CircleShape))
        Box(Modifier.size(size).graphicsLayer { rotationZ = pulseMotion.second }) {
            Box(
                Modifier.align(Alignment.TopCenter).offset(y = 2.dp)
                    .size(size * 0.085f).clip(CircleShape).background(tint.copy(alpha = 0.95f))
            )
            Box(
                Modifier.align(Alignment.BottomCenter).offset(y = (-2).dp)
                    .size(size * 0.055f).clip(CircleShape).background(tint.copy(alpha = 0.58f))
            )
        }
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
    val motionAllowed = remember(context, MashxallMotionPreferences.reduceMotion) { systemAllowsDecorativeMotion(context) }
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
    val leadingIconScale by animateFloatAsState(
        targetValue = premiumChoiceIconScale(enabled && selected),
        animationSpec = if (motionAllowed) spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium) else snap(),
        label = "Mashxall choice pill icon spring"
    )
    val leadingIconRotation by animateFloatAsState(
        targetValue = if (enabled) premiumChoiceIconRotationDegrees(selected) else 0f,
        animationSpec = if (motionAllowed) spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium) else snap(),
        label = "Mashxall choice pill icon turn"
    )
    val selectionDotScale by animateFloatAsState(
        targetValue = if (enabled && selected) 1.18f else 0.62f,
        animationSpec = if (motionAllowed) spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ) else snap(),
        label = "Mashxall choice pill selection jewel"
    )
    Row(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(15.dp))
            .background(Brush.horizontalGradient(listOf(fill, fill.copy(alpha = (fill.alpha * 0.58f).coerceAtLeast(0.02f)))))
            .border(1.dp, outline, RoundedCornerShape(15.dp))
            .semantics { this.selected = selected }
            .premiumClickable(enabled = enabled, role = Role.Button, hapticFeedback = true, onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null) {
            Icon(
                leadingIcon,
                contentDescription = null,
                tint = if (enabled && selected) accent else foreground.copy(alpha = if (enabled) 0.68f else 0.30f),
                modifier = Modifier.size(14.dp).graphicsLayer {
                    scaleX = leadingIconScale
                    scaleY = leadingIconScale
                    rotationZ = leadingIconRotation
                }
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(label, color = labelColor, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, maxLines = 1)
        Spacer(Modifier.width(7.dp))
        Box(
            Modifier.size(7.dp)
                .graphicsLayer { scaleX = selectionDotScale; scaleY = selectionDotScale }
                .clip(CircleShape)
                .background(if (enabled && selected) accent else foreground.copy(alpha = if (enabled) 0.18f else 0.07f))
                .border(
                    width = if (enabled && selected) 0.7.dp else 0.dp,
                    color = accent.copy(alpha = if (enabled && selected) 0.72f else 0f),
                    shape = CircleShape
                )
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
    val motionAllowed = remember(context, MashxallMotionPreferences.reduceMotion) { systemAllowsDecorativeMotion(context) }
    if (active && motionAllowed) {
        PremiumAnimatedWaveformBars(accent = accent, modifier = modifier, contentDescription = contentDescription)
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
    modifier: Modifier,
    contentDescription: String?
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
    clearContentDescription: String = "Clear search",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val motionAllowed = remember(context, MashxallMotionPreferences.reduceMotion) { systemAllowsDecorativeMotion(context) }
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
                    enter = if (motionAllowed) fadeIn(tween(120)) + scaleIn(
                        initialScale = 0.65f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                    ) else fadeIn(tween(0)),
                    exit = if (motionAllowed) fadeOut(tween(90)) + scaleOut(
                        targetScale = 0.65f,
                        animationSpec = tween(90)
                    ) else fadeOut(tween(0))
                ) {
                    PremiumIconButton(
                        icon = Icons.Outlined.Close,
                        contentDescription = clearContentDescription,
                        accent = accent,
                        size = 34.dp,
                        circular = true,
                        hapticFeedback = true
                    ) {
                        onValueChange("")
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
    pulseIcon: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val motionAllowed = remember(context, MashxallMotionPreferences.reduceMotion) { systemAllowsDecorativeMotion(context) }
    val heroOrbitRotation = if (motionAllowed) {
        val transition = rememberInfiniteTransition(label = "Mashxall section hero orbit")
        val value by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(26000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "Mashxall section hero orbital rotation"
        )
        value
    } else 0f
    val heroGlow = if (motionAllowed) {
        val transition = rememberInfiniteTransition(label = "Mashxall section hero breathing light")
        val scale by transition.animateFloat(
            initialValue = 0.92f,
            targetValue = 1.09f,
            animationSpec = infiniteRepeatable(
                animation = tween(3200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "Mashxall hero glow scale"
        )
        val alpha by transition.animateFloat(
            initialValue = 0.18f,
            targetValue = 0.32f,
            animationSpec = infiniteRepeatable(
                animation = tween(3200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "Mashxall hero glow alpha"
        )
        scale to alpha
    } else 1f to 0.20f
    val heroDivider = remember(title, motionAllowed) {
        Animatable(if (motionAllowed) 0f else 1f)
    }
    LaunchedEffect(title, motionAllowed) {
        if (motionAllowed) {
            heroDivider.snapTo(0f)
            delay(140L)
            heroDivider.animateTo(
                1f,
                animationSpec = tween(680, easing = FastOutSlowInEasing)
            )
        } else {
            heroDivider.snapTo(1f)
        }
    }
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
            PremiumIslamicOrnament(
                // Match the measured hero bounds without making this decorative
                // background participate in Box sizing or consume the whole screen.
                modifier = Modifier.matchParentSize(),
                accent = accent,
                dark = true
            )
            Box(
                Modifier.align(Alignment.TopEnd).offset(x = 37.dp, y = (-63).dp)
                    .size(152.dp).graphicsLayer { rotationZ = heroOrbitRotation }
            ) {
                Box(Modifier.fillMaxSize().border(1.dp, accent.copy(alpha = 0.24f), CircleShape))
                Box(
                    Modifier.align(Alignment.TopCenter).offset(y = (-3).dp)
                        .size(8.dp).clip(CircleShape)
                        .background(accent.copy(alpha = 0.92f))
                        .border(1.dp, Color(0xFF123D31), CircleShape)
                )
                Box(
                    Modifier.align(Alignment.BottomCenter).offset(y = 3.dp)
                        .size(5.dp).clip(CircleShape)
                        .background(Color(0xFF8FD4B1).copy(alpha = 0.90f))
                )
            }
            Box(
                Modifier.align(Alignment.TopEnd).offset(x = 44.dp, y = (-62).dp)
                    .size(166.dp)
                    .graphicsLayer {
                        scaleX = heroGlow.first
                        scaleY = heroGlow.first
                        alpha = heroGlow.second
                    }
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(accent.copy(alpha = 0.82f), Color.Transparent)))
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 5.dp, bottom = 2.dp)
                    ) {
                        Box(
                            Modifier.fillMaxWidth(0.58f * heroDivider.value)
                                .height(2.dp).clip(CircleShape)
                                .background(Brush.horizontalGradient(listOf(accent, accent.copy(alpha = 0.12f))))
                        )
                        Spacer(Modifier.width(5.dp))
                        Box(
                            Modifier.size(4.dp * heroDivider.value).clip(CircleShape)
                                .background(accent.copy(alpha = 0.82f))
                        )
                    }
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
                    pulse = pulseIcon
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
    val motionAllowed = remember(context, MashxallMotionPreferences.reduceMotion) { systemAllowsDecorativeMotion(context) }
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
            Modifier.fillMaxSize().padding(size * 0.105f)
                .border(1.dp, tint.copy(alpha = 0.13f), CircleShape)
        )
        Box(
            Modifier.fillMaxSize().padding(size * 0.205f)
                .border(0.7.dp, tint.copy(alpha = 0.085f), CircleShape)
        )
        (0 until 60).forEach { tick ->
            val major = tick % 5 == 0
            val radians = Math.toRadians(tick * 6.0 - 90.0)
            val radius = size.value / 2f - if (major) 7.5f else 4.5f
            val x = (radius * cos(radians)).dp
            val y = (radius * sin(radians)).dp
            Box(
                Modifier.align(Alignment.Center).offset(x = x, y = y)
                    .graphicsLayer { rotationZ = tick * 6f }
                    .size(if (major) 1.8.dp else 1.dp, if (major) 9.dp else 4.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = if (major) 0.86f else 0.34f))
            )
        }
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


/** Action-specific press depths used by prominent calls across Quran, prayer, backup and worship screens. */
internal fun premiumActionButtonPressScale(action: String): Float = when (action) {
    "try_again" -> 0.952f
    "browse_reciters" -> 0.963f
    "share_tafsir" -> 0.969f
    "retry_mushaf" -> 0.949f
    "open_bookmark" -> 0.942f
    "load_more_hadith" -> 0.958f
    "azkar_count" -> 0.935f
    "tasbih_reset" -> 0.938f
    "tasbih_new_round" -> 0.945f
    "backup_save" -> 0.951f
    "backup_restore" -> 0.962f
    "pdf_download" -> 0.966f
    "browse_adhan" -> 0.971f
    "choose_adhan_sound" -> 0.956f
    "exact_alarms" -> 0.964f
    "browse_online_adhan" -> 0.968f
    "word_study" -> 0.948f
    "farmuda_copy" -> 0.975f
    else -> 0.956f
}

/** Prominent native action surface shared by high-value screen actions. */
@Composable
internal fun PremiumActionButton(
    label: String,
    icon: ImageVector,
    accent: Color,
    foreground: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    pressedScale: Float = 0.956f,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val motionAllowed = remember(context, MashxallMotionPreferences.reduceMotion) { systemAllowsDecorativeMotion(context) }
    val fillAlpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.45f,
        animationSpec = if (motionAllowed) tween(180) else snap(),
        label = "Mashxall action button enabled state"
    )
    val outline by animateColorAsState(
        targetValue = accent.copy(alpha = if (enabled) 0.48f else 0.18f),
        animationSpec = if (motionAllowed) tween(180) else snap(),
        label = "Mashxall action button outline"
    )
    Row(
        modifier = modifier.clip(RoundedCornerShape(17.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(accent.copy(alpha = 0.22f * fillAlpha), accent.copy(alpha = 0.065f * fillAlpha))
                )
            )
            .border(1.dp, outline, RoundedCornerShape(17.dp))
            .premiumClickable(
                enabled = enabled,
                role = Role.Button,
                pressedScale = pressedScale,
                onClick = onClick
            )
            .padding(horizontal = 13.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(32.dp).clip(RoundedCornerShape(11.dp))
                .background(accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            if (loading) {
                PremiumLoadingIndicator(
                    modifier = Modifier.size(18.dp),
                    color = accent.copy(alpha = fillAlpha),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(icon, contentDescription = null, tint = accent.copy(alpha = fillAlpha), modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            label,
            color = foreground.copy(alpha = fillAlpha),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            lineHeight = 15.sp,
            modifier = Modifier.weight(1f)
        )
        if (!loading) {
            Spacer(Modifier.width(8.dp))
            Text("›", color = accent.copy(alpha = fillAlpha), fontSize = 24.sp, fontWeight = FontWeight.Light)
        }
    }
}


/** Map a slider value to a bounded tactile tick, including continuous sliders. */
internal fun premiumChoiceIconScale(selected: Boolean): Float = if (selected) 1.10f else 1f

internal fun premiumChoiceIconRotationDegrees(selected: Boolean): Float = if (selected) -8f else 0f

internal fun premiumSliderHapticBucket(
    value: Float,
    rangeStart: Float,
    rangeEnd: Float,
    steps: Int
): Int {
    if (!value.isFinite() || !rangeStart.isFinite() || !rangeEnd.isFinite() || rangeEnd <= rangeStart) {
        return 0
    }
    // Compose Slider.steps are intermediate stops. Cap the tick count and
    // resolve exact half-way values toward the lower bucket to avoid an
    // extra haptic tick at a tie.
    val segmentCount = if (steps > 0) steps.coerceAtMost(99) + 1 else 20
    val fraction = ((value - rangeStart) / (rangeEnd - rangeStart)).coerceIn(0f, 1f)
    val position = fraction * segmentCount
    return ceil(position - 0.5f).toInt().coerceIn(0, segmentCount)
}

/**
 * Native Material slider with a restrained drag lift, accent-aware track, and
 * tactile ticks. The user's value callback remains the source of truth.
 */
@Composable
internal fun PremiumSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    accent: Color,
    foreground: Color,
    modifier: Modifier = Modifier,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val motionAllowed = remember(context, MashxallMotionPreferences.reduceMotion) {
        systemAllowsDecorativeMotion(context)
    }
    val haptics = LocalHapticFeedback.current
    var dragging by remember { mutableStateOf(false) }
    var lastBucket by remember(valueRange, steps) {
        mutableStateOf(premiumSliderHapticBucket(value, valueRange.start, valueRange.endInclusive, steps))
    }
    val dragScale by animateFloatAsState(
        targetValue = if (dragging) 1.025f else 1f,
        animationSpec = if (motionAllowed) spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ) else snap(),
        label = "Mashxall slider drag response"
    )
    Slider(
        value = value.coerceIn(valueRange),
        onValueChange = { next ->
            val bucket = premiumSliderHapticBucket(
                next, valueRange.start, valueRange.endInclusive, steps
            )
            if (dragging && bucket != lastBucket) {
                haptics.performMashxallHaptic(HapticFeedbackType.TextHandleMove)
            }
            lastBucket = bucket
            dragging = true
            onValueChange(next.coerceIn(valueRange))
        },
        onValueChangeFinished = {
            dragging = false
            onValueChangeFinished?.invoke()
        },
        valueRange = valueRange,
        steps = steps.coerceAtLeast(0),
        colors = SliderDefaults.colors(
            thumbColor = accent,
            activeTrackColor = accent,
            inactiveTrackColor = foreground.copy(alpha = 0.14f),
            activeTickColor = accent,
            inactiveTickColor = foreground.copy(alpha = 0.10f)
        ),
        modifier = modifier.graphicsLayer { scaleY = dragScale }
    )
}

/**
 * Accessible page navigation for long-form readers. The preview value follows
 * the user's finger; a page is rendered only after the drag is released, so
 * rapid scrubbing never starts hundreds of expensive PDF/SVG render jobs.
 */
@Composable
internal fun PremiumPageScrubber(
    currentPage: Int,
    pageCount: Int,
    pageLabel: String,
    foreground: Color,
    accent: Color,
    modifier: Modifier = Modifier,
    onPageSelected: (Int) -> Unit
) {
    if (pageCount <= 1) return
    val safeCurrent = currentPage.coerceIn(1, pageCount)
    val previewPage = remember(safeCurrent, pageCount) {
        mutableStateOf(safeCurrent.toFloat())
    }
    val previewNumber = previewPage.value.roundToInt().coerceIn(1, pageCount)
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                pageLabel.uppercase(),
                color = foreground.copy(alpha = 0.55f),
                fontSize = 8.sp,
                letterSpacing = 0.8.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            Text(
                "$previewNumber / $pageCount",
                color = accent,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
        PremiumSlider(
            value = previewPage.value,
            onValueChange = { previewPage.value = it },
            onValueChangeFinished = {
                val selected = previewPage.value.roundToInt().coerceIn(1, pageCount)
                if (selected != safeCurrent) onPageSelected(selected)
            },
            valueRange = 1f..pageCount.toFloat(),
            accent = accent,
            foreground = foreground,
            modifier = Modifier.fillMaxWidth()
        )
    }
}


/** Compact expressive icon button for navigation, location and favourite actions. */
@Composable
internal fun PremiumIconButton(
    icon: ImageVector,
    contentDescription: String?,
    accent: Color,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    enabled: Boolean = true,
    selected: Boolean = false,
    rotationDegrees: Float = 0f,
    circular: Boolean = false,
    hapticFeedback: Boolean = true,
    pressedScale: Float = 0.968f,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val motionAllowed = remember(context, MashxallMotionPreferences.reduceMotion) { systemAllowsDecorativeMotion(context) }
    val shape = if (circular) CircleShape else RoundedCornerShape(size * 0.36f)
    val fill by animateColorAsState(
        targetValue = when {
            !enabled -> accent.copy(alpha = 0.025f)
            selected -> accent.copy(alpha = 0.18f)
            else -> accent.copy(alpha = 0.075f)
        },
        animationSpec = if (motionAllowed) tween(190, easing = FastOutSlowInEasing) else snap(),
        label = "Mashxall icon button fill"
    )
    val outline by animateColorAsState(
        targetValue = accent.copy(alpha = if (enabled && selected) 0.50f else if (enabled) 0.19f else 0.08f),
        animationSpec = if (motionAllowed) tween(190, easing = FastOutSlowInEasing) else snap(),
        label = "Mashxall icon button outline"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (enabled && selected) 1.12f else 1f,
        animationSpec = if (motionAllowed) spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium) else snap(),
        label = "Mashxall icon button scale"
    )
    val iconRotation by animateFloatAsState(
        targetValue = rotationDegrees + if (selected) -8f else 0f,
        animationSpec = if (motionAllowed) spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium) else snap(),
        label = "Mashxall icon button rotation"
    )
    Box(
        modifier = modifier.size(size)
            .graphicsLayer { scaleX = iconScale; scaleY = iconScale }
            .clip(shape)
            .background(Brush.linearGradient(listOf(fill, fill.copy(alpha = fill.alpha * 0.55f))))
            .border(1.dp, outline, shape)
            .premiumClickable(
                enabled = enabled,
                onClickLabel = contentDescription,
                role = Role.Button,
                hapticFeedback = hapticFeedback,
                pressedScale = pressedScale,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = accent.copy(alpha = if (enabled) 1f else 0.30f),
            modifier = Modifier.size(size * 0.48f).graphicsLayer { rotationZ = iconRotation }
        )
        AnimatedVisibility(
            visible = enabled && selected,
            modifier = Modifier.align(Alignment.TopEnd).offset(x = (-2).dp, y = 2.dp),
            enter = if (motionAllowed) scaleIn(
                initialScale = 0.25f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
            ) + fadeIn(tween(110)) else fadeIn(tween(0)),
            exit = if (motionAllowed) scaleOut(
                targetScale = 0.35f,
                animationSpec = tween(100)
            ) + fadeOut(tween(90)) else fadeOut(tween(0))
        ) {
            Box(
                Modifier.size(8.dp).clip(CircleShape)
                    .background(accent)
                    .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
    }
}


/** Tactile wrapper for icon actions whose inner content changes (waveform, loader, play/pause). */
@Composable
internal fun PremiumIconActionSurface(
    accent: Color,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    enabled: Boolean = true,
    selected: Boolean = false,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current
    val motionAllowed = remember(context, MashxallMotionPreferences.reduceMotion) { systemAllowsDecorativeMotion(context) }
    val fill by animateColorAsState(
        targetValue = when {
            !enabled -> accent.copy(alpha = 0.025f)
            selected -> accent.copy(alpha = 0.18f)
            else -> accent.copy(alpha = 0.075f)
        },
        animationSpec = if (motionAllowed) tween(190, easing = FastOutSlowInEasing) else snap(),
        label = "Mashxall icon action surface fill"
    )
    val outline by animateColorAsState(
        targetValue = accent.copy(alpha = if (enabled && selected) 0.50f else if (enabled) 0.19f else 0.08f),
        animationSpec = if (motionAllowed) tween(190, easing = FastOutSlowInEasing) else snap(),
        label = "Mashxall icon action surface outline"
    )
    val scale by animateFloatAsState(
        targetValue = if (enabled && selected) 1.055f else 1f,
        animationSpec = if (motionAllowed) spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium) else snap(),
        label = "Mashxall icon action surface scale"
    )
    Box(
        modifier = modifier.size(size)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(size * 0.36f))
            .background(Brush.linearGradient(listOf(fill, fill.copy(alpha = fill.alpha * 0.55f))))
            .border(1.dp, outline, RoundedCornerShape(size * 0.36f))
            .semantics(mergeDescendants = true) {
                if (contentDescription != null) this.contentDescription = contentDescription
            }
            .premiumClickable(
                enabled = enabled,
                onClickLabel = contentDescription,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
        AnimatedVisibility(
            visible = enabled && selected,
            modifier = Modifier.align(Alignment.TopEnd).offset(x = (-2).dp, y = 2.dp),
            enter = if (motionAllowed) scaleIn(
                initialScale = 0.25f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
            ) + fadeIn(tween(110)) else fadeIn(tween(0)),
            exit = if (motionAllowed) scaleOut(
                targetScale = 0.35f,
                animationSpec = tween(100)
            ) + fadeOut(tween(90)) else fadeOut(tween(0))
        ) {
            Box(
                Modifier.size(8.dp).clip(CircleShape)
                    .background(accent)
                    .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
    }
}
