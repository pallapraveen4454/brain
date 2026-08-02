package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.QuizResult
import com.example.ui.components.GlassCard
import com.example.ui.theme.AccentCoins
import com.example.ui.theme.AccentCoinsGradientEnd
import com.example.ui.theme.AccentLevel
import com.example.ui.theme.AccentLevelGradientEnd
import com.example.ui.theme.AccentStreak
import com.example.ui.theme.AccentStreakGradientEnd
import com.example.ui.theme.AccentXP
import com.example.ui.theme.AccentXPGradientEnd
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkCardSurface
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.PrimaryPurpleLight
import com.example.ui.localization.LocalAppStrings
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextWhite

@Composable
fun ProfileScreen(
    playerName: String,
    playerEmail: String,
    avatarId: String = "brain",
    xp: Int,
    level: Int,
    coins: Int,
    streakDays: Int,
    rank: String,
    unlockedAchievementsCount: Int = 0,
    totalAchievementsCount: Int = 0,
    totalQuizzesPlayed: Int = 0,
    totalQuestionsAnswered: Int = 0,
    totalCorrectAnswers: Int = 0,
    accuracyPercentage: Int = 0,
    bestScore: Int = 0,
    longestStreak: Int = 0,
    quizHistory: List<QuizResult> = emptyList(),
    onEditUsername: () -> Unit = {},
    onOpenAvatarShop: () -> Unit = {},
    onResetAccount: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSettingsScreen by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var futureFeatureNotice by remember { mutableStateOf<String?>(null) }

    if (showSettingsScreen) {
        SettingsScreen(
            playerName = playerName,
            playerEmail = playerEmail,
            onEditUsername = { _ -> onEditUsername() },
            onSignOut = onSignOut,
            onResetAccount = {
                showSettingsScreen = false
                onResetAccount()
            },
            onBackClick = { showSettingsScreen = false },
            modifier = modifier
        )
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .testTag("profile_screen"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            val strings = LocalAppStrings.current
            // Profile Header Title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.userProfile,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    ),
                    color = TextWhite,
                    modifier = Modifier.testTag("profile_title")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 🖼 Current Avatar Section
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(PrimaryPurple, PrimaryPurpleLight)
                        )
                    )
                    .border(2.dp, GlassBorder, CircleShape)
                    .testTag("profile_avatar_box"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = AvatarUtils.getEmoji(avatarId),
                    fontSize = 46.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Username and Edit Icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = playerName,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = TextWhite,
                    modifier = Modifier.testTag("profile_player_name")
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onEditUsername,
                    modifier = Modifier
                        .size(30.dp)
                        .testTag("edit_username_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Username",
                        tint = PrimaryPurpleLight,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = playerEmail.ifBlank { "guest@brainquiz.ai" },
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.testTag("profile_player_email")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Rank Chip
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = PrimaryPurple.copy(alpha = 0.25f),
                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryPurpleLight)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Rank",
                        tint = AccentCoins,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = rank,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        ),
                        color = TextWhite,
                        maxLines = 1,
                        softWrap = false,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ⭐ XP | 🪙 Coins | 🔥 Streak | 🏅 Rank Mini Metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MiniMetricCard("XP", "$xp", Icons.Default.Bolt, AccentXP, Modifier.weight(1f))
                MiniMetricCard("Level", "$level", Icons.Default.MilitaryTech, AccentLevel, Modifier.weight(1f))
                MiniMetricCard("Coins", "$coins", Icons.Default.MonetizationOn, AccentCoins, Modifier.weight(1f))
                MiniMetricCard("Streak", "$streakDays🔥", Icons.Default.LocalFireDepartment, AccentStreak, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 🎨 Customization Section
            Text(
                text = "Customization 🎨",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = TextWhite,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 🛒 Avatar Shop Card
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenAvatarShop() }
                    .testTag("customization_avatar_shop_card"),
                shape = RoundedCornerShape(16.dp),
                backgroundColor = DarkCardSurface,
                borderColor = PrimaryPurpleLight
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(PrimaryPurple, PrimaryPurpleLight)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Avatar Shop",
                            tint = TextWhite,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Avatar Shop 🛒",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = TextWhite
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Change your avatar using earned coins",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Go",
                        tint = PrimaryPurpleLight,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Performance Statistics Section
            Text(
                text = "Performance Statistics 📊",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = TextWhite,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DetailStatTile("Quizzes Played", "$totalQuizzesPlayed", Icons.Default.Quiz, Modifier.weight(1f), "stat_quizzes_played")
                    DetailStatTile("Questions", "$totalQuestionsAnswered", Icons.Default.Psychology, Modifier.weight(1f), "stat_questions_answered")
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DetailStatTile("Correct Answers", "$totalCorrectAnswers", Icons.Default.CheckCircle, Modifier.weight(1f), "stat_correct_answers")
                    DetailStatTile("Accuracy", "$accuracyPercentage%", Icons.Default.Percent, Modifier.weight(1f), "stat_accuracy")
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DetailStatTile("Best Score", "$bestScore/10", Icons.Default.Star, Modifier.weight(1f), "stat_best_score")
                    DetailStatTile("Longest Streak", "$longestStreak Days", Icons.Default.LocalFireDepartment, Modifier.weight(1f), "stat_longest_streak")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Quiz History Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quiz History 📜",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = TextWhite
                )
                if (quizHistory.isNotEmpty()) {
                    Text(
                        text = "${quizHistory.size} Quizzes",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (quizHistory.isEmpty()) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No quiz history yet",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                            color = TextWhite
                        )
                        Text(
                            text = "Complete your first quiz to track scores and history!",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quizHistory.take(10).forEachIndexed { index, result ->
                        HistoryItemCard(result, index)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Account & App Settings Section
            Text(
                text = "Account & Settings ⚙️",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = TextWhite,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProfileOptionRow(
                    icon = Icons.Default.Settings,
                    title = "Settings",
                    subtitle = "Audio, notifications & account data",
                    onClick = {
                        onOpenSettings()
                        showSettingsScreen = true
                    },
                    testTag = "settings_option_row"
                )

                ProfileOptionRow(
                    icon = Icons.Default.HelpOutline,
                    title = "Help & Support 📞",
                    subtitle = "FAQs, game rules & support contact",
                    onClick = { showHelpDialog = true },
                    testTag = "help_support_option_row"
                )

                ProfileOptionRow(
                    icon = Icons.Default.Lock,
                    title = "Privacy Policy 🔒",
                    subtitle = "Data security & terms of service",
                    onClick = { showPrivacyDialog = true },
                    testTag = "privacy_policy_option_row"
                )

                ProfileOptionRow(
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    title = "Logout 🚪",
                    subtitle = "Sign out of your BrainQuizAI account",
                    iconColor = androidx.compose.ui.graphics.Color(0xFFF43F5E),
                    onClick = onSignOut,
                    testTag = "sign_out_option_row"
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }

        // Help & Support Dialog
        if (showHelpDialog) {
            HelpSupportDialog(onDismiss = { showHelpDialog = false })
        }

        // Privacy Policy Dialog
        if (showPrivacyDialog) {
            PrivacyPolicyDialog(onDismiss = { showPrivacyDialog = false })
        }

        // Future Feature Notice Dialog
        futureFeatureNotice?.let { notice ->
            AlertDialog(
                onDismissRequest = { futureFeatureNotice = null },
                containerColor = DarkBackground,
                title = {
                    Text(
                        text = "Feature Preview 🚀",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextWhite
                    )
                },
                text = {
                    Text(
                        text = notice,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { futureFeatureNotice = null },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurpleLight)
                    ) {
                        Text("Awesome", color = TextWhite)
                    }
                }
            )
        }
    }
}

@Composable
private fun MiniMetricCard(
    label: String,
    value: String,
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = iconTint, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp), color = TextWhite)
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun DetailStatTile(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    Card(
        modifier = modifier.testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(PrimaryPurple.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = label, tint = PrimaryPurpleLight, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp), color = TextWhite)
                Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun HistoryItemCard(result: QuizResult, index: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("quiz_history_item_$index"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.categoryName.ifBlank { "Quiz" },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                    color = TextWhite
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = result.dateFormatted.ifBlank { "Recently" },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (result.scoreOutOfTen >= 7) PrimaryPurple.copy(alpha = 0.3f) else AccentCoins.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "${result.scoreOutOfTen}/10",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextWhite,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "+${result.xpEarned} XP | +${result.coinsEarned} 🪙",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun EditUsernameDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var textValue by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkBackground,
        title = {
            Text(
                text = "Edit Username ✏️",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = TextWhite
            )
        },
        text = {
            Column {
                Text(
                    text = "Enter a new display name for your profile:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { if (it.length <= 20) textValue = it },
                    singleLine = true,
                    placeholder = { Text("Enter username...", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = PrimaryPurpleLight,
                        unfocusedBorderColor = DarkCardBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("username_input_field")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (textValue.isNotBlank()) {
                        onConfirm(textValue.trim())
                    }
                },
                enabled = textValue.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurpleLight),
                modifier = Modifier.testTag("save_username_button")
            ) {
                Text("Save", color = TextWhite)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun ProfileOptionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconColor: androidx.compose.ui.graphics.Color = PrimaryPurpleLight,
    onClick: () -> Unit,
    testTag: String = ""
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = TextWhite
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun HelpSupportDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkBackground,
        title = {
            Text(
                text = "Help & Support 📞",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = TextWhite
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Welcome to BrainQuizAI Support!",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = PrimaryPurpleLight
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• How to earn coins: Complete daily quizzes, quick quizzes, or AI custom quizzes.\n" +
                           "• How to unlock avatars: Visit the Avatar Shop from your Profile tab.\n" +
                           "• Need assistance? Contact our team at support@brainquiz.ai",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurpleLight)
            ) {
                Text("Got It", color = TextWhite)
            }
        }
    )
}

@Composable
fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkBackground,
        title = {
            Text(
                text = "Privacy Policy 🔒",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = TextWhite
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Your Privacy Matters",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = PrimaryPurpleLight
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "BrainQuizAI stores your quiz progress, scores, and avatar preferences locally and securely on your device.\n\n" +
                           "We do not sell or share personal data with third parties. Gemini AI custom quiz generation uses anonymous topic prompts.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurpleLight)
            ) {
                Text("Close", color = TextWhite)
            }
        }
    )
}

