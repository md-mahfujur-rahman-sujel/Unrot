package com.unrot.app.domain.model

import com.unrot.app.data.local.entity.LimitedAppEntity

fun LimitedAppEntity.toDomain() = LimitedApp(
    packageName = packageName,
    appName = appName,
    dailyBudgetMinutes = dailyBudgetMinutes,
    usedTodayMs = usedTodayMs,
    extraTimeMs = extraTimeMs,
    unlockCountToday = unlockCountToday,
    isEnabled = isEnabled,
    scheduleStartMinutes = scheduleStartMinutes,
    scheduleEndMinutes = scheduleEndMinutes,
    scheduleDays = scheduleDays.toDaySet(),
    isFavorite = isFavorite,
    isWhitelisted = isWhitelisted,
    appGroup = runCatching { AppCategory.valueOf(appGroup) }.getOrDefault(AppCategory.USER)
)

fun LimitedApp.toEntity(lastResetDate: String = "") = LimitedAppEntity(
    packageName = packageName,
    appName = appName,
    dailyBudgetMinutes = dailyBudgetMinutes,
    usedTodayMs = usedTodayMs,
    extraTimeMs = extraTimeMs,
    unlockCountToday = unlockCountToday,
    lastResetDate = lastResetDate,
    isEnabled = isEnabled,
    scheduleStartMinutes = scheduleStartMinutes,
    scheduleEndMinutes = scheduleEndMinutes,
    scheduleDays = scheduleDays.toDaysCsv(),
    isFavorite = isFavorite,
    isWhitelisted = isWhitelisted,
    appGroup = appGroup.name
)

/** "1,2,3,4,5" -> {1,2,3,4,5}. Blank/null -> empty set. */
fun String?.toDaySet(): Set<Int> =
    this?.takeIf { it.isNotBlank() }
        ?.split(",")
        ?.mapNotNull { it.trim().toIntOrNull() }
        ?.toSet()
        ?: emptySet()

/** {1,2,3,4,5} -> "1,2,3,4,5". Empty set -> null. */
fun Set<Int>.toDaysCsv(): String? =
    takeIf { it.isNotEmpty() }?.sorted()?.joinToString(",")
