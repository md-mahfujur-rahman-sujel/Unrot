package com.unrot.app.ui.usage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unrot.app.data.repository.LimitRepository
import com.unrot.app.domain.model.AppUsage
import com.unrot.app.ui.theme.AppIcon
import com.unrot.app.ui.theme.GlassCard
import com.unrot.app.ui.theme.HorizontalBar
import com.unrot.app.ui.theme.rememberAppColor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllUsageScreen(onBack: () -> Unit, viewModel: AllUsageViewModel = hiltViewModel()) {
    val usage by viewModel.usage.collectAsState()
    val maxUsage = (usage.maxOfOrNull { it.usageMs } ?: 0L).coerceAtLeast(1L)

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("All app usage", fontWeight = FontWeight.Bold) },
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
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Today · resets at midnight",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(4.dp))
            }
            if (usage.isEmpty()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "No app usage recorded yet today.",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                items(usage, key = { it.packageName }) { app ->
                    UsageCard(app, fraction = app.usageMs.toFloat() / maxUsage)
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun UsageCard(app: AppUsage, fraction: Float) {
    val minutes = (app.usageMs / 60_000).toInt()
    val time = if (minutes >= 60) "${minutes / 60}h ${minutes % 60}m" else "${minutes}m"
    val brand by rememberAppColor(app.packageName)
    GlassCard(modifier = Modifier.fillMaxWidth(), tint = brand, tintAlpha = 0.18f) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(packageName = app.packageName, appName = app.appName, size = 36.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(app.appName, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(time, color = brand, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalBar(fraction = fraction, modifier = Modifier.fillMaxWidth(), color = brand, height = 6.dp)
        }
    }
}

@HiltViewModel
class AllUsageViewModel @Inject constructor(
    private val limitRepository: LimitRepository
) : ViewModel() {
    private val _usage = MutableStateFlow<List<AppUsage>>(emptyList())
    val usage = _usage.asStateFlow()

    init {
        viewModelScope.launch { _usage.value = limitRepository.getTodayAllAppUsage() }
    }
}
