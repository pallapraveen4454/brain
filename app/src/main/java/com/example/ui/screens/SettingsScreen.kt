package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Support
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SettingsStore
import com.example.data.UserSettings
import com.example.ui.components.GlassCard
import com.example.ui.localization.LocalAppStrings
import com.example.ui.theme.AccentCoins
import com.example.ui.theme.AccentCoinsGradientEnd
import com.example.ui.theme.AccentStreak
import com.example.ui.theme.AccentXP
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkCardSurface
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.PrimaryPurpleLight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextWhite
import com.example.utils.SoundEffects
import com.example.utils.bounceClick
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    playerName: String = "Player",
    playerEmail: String = "guest@brainquiz.ai",
    onEditUsername: ((String) -> Unit)? = null,
    onResetAccount: () -> Unit = {},
    onSignOut: () -> Unit = {},
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val settingsStore = remember { SettingsStore(context) }
    var userSettings by remember { mutableStateOf(settingsStore.getSettings()) }

    // Dialog state management
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showRateAppDialog by remember { mutableStateOf(false) }
    var showContactSupportDialog by remember { mutableStateOf(false) }
    var showRestoreDataDialog by remember { mutableStateOf(false) }

    // Text inputs & feedback state
    var newUsernameInput by remember { mutableStateOf(playerName) }
    var newPasswordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }
    var ratingStars by remember { mutableIntStateOf(5) }
    var ratingFeedback by remember { mutableStateOf("") }

    // Async state flags
    var isSyncing by remember { mutableStateOf(false) }
    var isBackingUp by remember { mutableStateOf(false) }
    var isRestoring by remember { mutableStateOf(false) }

    fun updateSettings(newSettings: UserSettings) {
        userSettings = newSettings
        settingsStore.saveSettings(newSettings)
        com.example.utils.BackgroundMusicPlayer.updateMusicState(context)
        com.example.utils.NotificationHelper.syncReminders(context)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Ambient background glow particles
        SettingsAmbientCanvas()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 18.dp)
                .verticalScroll(rememberScrollState())
                .testTag("settings_screen")
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // --------------------------------------------------
            // 1. HERO SETTINGS HEADER
            // --------------------------------------------------
            SettingsHeroHeader(
                title = strings.settingsHeader,
                onBackClick = {
                    if (userSettings.vibrationEnabled) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    if (userSettings.soundEffectsEnabled) {
                        SoundEffects.playCoinSound()
                    }
                    onBackClick()
                }
            )

            Spacer(modifier = Modifier.height(18.dp))

            // --------------------------------------------------
            // 2. AUDIO SETTINGS GROUP
            // --------------------------------------------------
            SettingsGroupHeader(title = "Audio", icon = Icons.Default.VolumeUp)
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                backgroundColor = DarkCardSurface,
                borderColor = GlassBorder,
                elevation = 6.dp
            ) {
                Column {
                    SettingsToggleRow(
                        title = strings.soundEffects,
                        subtitle = strings.soundEffectsSubtitle,
                        icon = if (userSettings.soundEffectsEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        checked = userSettings.soundEffectsEnabled,
                        testTag = "sound_effects_switch",
                        onCheckedChange = { checked ->
                            if (checked) {
                                SoundEffects.playCoinSound()
                            }
                            if (userSettings.vibrationEnabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            updateSettings(userSettings.copy(soundEffectsEnabled = checked))
                        }
                    )

                    SettingsGroupDivider()

                    SettingsToggleRow(
                        title = strings.bgMusic,
                        subtitle = strings.bgMusicSubtitle,
                        icon = Icons.Default.MusicNote,
                        checked = userSettings.bgMusicEnabled,
                        testTag = "bg_music_switch",
                        onCheckedChange = { checked ->
                            if (userSettings.vibrationEnabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            if (userSettings.soundEffectsEnabled) {
                                SoundEffects.playCoinSound()
                            }
                            updateSettings(userSettings.copy(bgMusicEnabled = checked))
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // --------------------------------------------------
            // 3. FEEDBACK SETTINGS GROUP
            // --------------------------------------------------
            SettingsGroupHeader(title = "Feedback", icon = Icons.Default.Vibration)
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                backgroundColor = DarkCardSurface,
                borderColor = GlassBorder,
                elevation = 6.dp
            ) {
                SettingsToggleRow(
                    title = strings.vibration,
                    subtitle = strings.vibrationSubtitle,
                    icon = Icons.Default.Vibration,
                    checked = userSettings.vibrationEnabled,
                    testTag = "vibration_switch",
                    onCheckedChange = { checked ->
                        if (checked) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        if (userSettings.soundEffectsEnabled) {
                            SoundEffects.playCoinSound()
                        }
                        updateSettings(userSettings.copy(vibrationEnabled = checked))
                    }
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // --------------------------------------------------
            // 4. NOTIFICATIONS SETTINGS GROUP
            // --------------------------------------------------
            SettingsGroupHeader(title = strings.notificationSection, icon = Icons.Default.Notifications)
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                backgroundColor = DarkCardSurface,
                borderColor = GlassBorder,
                elevation = 6.dp
            ) {
                Column {
                    SettingsToggleRow(
                        title = strings.dailyReminder,
                        subtitle = strings.dailyReminderSubtitle,
                        icon = Icons.Default.Notifications,
                        checked = userSettings.dailyChallengeReminder,
                        testTag = "daily_reminder_switch",
                        onCheckedChange = { checked ->
                            if (userSettings.vibrationEnabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            if (userSettings.soundEffectsEnabled) {
                                SoundEffects.playCoinSound()
                            }
                            updateSettings(userSettings.copy(dailyChallengeReminder = checked))
                            val msg = if (checked) "Daily Challenge reminders enabled!" else "Daily Challenge reminders disabled."
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    )

                    SettingsGroupDivider()

                    SettingsToggleRow(
                        title = strings.streakReminder,
                        subtitle = strings.streakReminderSubtitle,
                        icon = Icons.Default.LocalFireDepartment,
                        iconTint = AccentStreak,
                        checked = userSettings.streakReminder,
                        testTag = "streak_reminder_switch",
                        onCheckedChange = { checked ->
                            if (userSettings.vibrationEnabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            if (userSettings.soundEffectsEnabled) {
                                SoundEffects.playCoinSound()
                            }
                            updateSettings(userSettings.copy(streakReminder = checked))
                        }
                    )

                    SettingsGroupDivider()

                    SettingsToggleRow(
                        title = strings.achievementNotifs,
                        subtitle = strings.achievementNotifsSubtitle,
                        icon = Icons.Default.EmojiEvents,
                        iconTint = AccentCoins,
                        checked = userSettings.achievementNotifications,
                        testTag = "achievement_notifs_switch",
                        onCheckedChange = { checked ->
                            if (userSettings.vibrationEnabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            if (userSettings.soundEffectsEnabled) {
                                SoundEffects.playCoinSound()
                            }
                            updateSettings(userSettings.copy(achievementNotifications = checked))
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // --------------------------------------------------
            // 5. ACCOUNT & SECURITY GROUP
            // --------------------------------------------------
            SettingsGroupHeader(title = strings.accountSection, icon = Icons.Default.Person)
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                backgroundColor = DarkCardSurface,
                borderColor = GlassBorder,
                elevation = 6.dp
            ) {
                Column {
                    SettingsActionRow(
                        title = strings.editProfile,
                        subtitle = "${strings.editProfileSubtitle} ($playerEmail)",
                        icon = Icons.Default.Edit,
                        testTag = "edit_profile_button",
                        onClick = {
                            if (userSettings.vibrationEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            newUsernameInput = playerName
                            showEditProfileDialog = true
                        }
                    )

                    SettingsGroupDivider()

                    SettingsActionRow(
                        title = strings.changePassword,
                        subtitle = strings.changePasswordSubtitle,
                        icon = Icons.Default.Key,
                        testTag = "change_password_button",
                        onClick = {
                            if (userSettings.vibrationEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            newPasswordInput = ""
                            confirmPasswordInput = ""
                            showChangePasswordDialog = true
                        }
                    )

                    SettingsGroupDivider()

                    SettingsActionRow(
                        title = strings.logout,
                        subtitle = strings.logoutSubtitle,
                        icon = Icons.AutoMirrored.Filled.Logout,
                        iconTint = Color(0xFFFF5252),
                        titleColor = Color(0xFFFF5252),
                        testTag = "logout_button",
                        onClick = {
                            if (userSettings.vibrationEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showLogoutDialog = true
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // --------------------------------------------------
            // 6. DATA & SYNC GROUP
            // --------------------------------------------------
            SettingsGroupHeader(title = strings.dataSection, icon = Icons.Default.CloudSync)
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                backgroundColor = DarkCardSurface,
                borderColor = GlassBorder,
                elevation = 6.dp
            ) {
                Column {
                    val syncSubtitle = if (userSettings.lastSyncTimestamp > 0) {
                        "${strings.lastSynced} " + SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(userSettings.lastSyncTimestamp))
                    } else strings.syncSubtitle

                    SettingsActionRow(
                        title = strings.syncProgress,
                        subtitle = syncSubtitle,
                        icon = Icons.Default.CloudSync,
                        isLoading = isSyncing,
                        testTag = "sync_data_button",
                        onClick = {
                            if (!isSyncing) {
                                if (userSettings.vibrationEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                scope.launch {
                                    isSyncing = true
                                    delay(1200)
                                    val now = System.currentTimeMillis()
                                    updateSettings(userSettings.copy(lastSyncTimestamp = now))
                                    isSyncing = false
                                    if (userSettings.soundEffectsEnabled) SoundEffects.playCompleteSound()
                                    Toast.makeText(context, strings.syncSuccess, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )

                    SettingsGroupDivider()

                    val backupSubtitle = if (userSettings.lastBackupTimestamp > 0) {
                        "${strings.lastBackup} " + SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(userSettings.lastBackupTimestamp))
                    } else strings.backupSubtitle

                    SettingsActionRow(
                        title = strings.backupData,
                        subtitle = backupSubtitle,
                        icon = Icons.Default.CloudUpload,
                        isLoading = isBackingUp,
                        testTag = "backup_data_button",
                        onClick = {
                            if (!isBackingUp) {
                                if (userSettings.vibrationEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                scope.launch {
                                    isBackingUp = true
                                    delay(1000)
                                    val now = System.currentTimeMillis()
                                    updateSettings(userSettings.copy(lastBackupTimestamp = now))
                                    isBackingUp = false
                                    if (userSettings.soundEffectsEnabled) SoundEffects.playCompleteSound()
                                    Toast.makeText(context, strings.backupSuccess, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )

                    SettingsGroupDivider()

                    SettingsActionRow(
                        title = strings.restoreData,
                        subtitle = strings.restoreSubtitle,
                        icon = Icons.Default.Restore,
                        isLoading = isRestoring,
                        testTag = "restore_data_button",
                        onClick = {
                            if (userSettings.vibrationEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showRestoreDataDialog = true
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // --------------------------------------------------
            // 7. PRIVACY & LEGAL GROUP
            // --------------------------------------------------
            SettingsGroupHeader(title = strings.privacySection, icon = Icons.Default.Lock)
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                backgroundColor = DarkCardSurface,
                borderColor = GlassBorder,
                elevation = 6.dp
            ) {
                Column {
                    SettingsActionRow(
                        title = strings.privacyPolicy,
                        subtitle = "Read how we protect your personal data",
                        icon = Icons.Default.Policy,
                        testTag = "privacy_policy_button",
                        onClick = {
                            if (userSettings.vibrationEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showPrivacyPolicyDialog = true
                        }
                    )

                    SettingsGroupDivider()

                    SettingsActionRow(
                        title = strings.termsConditions,
                        subtitle = "View application terms of service",
                        icon = Icons.Default.Info,
                        testTag = "terms_button",
                        onClick = {
                            if (userSettings.vibrationEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showTermsDialog = true
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // --------------------------------------------------
            // 8. ABOUT APP GROUP
            // --------------------------------------------------
            SettingsGroupHeader(title = strings.aboutSection, icon = Icons.Default.Info)
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                backgroundColor = DarkCardSurface,
                borderColor = GlassBorder,
                elevation = 6.dp
            ) {
                Column {
                    SettingsInfoItem(
                        title = strings.appVersion,
                        value = "v1.0.0 (Build 100)",
                        icon = Icons.Default.Info
                    )

                    SettingsGroupDivider()

                    SettingsActionRow(
                        title = strings.rateApp,
                        subtitle = "Support Brain Quiz AI with a 5-star rating",
                        icon = Icons.Default.Star,
                        iconTint = AccentCoins,
                        testTag = "rate_app_button",
                        onClick = {
                            if (userSettings.vibrationEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showRateAppDialog = true
                        }
                    )

                    SettingsGroupDivider()

                    SettingsActionRow(
                        title = strings.shareApp,
                        subtitle = "Invite friends to challenge your high score",
                        icon = Icons.Default.Share,
                        testTag = "share_app_button",
                        onClick = {
                            if (userSettings.vibrationEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, strings.appName)
                                putExtra(Intent.EXTRA_TEXT, "Check out ${strings.appName}! Train your brain and level up your knowledge: https://brainquiz.ai")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share ${strings.appName} via"))
                        }
                    )

                    SettingsGroupDivider()

                    SettingsActionRow(
                        title = strings.contactSupport,
                        subtitle = "Get support or submit suggestions",
                        icon = Icons.Default.Support,
                        testTag = "contact_support_button",
                        onClick = {
                            if (userSettings.vibrationEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showContactSupportDialog = true
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // --------------------------------------------------
            // 9. DANGER ZONE
            // --------------------------------------------------
            SettingsGroupHeader(
                title = strings.dangerZone,
                icon = Icons.Default.Warning,
                headerColor = Color(0xFFEF5350)
            )

            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reset_account_button"),
                shape = RoundedCornerShape(22.dp),
                backgroundColor = Color(0xFF2C1318),
                borderColor = Color(0xFFEF5350).copy(alpha = 0.5f),
                elevation = 6.dp,
                onClick = {
                    if (userSettings.vibrationEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showResetConfirmDialog = true
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD32F2F).copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = strings.resetAccount,
                            tint = Color(0xFFEF5350),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.resetAccount,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            ),
                            color = Color(0xFFEF5350)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = strings.resetSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // ==================================================
    // DIALOGS SECTION
    // ==================================================

    // 1. Edit Profile Dialog
    if (showEditProfileDialog) {
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            containerColor = DarkBackground,
            title = {
                Text(
                    text = strings.editProfile,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextWhite
                )
            },
            text = {
                Column {
                    Text(text = "${strings.email}:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text(text = playerEmail, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = TextWhite)

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(text = strings.username, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = newUsernameInput,
                        onValueChange = { newUsernameInput = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = PrimaryPurple,
                            unfocusedBorderColor = DarkCardBorder,
                            cursorColor = PrimaryPurpleLight
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("username_text_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newUsernameInput.isNotBlank()) {
                            onEditUsername?.invoke(newUsernameInput.trim())
                            showEditProfileDialog = false
                            if (userSettings.soundEffectsEnabled) SoundEffects.playCoinSound()
                            Toast.makeText(context, strings.profileUpdated, Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                    modifier = Modifier.testTag("save_profile_button")
                ) {
                    Text(strings.saveChanges, color = TextWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text(strings.cancel, color = TextSecondary)
                }
            }
        )
    }

    // 2. Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = DarkBackground,
            title = {
                Text(strings.confirmLogoutTitle, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = TextWhite)
            },
            text = {
                Text(strings.confirmLogoutDesc, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                ) {
                    Text(strings.logout, color = TextWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(strings.cancel, color = TextSecondary)
                }
            }
        )
    }

    // 3. Restore Data Confirmation Dialog
    if (showRestoreDataDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDataDialog = false },
            containerColor = DarkBackground,
            title = {
                Text(strings.restoreData, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = TextWhite)
            },
            text = {
                Text(strings.restoreSubtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreDataDialog = false
                        scope.launch {
                            isRestoring = true
                            delay(1200)
                            isRestoring = false
                            if (userSettings.soundEffectsEnabled) SoundEffects.playCompleteSound()
                            Toast.makeText(context, strings.restoreSuccess, Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                ) {
                    Text(strings.restoreData, color = TextWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDataDialog = false }) {
                    Text(strings.cancel, color = TextSecondary)
                }
            }
        )
    }

    // 4. Change Password Dialog
    if (showChangePasswordDialog) {
        AlertDialog(
            onDismissRequest = { showChangePasswordDialog = false },
            containerColor = DarkBackground,
            title = {
                Text("Change Password", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = TextWhite)
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = newPasswordInput,
                        onValueChange = { newPasswordInput = it },
                        label = { Text("New Password", color = TextSecondary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = PrimaryPurple,
                            unfocusedBorderColor = DarkCardBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = confirmPasswordInput,
                        onValueChange = { confirmPasswordInput = it },
                        label = { Text("Confirm New Password", color = TextSecondary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = PrimaryPurple,
                            unfocusedBorderColor = DarkCardBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPasswordInput.length >= 6 && newPasswordInput == confirmPasswordInput) {
                            showChangePasswordDialog = false
                            if (userSettings.soundEffectsEnabled) SoundEffects.playCoinSound()
                            Toast.makeText(context, "Password updated successfully!", Toast.LENGTH_SHORT).show()
                        } else if (newPasswordInput != confirmPasswordInput) {
                            Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                ) {
                    Text("Update Password", color = TextWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePasswordDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // 5. Privacy Policy Dialog
    if (showPrivacyPolicyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyPolicyDialog = false },
            containerColor = DarkBackground,
            title = {
                Text("Privacy Policy", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = TextWhite)
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "Brain Quiz AI values your privacy. Here is how your data is handled:\n\n" +
                                "1. Information Collection: We collect account email, display name, quiz scores, XP, and streak progress to provide interactive leaderboards and sync across devices.\n\n" +
                                "2. Local Storage: All settings, audio preferences, and offline quiz progress are stored securely on your device.\n\n" +
                                "3. Cloud Synchronization: When logged in, your progress is synced with Firebase Firestore.\n\n" +
                                "4. No Data Sharing: We do not sell or share your personal data with third-party advertisers.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPrivacyPolicyDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                ) {
                    Text("I Understand", color = TextWhite)
                }
            }
        )
    }

    // 6. Terms & Conditions Dialog
    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            containerColor = DarkBackground,
            title = {
                Text("Terms & Conditions", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = TextWhite)
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "Terms of Service for Brain Quiz AI:\n\n" +
                                "1. Usage Agreement: By using Brain Quiz AI, you agree to engage respectfully with trivia content and community leaderboards.\n\n" +
                                "2. Fair Play: Automation, bots, or unauthorized manipulation of quiz scores and XP is strictly prohibited.\n\n" +
                                "3. Virtual Currency: Coins and XP earned in Brain Quiz AI have no monetary value and cannot be exchanged for cash.\n\n" +
                                "4. Modifications: Brain Quiz AI reserves the right to adjust question content and game balance to ensure a fair experience.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showTermsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                ) {
                    Text("Accept Terms", color = TextWhite)
                }
            }
        )
    }

    // 7. Rate App Dialog
    if (showRateAppDialog) {
        AlertDialog(
            onDismissRequest = { showRateAppDialog = false },
            containerColor = DarkBackground,
            title = {
                Text("Rate Brain Quiz AI", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = TextWhite)
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("How would you rate your experience?", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 1..5) {
                            IconButton(
                                onClick = {
                                    ratingStars = i
                                    if (userSettings.soundEffectsEnabled) SoundEffects.playCoinSound()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Star $i",
                                    tint = if (i <= ratingStars) AccentCoins else TextSecondary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = ratingFeedback,
                        onValueChange = { ratingFeedback = it },
                        placeholder = { Text("Optional feedback...", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = PrimaryPurple,
                            unfocusedBorderColor = DarkCardBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRateAppDialog = false
                        if (userSettings.soundEffectsEnabled) SoundEffects.playCompleteSound()
                        Toast.makeText(context, "Thank you for rating Brain Quiz AI $ratingStars stars!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                ) {
                    Text("Submit Rating", color = TextWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRateAppDialog = false }) {
                    Text("Later", color = TextSecondary)
                }
            }
        )
    }

    // 8. Contact Support Dialog
    if (showContactSupportDialog) {
        AlertDialog(
            onDismissRequest = { showContactSupportDialog = false },
            containerColor = DarkBackground,
            title = {
                Text("Contact Support", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = TextWhite)
            },
            text = {
                Column {
                    Text("Need help or want to suggest a question topic?", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Support Email:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text("support@brainquiz.ai", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = PrimaryPurpleLight)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Average response time: Within 24 hours", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val emailIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "message/rfc822"
                            putExtra(Intent.EXTRA_EMAIL, arrayOf("support@brainquiz.ai"))
                            putExtra(Intent.EXTRA_SUBJECT, "Brain Quiz AI Support Request")
                        }
                        try {
                            context.startActivity(Intent.createChooser(emailIntent, "Send email using..."))
                        } catch (e: Exception) {
                            Toast.makeText(context, "Support email: support@brainquiz.ai", Toast.LENGTH_LONG).show()
                        }
                        showContactSupportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                ) {
                    Text("Send Email", color = TextWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { showContactSupportDialog = false }) {
                    Text("Close", color = TextSecondary)
                }
            }
        )
    }

    // 9. Reset Account Confirmation Dialog
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            containerColor = DarkBackground,
            title = {
                Text(
                    text = "Reset Account Progress",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextWhite
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to reset your account? All local XP, coins, and quiz history will be erased. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirmDialog = false
                        onResetAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    modifier = Modifier.testTag("confirm_reset_account_button")
                ) {
                    Text("Reset Account", color = TextWhite)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetConfirmDialog = false },
                    modifier = Modifier.testTag("cancel_reset_account_button")
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun SettingsHeroHeader(
    title: String,
    onBackClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gearRotate")
    val gearRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gearRotation"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(PrimaryPurple.copy(alpha = 0.35f))
                    .border(1.dp, GlassBorder, CircleShape)
                    .bounceClick(scaleDown = 0.9f) { onBackClick() }
                    .testTag("settings_back_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextWhite,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = TextWhite
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = PrimaryPurpleLight,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = "Customize your Brain Quiz AI experience",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        // Floating Animated Gear Icon
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(PrimaryPurple.copy(alpha = 0.4f), Color.Transparent)
                    )
                )
                .border(1.5.dp, PrimaryPurpleLight, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = PrimaryPurpleLight,
                modifier = Modifier
                    .size(24.dp)
                    .scale(1f)
            )
        }
    }
}

@Composable
private fun SettingsGroupHeader(
    title: String,
    icon: ImageVector,
    headerColor: Color = PrimaryPurpleLight
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = headerColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp
            ),
            color = headerColor
        )
    }
    Spacer(modifier = Modifier.height(6.dp))
}

@Composable
private fun SettingsGroupDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 0.8.dp,
        color = GlassBorder.copy(alpha = 0.4f)
    )
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    iconTint: Color = PrimaryPurpleLight,
    testTag: String = "",
    onCheckedChange: (Boolean) -> Unit
) {
    val switchTrackColor by animateColorAsState(
        targetValue = if (checked) PrimaryPurple else DarkBackground,
        animationSpec = tween(250),
        label = "switchTrackColor"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                color = TextWhite
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontSize = 11.5.sp
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = TextWhite,
                checkedTrackColor = PrimaryPurple,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = DarkBackground,
                uncheckedBorderColor = DarkCardBorder
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color = PrimaryPurpleLight,
    titleColor: Color = TextWhite,
    isLoading: Boolean = false,
    testTag: String = "",
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(scaleDown = 0.98f) { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = iconTint,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                color = titleColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontSize = 11.5.sp
            )
        }
    }
}

@Composable
private fun SettingsInfoItem(
    title: String,
    value: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(PrimaryPurpleLight.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = PrimaryPurpleLight,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                color = TextWhite
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontSize = 11.5.sp
            )
        }
    }
}

@Composable
private fun SettingsAmbientCanvas() {
    val infiniteTransition = rememberInfiniteTransition(label = "settingsGlow")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "glowProgress"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        val particlePositions = listOf(
            Offset(width * 0.2f, height * ((0.1f + progress * 0.9f) % 1.0f)),
            Offset(width * 0.8f, height * ((0.7f - progress * 0.6f + 1.0f) % 1.0f)),
            Offset(width * 0.5f, height * ((0.4f + progress * 0.7f) % 1.0f))
        )

        particlePositions.forEachIndexed { i, pos ->
            val color = if (i % 2 == 0) PrimaryPurpleLight.copy(alpha = 0.18f) else AccentCoins.copy(alpha = 0.15f)
            val radius = if (i % 2 == 0) 3.dp.toPx() else 2.dp.toPx()
            drawCircle(color = color, radius = radius, center = pos)
        }
    }
}
