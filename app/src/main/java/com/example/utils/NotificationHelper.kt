package com.example.utils

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.SettingsStore
import java.util.Calendar

object NotificationHelper {

    private const val CHANNEL_DAILY = "brain_quiz_daily_channel"
    private const val CHANNEL_STREAK = "brain_quiz_streak_channel"
    private const val CHANNEL_ACHIEVEMENTS = "brain_quiz_achievements_channel"

    private const val REQ_CODE_DAILY = 1001
    private const val REQ_CODE_STREAK = 1002

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            val dailyChannel = NotificationChannel(
                CHANNEL_DAILY,
                "Daily Challenge Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily notification reminders to play Brain Quiz AI"
            }

            val streakChannel = NotificationChannel(
                CHANNEL_STREAK,
                "Streak Expiry Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts to keep your brain quiz streak active"
            }

            val achievementChannel = NotificationChannel(
                CHANNEL_ACHIEVEMENTS,
                "Achievement Unlocks",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications when you unlock new achievements"
            }

            manager.createNotificationChannel(dailyChannel)
            manager.createNotificationChannel(streakChannel)
            manager.createNotificationChannel(achievementChannel)
        }
    }

    fun syncReminders(context: Context) {
        val settings = try {
            SettingsStore(context).getSettings()
        } catch (e: Exception) {
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
                set(Calendar.HOUR_OF_DAY, 20) // 8 PM daily
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
            Log.d("NotificationHelper", "Scheduled daily challenge reminder for ${calendar.time}")
        } catch (e: Exception) {
            Log.w("NotificationHelper", "Failed to schedule daily reminder", e)
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
            }
        } catch (e: Exception) {
            Log.w("NotificationHelper", "Failed to cancel daily reminder", e)
        }
    }

    fun scheduleStreakReminder(context: Context) {
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
                set(Calendar.HOUR_OF_DAY, 22) // 10 PM streak warning
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
            Log.d("NotificationHelper", "Scheduled streak reminder for ${calendar.time}")
        } catch (e: Exception) {
            Log.w("NotificationHelper", "Failed to schedule streak reminder", e)
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
            }
        } catch (e: Exception) {
            Log.w("NotificationHelper", "Failed to cancel streak reminder", e)
        }
    }

    fun showAchievementNotification(context: Context, title: String, description: String) {
        val settings = try {
            SettingsStore(context).getSettings()
        } catch (e: Exception) {
            return
        }
        if (!settings.achievementNotifications) return

        createChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "achievements")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ACHIEVEMENTS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🏅 $title")
            .setContentText(description)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(System.currentTimeMillis().toInt(), notification)
    }

    fun showDailyReminderNotification(context: Context) {
        createChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "daily_challenge")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            2001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_DAILY)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🔥 Daily Challenge Ready!")
            .setContentText("Keep your brain sharp and earn extra XP today.")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(2001, notification)
    }

    fun showStreakReminderNotification(context: Context) {
        createChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "quiz")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            2002,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_STREAK)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("⚠️ Don't lose your streak!")
            .setContentText("Complete a quick quiz today to protect your streak.")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(2002, notification)
    }
}
