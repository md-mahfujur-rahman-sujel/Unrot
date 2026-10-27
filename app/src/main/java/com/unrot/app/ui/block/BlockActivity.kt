package com.unrot.app.ui.block

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.unrot.app.ui.theme.UnrotTheme
import dagger.hilt.android.AndroidEntryPoint

/** Why the block screen was shown. Schedule blocks cannot be bypassed with the math challenge. */
enum class BlockReason { BUDGET, SCHEDULE }

@AndroidEntryPoint
class BlockActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val packageName = intent.getStringExtra(EXTRA_PACKAGE) ?: finish().let { return }
        val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: packageName
        val reason = intent.getStringExtra(EXTRA_REASON)?.let {
            runCatching { BlockReason.valueOf(it) }.getOrNull()
        } ?: BlockReason.BUDGET

        setContent {
            UnrotTheme {
                BlockScreen(
                    packageName = packageName,
                    appName = appName,
                    reason = reason,
                    onGoHome = { goHome() },
                    onUnlocked = { finish() }
                )
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        goHome()
    }

    private fun goHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }

    companion object {
        private const val EXTRA_PACKAGE = "extra_package"
        private const val EXTRA_APP_NAME = "extra_app_name"
        private const val EXTRA_REASON = "extra_reason"

        fun createIntent(
            context: Context,
            packageName: String,
            appName: String,
            reason: BlockReason = BlockReason.BUDGET
        ): Intent =
            Intent(context, BlockActivity::class.java).apply {
                putExtra(EXTRA_PACKAGE, packageName)
                putExtra(EXTRA_APP_NAME, appName)
                putExtra(EXTRA_REASON, reason.name)
            }
    }
}

@Composable
fun BlockScreen(
    packageName: String,
    appName: String,
    reason: BlockReason,
    onGoHome: () -> Unit,
    onUnlocked: () -> Unit,
    viewModel: BlockViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.unlocked) {
        if (state.unlocked) onUnlocked()
    }

    LaunchedEffect(packageName, reason) {
        if (reason == BlockReason.SCHEDULE) viewModel.loadScheduleInfo(packageName)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (reason == BlockReason.SCHEDULE) {
            Text("Quiet hours", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "$appName is blocked right now",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                state.scheduleEndLabel?.let { "This app is blocked during your scheduled quiet hours. It unlocks again at $it." }
                    ?: "This app is blocked during your scheduled quiet hours.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Quiet hours can't be skipped by solving math problems — change them in Settings if needed.",
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(32.dp))
            OutlinedButton(
                onClick = onGoHome,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors()
            ) {
                Text("Go Home")
            }
            return@Column
        }

        Text("Time limit reached", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "$appName is locked",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "You've used your daily limit. Solve math problems to earn ${state.extraMinutes} extra minutes.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        if (state.showChallenge) {
            Spacer(modifier = Modifier.height(32.dp))
            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "${state.correctCount} / ${state.problemsRequired} solved",
                modifier = Modifier.padding(top = 8.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                state.currentProblem?.question ?: "",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = state.userAnswer,
                onValueChange = viewModel::onAnswerChanged,
                label = { Text("Your answer") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (state.feedback != null) {
                Text(
                    state.feedback!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = viewModel::submitAnswer,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.userAnswer.isNotBlank()
            ) {
                Text("Submit")
            }
        } else {
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { viewModel.startChallenge(packageName) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Solve to add time")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(
            onClick = onGoHome,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors()
        ) {
            Text("Go Home")
        }
    }
}
