package com.unrot.app.ui.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.unrot.app.domain.model.PermissionState
import com.unrot.app.ui.theme.GlassCard
import com.unrot.app.ui.theme.UnrotBlobBlue
import com.unrot.app.ui.theme.UnrotBlobMint
import com.unrot.app.ui.theme.UnrotBlobPurple
import com.unrot.app.ui.theme.UnrotBlobYellow
import com.unrot.app.ui.theme.UnrotError
import com.unrot.app.ui.theme.UnrotPrimary
import com.unrot.app.ui.theme.UnrotSuccess
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

/** Fixed height every onboarding card (step cards + the two summary cards) renders at, so
 *  swiping between pages never triggers a layout jump from differing content lengths. */
private val CARD_HEIGHT = 400.dp

private data class OnboardingStep(
    val key: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val required: Boolean,
    val tint: Color,
    val isGranted: (PermissionState) -> Boolean
)

private val STEPS = listOf(
    OnboardingStep(
        key = "usage_access",
        title = "Usage Access",
        description = "Lets Unrot see which app is active right now, so your screen time is tracked accurately.",
        icon = Icons.Default.QueryStats,
        required = true,
        tint = UnrotBlobBlue,
        isGranted = { it.usageAccessGranted }
    ),
    OnboardingStep(
        key = "overlay",
        title = "Display Over Apps",
        description = "Lets Unrot show the block screen the moment one of your time limits is reached.",
        icon = Icons.Default.Layers,
        required = true,
        tint = UnrotBlobPurple,
        isGranted = { it.overlayGranted }
    ),
    OnboardingStep(
        key = "battery",
        title = "Battery Optimization",
        description = "Keeps Unrot's monitoring service running reliably in the background on your device.",
        icon = Icons.Default.BatteryAlert,
        required = false,
        tint = UnrotBlobMint,
        isGranted = { it.batteryOptimizationDisabled }
    ),
    OnboardingStep(
        key = "notifications",
        title = "Notifications",
        description = "Lets Unrot show monitoring status updates and time-remaining reminders.",
        icon = Icons.Default.Notifications,
        required = false,
        tint = UnrotBlobYellow,
        isGranted = { it.notificationsGranted }
    )
)

/** Index of the first step at or after [from] that still needs the user's attention. Returns
 *  [STEPS].size (the summary page) once everything from [from] onward is granted or skipped. */
