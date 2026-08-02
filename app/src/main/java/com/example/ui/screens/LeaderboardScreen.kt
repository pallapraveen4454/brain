package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LeaderboardData
import com.example.data.LeaderboardPeriod
import com.example.ui.localization.LocalAppStrings
import com.example.ui.theme.AccentCoins
import com.example.ui.theme.DarkCardSurface
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.PrimaryPurpleLight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextWhite
import com.example.utils.RankUtils

data class LeaderboardUser(
    val rank: Int,
    val id: String = "",
    val name: String,
    val avatarId: String = "brain",
    val xp: Int,
    val level: Int = 1,
    val rankBadge: String = "Beginner",
    val quizzesPlayed: Int = 0,
    val achievementsCount: Int = 0,
    val score: Int = 0,
    val isCurrentUser: Boolean = false
)

@Composable
fun LeaderboardScreen(
    currentUserName: String,
    currentUserXp: Int,
    currentUserLevel: Int,
    currentUserAvatar: String = "brain",
    leaderboardData: LeaderboardData? = null,
    selectedPeriod: LeaderboardPeriod = LeaderboardPeriod.GLOBAL,
    onPeriodSelected: (LeaderboardPeriod) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Determine player list and user entry
    val players = leaderboardData?.topPlayers ?: listOf(
        LeaderboardUser(1, "1", "Sophia Chen", "wizard", 3850, 8, "Legend", 42, 12, 4500),
        LeaderboardUser(2, "2", "Alex Vance", "rocket", 3120, 7, "Grandmaster", 35, 10, 3700),
        LeaderboardUser(3, "3", "Elena Rostova", "crown", 2650, 6, "Master", 28, 8, 3100),
        LeaderboardUser(4, "4", "Marcus Brody", "star", 2100, 5, "Diamond", 22, 7, 2500),
        LeaderboardUser(
            rank = 5,
            id = "current",
            name = currentUserName.ifBlank { "You" },
            avatarId = currentUserAvatar,
            xp = currentUserXp,
            level = maxOf(1, currentUserLevel),
            rankBadge = RankUtils.getRankForXp(currentUserXp),
            isCurrentUser = true
        ),
        LeaderboardUser(6, "6", "David Kim", "brain", 1680, 4, "Gold", 18, 5, 2000),
        LeaderboardUser(7, "7", "Sarah Connor", "fire", 1350, 3, "Gold", 14, 4, 1600),
        LeaderboardUser(8, "8", "Liam Neeson", "ninja", 1050, 3, "Silver", 11, 3, 1250),
        LeaderboardUser(9, "9", "Priya Sharma", "cat", 820, 2, "Silver", 8, 2, 950),
        LeaderboardUser(10, "10", "Lucas Meyer", "fox", 600, 2, "Bronze", 6, 2, 720)
    ).sortedByDescending { it.xp }.mapIndexed { index, user -> user.copy(rank = index + 1) }

    val strings = LocalAppStrings.current

    val currentUserEntry = leaderboardData?.currentUserEntry ?: players.find { it.isCurrentUser } ?: LeaderboardUser(
        rank = players.size + 1,
        id = "current",
        name = currentUserName.ifBlank { "You" },
        avatarId = currentUserAvatar,
        xp = currentUserXp,
        level = maxOf(1, currentUserLevel),
        rankBadge = RankUtils.getRankForXp(currentUserXp),
        isCurrentUser = true
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .testTag("leaderboard_screen")
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Title Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = strings.globalLeaderboard,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    color = TextWhite
                )
                Text(
                    text = strings.topPlayers,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Period Filter Tabs (Global / Weekly / Friends)
        PeriodTabSelector(
            selectedPeriod = selectedPeriod,
            onPeriodSelected = onPeriodSelected
        )

        Spacer(modifier = Modifier.height(16.dp))

        // User Position Card (Highlighted Banner)
        CurrentUserRankBanner(user = currentUserEntry)

        Spacer(modifier = Modifier.height(16.dp))

        // Top 3 Podium
        val top3 = players.take(3)
        if (top3.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                if (top3.size >= 2) {
                    // Rank 2 (Silver - Left)
                    PodiumCard(
                        user = top3[1],
                        modifier = Modifier.weight(1f),
                        badgeColor = Color(0xFFC0C0C0),
                        medalIcon = "🥈",
                        heightDp = 135
                    )
                }
                if (top3.isNotEmpty()) {
                    // Rank 1 (Gold - Center)
                    PodiumCard(
                        user = top3[0],
                        modifier = Modifier.weight(1.15f),
                        badgeColor = AccentCoins,
                        medalIcon = "🥇",
                        heightDp = 155
                    )
                }
                if (top3.size >= 3) {
                    // Rank 3 (Bronze - Right)
                    PodiumCard(
                        user = top3[2],
                        modifier = Modifier.weight(1f),
                        badgeColor = Color(0xFFCD7F32),
                        medalIcon = "🥉",
                        heightDp = 120
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Rankings List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(players) { user ->
                LeaderboardRowItem(user = user)
            }
            item { Spacer(modifier = Modifier.height(30.dp)) }
        }
    }
}

@Composable
private fun PeriodTabSelector(
    selectedPeriod: LeaderboardPeriod,
    onPeriodSelected: (LeaderboardPeriod) -> Unit
) {
    val strings = LocalAppStrings.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(22.dp)),
        color = DarkCardSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LeaderboardPeriod.entries.forEach { period ->
                val isSelected = period == selectedPeriod
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) PrimaryPurple else Color.Transparent,
                    label = "tab_bg"
                )
                val textColor = if (isSelected) TextWhite else TextSecondary

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(bgColor)
                        .clickable { onPeriodSelected(period) }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (period) {
                            LeaderboardPeriod.GLOBAL -> strings.global
                            LeaderboardPeriod.WEEKLY -> strings.weekly
                            LeaderboardPeriod.FRIENDS -> "Friends"
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        ),
                        color = textColor
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrentUserRankBanner(user: LeaderboardUser) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(
                width = 2.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(PrimaryPurpleLight, AccentCoins)
                ),
                shape = RoundedCornerShape(18.dp)
            )
            .testTag("current_user_rank_banner"),
        color = PrimaryPurple.copy(alpha = 0.25f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Rank Number Box
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(PrimaryPurple, PrimaryPurpleLight)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#${user.rank}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = TextWhite
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Your Rank",
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryPurpleLight,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(${user.rankBadge})",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = TextWhite
                    )
                }
            }

            // XP and Level Indicator
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "XP",
                        tint = AccentCoins,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${user.xp} XP",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = AccentCoins
                    )
                }
                Text(
                    text = "Level ${user.level}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun PodiumCard(
    user: LeaderboardUser,
    badgeColor: Color,
    medalIcon: String,
    heightDp: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(heightDp.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(
                width = if (user.isCurrentUser) 2.dp else 1.dp,
                color = if (user.isCurrentUser) PrimaryPurpleLight else GlassBorder,
                shape = RoundedCornerShape(18.dp)
            ),
        color = if (user.isCurrentUser) PrimaryPurple.copy(alpha = 0.3f) else DarkCardSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Rank Badge with Medal
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(badgeColor.copy(alpha = 0.2f))
                    .border(1.dp, badgeColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = medalIcon,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Player Avatar Emoji
            Text(
                text = AvatarUtils.getEmoji(user.avatarId),
                fontSize = 24.sp
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = user.name,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                ),
                color = TextWhite,
                maxLines = 1
            )

            Text(
                text = "${user.xp} XP",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = PrimaryPurpleLight,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun LeaderboardRowItem(user: LeaderboardUser) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (user.isCurrentUser) 1.5.dp else 1.dp,
                color = if (user.isCurrentUser) PrimaryPurpleLight else GlassBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .testTag("leaderboard_row_${user.rank}"),
        color = if (user.isCurrentUser) PrimaryPurple.copy(alpha = 0.22f) else DarkCardSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Rank Number or Medal
                val rankText = when (user.rank) {
                    1 -> "🥇"
                    2 -> "🥈"
                    3 -> "🥉"
                    else -> "#${user.rank}"
                }

                Text(
                    text = rankText,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = if (user.rank <= 3) 18.sp else 14.sp
                    ),
                    color = if (user.rank <= 3) AccentCoins else TextMuted,
                    modifier = Modifier.width(36.dp)
                )

                // Avatar Icon / Emoji
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(PrimaryPurple.copy(alpha = 0.3f))
                        .border(1.dp, GlassBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = AvatarUtils.getEmoji(user.avatarId),
                        fontSize = 20.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = user.name + if (user.isCurrentUser) " (You)" else "",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = TextWhite,
                            maxLines = 1
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Level ${user.level}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        // Rank Badge Chip
                        Text(
                            text = "• ${user.rankBadge}",
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryPurpleLight,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // XP Display
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = "XP",
                    tint = PrimaryPurpleLight,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "${user.xp} XP",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    color = PrimaryPurpleLight
                )
            }
        }
    }
}
