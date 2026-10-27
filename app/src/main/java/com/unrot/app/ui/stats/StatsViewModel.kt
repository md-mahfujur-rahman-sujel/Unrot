package com.unrot.app.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unrot.app.data.repository.LimitRepository
import com.unrot.app.domain.model.DailyAppStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val limitRepository: LimitRepository
) : ViewModel() {

    val stats: StateFlow<List<DailyAppStats>> = limitRepository.observeWeeklyStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _unlockCountToday = MutableStateFlow(0)
    val unlockCountToday: StateFlow<Int> = _unlockCountToday.asStateFlow()

    init {
        viewModelScope.launch {
            _unlockCountToday.value = limitRepository.getUnlockCountToday()
        }
    }
}