private fun nextUndonePage(from: Int, state: PermissionState, skipped: Set<String>): Int {
    var i = from
    while (i < STEPS.size && (STEPS[i].isGranted(state) || STEPS[i].key in skipped)) {
        i++
    }
    return i
}

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // One page per permission step, plus a trailing summary page.
    val pagerState = rememberPagerState(initialPage = 0) { STEPS.size + 1 }
    val skipped = remember { mutableStateOf(setOf<String>()) }
    // The furthest page the auto-advance flow has reached on its own — used so that swiping
    // *back* to review an earlier card doesn't immediately auto-forward the user away again.
    var frontier by remember { mutableStateOf(0) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.refreshPermissions() }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshPermissions()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Auto-advance: only fires when the *frontier* card's permission gets granted (e.g. returning
    // from Settings) — not when the user manually swipes back to an already-granted card.
    LaunchedEffect(pagerState.currentPage, state) {
        val page = pagerState.currentPage
        if (page != frontier) return@LaunchedEffect
        val step = STEPS.getOrNull(page) ?: return@LaunchedEffect
        if (step.isGranted(state)) {
            delay(700)
            val next = nextUndonePage(page + 1, state, skipped.value)
            frontier = next
            pagerState.animateScrollToPage(next)
        }
    }

    // Drives the "Skip for now" control rendered below the pager (outside every card) — only
    // shown while the currently-focused page is an optional, not-yet-granted, not-yet-skipped step.
    val currentStep = STEPS.getOrNull(pagerState.currentPage)
    val canSkipCurrent = currentStep != null &&
        !currentStep.required &&
        !currentStep.isGranted(state) &&
        currentStep.key !in skipped.value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Take back your time.",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Set limits on social media. When time runs out, solve math problems to earn more — turning mindless scrolling into a deliberate choice.",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Step dots — tap to jump straight to that card. Filled green once granted/skipped.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            STEPS.forEachIndexed { index, step ->
                val done = step.isGranted(state) || step.key in skipped.value
                val active = index == pagerState.currentPage
                val dotSize by animateDpAsState(
                    targetValue = if (active) 10.dp else 8.dp,
                    label = "onboarding_dot_size"
                )
                val dotColor by animateColorAsState(
                    targetValue = when {
                        done -> UnrotSuccess
                        active -> UnrotPrimary
                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.20f)
                    },
                    label = "onboarding_dot_color"
                )
                Box(
                    modifier = Modifier
                        .padding(horizontal = 5.dp)
                        .size(dotSize)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Box(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.TopCenter
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 32.dp),
                pageSpacing = 14.dp
            ) { page ->
                // Distance of this page from the currently-settled page: 0f when fully focused,
                // up to 1f for a fully-adjacent page. Drives the scale/fade/lift below so the
                // active card visibly pops forward while swiping instead of every card sitting
                // flat and identical.
                val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                    .absoluteValue
                    .coerceIn(0f, 1f)
                // Shadow fades out in step with alpha instead of staying fixed at full elevation.
                // A semi-transparent card that still casts a shadow gets composited by Android
                // into an offscreen layer sized to its rectangular bounds, and the shadow's soft
                // blur gets hard-cut at that rectangle — which is what was showing up as a sharp
                // rectangular box inside/around the card mid-swipe. The fully focused card sits at
                // alpha = 1, so it never takes that path and keeps its full shadow.
                val cardElevation = lerp(0f, 16f, 1f - pageOffset).dp

                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            val scale = lerp(0.85f, 1f, 1f - pageOffset)
                            scaleX = scale
                            scaleY = scale
                            alpha = lerp(0.35f, 1f, 1f - pageOffset)
                            translationY = lerp(28f, 0f, 1f - pageOffset)
                        }
                ) {
                    val step = STEPS.getOrNull(page)
                    if (step == null) {
                        // Trailing summary page.
                        if (state.allGranted) {
                            AllSetCard(
                                elevation = cardElevation,
                                onContinue = {
                                    viewModel.completeOnboarding()
                                    onComplete()
                                }
                            )
                        } else {
                            val missing = STEPS.filter { it.required && !it.isGranted(state) }
                            NotReadyCard(
                                missing = missing,
                                elevation = cardElevation,
                                onReview = {
                                    val target = STEPS.indexOfFirst { it.required && !it.isGranted(state) }
                                        .coerceAtLeast(0)
                                    frontier = target
                                    scope.launch { pagerState.animateScrollToPage(target) }
                                }
                            )
                        }
                    } else {
                        val granted = step.isGranted(state)
                        StepCard(
                            step = step,
                            granted = granted,
                            elevation = cardElevation,
                            onGrant = {
                                when (step.key) {
                                    "usage_access" -> context.startActivity(viewModel.usageAccessIntent())
                                    "overlay" -> context.startActivity(viewModel.overlayIntent(context))
                                    "battery" -> context.startActivity(viewModel.batteryIntent(context))
                                    "notifications" -> notificationPermissionLauncher.launch(
                                        android.Manifest.permission.POST_NOTIFICATIONS
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Reserves the same vertical space whether or not a skip control is showing, so the
        // "Swipe to browse permissions" hint below never jumps up/down between pages.
        Box(
            modifier = Modifier.fillMaxWidth().height(40.dp),
            contentAlignment = Alignment.Center
        ) {
            if (canSkipCurrent) {
                val skipStep = currentStep
                TextButton(onClick = {
                    if (skipStep == null) return@TextButton
                    val page = pagerState.currentPage
                    skipped.value = skipped.value + skipStep.key
                    val next = nextUndonePage(page + 1, state, skipped.value)
                    frontier = maxOf(frontier, next)
                    scope.launch { pagerState.animateScrollToPage(next) }
                }) {
                    Text("Skip for now")
                }
            }
        }

        Text(
            "Swipe to browse permissions",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun StepCard(
    step: OnboardingStep,
    granted: Boolean,
    elevation: androidx.compose.ui.unit.Dp = 16.dp,
    onGrant: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth().height(CARD_HEIGHT),
        tint = step.tint,
        tintAlpha = 0.30f,
        elevation = elevation
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                if (granted) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Granted",
                        tint = UnrotSuccess,
                        modifier = Modifier.size(34.dp)
                    )
                } else {
                    Icon(step.icon, contentDescription = null, modifier = Modifier.size(30.dp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                step.title,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                if (granted) "Granted" else if (step.required) "Required" else "Recommended",
                style = MaterialTheme.typography.labelLarge,
                color = if (granted) UnrotSuccess else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                step.description,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            )
            Spacer(modifier = Modifier.height(22.dp))
            if (!granted) {
                Button(
                    onClick = onGrant,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = UnrotPrimary)
                ) {
                    Text("Allow permission", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun AllSetCard(
    elevation: androidx.compose.ui.unit.Dp = 16.dp,
    onContinue: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth().height(CARD_HEIGHT),
        tint = UnrotSuccess,
        tintAlpha = 0.28f,
        elevation = elevation
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = UnrotSuccess,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "You're all set!",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Unrot is ready to help you take back your time.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            )
            Spacer(modifier = Modifier.height(22.dp))
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = UnrotPrimary)
            ) {
                Text("Continue", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** Shown on the trailing page when the user has swiped ahead without granting every required
 *  permission yet — explains what's still needed instead of silently blocking them. */
@Composable
private fun NotReadyCard(
    missing: List<OnboardingStep>,
    elevation: androidx.compose.ui.unit.Dp = 16.dp,
    onReview: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth().height(CARD_HEIGHT),
        tint = UnrotError,
        tintAlpha = 0.22f,
        elevation = elevation
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = UnrotError,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Almost there",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "A couple of required permissions are still missing: " +
                    missing.joinToString(", ") { it.title } + ".",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            )
            Spacer(modifier = Modifier.height(22.dp))
            Button(
                onClick = onReview,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = UnrotPrimary)
            ) {
                Text("Review permissions", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
