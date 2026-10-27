package com.unrot.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.unrot.app.data.repository.LimitRepository
import com.unrot.app.ui.navigation.Screen
import com.unrot.app.ui.navigation.UnrotNavGraph
import com.unrot.app.ui.theme.UnrotTheme
import com.unrot.app.ui.theme.GlassBackground
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var limitRepository: LimitRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            var startDestination by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(Unit) {
                startDestination = if (limitRepository.isOnboardingComplete()) {
                    Screen.Home.route
                } else {
                    Screen.Onboarding.route
                }
            }

            UnrotTheme {
                GlassBackground {
                    startDestination?.let { dest ->
                        UnrotNavGraph(startDestination = dest)
                    }
                }
            }
        }
    }
}
