package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.data.LeaderboardData
import com.example.data.LeaderboardPeriod
import com.example.data.LeaderboardRepository
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
import com.example.utils.VibrationUtils
import com.example.utils.bounceClick
import com.example.utils.shimmerEffect
import kotlin.random.Random

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
    val countryFlag: String = "🇺🇸",
    val rankChange: Int = 0, // >0: up, <0: down, 0: same
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
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var searchQuery by remember { mutableStateOf("") }

    // Default mock data with flags & rank change dynamics if not provided
    val defaultPlayers = remember(selectedPeriod) {
        LeaderboardRepository(context).getLeaderboard(selectedPeriod).topPlayers
    }

    val players = (leaderboardData?.topPlayers ?: defaultPlayers)
        .sortedByDescending { it.xp }
        .mapIndexed { index, user -> user.copy(rank = index + 1) }

    val filteredPlayers = if (searchQuery.isBlank()) {
        players
    } else {
        players.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.rank.toString() == searchQuery ||
                    it.rankBadge.contains(searchQuery, ignoreCase = true)
        }
    }

    val currentUserEntry = leaderboardData?.currentUserEntry ?: players.find { it.isCurrentUser } ?: LeaderboardUser(
        rank = players.size + 1,
        id = "current",
        name = currentUserName.ifBlank { "You" },
        avatarId = currentUserAvatar,
        xp = currentUserXp,
        level = maxOf(1, currentUserLevel),
        rankBadge = RankUtils.getRankForXp(currentUserXp),
        countryFlag = "🌟",
        rankChange = 1,
        isCurrentUser = true
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // High Performance Floating Gold & Purple Particles Canvas
        LeaderboardAmbientParticlesCanvas()

        val top3 = remember(filteredPlayers, searchQuery) { filteredPlayers.take(3) }
        val listItems = remember(filteredPlayers, searchQuery) {
            if (searchQuery.isBlank() && filteredPlayers.size > 3) {
                filteredPlayers.drop(3)
            } else {
                filteredPlayers
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("leaderboard_screen"),
            contentPadding = PaddingValues(
                start = 18.dp,
                end = 18.dp,
                top = 8.dp,
                bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ------------------------------------------------
            // 1. HERO HEADER WITH SHINY TROPHY & TITLE
            // ------------------------------------------------
            item(key = "leaderboard_hero_header") {
                LeaderboardHeroHeader()
            }

            // ------------------------------------------------
            // 2. PERIOD FILTER TABS (Global / Weekly / Friends)
            // ------------------------------------------------
            item(key = "leaderboard_period_tabs") {
                LeaderboardPeriodTabs(
                    selectedPeriod = selectedPeriod,
                    onPeriodSelected = { period ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        SoundEffects.playCoinSound()
                        onPeriodSelected(period)
                    }
                )
            }

            // ------------------------------------------------
            // 3. USER POSITION BANNER (Glow Card)
            // ------------------------------------------------
            item(key = "leaderboard_user_banner") {
                CurrentUserRankBanner(user = currentUserEntry)
            }

            // ------------------------------------------------
            // 4. SEARCH BAR FOR COMPETITORS
            // ------------------------------------------------
            item(key = "leaderboard_search_bar") {
                LeaderboardSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it }
                )
            }

            // ------------------------------------------------
            // 5. TOP 3 PODIUM (Gold, Silver, Bronze Columns)
            // ------------------------------------------------
            if (top3.isNotEmpty() && searchQuery.isBlank()) {
                item(key = "leaderboard_podium") {
                    PodiumSection(top3 = top3)
                }
            }

            // ------------------------------------------------
            // 6. ACHIEVEMENT HIGHLIGHTS STRIP
            // ------------------------------------------------
            item(key = "leaderboard_achievement_strip") {
                LeaderboardAchievementStrip()
            }

            // ------------------------------------------------
            // 7. MAIN RANKINGS LIST
            // ------------------------------------------------
            if (listItems.isEmpty()) {
                item(key = "leaderboard_empty_state") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isBlank()) {
                                "More global champions will appear as quizzes are completed. Keep playing to defend your rank! 🏆"
                            } else {
                                "No competitors found matching \"$searchQuery\""
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                itemsIndexed(
                    items = listItems,
                    key = { _, user -> user.id.ifBlank { user.name + user.rank } }
                ) { _, user ->
                    LeaderboardRowCard(user = user)
                }
            }

            // Bottom spacing item to guarantee bottom content is never clipped or overlapping with navigation bar
            item(key = "leaderboard_bottom_spacer") {
                Spacer(
                    modifier = Modifier
                        .windowInsetsBottomHeight(WindowInsets.navigationBars)
                        .height(32.dp)
                )
            }
        }
    }
}

