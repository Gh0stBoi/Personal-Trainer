package com.personaltrainer.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.personaltrainer.data.dao.ReminderDao
import com.personaltrainer.data.entities.Reminder
import com.personaltrainer.data.entities.ReminderState
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AlarmScheduler — section 7 of the implementation plan.
 *
 * Responsibilities:
 *  - Schedule exact alarms using [AlarmManager.setExactAndAllowWhileIdle]
 *  - Check [AlarmManager.canScheduleExactAlarms] before every schedule call
 *  - Cancel alarms when sessions are skipped or reminders are dismissed
 *  - Re-schedule everything on boot/replace/timezone-change (called from [BootReceiver])
 */
@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val reminderDao: ReminderDao
) {
    companion object {
        private const val TAG = "AlarmScheduler"

        // PendingIntent extras
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_REMINDER_KIND = "reminder_kind"

        private const val PI_FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or
                                     PendingIntent.FLAG_IMMUTABLE
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Schedule a [Reminder] to fire at [Reminder.fireAt].
     *
     * If exact alarms are not permitted, the reminder is recorded in the DB
     * but the alarm is silently degraded to [AlarmManager.setAndAllowWhileIdle]
     * (inexact, but still fires in Doze mode).
     */
    fun schedule(reminder: Reminder) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_REMINDER_ID, reminder.id)
            putExtra(EXTRA_REMINDER_KIND, reminder.kind.name)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            intent,
            PI_FLAGS
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.fireAt, pi)
            Log.d(TAG, "Exact alarm set for reminder ${reminder.id} at ${reminder.fireAt}")
        } else {
            // Fallback — inexact but Doze-safe
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.fireAt, pi)
            Log.w(TAG, "SCHEDULE_EXACT_ALARM not granted — using inexact alarm for ${reminder.id}")
        }
    }

    /**
     * Cancel the alarm for a [Reminder].
     */
    fun cancel(reminderId: Long) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PI_FLAGS
        )
        alarmManager.cancel(pi)
        Log.d(TAG, "Cancelled alarm for reminder $reminderId")
    }

    /**
     * Reconcile: read all PENDING reminders from the DB and re-schedule
     * any whose PendingIntent is no longer registered with AlarmManager.
     *
     * Called on: boot, app open, WorkManager reconciler tick (every 15 min).
     */
    suspend fun reconcile() {
        val pending = reminderDao.getPending()
        val now = System.currentTimeMillis()

        for (reminder in pending) {
            if (reminder.fireAt < now) {
                // Missed while device was off — fire immediately or skip
                Log.w(TAG, "Reminder ${reminder.id} was missed — firing now")
                fireNow(reminder)
            } else {
                // Check if the alarm still exists
                if (!isAlarmScheduled(reminder.id)) {
                    schedule(reminder)
                    Log.d(TAG, "Re-scheduled missing alarm for ${reminder.id}")
                }
            }
        }
    }

    /** Check whether an alarm PendingIntent is currently registered. */
    private fun isAlarmScheduled(reminderId: Long): Boolean {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        return pi != null
    }

    /** Fire a missed reminder immediately by sending the broadcast. */
    private fun fireNow(reminder: Reminder) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_REMINDER_ID, reminder.id)
            putExtra(EXTRA_REMINDER_KIND, reminder.kind.name)
        }
        context.sendBroadcast(intent)
    }

    /**
     * Re-schedule ALL pending reminders.
     * Called from [BootReceiver] after device reboot.
     */
    suspend fun rescheduleAll() {
        val pending = reminderDao.getPending()
        pending.forEach { schedule(it) }
        Log.d(TAG, "Re-scheduled ${pending.size} reminders after boot/change")
    }
}
