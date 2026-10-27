package com.unrot.app.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** One slice of a [DonutChart]. */
data class ChartSlice(val label: String, val value: Float, val color: Color)

/** One bar of a [WeeklyBarChart]. */
data class BarEntry(val label: String, val value: Float, val highlighted: Boolean = false)

/**
 * A ring-shaped progress indicator (single value against a track), used for "today's screen
 * time used" on the Home screen. Renders with rounded end-caps for a soft, modern feel.
 */
@Composable
fun RingProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 14.dp,
    trackColor: Color = Color.White.copy(alpha = 0.40f),
    progressBrush: Brush = Brush.sweepGradient(listOf(UnrotBlobBlue, UnrotPrimary, UnrotBlobPurple, UnrotPrimary)),
    content: @Composable () -> Unit = {}
) {
    val strokePx = with(LocalDensity.current) { strokeWidth.toPx() }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val diameter = minOf(size.width, size.height) - strokePx
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
                topLeft = topLeft,
                size = arcSize
            )
            drawArc(
                brush = progressBrush,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
                topLeft = topLeft,
                size = arcSize
            )
        }
        content()
    }
}

/**
 * A donut chart for showing how a total splits across categories (e.g. time spent per app this
 * week). Pass [centerValue]/[centerLabel] to show a summary in the hole.
 */
@Composable
fun DonutChart(
    data: List<ChartSlice>,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 20.dp,
    centerLabel: String? = null,
    centerValue: String? = null
) {
    val total = data.sumOf { it.value.toDouble() }.toFloat().coerceAtLeast(0.0001f)
    val strokePx = with(LocalDensity.current) { strokeWidth.toPx() }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            var startAngle = -90f
            val diameter = minOf(size.width, size.height) - strokePx
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            data.forEach { slice ->
                val sweep = 360f * (slice.value / total)
                drawArc(
                    color = slice.color,
                    startAngle = startAngle,
                    sweepAngle = (sweep - 3f).coerceAtLeast(1f),
                    useCenter = false,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round),
                    topLeft = topLeft,
                    size = arcSize
                )
                startAngle += sweep
            }
        }
        if (centerValue != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(centerValue, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                if (centerLabel != null) {
                    Text(
                        centerLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

/** A compact legend row that pairs each [ChartSlice]'s color dot with its label and share. */
@Composable
fun DonutLegend(data: List<ChartSlice>, modifier: Modifier = Modifier) {
    val total = data.sumOf { it.value.toDouble() }.toFloat().coerceAtLeast(0.0001f)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        data.forEach { slice ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(slice.color)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    slice.label,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${((slice.value / total) * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/** A simple vertical bar chart, used for the 7-day usage trend on the Stats screen. */
@Composable
fun WeeklyBarChart(
    entries: List<BarEntry>,
    modifier: Modifier = Modifier,
    height: Dp = 120.dp,
    barColor: Color = UnrotPrimary,
    highlightColor: Color = UnrotBlobPurple
) {
    val maxVal = (entries.maxOfOrNull { it.value } ?: 0f).coerceAtLeast(1f)
    Row(
        modifier = modifier.fillMaxWidth().height(height),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        entries.forEach { entry ->
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                val fraction = (entry.value / maxVal).coerceIn(0.035f, 1f)
                val color = if (entry.highlighted) highlightColor else barColor
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .fillMaxHeight(fraction)
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.verticalGradient(listOf(color, color.copy(alpha = 0.55f)))
                        )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    entry.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/** A slim horizontal bar used to rank apps by usage (All Usage screen). */
@Composable
fun HorizontalBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    color: Color = UnrotPrimary,
    height: Dp = 8.dp
) {
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.35f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0.02f, 1f))
                .clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(listOf(color, color.copy(alpha = 0.6f))))
        )
    }
}
