package com.unrot.app.domain.model

data class LimitedApp(
    val packageName: String,
    val appName: String,
    val dailyBudgetMinutes: Int,
    val usedTodayMs: Long = 0L,
    val extraTimeMs: Long = 0L,
    val unlockCountToday: Int = 0,
    val isEnabled: Boolean = true,
    /** Quiet-hours hard block. Minutes-since-midnight; null/empty days = no schedule set. */
    val scheduleStartMinutes: Int? = null,
    val scheduleEndMinutes: Int? = null,
    /** ISO day-of-week values (1 = Monday .. 7 = Sunday). */
    val scheduleDays: Set<Int> = emptySet(),
    /** User-curated "show on Home" flag — set from the app picker. */
    val isFavorite: Boolean = false,
    val isWhitelisted: Boolean = false,
    val appGroup: AppCategory = AppCategory.USER
) {
    val totalBudgetMs: Long get() = dailyBudgetMinutes * 60_000L + extraTimeMs
    val remainingMs: Long get() = (totalBudgetMs - usedTodayMs).coerceAtLeast(0)
    val isOverLimit: Boolean get() = usedTodayMs >= totalBudgetMs
    val remainingMinutes: Int get() = (remainingMs / 60_000).toInt()
    val usedMinutes: Int get() = (usedTodayMs / 60_000).toInt()
    val hasSchedule: Boolean
        get() = scheduleStartMinutes != null && scheduleEndMinutes != null && scheduleDays.isNotEmpty()

    /**
     * True if [dayOfWeekIso] (1=Mon..7=Sun) / [minuteOfDay] (0..1439) falls inside this app's
     * quiet-hours window. Handles windows that cross midnight (e.g. 22:00-07:00) by checking
     * membership against the day the window *started* on.
     */
    fun isBlockedBySchedule(dayOfWeekIso: Int, minuteOfDay: Int): Boolean {
        val start = scheduleStartMinutes ?: return false
        val end = scheduleEndMinutes ?: return false
        if (scheduleDays.isEmpty()) return false

        return if (start <= end) {
            dayOfWeekIso in scheduleDays && minuteOfDay in start until end
        } else {
            val startedYesterday = minuteOfDay < end
            val effectiveDay = if (startedYesterday) previousIsoDay(dayOfWeekIso) else dayOfWeekIso
            effectiveDay in scheduleDays && (minuteOfDay >= start || minuteOfDay < end)
        }
    }

    private fun previousIsoDay(day: Int): Int = if (day == 1) 7 else day - 1
}

data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val isSocialApp: Boolean,
    val isLimited: Boolean,
    val dailyBudgetMinutes: Int,
    val isFavorite: Boolean = false,
    val isSystemApp: Boolean = false,
    val category: AppCategory = AppCategory.USER,
    val isWhitelisted: Boolean = false
)

enum class MathDifficulty { EASY, MEDIUM, HARD }

data class MathProblem(
    val question: String,
    val correctAnswer: Int
)

data class UnlockDifficulty(
    val problemsRequired: Int,
    val extraMinutesGranted: Int,
    val difficulty: MathDifficulty
)

data class DailyAppStats(
    val packageName: String,
    val appName: String,
    val usageMs: Long,
    val unlockRequests: Int,
    val budgetMinutes: Int,
    val previousWeekUsageMs: Long = 0L
)

data class PermissionState(
    val usageAccessGranted: Boolean,
    val overlayGranted: Boolean,
    val batteryOptimizationDisabled: Boolean,
    val notificationsGranted: Boolean,
    val accessibilityGranted: Boolean = false
) {
    val allGranted: Boolean get() = usageAccessGranted && overlayGranted
}

data class BlockAttempt(
    val packageName: String,
    val appName: String,
    val timestamp: Long,
    val reason: String
)

data class AppUsage(
    val packageName: String,
    val appName: String,
    val usageMs: Long
)
