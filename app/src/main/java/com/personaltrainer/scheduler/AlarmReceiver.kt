package com.personaltrainer.scheduler

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.personaltrainer.MainActivity
import com.personaltrainer.R
import com.personaltrainer.data.entities.ReminderKind
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * BroadcastReceiver that fires when an alarm triggers.
 *
 * Reads the reminder kind from the Intent extra and shows the
 * appropriate notification with action buttons.
 *
 * Notification channels:
 *   GYM_ALERT   — high importance (appears as heads-up)
 *   CHECK_IN    — default importance
 *   SUMMARY     — low importance
 */
@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var alarmScheduler: AlarmScheduler

    companion object {
        private const val TAG = "AlarmReceiver"

        // Notification channel IDs
        const val CHANNEL_GYM_ALERT = "gym_alert"
        const val CHANNEL_CHECK_IN = "check_in"
        const val CHANNEL_SUMMARY = "summary"

        // Action strings for notification buttons
        const val ACTION_DONE = "com.personaltrainer.action.DONE"
        const val ACTION_SNOOZE = "com.personaltrainer.action.SNOOZE"
        const val ACTION_SKIP = "com.personaltrainer.action.SKIP"
        const val ACTION_START = "com.personaltrainer.action.START"

        fun createNotificationChannels(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.createNotificationChannels(listOf(
                    NotificationChannel(
                        CHANNEL_GYM_ALERT,
                        "Gym Alerts",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Workout reminders and escalation alerts"
                        enableVibration(true)
                    },
                    NotificationChannel(
                        CHANNEL_CHECK_IN,
                        "Check-ins",
                        NotificationManager.IMPORTANCE_DEFAULT
                    ).apply {
                        description = "Morning, meal, and evening check-in questions"
                    },
                    NotificationChannel(
                        CHANNEL_SUMMARY,
                        "Summaries",
                        NotificationManager.IMPORTANCE_LOW
                    ).apply {
                        description = "Daily and weekly review summaries"
                    }
                ))
            }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(AlarmScheduler.EXTRA_REMINDER_ID, -1L)
        val kindName = intent.getStringExtra(AlarmScheduler.EXTRA_REMINDER_KIND)
        Log.d(TAG, "Alarm received: id=$reminderId kind=$kindName")

        if (reminderId == -1L || kindName == null) return

        val kind = runCatching { ReminderKind.valueOf(kindName) }.getOrNull() ?: return
        showNotification(context, reminderId, kind)
    }

    private fun showNotification(context: Context, reminderId: Long, kind: ReminderKind) {
        val (channelId, title, body, importance) = notificationContent(kind)

        // Open app on tap
        val openAppIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(AlarmScheduler.EXTRA_REMINDER_ID, reminderId)
                putExtra(AlarmScheduler.EXTRA_REMINDER_KIND, kind.name)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(openAppIntent)
            .setAutoCancel(true)
            .setPriority(importance)

        // Add action buttons based on reminder kind
        when (kind) {
            ReminderKind.GYM_ALERT_L1 -> {
                builder.addAction(0, "Start ✓", makeActionIntent(context, ACTION_START, reminderId))
                builder.addAction(0, "Snooze 10'", makeActionIntent(context, ACTION_SNOOZE, reminderId))
                builder.addAction(0, "Can't today", makeActionIntent(context, ACTION_SKIP, reminderId))
            }
            ReminderKind.POST_GYM_CHECKIN -> {
                builder.addAction(0, "Done ✓", makeActionIntent(context, ACTION_DONE, reminderId))
            }
            else -> { /* open app on tap is enough */ }
        }

        try {
            NotificationManagerCompat.from(context).notify(reminderId.toInt(), builder.build())
        } catch (e: SecurityException) {
            Log.w(TAG, "POST_NOTIFICATIONS permission not granted")
        }
    }

    private fun makeActionIntent(context: Context, action: String, reminderId: Long): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            this.action = action
            putExtra(AlarmScheduler.EXTRA_REMINDER_ID, reminderId)
        }
        return PendingIntent.getBroadcast(
            context, reminderId.toInt() + action.hashCode(),
            intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private data class NotifContent(
        val channelId: String,
        val title: String,
        val body: String,
        val priority: Int
    )

    private fun notificationContent(kind: ReminderKind) = when (kind) {
        ReminderKind.MORNING_CHECKIN -> NotifContent(
            CHANNEL_CHECK_IN,
            "Good morning! 🌅",
            "How did you sleep? Quick morning check-in",
            NotificationCompat.PRIORITY_DEFAULT
        )
        ReminderKind.GYM_ALERT_L0 -> NotifContent(
            CHANNEL_GYM_ALERT,
            "Gym in 1 hour 💪",
            "Time to prepare — eat something light and pack your bag",
            NotificationCompat.PRIORITY_DEFAULT
        )
        ReminderKind.GYM_ALERT_L1 -> NotifContent(
            CHANNEL_GYM_ALERT,
            "Gym time! 🏋️",
            "Ready when you are. Let's go!",
            NotificationCompat.PRIORITY_HIGH
        )
        ReminderKind.GYM_ALERT_L2 -> NotifContent(
            CHANNEL_GYM_ALERT,
            "Still at home?",
            "A 20-minute version counts too. Going now / Move to later?",
            NotificationCompat.PRIORITY_HIGH
        )
        ReminderKind.GYM_ALERT_L3 -> NotifContent(
            CHANNEL_GYM_ALERT,
            "Last call 🔔",
            "Train, shorten, or tell me why you're skipping.",
            NotificationCompat.PRIORITY_HIGH
        )
        ReminderKind.GYM_ALERT_L4 -> NotifContent(
            CHANNEL_GYM_ALERT,
            "Missed today?",
            "Let me know — I'll adjust your plan automatically.",
            NotificationCompat.PRIORITY_DEFAULT
        )
        ReminderKind.POST_GYM_CHECKIN -> NotifContent(
            CHANNEL_CHECK_IN,
            "Workout check-in",
            "How did it go? Log your session",
            NotificationCompat.PRIORITY_DEFAULT
        )
        ReminderKind.MEAL_PROMPT -> NotifContent(
            CHANNEL_CHECK_IN,
            "Meal time 🍽️",
            "What did you have? Tap to log quickly",
            NotificationCompat.PRIORITY_LOW
        )
        ReminderKind.EVENING_CHECKIN -> NotifContent(
            CHANNEL_CHECK_IN,
            "End of day check-in 🌙",
            "Anything off-plan today? Just data, no judgment",
            NotificationCompat.PRIORITY_LOW
        )
        ReminderKind.BEDTIME_WIND_DOWN -> NotifContent(
            CHANNEL_SUMMARY,
            "Wind down time 😴",
            "Tomorrow: ${/* TODO: inject tomorrow's session */ "check your plan"}",
            NotificationCompat.PRIORITY_LOW
        )
        ReminderKind.WEEKLY_REVIEW -> NotifContent(
            CHANNEL_SUMMARY,
            "Weekly Review 📊",
            "Your week in numbers — tap to see your progress",
            NotificationCompat.PRIORITY_DEFAULT
        )
        else -> NotifContent(
            CHANNEL_CHECK_IN,
            "Personal Trainer",
            "Tap to open your trainer",
            NotificationCompat.PRIORITY_LOW
        )
    }
}
