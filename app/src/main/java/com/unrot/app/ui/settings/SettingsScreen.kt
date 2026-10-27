package com.unrot.app.ui.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.unrot.app.domain.model.MathDifficulty
import com.unrot.app.ui.theme.GlassCard
import com.unrot.app.ui.theme.UnrotBlobBlue
import com.unrot.app.ui.theme.UnrotBlobMint
import com.unrot.app.ui.theme.UnrotBlobPurple
import com.unrot.app.ui.theme.UnrotSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showPauseDialog by remember { mutableStateOf(false) }
    var pauseStep by remember { mutableStateOf(0) }

    if (showPauseDialog) {
        AlertDialog(
            onDismissRequest = { showPauseDialog = false; pauseStep = 0 },
            title = { Text("Pause protection?") },
            text = {
                Text(
                    when (pauseStep) {
                        0 -> "This will stop blocking apps. Are you sure?"
                        1 -> "Really? Your limits won't be enforced until you re-enable protection."
                        else -> "Last chance. Protection will be disabled."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (pauseStep < 2) {
                        pauseStep++
                    } else {
                        viewModel.setProtectionEnabled(false)
                        showPauseDialog = false
                        pauseStep = 0
                    }
                }) {
                    Text(if (pauseStep < 2) "Continue" else "Disable")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPauseDialog = false; pauseStep = 0 }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GlassCard(modifier = Modifier.fillMaxWidth(), tint = UnrotBlobPurple, tintAlpha = 0.24f) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Math difficulty", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MathDifficulty.entries.forEach { diff ->
                            FilterChip(
                                selected = state.difficulty == diff,
                                onClick = { viewModel.setDifficulty(diff) },
                                label = { Text(diff.name.lowercase().replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }
                }
            }

            GlassCard(modifier = Modifier.fillMaxWidth(), tint = UnrotBlobBlue, tintAlpha = 0.24f) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Extra time per unlock", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(3, 5, 10, 15).forEach { mins ->
                            FilterChip(
                                selected = state.extraMinutes == mins,
                                onClick = { viewModel.setExtraMinutes(mins) },
                                label = { Text("${mins} min") }
                            )
                        }
                    }
                }
            }

            GlassCard(modifier = Modifier.fillMaxWidth(), tint = UnrotBlobMint, tintAlpha = 0.24f) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Problems per unlock (base)", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(3, 5, 8, 12).forEach { count ->
                            FilterChip(
                                selected = state.baseProblems == count,
                                onClick = { viewModel.setBaseProblems(count) },
                                label = { Text("$count") }
                            )
                        }
                    }
                }
            }

            GlassCard(modifier = Modifier.fillMaxWidth(), tint = UnrotSuccess, tintAlpha = 0.20f) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Protection enabled", fontWeight = FontWeight.SemiBold)
                        Text(
                            if (state.protectionEnabled) "Apps are being monitored" else "Protection is paused",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (state.protectionEnabled) UnrotSuccess else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Switch(
                        checked = state.protectionEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                viewModel.setProtectionEnabled(true)
                            } else {
                                showPauseDialog = true
                            }
                        }
                    )
                }
            }
        }
    }
}
