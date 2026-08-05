package com.example.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d("ReminderReceiver", "Received broadcast action: $action")

        when (action) {
            "com.example.ACTION_DAILY_REMINDER" -> {
                NotificationHelper.showDailyReminderNotification(context)
                // Automatically reschedule for next day
                NotificationHelper.scheduleDailyReminder(context)
            }
            "com.example.ACTION_STREAK_REMINDER" -> {
                NotificationHelper.showStreakReminderNotification(context)
                // Automatically reschedule for next day
                NotificationHelper.scheduleStreakReminder(context)
            }
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON",
            Intent.ACTION_LOCKED_BOOT_COMPLETED -> {
                Log.d("ReminderReceiver", "System reboot / app update detected ($action). Restoring notification reminders...")
                NotificationHelper.syncReminders(context)
            }
        }
    }
}
