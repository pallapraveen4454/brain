package com.example.data

import android.content.Context
import com.example.BrainQuizApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserSettings(
    val isDarkMode: Boolean = true,
    val soundEffectsEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val dailyChallengeReminder: Boolean = true,
    val streakReminder: Boolean = true,
    val achievementNotifications: Boolean = true,
    val lastSyncTimestamp: Long = 0L,
    val lastBackupTimestamp: Long = 0L
)

class SettingsStore(
    private val context: Context? = try { BrainQuizApplication.instance } catch (e: Exception) { null }
) {
    private val prefsName = "brain_quiz_settings_prefs"

    private fun getPrefs() = context?.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    private val _settingsFlow = MutableStateFlow(getSettings())
    val settingsFlow: StateFlow<UserSettings> = _settingsFlow.asStateFlow()

    fun getSettings(): UserSettings {
        val prefs = getPrefs() ?: return UserSettings()
        return UserSettings(
            isDarkMode = prefs.getBoolean("is_dark_mode", true),
            soundEffectsEnabled = prefs.getBoolean("sound_effects", true),
            vibrationEnabled = prefs.getBoolean("vibration", true),
            dailyChallengeReminder = prefs.getBoolean("daily_reminder", true),
            streakReminder = prefs.getBoolean("streak_reminder", true),
            achievementNotifications = prefs.getBoolean("achievement_notifs", true),
            lastSyncTimestamp = prefs.getLong("last_sync_ts", 0L),
            lastBackupTimestamp = prefs.getLong("last_backup_ts", 0L)
        )
    }

    fun saveSettings(settings: UserSettings) {
        getPrefs()?.edit()?.apply {
            putBoolean("is_dark_mode", settings.isDarkMode)
            putBoolean("sound_effects", settings.soundEffectsEnabled)
            putBoolean("vibration", settings.vibrationEnabled)
            remove("bg_music")
            putBoolean("daily_reminder", settings.dailyChallengeReminder)
            putBoolean("streak_reminder", settings.streakReminder)
            putBoolean("achievement_notifs", settings.achievementNotifications)
            remove("language")
            putLong("last_sync_ts", settings.lastSyncTimestamp)
            putLong("last_backup_ts", settings.lastBackupTimestamp)
            apply()
        }
        _settingsFlow.value = settings
    }
}
