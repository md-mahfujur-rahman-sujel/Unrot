package com.unrot.app.ui.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unrot.app.data.repository.LimitRepository
import com.unrot.app.domain.model.PermissionState
import com.unrot.app.service.UsageMonitorService
import com.unrot.app.util.PermissionHelper
import com.unrot.app.worker.DailyResetWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val limitRepository: LimitRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PermissionHelper.getPermissionState(context))
    val state: StateFlow<PermissionState> = _state.asStateFlow()

    init {
        refreshPermissions()
    }

    fun refreshPermissions() {
        _state.value = PermissionHelper.getPermissionState(context)
    }

    fun usageAccessIntent() = PermissionHelper.usageAccessIntent()
    fun overlayIntent(context: Context) = PermissionHelper.overlayIntent(context)
    fun batteryIntent(context: Context) = PermissionHelper.batteryOptimizationIntent(context)

    fun completeOnboarding() {
        viewModelScope.launch {
            limitRepository.completeOnboarding()
            UsageMonitorService.start(context)
            DailyResetWorker.schedule(context)
        }
    }
}
