package com.example.utils

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.SettingsStore
import java.util.Calendar

object NotificationHelper {

    private const val TAG = "NotificationHelper"

    private const val CHANNEL_DAILY = "brain_quiz_daily_channel"
    private const val CHANNEL_STREAK = "brain_quiz_streak_channel"
    private const val CHANNEL_ACHIEVEMENTS = "brain_quiz_achievements_channel"

    private const val REQ_CODE_DAILY = 1001
    private const val REQ_CODE_STREAK = 1002

    private const val NOTIF_ID_DAILY = 2001
    private const val NOTIF_ID_STREAK = 2002
    private const val NOTIF_ID_ACHIEVEMENT_BASE = 3000

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            val dailyChannel = NotificationChannel(
                CHANNEL_DAILY,
                "Daily Challenge Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily notification reminders to play Brain Quiz AI"
                enableVibration(true)
                enableLights(true)
            }

            val streakChannel = NotificationChannel(
                CHANNEL_STREAK,
                "Streak Expiry Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts to keep your brain quiz streak active"
                enableVibration(true)
                enableLights(true)
            }

            val achievementChannel = NotificationChannel(
                CHANNEL_ACHIEVEMENTS,
                "Achievement Unlocks",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications when you unlock new achievements"
                enableVibration(true)
                enableLights(true)
            }

            manager.createNotificationChannel(dailyChannel)
            manager.createNotificationChannel(streakChannel)
            manager.createNotificationChannel(achievementChannel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun isNotificationPromptShown(context: Context): Boolean {
        val prefs = context.getSharedPreferences("brain_quiz_settings_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("notif_prompt_shown", false)
    }

    fun setNotificationPromptShown(context: Context, shown: Boolean = true) {
        val prefs = context.getSharedPreferences("brain_quiz_settings_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("notif_prompt_shown", shown).apply()
    }

    fun syncReminders(context: Context) {
        createChannels(context)

        val settings = try {
            SettingsStore(context).getSettings()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load settings during syncReminders", e)
            return
        }

        if (settings.dailyChallengeReminder) {
            scheduleDailyReminder(context)
        } else {
            cancelDailyReminder(context)
        }

        if (settings.streakReminder) {
            scheduleStreakReminder(context)
        } else {
            cancelStreakReminder(context)
        }
    }

    fun scheduleDailyReminder(context: Context) {
        val settings = try {
            SettingsStore(context).getSettings()
        } catch (e: Exception) {
            null
        }
        if (settings != null && !settings.dailyChallengeReminder) {
            cancelDailyReminder(context)
            return
        }

        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                action = "com.example.ACTION_DAILY_REMINDER"
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQ_CODE_DAILY,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, 6) // 6:30 AM daily
                set(Calendar.MINUTE, 30)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            setAlarmSafely(alarmManager, calendar.timeInMillis, pendingIntent)
            Log.d(TAG, "Scheduled daily challenge reminder for ${calendar.time}")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to schedule daily reminder", e)
        }
    }

    fun cancelDailyReminder(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                action = "com.example.ACTION_DAILY_REMINDER"
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQ_CODE_DAILY,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
                Log.d(TAG, "Cancelled daily challenge reminder")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cancel daily reminder", e)
        }
    }

    fun scheduleStreakReminder(context: Context) {
        val settings = try {
            SettingsStore(context).getSettings()
        } catch (e: Exception) {
            null
        }
        if (settings != null && !settings.streakReminder) {
            cancelStreakReminder(context)
            return
        }

        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                action = "com.example.ACTION_STREAK_REMINDER"
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQ_CODE_STREAK,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, 20) // 8:30 PM evening streak warning
                set(Calendar.MINUTE, 30)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            setAlarmSafely(alarmManager, calendar.timeInMillis, pendingIntent)
            Log.d(TAG, "Scheduled streak reminder for ${calendar.time}")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to schedule streak reminder", e)
        }
    }

    fun cancelStreakReminder(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                action = "com.example.ACTION_STREAK_REMINDER"
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQ_CODE_STREAK,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
                Log.d(TAG, "Cancelled streak reminder")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cancel streak reminder", e)
        }
    }

    private fun setAlarmSafely(alarmManager: AlarmManager, triggerAtMillis: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                try {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                    return
                } catch (e: SecurityException) {
                    Log.w(TAG, "canScheduleExactAlarms returned true but setExactAndAllowWhileIdle threw SecurityException", e)
                }
            }
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    fun showDailyReminderNotification(context: Context) {
        if (!hasNotificationPermission(context)) return

        createChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigate_to", "daily_challenge")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            2001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationText = "Your Daily Challenge is waiting!\nKeep your streak alive and earn extra XP & Coins today."

        val notification = NotificationCompat.Builder(context, CHANNEL_DAILY)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🧠 Brain Quiz AI")
            .setContentText(notificationText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(notificationText)
            )
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIF_ID_DAILY, notification)
    }

    fun showStreakReminderNotification(context: Context) {
        if (!hasNotificationPermission(context)) return

        // Skip streak reminder if user already completed today's challenge/quiz
        if (hasPlayedToday(context)) {
            Log.d(TAG, "Streak reminder skipped as user already played quiz today.")
            return
        }

        createChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigate_to", "daily_challenge")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            2002,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationText = "Complete today's challenge before midnight and protect your streak."

        val notification = NotificationCompat.Builder(context, CHANNEL_STREAK)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🔥 Don't lose your streak!")
            .setContentText(notificationText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(notificationText)
            )
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIF_ID_STREAK, notification)
    }

    fun showAchievementNotification(context: Context, title: String, description: String) {
        val settings = try {
            SettingsStore(context).getSettings()
        } catch (e: Exception) {
            return
        }
        if (!settings.achievementNotifications) return
        if (!hasNotificationPermission(context)) return

        createChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigate_to", "achievements")
        }
        val notifId = NOTIF_ID_ACHIEVEMENT_BASE + (title.hashCode() and 0x7FFFFFFF % 5000)
        val pendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationText = if (description.isNotBlank()) "$title\n$description" else title

        val notification = NotificationCompat.Builder(context, CHANNEL_ACHIEVEMENTS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🏆 Achievement Unlocked!")
            .setContentText(notificationText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(notificationText)
            )
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(notifId, notification)
    }

    private fun hasPlayedToday(context: Context): Boolean {
        return try {
            val prefs = context.getSharedPreferences("quiz_results_prefs", Context.MODE_PRIVATE)
            val lastTimestamp = prefs.getLong("last_quiz_timestamp", 0L)
            if (lastTimestamp > 0) {
                val lastCal = Calendar.getInstance().apply { timeInMillis = lastTimestamp }
                val nowCal = Calendar.getInstance()
                lastCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                        lastCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}
