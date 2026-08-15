package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AchievementRepository
import com.example.data.model.Achievement
import com.example.data.model.QuizResult
import com.example.ui.components.GlassCard
import com.example.ui.localization.LocalAppStrings
import com.example.ui.theme.AccentCoins
import com.example.ui.theme.AccentCoinsGradientEnd
import com.example.ui.theme.AccentLevel
import com.example.ui.theme.AccentStreak
import com.example.ui.theme.AccentXP
import com.example.ui.theme.AccentXPGradientEnd
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkCardSurface
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.PrimaryPurpleLight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextWhite
import com.example.utils.RankUtils
import com.example.utils.SoundEffects
import com.example.utils.bounceClick
import com.example.utils.shimmerEffect
import kotlin.random.Random

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
    achievements: List<Achievement> = emptyList(),
    onEditUsername: () -> Unit = {},
    onOpenAvatarShop: () -> Unit = {},
    onResetAccount: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var showSettingsScreen by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var futureFeatureNotice by remember { mutableStateOf<String?>(null) }

    // Number Count-Up Animations for flagship feel
    val animatedXp by animateIntAsState(
        targetValue = xp,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "xpCountUp"
    )
    val animatedLevel by animateIntAsState(
        targetValue = level,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "levelCountUp"
    )
    val animatedCoins by animateIntAsState(
        targetValue = coins,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "coinsCountUp"
    )
    val animatedStreak by animateIntAsState(
        targetValue = streakDays,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "streakCountUp"
    )

    // Detailed stats count-up
    val animatedQuizzesPlayed by animateIntAsState(
        targetValue = totalQuizzesPlayed,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "quizzesCountUp"
    )
    val animatedQuestions by animateIntAsState(
        targetValue = totalQuestionsAnswered,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "questionsCountUp"
    )
    val animatedCorrect by animateIntAsState(
        targetValue = totalCorrectAnswers,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "correctCountUp"
    )
    val animatedAccuracy by animateIntAsState(
        targetValue = accuracyPercentage,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "accuracyCountUp"
    )
    val animatedBestScore by animateIntAsState(
        targetValue = bestScore,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "bestScoreCountUp"
    )
    val animatedLongestStreak by animateIntAsState(
        targetValue = longestStreak,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "longestStreakCountUp"
    )

    // Level & Rank Threshold Calculations
    val calculatedRank = RankUtils.getRankForXp(xp)
    val displayRank = if (rank.isNotBlank() && rank != "Beginner") rank else calculatedRank

    val (prevThreshold, nextThreshold) = when {
        xp >= 1000 -> 1000 to 2000
        xp >= 500 -> 500 to 1000
        xp >= 100 -> 100 to 500
        else -> 0 to 100
    }
    val levelXpProgress = ((xp - prevThreshold).toFloat() / (nextThreshold - prevThreshold).coerceAtLeast(1)).coerceIn(0f, 1f)
    val animatedLevelProgress by animateFloatAsState(
        targetValue = levelXpProgress,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "levelProgressBar"
    )

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
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(DarkBackground)
        ) {
            // High Performance Ambient Floating Light Particles Canvas
            ProfileAmbientParticlesCanvas()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 18.dp)
                    .verticalScroll(rememberScrollState())
                    .testTag("profile_screen"),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                val strings = LocalAppStrings.current
                // Profile Header Title
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.userProfile,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = TextWhite,
                        modifier = Modifier.testTag("profile_title")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ------------------------------------------------
                // 1. HERO PROFILE CARD WITH ANIMATED XP RING & AVATAR
                // ------------------------------------------------
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    backgroundColor = DarkCardSurface,
                    borderColor = PrimaryPurpleLight.copy(alpha = 0.4f),
                    elevation = 12.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Avatar Box with Animated XP Circular Ring
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            // Animated XP Canvas Progress Ring around Avatar
                            Canvas(modifier = Modifier.size(126.dp)) {
                                val strokeWidth = 6.dp.toPx()
                                val radius = (size.minDimension - strokeWidth) / 2
                                val centerOffset = Offset(size.width / 2, size.height / 2)

                                // Track circle
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.12f),
                                    radius = radius,
                                    center = centerOffset,
                                    style = Stroke(width = strokeWidth)
                                )

                                // Active XP Sweep Arc
                                drawArc(
                                    brush = Brush.sweepGradient(
                                        colors = listOf(
                                            AccentXP,
                                            PrimaryPurpleLight,
                                            AccentCoins,
                                            AccentXP
                                        )
                                    ),
                                    startAngle = -90f,
                                    sweepAngle = 360f * animatedLevelProgress,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                            }

                            // Avatar Circle Container
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(CircleShape)
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(PrimaryPurple, PrimaryPurpleLight)
                                        )
                                    )
                                    .border(3.dp, GlassBorder, CircleShape)
                                    .shadow(12.dp, CircleShape, ambientColor = PrimaryPurple, spotColor = PrimaryPurpleLight)
                                    .bounceClick(scaleDown = 0.94f) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        SoundEffects.playCoinSound()
                                        onOpenAvatarShop()
                                    }
                                    .testTag("profile_avatar_box"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = AvatarUtils.getEmoji(avatarId),
                                    fontSize = 50.sp
                                )
                            }

                            // Level Badge Overlay Pill
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .offset(y = 10.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DarkBackground)
                                    .border(1.5.dp, AccentXP, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "LVL $animatedLevel",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.sp,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = AccentXP
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Player Name & Edit Icon Button
                        val isGuestSession = (playerEmail == "Guest Account" || playerEmail == "guest@brainquiz.ai") || (playerName == "Guest")
                        val formattedName = if (isGuestSession) {
                            if (playerName.isBlank() || playerName == "Player" || playerName == "Guest Player") "Guest" else playerName
                        } else {
                            when {
                                playerName.isNotBlank() && playerName != "Player" && playerName != "Guest Player" && playerName != "Guest" -> playerName
                                playerEmail.isNotBlank() -> playerEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
                                else -> "Player"
                            }
                        }
                        val formattedEmail = if (isGuestSession) "Guest Account" else playerEmail

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = formattedName,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 22.sp
                                ),
                                color = TextWhite,
                                modifier = Modifier.testTag("profile_player_name")
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onEditUsername()
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryPurple.copy(alpha = 0.3f))
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
                            text = formattedEmail,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            modifier = Modifier.testTag("profile_player_email")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Shiny Rank Chip
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = PrimaryPurple.copy(alpha = 0.25f),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, PrimaryPurpleLight.copy(alpha = 0.8f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = "Rank",
                                    tint = AccentCoins,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = displayRank.uppercase(),
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp,
                                        letterSpacing = 1.sp
                                    ),
                                    color = TextWhite,
                                    maxLines = 1,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ------------------------------------------------
                // 2. MINI METRICS ROW (XP | Level | Coins | Streak)
                // ------------------------------------------------
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MiniMetricCard("XP", "$animatedXp", Icons.Default.Bolt, AccentXP, Modifier.weight(1f))
                    MiniMetricCard("Level", "$animatedLevel", Icons.Default.MilitaryTech, AccentLevel, Modifier.weight(1f))
                    MiniMetricCard("Coins", "$animatedCoins", Icons.Default.MonetizationOn, AccentCoins, Modifier.weight(1f))
                    MiniMetricCard("Streak", "${animatedStreak}d 🔥", Icons.Default.LocalFireDepartment, AccentStreak, Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ------------------------------------------------
                // 3. LEVEL PROGRESSION CARD
                // ------------------------------------------------
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    backgroundColor = DarkCardSurface,
                    borderColor = GlassBorder,
                    elevation = 6.dp
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = AccentXP,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "LEVEL PROGRESSION",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp,
                                        letterSpacing = 0.8.sp
                                    ),
                                    color = TextWhite
                                )
                            }

                            Text(
                                text = "${(animatedLevelProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp
                                ),
                                color = AccentXP
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Current: $animatedXp XP",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextWhite
                            )
                            Text(
                                text = "Next Level: $nextThreshold XP",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Animated Level XP Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(DarkBackground)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(animatedLevelProgress)
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(AccentXP, AccentXPGradientEnd)
                                        )
                                    )
                                    .shimmerEffect()
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${nextThreshold - xp} XP needed for next rank tier",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ------------------------------------------------
                // 4. CUSTOMIZATION & AVATAR SHOP CARD
                // ------------------------------------------------
                Text(
                    text = "Customization 🎨",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    ),
                    color = TextWhite,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(10.dp))

                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .bounceClick(scaleDown = 0.97f) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            SoundEffects.playCoinSound()
                            onOpenAvatarShop()
                        }
                        .testTag("customization_avatar_shop_card"),
                    shape = RoundedCornerShape(20.dp),
                    backgroundColor = DarkCardSurface,
                    borderColor = PrimaryPurpleLight
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
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
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Avatar Shop 🛒",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp
                                ),
                                color = TextWhite
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Unlock premium avatars using earned coins",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Go",
                            tint = PrimaryPurpleLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ------------------------------------------------
                // 5. PERFORMANCE STATISTICS SECTION (Grid of Cards)
                // ------------------------------------------------
                Text(
                    text = "Performance Statistics 📊",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    ),
                    color = TextWhite,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DetailStatTile("Quizzes Played", "$animatedQuizzesPlayed", Icons.Default.Quiz, Modifier.weight(1f), "stat_quizzes_played")
                        DetailStatTile("Questions", "$animatedQuestions", Icons.Default.Psychology, Modifier.weight(1f), "stat_questions_answered")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DetailStatTile("Correct Answers", "$animatedCorrect", Icons.Default.CheckCircle, Modifier.weight(1f), "stat_correct_answers")
                        DetailStatTile("Accuracy", "$animatedAccuracy%", Icons.Default.Percent, Modifier.weight(1f), "stat_accuracy")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DetailStatTile("Best Score", "$animatedBestScore/10", Icons.Default.Star, Modifier.weight(1f), "stat_best_score")
                        DetailStatTile("Longest Streak", "$animatedLongestStreak Days", Icons.Default.LocalFireDepartment, Modifier.weight(1f), "stat_longest_streak")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ------------------------------------------------
                // 6. ACHIEVEMENTS SHOWCASE CARD
                // ------------------------------------------------
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Achievements Showcase 🏆",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp
                        ),
                        color = TextWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(AccentCoins.copy(alpha = 0.2f))
                            .border(1.dp, AccentCoins.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$unlockedAchievementsCount/${totalAchievementsCount.coerceAtLeast(6)} Unlocked",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = AccentCoins,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    backgroundColor = DarkCardSurface,
                    borderColor = GlassBorder,
                    elevation = 6.dp
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        val showcaseList = remember(achievements, xp, streakDays) {
                            val all = if (achievements.isNotEmpty()) achievements else AchievementRepository().getAllAchievements(xp, coins, streakDays)
                            val keyIds = listOf("first_step", "getting_started", "quiz_warrior", "accuracy_pro", "perfect_10", "streak_3_day")
                            keyIds.mapNotNull { id -> all.find { it.id == id } }
                        }

                        val badgeEmojis = mapOf(
                            "first_step" to "🌱",
                            "getting_started" to "🚀",
                            "quiz_warrior" to "⚔️",
                            "accuracy_pro" to "🎯",
                            "perfect_10" to "🌟",
                            "streak_3_day" to "🔥"
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            for (ach in showcaseList.take(3)) {
                                val emoji = badgeEmojis[ach.id] ?: "🏆"
                                AchievementBadgeItem(ach.title, emoji, ach.isUnlocked)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            for (ach in showcaseList.drop(3).take(3)) {
                                val emoji = badgeEmojis[ach.id] ?: "🏆"
                                AchievementBadgeItem(ach.title, emoji, ach.isUnlocked)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ------------------------------------------------
                // 7. QUIZ HISTORY SECTION
                // ------------------------------------------------
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Quiz History 📜",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        ),
                        color = TextWhite
                    )
                    if (quizHistory.isNotEmpty()) {
                        Text(
                            text = "${quizHistory.size} Quizzes Completed",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (quizHistory.isEmpty()) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No quiz history recorded yet",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextWhite
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Play your first quiz to earn XP, coins, and track history!",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
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

                // ------------------------------------------------
                // 8. ACCOUNT & SETTINGS SECTION
                // ------------------------------------------------
                Text(
                    text = "Account & Settings ⚙️",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    ),
                    color = TextWhite,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProfileOptionRow(
                        icon = Icons.Default.Settings,
                        title = "Settings",
                        subtitle = "Audio, notifications & account management",
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onOpenSettings()
                            showSettingsScreen = true
                        },
                        testTag = "settings_button"
                    )

                    ProfileOptionRow(
                        icon = Icons.Default.HelpOutline,
                        title = "Help & Support 📞",
                        subtitle = "FAQs, game rules & support contact",
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showHelpDialog = true
                        },
                        testTag = "help_support_option_row"
                    )

                    ProfileOptionRow(
                        icon = Icons.Default.Lock,
                        title = "Privacy Policy 🔒",
                        subtitle = "Data security & terms of service",
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showPrivacyDialog = true
                        },
                        testTag = "privacy_policy_option_row"
                    )

                    ProfileOptionRow(
                        icon = Icons.AutoMirrored.Filled.ExitToApp,
                        title = "Logout 🚪",
                        subtitle = "Sign out of your BrainQuizAI account",
                        iconColor = Color(0xFFF43F5E),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSignOut()
                        },
                        testTag = "sign_out_option_row"
                    )
                }

                Spacer(modifier = Modifier.height(36.dp))
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
}

