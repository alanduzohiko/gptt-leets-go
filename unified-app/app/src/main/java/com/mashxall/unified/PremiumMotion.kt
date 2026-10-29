package com.mashxall.unified

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role

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
        targetValue = if (enabled && pressed) 0.975f else 1f,
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
