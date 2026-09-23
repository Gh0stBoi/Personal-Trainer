package com.personaltrainer.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Boot receiver — re-schedules all alarms after:
 *   - Device reboot (BOOT_COMPLETED)
 *   - App update (MY_PACKAGE_REPLACED)
 *   - Timezone change (TIMEZONE_CHANGED)
 *   - Clock change (TIME_SET)
 *
 * Section 7.4 of the implementation plan.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var alarmScheduler: AlarmScheduler

    companion object {
        private const val TAG = "BootReceiver"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.i(TAG, "Received: $action — re-scheduling all alarms")

        val pending = goAsync()
        scope.launch {
            try {
                alarmScheduler.rescheduleAll()
                Log.i(TAG, "All alarms re-scheduled after $action")
            } catch (e: Exception) {
                Log.e(TAG, "Error re-scheduling alarms", e)
            } finally {
                pending.finish()
            }
        }
    }
}
