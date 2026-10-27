package com.unrot.app.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unrot.app.data.repository.LimitRepository
import com.unrot.app.domain.model.MathDifficulty
import com.unrot.app.service.UsageMonitorService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val difficulty: MathDifficulty = MathDifficulty.MEDIUM,
    val extraMinutes: Int = 5,
    val baseProblems: Int = 5,
    val protectionEnabled: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val limitRepository: LimitRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = limitRepository.getSettings()
            _state.update {
                it.copy(
                    difficulty = limitRepository.parseDifficulty(settings.mathDifficulty),
                    extraMinutes = settings.extraMinutesPerUnlock,
                    baseProblems = settings.baseProblemsCount,
                    protectionEnabled = settings.protectionEnabled
                )
            }
        }
    }

    fun setDifficulty(difficulty: MathDifficulty) {
        _state.update { it.copy(difficulty = difficulty) }
        persist()
    }

    fun setExtraMinutes(minutes: Int) {
        _state.update { it.copy(extraMinutes = minutes) }
        persist()
    }

    fun setBaseProblems(count: Int) {
        _state.update { it.copy(baseProblems = count) }
        persist()
    }

    fun setProtectionEnabled(enabled: Boolean) {
        _state.update { it.copy(protectionEnabled = enabled) }
        viewModelScope.launch {
            val current = limitRepository.getSettings()
            limitRepository.updateSettings(current.copy(protectionEnabled = enabled))
            if (enabled) {
                UsageMonitorService.start(context)
            } else {
                UsageMonitorService.stop(context)
            }
        }
    }

    private fun persist() {
        viewModelScope.launch {
            val current = limitRepository.getSettings()
            val ui = _state.value
            limitRepository.updateSettings(
                current.copy(
                    mathDifficulty = ui.difficulty.name,
                    extraMinutesPerUnlock = ui.extraMinutes,
                    baseProblemsCount = ui.baseProblems
                )
            )
        }
    }
}
