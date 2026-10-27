package com.unrot.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.unrot.app.ui.apps.AppPickerScreen
import com.unrot.app.ui.home.HomeScreen
import com.unrot.app.ui.onboarding.OnboardingScreen
import com.unrot.app.ui.settings.SettingsScreen
import com.unrot.app.ui.stats.StatsScreen
import com.unrot.app.ui.usage.AllUsageScreen

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Home : Screen("home")
    data object AppPicker : Screen("apps")
    data object Stats : Screen("stats")
    data object Settings : Screen("settings")
    data object AllUsage : Screen("all_usage")
}

@Composable
fun UnrotNavGraph(
    startDestination: String,
    navController: NavHostController = rememberNavController()
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Screen.AppPicker.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToApps = { navController.navigate(Screen.AppPicker.route) },
                onNavigateToStats = { navController.navigate(Screen.Stats.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToAllUsage = { navController.navigate(Screen.AllUsage.route) }
            )
        }
        composable(Screen.AppPicker.route) {
            AppPickerScreen(
                onDone = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.AppPicker.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Stats.route) {
            StatsScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.AllUsage.route) {
            AllUsageScreen(onBack = { navController.popBackStack() })
        }
    }
}