@Composable
private fun LeaderboardHeroHeader() {
    val strings = LocalAppStrings.current
    val infiniteTransition = rememberInfiniteTransition(label = "heroTrophyFloat")
    val trophyOffsetY by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "trophyFloat"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = strings.globalLeaderboard,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        letterSpacing = 0.5.sp
                    ),
                    color = TextWhite,
                    modifier = Modifier.testTag("leaderboard_title")
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = AccentCoins,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = "Compete with top players worldwide & claim the crown 👑",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontSize = 12.sp
            )
        }

        // Floating Animated Trophy Badge
        Box(
            modifier = Modifier
                .offset(y = trophyOffsetY.dp)
                .size(46.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(AccentCoins.copy(alpha = 0.4f), Color.Transparent)
                    )
                )
                .border(1.5.dp, AccentCoins, CircleShape)
                .shadow(8.dp, CircleShape, ambientColor = AccentCoins, spotColor = AccentCoinsGradientEnd),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = "Leaderboard Trophy",
                tint = AccentCoins,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
private fun LeaderboardPeriodTabs(
    selectedPeriod: LeaderboardPeriod,
    onPeriodSelected: (LeaderboardPeriod) -> Unit
) {
    val strings = LocalAppStrings.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(23.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(23.dp)),
        color = DarkCardSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val periods = listOf(
                LeaderboardPeriod.GLOBAL to strings.global,
                LeaderboardPeriod.WEEKLY to strings.weekly,
                LeaderboardPeriod.FRIENDS to "Friends"
            )

            periods.forEach { (period, label) ->
                val isSelected = period == selectedPeriod
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) PrimaryPurple else Color.Transparent,
                    animationSpec = tween(250),
                    label = "tab_bg"
                )
                val textColor = if (isSelected) TextWhite else TextSecondary

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(bgColor)
                        .clickable {
                            SoundEffects.playClickSound()
                            onPeriodSelected(period)
                        }
                        .padding(vertical = 8.dp)
                        .testTag("period_tab_${period.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
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
    val animatedXp by animateIntAsState(
        targetValue = user.xp,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "userBannerXp"
    )

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("current_user_rank_banner"),
        shape = RoundedCornerShape(22.dp),
        backgroundColor = PrimaryPurple.copy(alpha = 0.28f),
        borderColor = PrimaryPurpleLight.copy(alpha = 0.8f),
        elevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Shiny Rank Circle Badge
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(PrimaryPurple, PrimaryPurpleLight)
                            )
                        )
                        .border(2.dp, AccentCoins, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#${user.rank}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        ),
                        color = TextWhite
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Your Position",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = PrimaryPurpleLight
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        RankMovementIndicator(user.rankChange)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        ),
                        color = TextWhite
                    )
                }
            }

            // XP and Level Status Pill
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
                        text = "$animatedXp XP",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        ),
                        color = AccentCoins
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = DarkBackground.copy(alpha = 0.6f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryPurpleLight.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "LVL ${user.level} • ${user.rankBadge}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        color = TextWhite,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LeaderboardSearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("leaderboard_search_bar"),
        placeholder = {
            Text(
                text = "Search player name or rank...",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                fontSize = 13.sp
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = PrimaryPurpleLight,
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        shape = RoundedCornerShape(26.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = DarkCardSurface,
            unfocusedContainerColor = DarkCardSurface,
            disabledContainerColor = DarkCardSurface,
            focusedBorderColor = PrimaryPurpleLight,
            unfocusedBorderColor = DarkCardBorder,
            focusedTextColor = TextWhite,
            unfocusedTextColor = TextWhite
        ),
        singleLine = true
    )
}

@Composable
private fun PodiumSection(top3: List<LeaderboardUser>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        if (top3.size >= 2) {
            // Rank 2 (Silver - Left)
            PodiumCard(
                user = top3[1],
                rankNumber = 2,
                modifier = Modifier.weight(1f),
                badgeColor = Color(0xFFD0D0E0),
                medalEmoji = "🥈",
                columnHeightDp = 185
            )
        }
        if (top3.isNotEmpty()) {
            // Rank 1 (Gold - Center - Taller & Eye-catching)
            PodiumCard(
                user = top3[0],
                rankNumber = 1,
                modifier = Modifier.weight(1.22f),
                badgeColor = AccentCoins,
                medalEmoji = "👑 🥇",
                columnHeightDp = 220
            )
        }
        if (top3.size >= 3) {
            // Rank 3 (Bronze - Right)
            PodiumCard(
                user = top3[2],
                rankNumber = 3,
                modifier = Modifier.weight(1f),
                badgeColor = Color(0xFFE5A062),
                medalEmoji = "🥉",
                columnHeightDp = 168
            )
        }
    }
}

