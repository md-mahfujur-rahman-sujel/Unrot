package com.unrot.app.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.unrot.app.domain.model.DailyAppStats
import com.unrot.app.ui.theme.AppIcon
import com.unrot.app.ui.theme.ChartPalette
import com.unrot.app.ui.theme.ChartSlice
import com.unrot.app.ui.theme.DonutChart
import com.unrot.app.ui.theme.DonutLegend
import com.unrot.app.ui.theme.GlassCard
import com.unrot.app.ui.theme.GlassPill
import com.unrot.app.ui.theme.HorizontalBar
import com.unrot.app.ui.theme.rememberAppColor
import com.unrot.app.ui.theme.rememberAppColors
import com.unrot.app.ui.theme.UnrotBlobBlue
import com.unrot.app.ui.theme.UnrotBlobPurple
import com.unrot.app.ui.theme.UnrotError
import com.unrot.app.ui.theme.UnrotSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onBack: () -> Unit,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val stats by viewModel.stats.collectAsState()
    val unlockCount by viewModel.unlockCountToday.collectAsState()

    val topStats = stats.sortedByDescending { it.usageMs }.take(6)
    val chartColors = rememberAppColors(topStats.map { it.packageName })
    val chartSlices = topStats.map { stat ->
            ChartSlice(
                label = stat.appName,
                value = (stat.usageMs / 60_000f).coerceAtLeast(0.5f),
                color = chartColors[stat.packageName] ?: UnrotBlobBlue
            )
        }
    val totalWeeklyMinutes = stats.sumOf { it.usageMs / 60_000L }.toInt()
    val maxUsage = (stats.maxOfOrNull { it.usageMs } ?: 0L).coerceAtLeast(1L)

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Stats", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GlassCard(
                        modifier = Modifier.weight(1f),
                        tint = UnrotBlobPurple,
                        tintAlpha = 0.28f
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Unlock requests",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "$unlockCount",
                                style = MaterialTheme.typography.displayLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "today",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                            )
                        }
                    }
                    GlassCard(
                        modifier = Modifier.weight(1f),
                        tint = UnrotBlobBlue,
                        tintAlpha = 0.28f
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Total this week",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                if (totalWeeklyMinutes >= 60) "${totalWeeklyMinutes / 60}h ${totalWeeklyMinutes % 60}m" else "${totalWeeklyMinutes}m",
                                style = MaterialTheme.typography.displayLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "across ${stats.size} app(s)",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                            )
                        }
                    }
                }
            }
            if (chartSlices.isNotEmpty()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                "Where your time goes",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Share of screen time by app this week",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                DonutChart(
                                    data = chartSlices,
                                    modifier = Modifier.size(140.dp),
                                    centerValue = if (totalWeeklyMinutes >= 60) "${totalWeeklyMinutes / 60}h" else "${totalWeeklyMinutes}m",
                                    centerLabel = "this week"
                                )
                                Spacer(modifier = Modifier.width(20.dp))
                                DonutLegend(data = chartSlices, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            item {
                Text(
                    "Last 7 days",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            if (stats.isEmpty()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "No usage data yet. Limit some apps to start tracking.",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                items(stats.sortedByDescending { it.usageMs }, key = { it.packageName }) { stat ->
                    StatsCard(stat, fraction = stat.usageMs.toFloat() / maxUsage)
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun StatsCard(stat: DailyAppStats, fraction: Float) {
    val usedMinutes = (stat.usageMs / 60_000).toInt()
    val previousMinutes = (stat.previousWeekUsageMs / 60_000).toInt()
    val trendIcon = when {
        stat.previousWeekUsageMs == 0L -> Icons.Filled.TrendingFlat
        stat.usageMs < stat.previousWeekUsageMs -> Icons.Filled.TrendingDown
        stat.usageMs > stat.previousWeekUsageMs -> Icons.Filled.TrendingUp
        else -> Icons.Filled.TrendingFlat
    }
    val trendColor = when {
        stat.previousWeekUsageMs == 0L -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        stat.usageMs < stat.previousWeekUsageMs -> UnrotSuccess
        stat.usageMs > stat.previousWeekUsageMs -> UnrotError
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    }
    val trend = when {
        stat.previousWeekUsageMs == 0L -> "First week of data"
        stat.usageMs < stat.previousWeekUsageMs -> "Down ${previousMinutes - usedMinutes} min"
        stat.usageMs > stat.previousWeekUsageMs -> "Up ${usedMinutes - previousMinutes} min"
        else -> "Same as last week"
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(packageName = stat.packageName, appName = stat.appName, size = 40.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(stat.appName, fontWeight = FontWeight.SemiBold)
                    Text(
                        "$usedMinutes min this week · ${stat.budgetMinutes} min/day",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                if (stat.unlockRequests > 0) {
                    GlassPill(tint = UnrotBlobPurple) {
                        Text(
                            "${stat.unlockRequests} unlocks",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            val brand by rememberAppColor(stat.packageName)
            HorizontalBar(
                fraction = fraction,
                modifier = Modifier.fillMaxWidth(),
                color = brand
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    trendIcon,
                    contentDescription = null,
                    tint = trendColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    trend,
                    style = MaterialTheme.typography.labelLarge,
                    color = trendColor
                )
            }
        }
    }
}
