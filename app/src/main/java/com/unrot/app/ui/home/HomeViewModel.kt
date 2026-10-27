package com.unrot.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unrot.app.data.repository.LimitRepository
import com.unrot.app.domain.model.LimitedApp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val limitRepository: LimitRepository
) : ViewModel() {

    // Home shows only the apps the user has starred as favorites in the picker — keeps the
    // dashboard short and relevant instead of listing every limited app. Falls back to showing
    // every limited app until the user favorites at least one, so a fresh setup with no
    // favorites chosen yet still shows something instead of an empty Home screen.
    val limitedApps: StateFlow<List<LimitedApp>> = combine(
        limitRepository.observeLimitedApps(),
        limitRepository.observeFavoriteApps()
    ) { all, favorites -> favorites.ifEmpty { all } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _screenTimeMs = MutableStateFlow(0L)
    val screenTimeMs = _screenTimeMs.asStateFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                _screenTimeMs.value = limitRepository.getTodayScreenTimeMs()
                delay(30_000)
            }
        }
    }
}
