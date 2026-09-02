package com.example.ui.screens

import android.Manifest
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Museum
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.ui.localization.LocalAppStrings
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.R
import com.example.ui.components.AchievementUnlockedDialog
import com.example.ui.components.CategoryCard
import com.example.ui.components.GlassCard
import com.example.ui.components.NotificationPermissionDialog
import com.example.ui.components.NotificationsDialog
import com.example.ui.components.QuickPlayCard
import com.example.ui.components.StatCard
import com.example.ui.screens.AchievementsScreen
import com.example.ui.screens.LeaderboardScreen
import com.example.ui.screens.ProfileScreen
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
import androidx.compose.ui.platform.LocalContext
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextWhite
import com.example.utils.LevelUtils
import com.example.utils.NotificationHelper
import com.example.utils.SoundEffects
import com.example.utils.VibrationUtils
import com.example.viewmodel.BottomNavTab
import com.example.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToQuiz: (String) -> Unit = {},
    onNavigateToAiGenerator: () -> Unit = {},
    onNavigateToAiQuickAnswer: () -> Unit = {},
    onNavigateToAvatarShop: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Point 9: Immediately before HomeScreen receives / renders uiState
    Log.d("RUNTIME_TRACE", "[Point 9: HomeScreen received uiState] playerName=${uiState.playerName}, xp=${uiState.xp}, coins=${uiState.coins}, streakDays=${uiState.streakDays}, level=${uiState.level}")

    var showNotificationPermissionRationale by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            NotificationHelper.syncReminders(context)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadUserProfile()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPerm = NotificationHelper.hasNotificationPermission(context)
            val isPromptShown = NotificationHelper.isNotificationPromptShown(context)
            if (!hasPerm && !isPromptShown) {
                showNotificationPermissionRationale = true
            } else if (hasPerm) {
                NotificationHelper.syncReminders(context)
            }
        } else {
            NotificationHelper.syncReminders(context)
        }
    }

    if (showNotificationPermissionRationale) {
        NotificationPermissionDialog(
            onEnableClick = {
                NotificationHelper.setNotificationPromptShown(context, true)
                showNotificationPermissionRationale = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (!NotificationHelper.hasNotificationPermission(context)) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        NotificationHelper.syncReminders(context)
                    }
                } else {
                    NotificationHelper.syncReminders(context)
                }
            },
            onMaybeLaterClick = {
                NotificationHelper.setNotificationPromptShown(context, true)
                showNotificationPermissionRationale = false
            }
        )
    }

    if (uiState.showNotificationsDialog) {
        NotificationsDialog(
            onDismiss = { viewModel.toggleNotificationsDialog(false) },
            testTag = "home_notifications_dialog"
        )
    }

    if (uiState.showEditUsernameDialog) {
        EditUsernameDialog(
            currentName = uiState.playerName,
            onConfirm = { viewModel.updateUsername(it) },
            onDismiss = { viewModel.setShowEditUsernameDialog(false) }
        )
    }

    if (uiState.newlyUnlockedAchievements.isNotEmpty()) {
        AchievementUnlockedDialog(
            achievement = uiState.newlyUnlockedAchievements.first(),
            onDismiss = { viewModel.dismissAchievementDialog() }
        )
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen"),
        containerColor = DarkBackground,
        topBar = {
            HomeTopAppBar(
                playerName = uiState.playerName,
                unreadNotifications = uiState.unreadNotificationsCount,
                onNotificationClick = {
                    VibrationUtils.vibrateClick(context)
                    SoundEffects.playCoinSound(context)
                    viewModel.toggleNotificationsDialog(true)
                },
                onProfileClick = {
                    VibrationUtils.vibrateClick(context)
                    viewModel.selectNavTab(BottomNavTab.Profile)
                }
            )
        },
        bottomBar = {
            HomeBottomNavigationBar(
                selectedTab = uiState.selectedTab,
                onTabSelect = { viewModel.selectNavTab(it) }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState.selectedTab) {
                BottomNavTab.Home -> {
                    MainHomeContent(
                        uiState = uiState,
                        onNavigateToQuiz = onNavigateToQuiz,
                        onNavigateToAiGenerator = onNavigateToAiGenerator,
                        onNavigateToAiQuickAnswer = onNavigateToAiQuickAnswer
                    )
                }
                BottomNavTab.Leaderboard -> {
                    LeaderboardScreen(
                        currentUserName = uiState.playerName,
                        currentUserXp = uiState.xp,
                        currentUserLevel = uiState.level,
                        currentUserAvatar = uiState.avatarId,
                        leaderboardData = uiState.leaderboardData,
                        selectedPeriod = uiState.leaderboardPeriod,
                        onPeriodSelected = { period -> viewModel.loadLeaderboard(period) }
                    )
                }
                BottomNavTab.Achievements -> {
                    AchievementsScreen(
                        userXp = uiState.xp,
                        userStreak = uiState.streakDays,
                        achievements = uiState.achievements
                    )
                }
                BottomNavTab.Profile -> {
                    ProfileScreen(
                        playerName = uiState.playerName,
                        playerEmail = uiState.playerEmail,
                        avatarId = uiState.avatarId,
                        xp = uiState.xp,
                        level = uiState.level,
                        coins = uiState.coins,
                        streakDays = uiState.streakDays,
                        rank = uiState.rank,
                        unlockedAchievementsCount = uiState.unlockedAchievementsCount,
                        totalAchievementsCount = uiState.totalAchievementsCount,
                        totalQuizzesPlayed = uiState.totalQuizzesPlayed,
                        totalQuestionsAnswered = uiState.totalQuestionsAnswered,
                        totalCorrectAnswers = uiState.totalCorrectAnswers,
                        accuracyPercentage = uiState.accuracyPercentage,
                        bestScore = uiState.bestScore,
                        longestStreak = uiState.longestStreak,
                        quizHistory = uiState.quizHistory,
                        achievements = uiState.achievements,
                        onEditUsername = { viewModel.setShowEditUsernameDialog(true) },
                        onOpenAvatarShop = onNavigateToAvatarShop,
                        onResetAccount = { viewModel.resetGuestAccount() },
                        onSignOut = {
                            viewModel.signOut()
                            onNavigateToLogin()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MainHomeContent(
    uiState: com.example.viewmodel.HomeUiState,
    onNavigateToQuiz: (String) -> Unit,
    onNavigateToAiGenerator: () -> Unit,
    onNavigateToAiQuickAnswer: () -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Premium Hero Welcome Header
        item(span = { GridItemSpan(2) }) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                shape = RoundedCornerShape(24.dp),
                backgroundColor = DarkCardSurface,
                borderColor = PrimaryPurple.copy(alpha = 0.5f),
                elevation = 8.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    PrimaryPurple.copy(alpha = 0.35f),
                                    DarkCardSurface,
                                    PrimaryPurpleLight.copy(alpha = 0.15f)
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Text(
                                    text = "Hello, ${uiState.playerName} 👋",
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 23.sp
                                    ),
                                    color = TextWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.testTag("greeting_text")
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "Challenge Your Mind Every Day",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.testTag("greeting_subtext")
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Rank Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(AccentCoins, AccentCoinsGradientEnd)
                                        )
                                    )
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                                    .testTag("rank_badge"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEvents,
                                        contentDescription = "Rank",
                                        tint = TextWhite,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = uiState.rank,
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
                        }
                    }
                }
            }
        }

        // 2. Animated XP & Level Progress Card
        item(span = { GridItemSpan(2) }) {
            val levelProgress = LevelUtils.getProgress(uiState.xp)
            val animatedProgress by animateFloatAsState(
                targetValue = levelProgress,
                animationSpec = tween(durationMillis = 300),
                label = "xp_level_progress"
            )
            val currentLevel = LevelUtils.getLevel(uiState.xp)
            val xpToNext = LevelUtils.getXpToNextLevel(uiState.xp)

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                backgroundColor = DarkCardSurface,
                borderColor = AccentXP.copy(alpha = 0.4f),
                elevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(AccentXP.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Level",
                                    tint = AccentXP,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "LEVEL $currentLevel",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    letterSpacing = 0.5.sp
                                ),
                                color = TextWhite
                            )
                        }

                        Text(
                            text = "$xpToNext XP to Level ${currentLevel + 1}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            color = AccentXP
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Progress Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(DarkBackground)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedProgress)
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(AccentXP, AccentXPGradientEnd)
                                    )
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${uiState.xp} Total XP",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Text(
                            text = "${(animatedProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // 3. Featured Daily Challenge Card Banner
        item(span = { GridItemSpan(2) }) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                backgroundColor = DarkCardSurface,
                borderColor = AccentCoins.copy(alpha = 0.5f),
                elevation = 6.dp,
                onClick = { onNavigateToQuiz("daily") }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    AccentCoins.copy(alpha = 0.2f),
                                    DarkCardSurface,
                                    AccentStreak.copy(alpha = 0.15f)
                                )
                            )
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AccentCoins)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "⚡ 2X REWARDS",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 10.sp
                                        ),
                                        color = DarkBackground
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Daily Challenge",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    ),
                                    color = TextWhite
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Earn double Coins & XP today!",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Button(
                            onClick = { onNavigateToQuiz("daily") },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentCoins,
                                contentColor = DarkBackground
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "Start",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp
                                )
                            )
                        }
                    }
                }
            }
        }

        // 4. Statistics Grid (XP, Rank, Coins, Streak, Achievements)
        item(span = { GridItemSpan(2) }) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Total XP",
                        value = "${uiState.xp} XP",
                        icon = Icons.Default.Bolt,
                        accentColors = listOf(AccentXP, AccentXPGradientEnd),
                        modifier = Modifier.weight(1f),
                        testTag = "xp_stat_card"
                    )

                    StatCard(
                        title = "Current Rank",
                        value = uiState.rank,
                        icon = Icons.Default.EmojiEvents,
                        accentColors = listOf(AccentCoins, AccentCoinsGradientEnd),
                        modifier = Modifier.weight(1f),
                        testTag = "rank_stat_card"
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Coins",
                        value = "${uiState.coins}",
                        icon = Icons.Default.MonetizationOn,
                        accentColors = listOf(AccentCoins, AccentCoinsGradientEnd),
                        modifier = Modifier.weight(1f),
                        testTag = "coins_stat_card"
                    )

                    StatCard(
                        title = "Streak",
                        value = "${uiState.streakDays} Days 🔥",
                        icon = Icons.Default.LocalFireDepartment,
                        accentColors = listOf(AccentStreak, AccentStreakGradientEnd),
                        modifier = Modifier.weight(1f),
                        testTag = "streak_stat_card"
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                StatCard(
                    title = "Achievements",
                    value = "🏅 ${uiState.unlockedAchievementsCount}/${uiState.totalAchievementsCount}",
                    icon = Icons.Default.EmojiEvents,
                    accentColors = listOf(PrimaryPurple, PrimaryPurpleLight),
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "achievements_stat_card"
                )
            }
        }

        // 5. Latest Quiz Result Card
        item(span = { GridItemSpan(2) }) {
            LatestQuizResultCard(
                categoryName = uiState.lastQuizCategory,
                score = uiState.lastQuizScore,
                xpEarned = uiState.lastQuizXpEarned,
                date = uiState.lastQuizDate,
                hasHistory = uiState.hasQuizHistory,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // 6. Horizontal Premium Cards (Quick Play Section)
        item(span = { GridItemSpan(2) }) {
            Column(
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Text(
                    text = "Quick Modes",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = TextWhite
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(end = 12.dp)
                ) {
                    items(uiState.quickPlayOptions) { option ->
                        val icon = when (option.id) {
                            "quick" -> Icons.Default.PlayArrow
                            "ai_quick_answer" -> Icons.Default.SmartToy
                            "daily" -> Icons.Default.AutoAwesome
                            "ai_custom" -> Icons.Default.AutoAwesome
                            else -> Icons.Default.Psychology
                        }
                        val colors = when (option.id) {
                            "quick" -> listOf(PrimaryPurple, PrimaryPurpleLight)
                            "ai_quick_answer" -> listOf(AccentLevel, AccentLevelGradientEnd)
                            "daily" -> listOf(AccentCoins, AccentCoinsGradientEnd)
                            "ai_custom" -> listOf(PrimaryPurple, PrimaryPurpleLight)
                            else -> listOf(DarkCardBorder, TextMuted)
                        }

                        QuickPlayCard(
                            title = option.title,
                            subtitle = option.subtitle,
                            badgeText = option.badgeText,
                            icon = icon,
                            accentColors = colors,
                            actionText = if (option.id == "ai_quick_answer") "Ask Now" else null,
                            isComingSoon = false,
                            onClick = {
                                when (option.id) {
                                    "ai_quick_answer" -> onNavigateToAiQuickAnswer()
                                    "ai_custom" -> onNavigateToAiGenerator()
                                    else -> onNavigateToQuiz(option.id)
                                }
                            },
                            testTag = "quick_play_${option.id}"
                        )
                    }
                }
            }
        }

        // 7. Categories Section Title
        item(span = { GridItemSpan(2) }) {
            Text(
                text = "Quiz Categories",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = TextWhite,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        // 8. Categories Grid (8 items)
        items(uiState.categories) { category ->
            val icon = getCategoryIcon(category.iconName)
            CategoryCard(
                title = category.title,
                questionsCount = category.questionsCount,
                icon = icon,
                accentColor = category.accentColor,
                onClick = { onNavigateToQuiz(category.id) },
                testTag = "category_${category.id}"
            )
        }

        // Bottom Spacer
        item(span = { GridItemSpan(2) }) {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun HomeTopAppBar(
    playerName: String,
    unreadNotifications: Int,
    onNotificationClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(DarkBackground)
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Brand Title
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(PrimaryPurple)
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.brain_quiz_logo),
                    contentDescription = "Logo",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "BrainQuizAI",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    letterSpacing = 0.5.sp
                ),
                color = TextWhite
            )
        }

        // Actions: Notifications & Avatar Profile
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Notification Icon with Badge
            IconButton(
                onClick = onNotificationClick,
                modifier = Modifier.testTag("notification_button")
            ) {
                BadgedBox(
                    badge = {
                        if (unreadNotifications > 0) {
                            Badge(
                                containerColor = PrimaryPurpleLight,
                                contentColor = TextWhite
                            ) {
                                Text("$unreadNotifications")
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = TextWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Profile Avatar
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(PrimaryPurple, PrimaryPurpleLight)
                        )
                    )
                    .border(1.5.dp, GlassBorder, CircleShape)
                    .clickable { onProfileClick() }
                    .testTag("profile_avatar"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = playerName.firstOrNull()?.toString()?.uppercase() ?: "P",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = TextWhite
                )
            }
        }
    }
}

@Composable
fun HomeBottomNavigationBar(
    selectedTab: BottomNavTab,
    onTabSelect: (BottomNavTab) -> Unit
) {
    val strings = LocalAppStrings.current
    NavigationBar(
        modifier = Modifier
            .navigationBarsPadding()
            .testTag("bottom_navigation_bar"),
        containerColor = DarkCardSurface,
        tonalElevation = 12.dp
    ) {
        val items = listOf(
            NavTabItem(BottomNavTab.Home, strings.navHome, Icons.Filled.Home, Icons.Outlined.Home),
            NavTabItem(BottomNavTab.Leaderboard, strings.navLeaderboard, Icons.Filled.EmojiEvents, Icons.Outlined.EmojiEvents),
            NavTabItem(BottomNavTab.Achievements, strings.navAchievements, Icons.Filled.MilitaryTech, Icons.Outlined.MilitaryTech),
            NavTabItem(BottomNavTab.Profile, strings.navProfile, Icons.Filled.Person, Icons.Outlined.Person)
        )

        items.forEach { item ->
            val isSelected = selectedTab == item.tab
            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    SoundEffects.playClickSound()
                    onTabSelect(item.tab)
                },
                alwaysShowLabel = true,
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 10.sp
                        ),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PrimaryPurpleLight,
                    selectedTextColor = PrimaryPurpleLight,
                    indicatorColor = PrimaryPurple.copy(alpha = 0.25f),
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted
                ),
                modifier = Modifier.testTag("nav_item_${item.label.lowercase()}")
            )
        }
    }
}

