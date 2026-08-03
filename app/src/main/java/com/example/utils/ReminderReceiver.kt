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
            }
            "com.example.ACTION_STREAK_REMINDER" -> {
                NotificationHelper.showStreakReminderNotification(context)
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                NotificationHelper.syncReminders(context)
            }
        }
    }
}
