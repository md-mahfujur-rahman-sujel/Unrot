package com.unrot.app.ui.apps

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import com.unrot.app.domain.model.InstalledAppInfo
import com.unrot.app.ui.theme.AppIcon
import com.unrot.app.ui.theme.GlassCard
import com.unrot.app.ui.theme.rememberAppColor
import com.unrot.app.ui.theme.UnrotPrimary
import com.unrot.app.ui.theme.UnrotBlobYellow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: AppPickerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Choose Apps to Limit", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    viewModel.saveSelections()
                    onDone()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = UnrotPrimary),
                enabled = state.selectedCount > 0
            ) {
                Text("Save ${state.selectedCount} app(s)", fontWeight = FontWeight.SemiBold)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.showSocialOnly,
                    onClick = { viewModel.toggleSocialFilter() },
                    label = { Text("Social only") }
                )
                FilterChip(
                    selected = state.showLimitedOnly,
                    onClick = { viewModel.toggleLimitedFilter() },
                    label = { Text("Limited") }
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 12.dp)
            ) {
                items(state.filteredApps, key = { it.packageName }) { app ->
                    AppPickerItem(
                        app = app,
                        budgetMinutes = state.budgets[app.packageName] ?: 30,
                        schedule = state.schedules[app.packageName],
                        onToggle = { viewModel.toggleApp(app) },
                        onToggleFavorite = { viewModel.toggleFavorite(app) },
                        onBudgetChange = { viewModel.setBudget(app.packageName, it) },
                        onScheduleRangeChange = { start, end -> viewModel.setScheduleRange(app.packageName, start, end) },
                        onScheduleDayToggle = { day -> viewModel.toggleScheduleDay(app.packageName, day) },
                        onScheduleClear = { viewModel.clearSchedule(app.packageName) }
                    )
                }
            }
        }
    }
}

private val QUIET_HOUR_PRESETS = listOf(
    "10PM–7AM" to (22 * 60 to 7 * 60),
    "11PM–6AM" to (23 * 60 to 6 * 60),
    "9PM–8AM" to (21 * 60 to 8 * 60)
)
private val WEEKDAY_LABELS = listOf(1 to "M", 2 to "T", 3 to "W", 4 to "T", 5 to "F", 6 to "S", 7 to "S")

@Composable
private fun AppPickerItem(
    app: InstalledAppInfo,
    budgetMinutes: Int,
    schedule: ScheduleConfig?,
    onToggle: () -> Unit,
    onToggleFavorite: () -> Unit,
    onBudgetChange: (Int) -> Unit,
    onScheduleRangeChange: (Int, Int) -> Unit,
    onScheduleDayToggle: (Int) -> Unit,
    onScheduleClear: () -> Unit
) {
    val brand by rememberAppColor(app.packageName)
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        tint = if (app.isLimited) brand else Color.White,
        tintAlpha = if (app.isLimited) 0.30f else 0.55f
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(packageName = app.packageName, appName = app.appName, size = 40.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(app.appName, fontWeight = FontWeight.Medium)
                    Text(
                        app.packageName,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                // Favorite/priority star — only meaningful once an app is limited, since only
                // limited apps can appear on Home. Tapping pins/unpins it from the Home dashboard.
                if (app.isLimited) {
                    IconButton(onClick = onToggleFavorite, modifier = Modifier.size(32.dp)) {
                        Icon(
                            if (app.isFavorite) Icons.Default.Star else Icons.Outlined.StarOutline,
                            contentDescription = if (app.isFavorite) "Remove from Home" else "Show on Home",
                            tint = if (app.isFavorite) UnrotBlobYellow else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
                Switch(checked = app.isLimited, onCheckedChange = { onToggle() })
            }
            if (app.isLimited) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(15, 30, 60, 90).forEach { mins ->
                        FilterChip(
                            selected = budgetMinutes == mins,
                            onClick = { onBudgetChange(mins) },
                            label = { Text("${mins}m") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Text(
                    "Quiet hours (hard block, no math bypass)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = schedule == null,
                        onClick = onScheduleClear,
                        label = { Text("None") }
                    )
                    QUIET_HOUR_PRESETS.forEach { (label, range) ->
                        FilterChip(
                            selected = schedule?.startMinutes == range.first && schedule.endMinutes == range.second,
                            onClick = { onScheduleRangeChange(range.first, range.second) },
                            label = { Text(label) }
                        )
                    }
                    // Trailing spacer so the last chip has breathing room instead of ending flush
                    // against the scroll edge (that flush edge was reading as a stray gray box).
                    Spacer(modifier = Modifier.width(4.dp))
                }
                if (schedule != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        WEEKDAY_LABELS.forEach { (day, label) ->
                            FilterChip(
                                selected = day in schedule.days,
                                onClick = { onScheduleDayToggle(day) },
                                label = { Text(label) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