@Composable
private fun MiniMetricCard(
    label: String,
    value: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.bounceClick(scaleDown = 0.95f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = iconTint, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, fontSize = 15.sp),
                color = TextWhite,
                maxLines = 1
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontSize = 11.sp
            )
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
        modifier = modifier
            .bounceClick(scaleDown = 0.96f)
            .testTag(testTag),
        shape = RoundedCornerShape(18.dp),
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
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(PrimaryPurple.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = label, tint = PrimaryPurpleLight, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, fontSize = 16.sp),
                    color = TextWhite
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun AchievementBadgeItem(
    name: String,
    emoji: String,
    isUnlocked: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(
                    if (isUnlocked) Brush.linearGradient(listOf(AccentCoins, AccentCoinsGradientEnd))
                    else Brush.linearGradient(listOf(Color(0xFF2C2C3E), Color(0xFF1E1E2A)))
                )
                .border(
                    width = 2.dp,
                    color = if (isUnlocked) AccentCoins else Color.White.copy(alpha = 0.15f),
                    shape = CircleShape
                )
        ) {
            Text(
                text = emoji,
                fontSize = 28.sp,
                modifier = Modifier.alpha(if (isUnlocked) 1f else 0.4f)
            )

            if (!isUnlocked) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isUnlocked) FontWeight.Bold else FontWeight.Normal,
                fontSize = 11.sp
            ),
            color = if (isUnlocked) TextWhite else TextMuted,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun HistoryItemCard(result: QuizResult, index: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(scaleDown = 0.98f)
            .testTag("quiz_history_item_$index"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(PrimaryPurple.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Quiz,
                        contentDescription = null,
                        tint = PrimaryPurpleLight,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
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
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (result.scoreOutOfTen >= 7) Color(0xFF2ECC71).copy(alpha = 0.25f) else AccentCoins.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "${result.scoreOutOfTen}/10",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = if (result.scoreOutOfTen >= 7) Color(0xFF2ECC71) else AccentCoins,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
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
    iconColor: Color = PrimaryPurpleLight,
    onClick: () -> Unit,
    testTag: String = ""
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(scaleDown = 0.98f, onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
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
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
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
                modifier = Modifier.size(18.dp)
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

private data class ProfileParticle(
    val xPct: Float,
    val yPct: Float,
    val radius: Float,
    val color: Color
)

@Composable
private fun ProfileAmbientParticlesCanvas() {
    val particles = remember {
        List(35) {
            ProfileParticle(
                xPct = Random.nextFloat(),
                yPct = Random.nextFloat(),
                radius = Random.nextFloat() * 4f + 2f,
                color = listOf(
                    PrimaryPurple,
                    PrimaryPurpleLight,
                    AccentXP,
                    AccentCoins
                ).random()
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "profile_ambient_particles")
    val floatAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "particleFloat"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEachIndexed { idx, particle ->
            val offsetY = (particle.yPct * size.height) + (if (idx % 2 == 0) floatAnim else -floatAnim)
            val offsetX = particle.xPct * size.width
            drawCircle(
                color = particle.color.copy(alpha = 0.25f),
                radius = particle.radius.dp.toPx(),
                center = Offset(offsetX, offsetY)
            )
        }
    }
}
