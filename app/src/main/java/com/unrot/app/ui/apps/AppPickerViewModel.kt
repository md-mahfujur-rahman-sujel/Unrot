package com.unrot.app.ui.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unrot.app.data.repository.LimitRepository
import com.unrot.app.domain.model.InstalledAppInfo
import com.unrot.app.domain.model.LimitedApp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Quiet-hours config being edited for one app. Days are ISO day-of-week (1=Mon..7=Sun). */
data class ScheduleConfig(
    val startMinutes: Int? = null,
    val endMinutes: Int? = null,
    val days: Set<Int> = emptySet()
)

data class AppPickerState(
    val allApps: List<InstalledAppInfo> = emptyList(),
    val selectedPackages: Set<String> = emptySet(),
    val favoritePackages: Set<String> = emptySet(),
    val budgets: Map<String, Int> = emptyMap(),
    val schedules: Map<String, ScheduleConfig> = emptyMap(),
    val showSocialOnly: Boolean = false,
    val showLimitedOnly: Boolean = false
) {
    val filteredApps: List<InstalledAppInfo>
        get() = allApps
            .filter { !showSocialOnly || it.isSocialApp }
            .filter { !showLimitedOnly || selectedPackages.contains(it.packageName) }
            .map {
                it.copy(
                    isLimited = selectedPackages.contains(it.packageName),
                    isFavorite = favoritePackages.contains(it.packageName)
                )
            }

    val selectedCount: Int get() = selectedPackages.size
}

@HiltViewModel
class AppPickerViewModel @Inject constructor(
    private val limitRepository: LimitRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AppPickerState())
    val state: StateFlow<AppPickerState> = _state.asStateFlow()

    init {
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch {
            val apps = limitRepository.getInstalledApps()
            val existingLimited = limitRepository.getEnabledApps().associateBy { it.packageName }
            val selected = apps.filter { it.isLimited }.map { it.packageName }.toSet()
            val favorites = existingLimited.filterValues { it.isFavorite }.keys
            val budgets = apps.filter { it.isLimited }
                .associate { it.packageName to it.dailyBudgetMinutes }
            val schedules = existingLimited.mapNotNull { (pkg, app) ->
                if (app.hasSchedule) {
                    pkg to ScheduleConfig(app.scheduleStartMinutes, app.scheduleEndMinutes, app.scheduleDays)
                } else null
            }.toMap()
            _state.update {
                it.copy(
                    allApps = apps,
                    selectedPackages = selected,
                    favoritePackages = favorites,
                    budgets = budgets,
                    schedules = schedules
                )
            }
        }
    }

    fun toggleApp(app: InstalledAppInfo) {
        _state.update { current ->
            val selected = current.selectedPackages.toMutableSet()
            val budgets = current.budgets.toMutableMap()
            var favorites = current.favoritePackages
            if (selected.contains(app.packageName)) {
                selected.remove(app.packageName)
                budgets.remove(app.packageName)
                // An app that's no longer limited can't stay pinned to Home.
                favorites = favorites - app.packageName
            } else {
                selected.add(app.packageName)
                budgets[app.packageName] = 30
            }
            current.copy(selectedPackages = selected, favoritePackages = favorites, budgets = budgets)
        }
    }

    fun toggleFavorite(app: InstalledAppInfo) {
        _state.update { current ->
            if (!current.selectedPackages.contains(app.packageName)) return@update current
            val favorites = current.favoritePackages.toMutableSet().apply {
                if (contains(app.packageName)) remove(app.packageName) else add(app.packageName)
            }
            current.copy(favoritePackages = favorites)
        }
    }

    fun setBudget(packageName: String, minutes: Int) {
        _state.update { current ->
            current.copy(budgets = current.budgets + (packageName to minutes))
        }
    }

    fun setScheduleRange(packageName: String, startMinutes: Int, endMinutes: Int) {
        _state.update { current ->
            val existing = current.schedules[packageName] ?: ScheduleConfig()
            // Default to every day the first time a range is picked, so the schedule is
            // immediately effective instead of silently doing nothing with 0 days selected.
            val days = existing.days.ifEmpty { (1..7).toSet() }
            current.copy(
                schedules = current.schedules + (packageName to existing.copy(
                    startMinutes = startMinutes,
                    endMinutes = endMinutes,
                    days = days
                ))
            )
        }
    }

    fun toggleScheduleDay(packageName: String, day: Int) {
        _state.update { current ->
            val existing = current.schedules[packageName] ?: return@update current
            val days = existing.days.toMutableSet().apply {
                if (contains(day)) remove(day) else add(day)
            }
            current.copy(schedules = current.schedules + (packageName to existing.copy(days = days)))
        }
    }

    fun clearSchedule(packageName: String) {
        _state.update { current -> current.copy(schedules = current.schedules - packageName) }
    }

    fun toggleSocialFilter() {
        _state.update { it.copy(showSocialOnly = !it.showSocialOnly) }
    }

    fun toggleLimitedFilter() {
        _state.update { it.copy(showLimitedOnly = !it.showLimitedOnly) }
    }

    fun saveSelections() {
        viewModelScope.launch {
            val current = _state.value
            val allAppsMap = current.allApps.associateBy { it.packageName }

            current.allApps.filter { it.isLimited && !current.selectedPackages.contains(it.packageName) }
                .forEach { limitRepository.removeAppLimit(it.packageName) }

            current.selectedPackages.forEach { pkg ->
                val info = allAppsMap[pkg] ?: return@forEach
                limitRepository.setAppLimit(
                    LimitedApp(
                        packageName = pkg,
                        appName = info.appName,
                        dailyBudgetMinutes = current.budgets[pkg] ?: 30,
                        isEnabled = true
                    )
                )
                limitRepository.setFavorite(pkg, current.favoritePackages.contains(pkg))
                val schedule = current.schedules[pkg]
                limitRepository.updateSchedule(
                    packageName = pkg,
                    startMinutes = schedule?.startMinutes,
                    endMinutes = schedule?.endMinutes,
                    days = schedule?.days ?: emptySet()
                )
            }
        }
    }
}
