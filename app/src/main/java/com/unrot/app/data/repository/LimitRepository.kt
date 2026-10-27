package com.unrot.app.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import com.unrot.app.data.local.dao.AppSettingsDao
import com.unrot.app.data.local.dao.LimitedAppDao
import com.unrot.app.data.local.dao.UnlockEventDao
import com.unrot.app.data.local.dao.UsageLogDao
import com.unrot.app.data.local.entity.AppSettingsEntity
import com.unrot.app.data.local.entity.LimitedAppEntity
import com.unrot.app.data.local.entity.UnlockEventEntity
import com.unrot.app.data.local.entity.UsageLogEntity
import com.unrot.app.domain.model.DailyAppStats
import com.unrot.app.domain.model.AppUsage
import com.unrot.app.domain.model.InstalledAppInfo
import com.unrot.app.domain.model.LimitedApp
import com.unrot.app.domain.model.MathDifficulty
import com.unrot.app.domain.model.toDomain
import com.unrot.app.domain.model.toEntity
import com.unrot.app.domain.model.toDaysCsv
import com.unrot.app.util.DateUtils
import com.unrot.app.data.local.dao.BlockAttemptDao
import com.unrot.app.data.local.entity.BlockAttemptEntity
import com.unrot.app.domain.model.BlockAttempt
import com.unrot.app.util.AppCategories
import com.unrot.app.util.SystemPackages
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LimitRepository @Inject constructor(
    private val limitedAppDao: LimitedAppDao,
    private val usageLogDao: UsageLogDao,
    private val unlockEventDao: UnlockEventDao,
    private val settingsDao: AppSettingsDao,
    private val blockAttemptDao: BlockAttemptDao,
    @ApplicationContext private val context: Context
) {
    fun observeLimitedApps(): Flow<List<LimitedApp>> =
        limitedAppDao.observeEnabledApps().map { list -> list.map { it.toDomain() } }

    fun observeFavoriteApps(): Flow<List<LimitedApp>> =
        limitedAppDao.observeFavoriteApps().map { list -> list.map { it.toDomain() } }

    fun observeAllLimitedApps(): Flow<List<LimitedApp>> =
        limitedAppDao.observeAllApps().map { list -> list.map { it.toDomain() } }

    suspend fun getLimitedApp(packageName: String): LimitedApp? =
        limitedAppDao.getByPackage(packageName)?.let { ensureDailyReset(it).toDomain() }

    suspend fun getEnabledApps(): List<LimitedApp> =
        limitedAppDao.getEnabledApps().map { it.toDomain() }

    suspend fun setAppLimit(app: LimitedApp) {
        val today = DateUtils.todayString()
        val existing = limitedAppDao.getByPackage(app.packageName)
        val current = existing?.let { ensureDailyReset(it) }
        limitedAppDao.upsert(
            app.toEntity(lastResetDate = current?.lastResetDate ?: today).copy(
                usedTodayMs = current?.usedTodayMs ?: app.usedTodayMs,
                extraTimeMs = current?.extraTimeMs ?: app.extraTimeMs,
                unlockCountToday = current?.unlockCountToday ?: app.unlockCountToday,
                // Budget/enable edits go through here; quiet-hours schedule and the favorite flag
                // are only changed via updateSchedule()/setFavorite() so they aren't silently
                // wiped whenever the budget is edited.
                scheduleStartMinutes = current?.scheduleStartMinutes,
                scheduleEndMinutes = current?.scheduleEndMinutes,
                scheduleDays = current?.scheduleDays,
                isFavorite = current?.isFavorite ?: app.isFavorite,
                isWhitelisted = current?.isWhitelisted ?: app.isWhitelisted,
                appGroup = current?.appGroup ?: app.appGroup.name
            )
        )
    }

    suspend fun setFavorite(packageName: String, isFavorite: Boolean) {
        limitedAppDao.setFavorite(packageName, isFavorite)
    }

    suspend fun setWhitelisted(packageName: String, isWhitelisted: Boolean) {
        limitedAppDao.setWhitelisted(packageName, isWhitelisted)
    }

    suspend fun removeAppLimit(packageName: String) {
        limitedAppDao.delete(packageName)
    }

    /** Sets or clears an app's quiet-hours hard-block window. Pass empty [days] to clear it. */
    suspend fun updateSchedule(
        packageName: String,
        startMinutes: Int?,
        endMinutes: Int?,
        days: Set<Int>
    ) {
        val app = limitedAppDao.getByPackage(packageName) ?: return
        limitedAppDao.update(
            app.copy(
                scheduleStartMinutes = if (days.isEmpty()) null else startMinutes,
                scheduleEndMinutes = if (days.isEmpty()) null else endMinutes,
                scheduleDays = days.toDaysCsv()
            )
        )
    }

    suspend fun addUsageTime(packageName: String, deltaMs: Long) {
        val app = limitedAppDao.getByPackage(packageName) ?: return
        ensureDailyReset(app)
        limitedAppDao.update(app.copy(usedTodayMs = app.usedTodayMs + deltaMs))
    }

    suspend fun grantExtraTime(packageName: String, extraMs: Long, problemsSolved: Int, problemsRequired: Int) {
        val app = limitedAppDao.getByPackage(packageName) ?: return
        val updated = app.copy(
            extraTimeMs = app.extraTimeMs + extraMs,
            unlockCountToday = app.unlockCountToday + 1
        )
        limitedAppDao.update(updated)
        unlockEventDao.insert(
            UnlockEventEntity(
                packageName = packageName,
                timestamp = System.currentTimeMillis(),
                problemsRequired = problemsRequired,
                problemsSolved = problemsSolved,
                extraMinutesGranted = (extraMs / 60_000).toInt()
            )
        )
    }

    suspend fun resetDailyLimits() {
        limitedAppDao.resetAllDaily(DateUtils.todayString())
    }

    private suspend fun ensureDailyReset(app: LimitedAppEntity): LimitedAppEntity {
        val today = DateUtils.todayString()
        if (app.lastResetDate == today) return app
        if (app.lastResetDate.isNotBlank()) {
            usageLogDao.insert(
                UsageLogEntity(
                    packageName = app.packageName,
                    date = app.lastResetDate,
                    usageMs = app.usedTodayMs,
                    unlockRequests = app.unlockCountToday
                )
            )
        }
        val reset = app.copy(
            usedTodayMs = 0,
            extraTimeMs = 0,
            unlockCountToday = 0,
            lastResetDate = today
        )
        limitedAppDao.update(reset)
        return reset
    }

    suspend fun getInstalledApps(): List<InstalledAppInfo> {
        val pm = context.packageManager
        val limitedPackages = limitedAppDao.getEnabledApps().associateBy { it.packageName }
        val launchIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        return pm.queryIntentActivities(launchIntent, PackageManager.MATCH_ALL)
            .mapNotNull { resolveInfo ->
                val packageName = resolveInfo.activityInfo.packageName
                if (packageName == context.packageName) return@mapNotNull null
                val appInfo = try {
                    pm.getApplicationInfo(packageName, 0)
                } catch (_: PackageManager.NameNotFoundException) {
                    return@mapNotNull null
                }
                val isSystem = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
                val isGame = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    appInfo.category == ApplicationInfo.CATEGORY_GAME
                } else false
                val appName = pm.getApplicationLabel(appInfo).toString()
                val limited = limitedPackages[packageName]
                val category = AppCategories.categorize(packageName, isSystem, isGame)
                InstalledAppInfo(
                    packageName = packageName,
                    appName = appName,
                    isSocialApp = AppCategories.isSocialApp(packageName, category),
                    isLimited = limited != null,
                    dailyBudgetMinutes = limited?.dailyBudgetMinutes ?: 30,
                    isFavorite = limited?.isFavorite ?: false,
                    isSystemApp = isSystem,
                    category = category,
                    isWhitelisted = limited?.isWhitelisted ?: false
                )
            }
            .distinctBy { it.packageName }
            .sortedWith(
                compareByDescending<InstalledAppInfo> { it.isSocialApp }
                    .thenByDescending { it.isLimited }
                    .thenBy { it.appName.lowercase() }
            )
    }

    fun observeSettings(): Flow<AppSettingsEntity> =
        settingsDao.observe().map { it ?: AppSettingsEntity() }

    suspend fun getSettings(): AppSettingsEntity =
        settingsDao.get() ?: AppSettingsEntity()

    suspend fun updateSettings(settings: AppSettingsEntity) {
        settingsDao.upsert(settings)
    }

    suspend fun completeOnboarding() {
        val current = getSettings()
        settingsDao.upsert(current.copy(onboardingComplete = true))
    }

    suspend fun isOnboardingComplete(): Boolean = getSettings().onboardingComplete

    fun observeWeeklyStats(): Flow<List<DailyAppStats>> {
        val currentWeekStart = DateUtils.daysAgoString(6)
        val previousWeekStart = DateUtils.daysAgoString(13)
        return combine(
            limitedAppDao.observeEnabledApps(),
            usageLogDao.observeSince(previousWeekStart)
        ) { apps, logs ->
            apps.map { app ->
                val appLogs = logs.filter { it.packageName == app.packageName }
                val currentWeekLogs = appLogs.filter { it.date >= currentWeekStart }
                val previousWeekLogs = appLogs.filter {
                    it.date >= previousWeekStart && it.date < currentWeekStart
                }
                DailyAppStats(
                    packageName = app.packageName,
                    appName = app.appName,
                    usageMs = currentWeekLogs.sumOf { it.usageMs } + app.usedTodayMs,
                    unlockRequests = currentWeekLogs.sumOf { it.unlockRequests } + app.unlockCountToday,
                    budgetMinutes = app.dailyBudgetMinutes,
                    previousWeekUsageMs = previousWeekLogs.sumOf { it.usageMs }
                )
            }
        }
    }

    suspend fun logDailyUsage() {
        limitedAppDao.getEnabledApps().forEach { app ->
            if (app.lastResetDate.isBlank() || app.lastResetDate == DateUtils.todayString()) {
                return@forEach
            }
            usageLogDao.insert(
                UsageLogEntity(
                    packageName = app.packageName,
                    date = app.lastResetDate,
                    usageMs = app.usedTodayMs,
                    unlockRequests = app.unlockCountToday
                )
            )
        }
    }

    suspend fun getUnlockCountToday(): Int =
        unlockEventDao.countToday(DateUtils.startOfTodayMillis())

    /** Total time today while the device was both interactive and unlocked. */
    suspend fun getTodayScreenTimeMs(): Long = withContext(Dispatchers.Default) {
        val usageStats = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val startOfToday = DateUtils.startOfTodayMillis()
        val now = System.currentTimeMillis()
        // Start before midnight so current screen/keyguard state is known at the daily boundary.
        val events = usageStats.queryEvents(startOfToday - 24 * 60 * 60 * 1_000L, now)
        val event = UsageEvents.Event()
        var interactive = false
        var unlocked = false
        var previousTimestamp = startOfToday
        var total = 0L

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val timestamp = event.timeStamp
            if (timestamp >= startOfToday && interactive && unlocked) {
                total += (timestamp - previousTimestamp).coerceAtLeast(0L)
            }
            when (event.eventType) {
                UsageEvents.Event.SCREEN_INTERACTIVE -> interactive = true
                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> interactive = false
                UsageEvents.Event.KEYGUARD_HIDDEN -> unlocked = true
                UsageEvents.Event.KEYGUARD_SHOWN -> unlocked = false
            }
            if (timestamp >= startOfToday) previousTimestamp = timestamp
        }
        if (interactive && unlocked) total += (now - previousTimestamp).coerceAtLeast(0L)
        total
    }

    /** Usage for every app used since local midnight, excluding Unrot itself, the device's
     *  home/launcher, and non-interactive system-UI components (see [SystemPackages]). Genuine
     *  preloaded apps the user actually opened — Settings, Chrome, etc. — are kept. */
    suspend fun getTodayAllAppUsage(): List<AppUsage> = withContext(Dispatchers.Default) {
        val usageStats = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val pm = context.packageManager
        val homePackage = pm.resolveActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
            PackageManager.MATCH_DEFAULT_ONLY
        )?.activityInfo?.packageName
        val start = DateUtils.startOfTodayMillis()
        val totals = usageStats.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, System.currentTimeMillis())
            .orEmpty()
            .filter {
                it.packageName != context.packageName &&
                    it.packageName != homePackage &&
                    !SystemPackages.isHiddenFromUsage(it.packageName) &&
                    it.totalTimeInForeground > 0L
            }
            .groupBy { it.packageName }
            .mapValues { (_, stats) -> stats.sumOf { it.totalTimeInForeground } }

        totals.mapNotNull { (packageName, usageMs) ->
            val label = runCatching {
                context.packageManager.getApplicationLabel(
                    context.packageManager.getApplicationInfo(packageName, 0)
                ).toString()
            }.getOrNull() ?: return@mapNotNull null
            AppUsage(packageName, label, usageMs)
        }.sortedByDescending { it.usageMs }
    }

    fun parseDifficulty(value: String): MathDifficulty =
        runCatching { MathDifficulty.valueOf(value) }.getOrDefault(MathDifficulty.MEDIUM)

    suspend fun recordBlockAttempt(packageName: String, appName: String, reason: String) {
        blockAttemptDao.insert(
            BlockAttemptEntity(
                packageName = packageName,
                appName = appName,
                timestamp = System.currentTimeMillis(),
                reason = reason
            )
        )
    }

    fun observeBlockAttemptsToday(): Flow<List<BlockAttempt>> =
        blockAttemptDao.observeSince(DateUtils.startOfTodayMillis()).map { rows ->
            rows.map { BlockAttempt(it.packageName, it.appName, it.timestamp, it.reason) }
        }

    suspend fun getBlockAttemptCountToday(): Int =
        blockAttemptDao.countToday(DateUtils.startOfTodayMillis())

    fun observeDailyUsageBars(): Flow<List<Pair<String, Float>>> {
        val start = DateUtils.daysAgoString(6)
        return usageLogDao.observeSince(start).map { logs ->
            val byDate = logs.groupBy { it.date }.mapValues { (_, rows) ->
                rows.sumOf { it.usageMs }.toFloat() / 60_000f
            }
            (6 downTo 0).map { offset ->
                val date = DateUtils.daysAgoString(offset.toLong())
                val label = DateUtils.shortWeekday(date)
                label to (byDate[date] ?: 0f)
            }
        }
    }

    suspend fun exportBackupJson(): String {
        val apps = limitedAppDao.getEnabledApps()
        val settings = getSettings()
        return buildString {
            append("{")
            append("\"exportedAt\":").append(System.currentTimeMillis()).append(",")
            append("\"settings\":{")
            append("\"mathDifficulty\":\"").append(settings.mathDifficulty).append("\",")
            append("\"extraMinutesPerUnlock\":").append(settings.extraMinutesPerUnlock).append(",")
            append("\"baseProblemsCount\":").append(settings.baseProblemsCount).append(",")
            append("\"strictModeEnabled\":").append(settings.strictModeEnabled)
            append("},\"apps\":[")
            apps.forEachIndexed { index, app ->
                if (index > 0) append(",")
                append("{")
                append("\"packageName\":\"").append(app.packageName).append("\",")
                append("\"appName\":\"").append(app.appName.replace("\"", "'")).append("\",")
                append("\"dailyBudgetMinutes\":").append(app.dailyBudgetMinutes).append(",")
                append("\"isFavorite\":").append(app.isFavorite).append(",")
                append("\"isWhitelisted\":").append(app.isWhitelisted).append(",")
                append("\"scheduleStartMinutes\":").append(app.scheduleStartMinutes ?: -1).append(",")
                append("\"scheduleEndMinutes\":").append(app.scheduleEndMinutes ?: -1).append(",")
                append("\"scheduleDays\":\"").append(app.scheduleDays ?: "").append("\"")
                append("}")
            }
            append("]}")
        }
    }

    suspend fun importBackupJson(json: String): Boolean {
        return runCatching {
            val appsBlock = json.substringAfter("\"apps\":[").substringBeforeLast("]")
            val chunks = appsBlock.split("},{").map {
                it.trim().trimStart('{').trimEnd('}')
            }
            chunks.forEach { chunk ->
                if (chunk.isBlank()) return@forEach
                fun field(name: String): String {
                    val key = "\"$name\":"
                    val rest = chunk.substringAfter(key, "")
                    return if (rest.startsWith("\"")) rest.substringAfter("\"").substringBefore("\"")
                    else rest.substringBefore(",").trim()
                }
                val pkg = field("packageName")
                if (pkg.isBlank()) return@forEach
                limitedAppDao.upsert(
                    LimitedAppEntity(
                        packageName = pkg,
                        appName = field("appName"),
                        dailyBudgetMinutes = field("dailyBudgetMinutes").toIntOrNull() ?: 30,
                        isFavorite = field("isFavorite").toBooleanStrictOrNull() ?: false,
                        isWhitelisted = field("isWhitelisted").toBooleanStrictOrNull() ?: false,
                        scheduleStartMinutes = field("scheduleStartMinutes").toIntOrNull()?.takeIf { it >= 0 },
                        scheduleEndMinutes = field("scheduleEndMinutes").toIntOrNull()?.takeIf { it >= 0 },
                        scheduleDays = field("scheduleDays").ifBlank { null }
                    )
                )
            }
            true
        }.getOrDefault(false)
    }
}