@Composable
private fun PodiumCard(
    user: LeaderboardUser,
    rankNumber: Int,
    badgeColor: Color,
    medalEmoji: String,
    columnHeightDp: Int,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    val avatarSize = when (rankNumber) {
        1 -> 64.dp
        2 -> 54.dp
        else -> 50.dp
    }

    val emojiFontSize = when (rankNumber) {
        1 -> 34.sp
        2 -> 28.sp
        else -> 26.sp
    }

    GlassCard(
        modifier = modifier
            .height(columnHeightDp.dp)
            .bounceClick(scaleDown = 0.95f) {
                VibrationUtils.vibrateClick(context)
                SoundEffects.playCoinSound(context)
            }
            .testTag("podium_card_$rankNumber"),
        shape = RoundedCornerShape(24.dp),
        backgroundColor = if (user.isCurrentUser) PrimaryPurple.copy(alpha = 0.45f) else DarkCardSurface,
        borderColor = if (rankNumber == 1) AccentCoins else badgeColor.copy(alpha = 0.7f),
        elevation = if (rankNumber == 1) 14.dp else 7.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Rank Badge Header with Crown
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = medalEmoji,
                    fontSize = if (rankNumber == 1) 18.sp else 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Player Avatar with Crown/Halo
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                if (rankNumber == 1) {
                    Box(
                        modifier = Modifier
                            .size(avatarSize + 10.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(AccentCoins.copy(alpha = 0.6f), Color.Transparent)
                                )
                            )
                    )
                }

                Box(
                    modifier = Modifier
                        .size(avatarSize)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(PrimaryPurple, PrimaryPurpleLight)
                            )
                        )
                        .border(3.dp, badgeColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = AvatarUtils.getEmoji(user.avatarId),
                        fontSize = emojiFontSize
                    )
                }
            }

            // Player Name & XP
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = if (rankNumber == 1) 14.sp else 13.sp
                    ),
                    color = TextWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (rankNumber == 1) AccentCoins.copy(alpha = 0.25f) else DarkCardBorder
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${user.xp} XP",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = if (rankNumber == 1) AccentCoins else TextSecondary,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
private fun LeaderboardAchievementStrip() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AchievementPill("👑 Champion League", AccentCoins, Modifier.weight(1f))
        AchievementPill("🔥 Top Performer", AccentStreak, Modifier.weight(1f))
        AchievementPill("⚡ Speed Demon", AccentXP, Modifier.weight(1f))
    }
}

@Composable
private fun AchievementPill(
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(vertical = 6.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            ),
            color = color,
            maxLines = 1
        )
    }
}

@Composable
private fun LeaderboardRowCard(user: LeaderboardUser) {
    val haptic = LocalHapticFeedback.current

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(scaleDown = 0.97f) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            .testTag("leaderboard_row_${user.rank}"),
        shape = RoundedCornerShape(18.dp),
        backgroundColor = if (user.isCurrentUser) PrimaryPurple.copy(alpha = 0.26f) else DarkCardSurface,
        borderColor = if (user.isCurrentUser) PrimaryPurpleLight else GlassBorder,
        elevation = if (user.isCurrentUser) 6.dp else 2.dp
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
                // Rank Medal or Position Number
                val rankDisplay = when (user.rank) {
                    1 -> "🥇"
                    2 -> "🥈"
                    3 -> "🥉"
                    else -> "#${user.rank}"
                }

                Text(
                    text = rankDisplay,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = if (user.rank <= 3) 18.sp else 14.sp
                    ),
                    color = if (user.rank <= 3) AccentCoins else TextMuted,
                    modifier = Modifier.width(36.dp)
                )

                // Avatar Icon Container
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(PrimaryPurple.copy(alpha = 0.35f))
                        .border(1.dp, GlassBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = AvatarUtils.getEmoji(user.avatarId),
                        fontSize = 22.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = user.name + if (user.isCurrentUser) " (You)" else "",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            ),
                            color = TextWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = user.countryFlag,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "LVL ${user.level}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• ${user.rankBadge}",
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryPurpleLight,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // XP and Movement Indicator
            Column(horizontalAlignment = Alignment.End) {
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

                Spacer(modifier = Modifier.height(2.dp))

                RankMovementIndicator(user.rankChange)
            }
        }
    }
}

@Composable
private fun RankMovementIndicator(change: Int) {
    when {
        change > 0 -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = "Rank Up",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "+$change",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    ),
                    color = Color(0xFF10B981)
                )
            }
        }
        change < 0 -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ArrowDownward,
                    contentDescription = "Rank Down",
                    tint = Color(0xFFF43F5E),
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "$change",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    ),
                    color = Color(0xFFF43F5E)
                )
            }
        }
        else -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Same Position",
                    tint = TextMuted,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "0",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
private fun LeaderboardAmbientParticlesCanvas() {
    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        val particlePositions = listOf(
            Offset(width * 0.15f, height * ((0.2f + progress * 0.8f) % 1.0f)),
            Offset(width * 0.85f, height * ((0.8f - progress * 0.7f + 1.0f) % 1.0f)),
            Offset(width * 0.4f, height * ((0.5f + progress * 0.5f) % 1.0f)),
            Offset(width * 0.7f, height * ((0.1f + progress * 0.9f) % 1.0f)),
            Offset(width * 0.25f, height * ((0.9f - progress * 0.6f + 1.0f) % 1.0f))
        )

        particlePositions.forEachIndexed { i, pos ->
            val color = if (i % 2 == 0) AccentCoins.copy(alpha = 0.25f) else PrimaryPurpleLight.copy(alpha = 0.2f)
            val radius = if (i % 2 == 0) 3.5.dp.toPx() else 2.5.dp.toPx()
            drawCircle(color = color, radius = radius, center = pos)
        }
    }
}
