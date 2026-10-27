package com.unrot.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.unrot.app.domain.model.LimitedApp
import com.unrot.app.ui.theme.AppIcon
import com.unrot.app.ui.theme.GlassCard
import com.unrot.app.ui.theme.RingProgress
import com.unrot.app.ui.theme.rememberAppColor
import com.unrot.app.ui.theme.UnrotBlobBlue
import com.unrot.app.ui.theme.UnrotError
import com.unrot.app.ui.theme.UnrotPrimary
import com.unrot.app.ui.theme.UnrotSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToApps: () -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAllUsage: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val apps by viewModel.limitedApps.collectAsState()
    val screenTimeMs by viewModel.screenTimeMs.collectAsState()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Unrot", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                actions = {
                    IconButton(onClick = onNavigateToStats) {
                        Icon(Icons.Default.BarChart, contentDescription = "Stats")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToApps,
                containerColor = UnrotPrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add apps")
            }
        }
    ) { padding ->
        if (apps.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "No apps limited yet",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Tap + to pick social media apps and set daily time limits.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(24.dp))
                ScreenTimeCard(screenTimeMs, apps, onNavigateToAllUsage)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    ScreenTimeCard(screenTimeMs, apps, onNavigateToAllUsage)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Today's limits",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                items(apps, key = { it.packageName }) { app ->
                    AppLimitCard(app)
                }
                item { Spacer(modifier = Modifier.height(104.dp)) }
            }
        }
    }
}

@Composable
private fun ScreenTimeCard(screenTimeMs: Long, apps: List<LimitedApp>, onClick: () -> Unit) {
    val totalMinutes = (screenTimeMs / 60_000).toInt()
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    val formattedTime = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"

    val totalBudgetMs = apps.sumOf { it.totalBudgetMs }
    val totalUsedMs = apps.sumOf { it.usedTodayMs }
    val progress = if (totalBudgetMs > 0) (totalUsedMs.toFloat() / totalBudgetMs).coerceIn(0f, 1f) else 0f

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        tint = UnrotBlobBlue,
        tintAlpha = 0.28f
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Screen time today", fontWeight = FontWeight.SemiBold)
                Text(
                    formattedTime,
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (apps.isEmpty()) "Unlocked time since midnight" else "Total device time · tap to see every app",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            }
            if (apps.isNotEmpty()) {
                Spacer(modifier = Modifier.width(12.dp))
                RingProgress(
                    progress = progress,
                    modifier = Modifier.size(64.dp),
                    strokeWidth = 8.dp
                ) {
                    Text(
                        "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        if (apps.isNotEmpty()) {
            Text(
                "Ring shows % of today's app budgets used, not total screen time",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 14.dp)
            )
        }
    }
}

@Composable
private fun AppLimitCard(app: LimitedApp) {
    val progress = if (app.totalBudgetMs > 0) {
        (app.usedTodayMs.toFloat() / app.totalBudgetMs).coerceIn(0f, 1f)
    } else 0f
    val isOver = app.isOverLimit

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            AppIcon(packageName = app.packageName, appName = app.appName, size = 44.dp)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(app.appName, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (isOver) "Blocked" else "${app.remainingMinutes} min left",
                        color = if (isOver) UnrotError else UnrotSuccess,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.35f))
                ) {
                    val brand by rememberAppColor(app.packageName)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.coerceIn(0.015f, 1f))
                            .height(8.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
                            .background(
                                androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    listOf(
                                        if (isOver) UnrotError else brand,
                                        if (isOver) UnrotError.copy(alpha = 0.6f) else brand.copy(alpha = 0.6f)
                                    )
                                )
                            )
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${app.usedMinutes} / ${app.dailyBudgetMinutes} min used",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}
