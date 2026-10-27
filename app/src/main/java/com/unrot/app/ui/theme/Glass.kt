package com.unrot.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * The base "frosted glass" panel used everywhere in Unrot: a translucent gradient fill, a soft
 * light-catching gradient border, and an ambient shadow — the layered look of iOS's Liquid Glass
 * material sitting on top of [GlassBackground]'s drifting color wash.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    tint: Color = Color.White,
    tintAlpha: Float = 0.55f,
    // Elevation of the drop shadow. Callers that fade a card's *alpha* toward 0 (e.g. an
    // unfocused card inside a swipeable pager) should also fade this toward 0.dp instead of
    // leaving it fixed: Android composites a semi-transparent RenderNode that casts a shadow into
    // an offscreen layer sized to its rectangular bounds, and the soft shadow blur gets hard-cut
    // at those bounds — which shows up as a faint rectangle with sharp corners sitting inside the
    // rounded card. A card at alpha = 1 never takes that offscreen path, so it's safe to keep
    // this at full elevation there.
    elevation: androidx.compose.ui.unit.Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val clickableModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Column(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.10f),
                spotColor = Color.Black.copy(alpha = 0.14f)
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        tint.copy(alpha = (tintAlpha + 0.16f).coerceIn(0f, 1f)),
                        tint.copy(alpha = (tintAlpha - 0.16f).coerceIn(0f, 1f))
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.85f), Color.White.copy(alpha = 0.16f))
                ),
                shape = shape
            )
            .then(clickableModifier),
        content = content
    )
}

/** A small pill-shaped glass badge, used for inline stat labels ("3 unlocks", "Blocked", etc). */
@Composable
fun GlassPill(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    content: @Composable ColumnScope.() -> Unit
) {
    GlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        tint = tint,
        tintAlpha = 0.65f,
        content = content
    )
}
