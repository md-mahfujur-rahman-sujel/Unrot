package com.unrot.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.unrot.app.MainActivity
import com.unrot.app.R
import com.unrot.app.data.repository.LimitRepository
import com.unrot.app.ui.block.BlockActivity
import com.unrot.app.ui.block.BlockReason
import com.unrot.app.util.DateUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class UsageMonitorService : Service() {

    @Inject lateinit var limitRepository: LimitRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var monitorJob: Job? = null
    private var lastForegroundPackage: String? = null
    private var sessionStartMs: Long = 0L
    private var blockShowing = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Monitoring active"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startMonitoring()
        return START_STICKY
    }

    override fun onDestroy() {
        monitorJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    private fun startMonitoring() {
        if (monitorJob?.isActive == true) return
        monitorJob = scope.launch {
            while (isActive) {
                tick()
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private suspend fun tick() {
        val settings = limitRepository.getSettings()
        if (!settings.protectionEnabled) return

        val foreground = getForegroundApp()
        if (foreground == null || foreground == packageName) {
            flushSession()
            return
        }

        val limitedApp = limitRepository.getLimitedApp(foreground)
        if (limitedApp == null || !limitedApp.isEnabled) {
            flushSession()
            blockShowing = false
            return
        }

        if (foreground != lastForegroundPackage) {
            // Switching to a (possibly different) app: flush whatever was pending for the
            // previous app, then start a fresh session for this one. Any stale block-showing
            // flag from whatever was in the foreground before no longer applies here.
            flushSession()
            lastForegroundPackage = foreground
            sessionStartMs = System.currentTimeMillis()
            blockShowing = false
        } else {
            // Still in the same limited app: persist the elapsed slice right now instead of
            // waiting for the user to switch away. Previously usedTodayMs only got written on
            // app-switch, so a continuous session never updated its own budget, the block
            // screen never triggered mid-session, and the notification/Home/Stats screens
            // stayed frozen at whatever value was last flushed.
            val elapsed = System.currentTimeMillis() - sessionStartMs
            if (elapsed > 0) {
                limitRepository.addUsageTime(foreground, elapsed)
            }
            sessionStartMs = System.currentTimeMillis()
        }

        // Re-read after the flush above so the block/notification decision below reflects the
        // time just recorded, not the stale pre-flush snapshot.
        val current = limitRepository.getLimitedApp(foreground) ?: return

        if (current.isBlockedBySchedule(DateUtils.currentIsoDayOfWeek(), DateUtils.currentMinuteOfDay())) {
            if (!blockShowing) {
                blockShowing = true
                showBlockScreen(foreground, current.appName, BlockReason.SCHEDULE)
            }
            return
        }

        updateNotification(current.appName, current.remainingMinutes)

        if (!current.isOverLimit) {
            // A completed math challenge grants time while the block activity is on top.
            // Clear this flag so the app can be blocked again after that time is consumed.
            blockShowing = false
        } else if (!blockShowing) {
            blockShowing = true
            showBlockScreen(foreground, current.appName, BlockReason.BUDGET)
        }
    }

    private suspend fun flushSession() {
        val pkg = lastForegroundPackage
        val start = sessionStartMs
        lastForegroundPackage = null
        sessionStartMs = 0L
        if (pkg == null || start <= 0L) return
        val elapsed = System.currentTimeMillis() - start
        if (elapsed > 0) {
            limitRepository.addUsageTime(pkg, elapsed)
        }
    }

    private fun getForegroundApp(): String? {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val events = usm.queryEvents(end - 10_000, end)
        var lastPackage: String? = null
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
            ) {
                lastPackage = event.packageName
            }
        }
        return lastPackage
    }

    private fun showBlockScreen(packageName: String, appName: String, reason: BlockReason) {
        val intent = BlockActivity.createIntent(this, packageName, appName, reason).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(intent)
    }

    private fun updateNotification(appName: String, remainingMinutes: Int) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification("$appName: $remainingMinutes min left"))
    }

    private fun buildNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_monitor_title))
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_monitor),
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "usage_monitor"
        private const val NOTIFICATION_ID = 1001
        private const val POLL_INTERVAL_MS = 2_000L

        fun start(context: Context) {
            val intent = Intent(context, UsageMonitorService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, UsageMonitorService::class.java))
        }
    }
}
