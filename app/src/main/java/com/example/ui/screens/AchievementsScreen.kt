package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AchievementRepository
import com.example.data.model.Achievement
import com.example.ui.components.GlassCard
import com.example.ui.theme.AccentCoins
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkCardSurface
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.PrimaryPurpleLight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextWhite

@Composable
fun AchievementsScreen(
    userXp: Int,
    userStreak: Int,
    achievements: List<Achievement> = emptyList(),
    modifier: Modifier = Modifier
) {
    val listToDisplay = remember(achievements, userXp, userStreak) {
        if (achievements.isNotEmpty()) achievements
        else AchievementRepository().getAllAchievements(userXp, 0, userStreak)
    }

    val unlockedList = listToDisplay.filter { it.isUnlocked }
    val lockedList = listToDisplay.filter { !it.isUnlocked }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .testTag("achievements_screen")
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Trophies & Badges 🎖️",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            ),
            color = TextWhite
        )
        Text(
            text = "Unlock rewards as you learn and compete",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Header Progress Card
        GlassCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Achievements Unlocked",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = TextWhite
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${unlockedList.size} of ${listToDisplay.size} Milestones Completed",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PrimaryPurpleLight
                    )
                }

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
                    Text(
                        text = "${unlockedList.size}/${listToDisplay.size}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextWhite
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Unlocked Section
            if (unlockedList.isNotEmpty()) {
                item {
                    Text(
                        text = "🏅 Unlocked Achievements (${unlockedList.size})",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = AccentCoins,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                items(unlockedList, key = { it.id }) { ach ->
                    AchievementItemCard(achievement = ach)
                }
            }

            // Locked Section
            if (lockedList.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "🔒 Locked Achievements (${lockedList.size})",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                items(lockedList, key = { it.id }) { ach ->
                    AchievementItemCard(achievement = ach)
                }
            }

            item { Spacer(modifier = Modifier.height(30.dp)) }
        }
    }
}

@Composable
private fun AchievementItemCard(achievement: Achievement) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                color = if (achievement.isUnlocked) PrimaryPurpleLight.copy(alpha = 0.5f) else GlassBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .testTag("badge_card_${achievement.id}"),
        color = DarkCardSurface
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
                        if (achievement.isUnlocked) {
                            Brush.linearGradient(colors = listOf(PrimaryPurple, PrimaryPurpleLight))
                        } else {
                            Brush.linearGradient(colors = listOf(DarkCardBorder, TextMuted))
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (achievement.isUnlocked) getAchievementIcon(achievement.iconName) else Icons.Default.Lock,
                    contentDescription = achievement.title,
                    tint = TextWhite,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = achievement.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = TextWhite
                    )

                    if (achievement.isUnlocked) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "✅ Completed",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = PrimaryPurpleLight
                            )
                        }
                    } else {
                        Text(
                            text = "🔒 ${achievement.currentProgress}/${achievement.targetProgress}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = achievement.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (achievement.isUnlocked) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (achievement.unlockDate.isNotBlank()) {
                            Text(
                                text = "Unlocked: ${achievement.unlockDate}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        Text(
                            text = "+${achievement.rewardCoins} Coins Claimed 🪙",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = AccentCoins,
                            fontSize = 11.sp
                        )
                    }
                } else {
                    Column {
                        LinearProgressIndicator(
                            progress = {
                                if (achievement.targetProgress > 0) {
                                    achievement.currentProgress.toFloat() / achievement.targetProgress.toFloat()
                                } else 0f
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = PrimaryPurpleLight,
                            trackColor = DarkCardBorder
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "Reward: +${achievement.rewardCoins} Coins 🪙",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = AccentCoins,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getAchievementIcon(iconName: String): ImageVector {
    return when (iconName) {
        "first_quiz" -> Icons.Default.Psychology
        "first_10_q" -> Icons.Default.CheckCircle
        "xp_starter" -> Icons.Default.Bolt
        "xp_master" -> Icons.Default.MilitaryTech
        "xp_legend" -> Icons.Default.WorkspacePremium
        "perfect_score" -> Icons.Default.EmojiEvents
        "quiz_player" -> Icons.Default.School
        "quiz_master" -> Icons.Default.Stars
        "streak_3_day", "streak_7_day", "streak_30_day" -> Icons.Default.LocalFireDepartment
        "coin_collector", "coin_master" -> Icons.Default.MonetizationOn
        "ai_pioneer" -> Icons.Default.AutoAwesome
        else -> Icons.Default.EmojiEvents
    }
}