private data class NavTabItem(
    val tab: BottomNavTab,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private fun getCategoryIcon(name: String): ImageVector {
    return when (name) {
        "Psychology" -> Icons.Default.Psychology
        "Science" -> Icons.Default.Science
        "Museum" -> Icons.Default.Museum
        "SportsSoccer" -> Icons.Default.SportsSoccer
        "Movie" -> Icons.Default.Movie
        "Terminal" -> Icons.Default.Terminal
        "Public" -> Icons.Default.Public
        "Calculate" -> Icons.Default.Calculate
        else -> Icons.Default.MenuBook
    }
}

@Composable
fun LatestQuizResultCard(
    categoryName: String,
    score: Int,
    xpEarned: Int,
    date: String,
    hasHistory: Boolean,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("latest_quiz_result_card"),
        shape = RoundedCornerShape(20.dp),
        backgroundColor = DarkCardSurface,
        borderColor = GlassBorder,
        elevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(PrimaryPurple.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = "Quiz Result",
                            tint = PrimaryPurpleLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Latest Quiz Result",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        Text(
                            text = if (hasHistory && categoryName.isNotBlank()) categoryName else "No quiz taken yet",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = TextWhite,
                            modifier = Modifier.testTag("latest_quiz_category")
                        )
                    }
                }

                if (hasHistory && categoryName.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PrimaryPurple.copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, PrimaryPurpleLight.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "+$xpEarned XP",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            ),
                            color = PrimaryPurpleLight,
                            modifier = Modifier
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                .testTag("latest_quiz_xp_earned")
                        )
                    }
                }
            }

            if (hasHistory && categoryName.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Last Score",
                            tint = AccentCoins,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Last Score: ",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Text(
                            text = "$score / 10",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = TextWhite,
                            modifier = Modifier.testTag("latest_quiz_score")
                        )
                    }

                    if (date.isNotBlank()) {
                        Text(
                            text = date,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            modifier = Modifier.testTag("latest_quiz_date")
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Play a quiz to earn XP and advance your Rank!",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }
    }
}
