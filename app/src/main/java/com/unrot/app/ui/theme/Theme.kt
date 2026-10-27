package com.unrot.app.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = UnrotPrimaryDark,
    onPrimary = Color.White,
    secondary = UnrotSecondary,
    background = UnrotBackground,
    surface = UnrotSurface,
    surfaceVariant = UnrotSurfaceVariant,
    onBackground = UnrotOnBackground,
    onSurface = UnrotOnSurface,
    error = UnrotError
)

@Composable
fun UnrotTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
        window.statusBarColor = Color.Transparent.toArgb()
        window.navigationBarColor = Color.Transparent.toArgb()
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}

/**
 * Shared ambient backdrop for the whole app: soft, slowly-drifting color blobs washed out under
 * a light scrim. Every [com.unrot.app.ui.components.GlassCard] is a translucent panel that sits
 * on top of this — that's what produces the iOS "Liquid Glass" look without needing a heavier
 * real-time blur pipeline.
 */
@Composable
fun GlassBackground(content: @Composable () -> Unit) {
    val transition = rememberInfiniteTransition(label = "unrot_blobs")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 26_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift"
    )

    Box(modifier = Modifier.fillMaxSize().background(UnrotBackground)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawBlob(Offset(w * (0.10f + 0.14f * drift), h * 0.10f), w * 0.62f, UnrotBlobBlue)
            drawBlob(Offset(w * (0.95f - 0.18f * drift), h * 0.22f), w * 0.5f, UnrotBlobPurple)
            drawBlob(Offset(w * (0.85f - 0.1f * drift), h * 0.78f), w * 0.55f, UnrotBlobPink)
            drawBlob(Offset(w * (0.15f + 0.12f * (1f - drift)), h * 0.92f), w * 0.5f, UnrotBlobMint)
        }
        Box(modifier = Modifier.fillMaxSize().background(UnrotBackground.copy(alpha = 0.58f)))
        content()
    }
}

private fun DrawScope.drawBlob(center: Offset, radius: Float, color: Color) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = 0.50f), color.copy(alpha = 0f)),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
}
