package com.unrot.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.unrot.app.data.repository.LimitRepository
import com.unrot.app.service.UsageMonitorService
import com.unrot.app.worker.DailyResetWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var limitRepository: LimitRepository

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (limitRepository.isOnboardingComplete()) {
                    UsageMonitorService.start(context)
                    DailyResetWorker.schedule(context)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
