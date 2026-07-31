package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassCard
import com.example.ui.theme.AccentCoins
import com.example.ui.theme.AccentCoinsGradientEnd
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkCardSurface
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.PrimaryPurpleLight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextWhite

enum class AvatarCategory(val displayName: String) {
    ALL("All"),
    FREE("Free"),
    COMMON("Common"),
    RARE("Rare"),
    EPIC("Epic")
}

enum class AvatarStatus(val label: String) {
    USING("USING"),
    OWNED("OWNED"),
    BUY("BUY"),
    LOCKED("LOCKED")
}

data class AvatarShopItem(
    val id: String,
    val name: String,
    val emoji: String,
    val category: AvatarCategory,
    val price: Int,
    val status: AvatarStatus,
    val description: String,
    val gradientColors: List<Color>
)

object AvatarUtils {
    fun getEmoji(avatarId: String): String {
        val shopItem = AvatarShopData.SAMPLE_AVATARS.find { it.id == avatarId }
        if (shopItem != null) return shopItem.emoji
        return when (avatarId) {
            "student_boy" -> "👦"
            "student_girl" -> "👧"
            "reader" -> "🧑‍🎓"
            "programmer" -> "👨‍💻"
            "gamer" -> "🎮"
            "robot" -> "🤖"
            "scientist" -> "🔬"
            "astronaut" -> "👨‍🚀"
            "quiz_king" -> "👑"
            "brain_master" -> "🧠"
            "brain" -> "🧠"
            else -> "🧠"
        }
    }
}

object AvatarShopData {
    val SAMPLE_AVATARS = listOf(
        // FREE
        AvatarShopItem(
            id = "student_boy",
            name = "Student Boy",
            emoji = "👦",
            category = AvatarCategory.FREE,
            price = 0,
            status = AvatarStatus.USING,
            description = "A curious young learner eager to conquer daily brain quizzes.",
            gradientColors = listOf(Color(0xFF38BDF8), Color(0xFF0284C7))
        ),
        AvatarShopItem(
            id = "student_girl",
            name = "Student Girl",
            emoji = "👧",
            category = AvatarCategory.FREE,
            price = 0,
            status = AvatarStatus.OWNED,
            description = "A sharp student with a passion for knowledge and quick answers.",
            gradientColors = listOf(Color(0xFF2DD4BF), Color(0xFF0D9488))
        ),
        // COMMON
        AvatarShopItem(
            id = "reader",
            name = "Reader",
            emoji = "🧑‍🎓",
            category = AvatarCategory.COMMON,
            price = 500,
            status = AvatarStatus.BUY,
            description = "Bookworm scholar with an impressive memory for trivia and facts.",
            gradientColors = listOf(Color(0xFF4ADE80), Color(0xFF15803D))
        ),
        AvatarShopItem(
            id = "programmer",
            name = "Programmer",
            emoji = "👨‍💻",
            category = AvatarCategory.COMMON,
            price = 750,
            status = AvatarStatus.BUY,
            description = "Master of algorithms, logical thinking, and bug-free quiz streaks.",
            gradientColors = listOf(Color(0xFF818CF8), Color(0xFF4338CA))
        ),
        AvatarShopItem(
            id = "gamer",
            name = "Gamer",
            emoji = "🎮",
            category = AvatarCategory.COMMON,
            price = 1000,
            status = AvatarStatus.BUY,
            description = "Reflex mastermind with lightning speed on timed challenge questions.",
            gradientColors = listOf(Color(0xFFA78BFA), Color(0xFF6D28D9))
        ),
        // RARE
        AvatarShopItem(
            id = "robot",
            name = "Robot",
            emoji = "🤖",
            category = AvatarCategory.RARE,
            price = 1500,
            status = AvatarStatus.BUY,
            description = "A futuristic AI companion for BrainQuiz champions.",
            gradientColors = listOf(Color(0xFFC084FC), Color(0xFF7E22CE))
        ),
        AvatarShopItem(
            id = "scientist",
            name = "Scientist",
            emoji = "🔬",
            category = AvatarCategory.RARE,
            price = 2000,
            status = AvatarStatus.BUY,
            description = "Analytical researcher who dissects complex questions with scientific precision.",
            gradientColors = listOf(Color(0xFFF43F5E), Color(0xFFBE123C))
        ),
        AvatarShopItem(
            id = "astronaut",
            name = "Astronaut",
            emoji = "👨‍🚀",
            category = AvatarCategory.RARE,
            price = 2500,
            status = AvatarStatus.BUY,
            description = "Space explorer discovering new frontiers of cosmic general knowledge.",
            gradientColors = listOf(Color(0xFFF59E0B), Color(0xFFB45309))
        ),
        // EPIC
        AvatarShopItem(
            id = "quiz_king",
            name = "Quiz King",
            emoji = "👑",
            category = AvatarCategory.EPIC,
            price = 5000,
            status = AvatarStatus.LOCKED,
            description = "Royal trivia monarch ruling at the top of the BrainQuiz leaderboard.",
            gradientColors = listOf(Color(0xFFFBBF24), Color(0xFFD97706))
        ),
        AvatarShopItem(
            id = "brain_master",
            name = "Brain Master",
            emoji = "🧠",
            category = AvatarCategory.EPIC,
            price = 7000,
            status = AvatarStatus.LOCKED,
            description = "Ultimate cosmic intellect with unmatched accuracy and unstoppable streaks.",
            gradientColors = listOf(Color(0xFFEC4899), Color(0xFF9D174D))
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarShopScreen(
    userCoins: Int = 850,
    equippedAvatarId: String = "brain",
    unlockedAvatars: Set<String> = setOf("student_boy", "student_girl", "brain"),
    onBuyAvatar: (String, Int) -> Boolean = { _, _ -> false },
    onEquipAvatar: (String) -> Unit = {},
    onNavigateToQuiz: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(AvatarCategory.ALL) }
    var selectedAvatarForDetails by remember { mutableStateOf<AvatarShopItem?>(null) }
    var avatarForNotEnoughCoins by remember { mutableStateOf<AvatarShopItem?>(null) }
    var avatarForBuyConfirmation by remember { mutableStateOf<AvatarShopItem?>(null) }
    var avatarForSuccess by remember { mutableStateOf<AvatarShopItem?>(null) }
    var nowUsingAvatarName by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(nowUsingAvatarName) {
        if (nowUsingAvatarName != null) {
            delay(2000)
            nowUsingAvatarName = null
        }
    }

    val allAvatarsWithStatus = remember(equippedAvatarId, unlockedAvatars) {
        AvatarShopData.SAMPLE_AVATARS.map { item ->
            val status = when {
                item.id == equippedAvatarId -> AvatarStatus.USING
                unlockedAvatars.contains(item.id) || item.price == 0 || item.category == AvatarCategory.FREE -> AvatarStatus.OWNED
                else -> AvatarStatus.BUY
            }
            item.copy(status = status)
        }
    }

    val filteredAvatars = remember(selectedCategory, allAvatarsWithStatus) {
        if (selectedCategory == AvatarCategory.ALL) {
            allAvatarsWithStatus
        } else {
            allAvatarsWithStatus.filter { it.category == selectedCategory }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("avatar_shop_screen"),
        containerColor = DarkBackground,
        topBar = {
            AvatarShopTopAppBar(
                userCoins = userCoins,
                onBackClick = onNavigateBack
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Filter Tabs Row
                AvatarFilterTabs(
                    selectedCategory = selectedCategory,
                    onCategorySelect = { selectedCategory = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 2-Column Responsive Grid
                AnimatedContent(
                    targetState = filteredAvatars,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "AvatarGridTransition"
                ) { avatarList ->
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(avatarList, key = { it.id }) { avatar ->
                            AvatarCard(
                                avatar = avatar,
                                onClick = { selectedAvatarForDetails = avatar }
                            )
                        }
                    }
                }
            }

            // Floating "Now Using" Equip Banner
            AnimatedVisibility(
                visible = nowUsingAvatarName != null,
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut() + slideOutVertically { -it },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF22C55E),
                    contentColor = TextWhite,
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.dp, Color(0xFF86EFAC))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = TextWhite,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "✅ Now Using ${nowUsingAvatarName ?: ""}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextWhite
                        )
                    }
                }
            }
        }
    }

    // Avatar Details Bottom Sheet
    selectedAvatarForDetails?.let { avatar ->
        val currentStatus = when {
            avatar.id == equippedAvatarId -> AvatarStatus.USING
            unlockedAvatars.contains(avatar.id) || avatar.price == 0 || avatar.category == AvatarCategory.FREE -> AvatarStatus.OWNED
            else -> AvatarStatus.BUY
        }
        val updatedItem = avatar.copy(status = currentStatus)

        AvatarDetailBottomSheet(
            avatar = updatedItem,
            sheetState = sheetState,
            onDismiss = { selectedAvatarForDetails = null },
            onBuyClick = { itemToBuy ->
                if (userCoins < itemToBuy.price) {
                    avatarForNotEnoughCoins = itemToBuy
                } else {
                    avatarForBuyConfirmation = itemToBuy
                }
            },
            onEquipClick = { itemToEquip ->
                onEquipAvatar(itemToEquip.id)
                nowUsingAvatarName = itemToEquip.name
                selectedAvatarForDetails = itemToEquip.copy(status = AvatarStatus.USING)
            }
        )
    }

    // 1. Not Enough Coins Dialog
    avatarForNotEnoughCoins?.let { avatar ->
        NotEnoughCoinsDialog(
            avatarName = avatar.name,
            onDismiss = { avatarForNotEnoughCoins = null },
            onPlayQuiz = {
                avatarForNotEnoughCoins = null
                selectedAvatarForDetails = null
                onNavigateToQuiz()
            }
        )
    }

    // 2. Buy Confirmation Dialog
    avatarForBuyConfirmation?.let { avatar ->
        BuyConfirmationDialog(
            avatar = avatar,
            onDismiss = { avatarForBuyConfirmation = null },
            onConfirmBuy = {
                val item = avatarForBuyConfirmation
                avatarForBuyConfirmation = null
                if (item != null) {
                    val success = onBuyAvatar(item.id, item.price)
                    if (success) {
                        avatarForSuccess = item
                        selectedAvatarForDetails = item.copy(status = AvatarStatus.OWNED)
                    }
                }
            }
        )
    }

    // 3. Success Unlocked Dialog
    avatarForSuccess?.let { avatar ->
        SuccessUnlockedDialog(
            avatar = avatar,
            onDismiss = { avatarForSuccess = null }
        )
    }
}

@Composable
fun AvatarShopTopAppBar(
    userCoins: Int,
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(DarkBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.testTag("avatar_shop_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextWhite,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Avatar Shop",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = TextWhite,
                modifier = Modifier.testTag("avatar_shop_title")
            )
        }

        // Coin Balance Card
        CoinBalanceCard(coins = userCoins)
    }
}

@Composable
fun CoinBalanceCard(
    coins: Int,
    modifier: Modifier = Modifier
) {
    val animatedCoins by animateIntAsState(
        targetValue = coins,
        animationSpec = tween(durationMillis = 600),
        label = "CoinAnimation"
    )

    Surface(
        modifier = modifier.testTag("coin_balance_badge"),
        shape = RoundedCornerShape(20.dp),
        color = DarkCardSurface,
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.MonetizationOn,
                contentDescription = "Coins",
                tint = AccentCoins,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$animatedCoins Coins",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                ),
                color = TextWhite
            )
        }
    }
}

@Composable
fun AvatarFilterTabs(
    selectedCategory: AvatarCategory,
    onCategorySelect: (AvatarCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(AvatarCategory.entries.toTypedArray()) { category ->
            val isSelected = selectedCategory == category
            val animatedBgColor by animateColorAsState(
                targetValue = if (isSelected) PrimaryPurple else DarkCardSurface,
                label = "TabBgColor"
            )
            val animatedTextColor by animateColorAsState(
                targetValue = if (isSelected) TextWhite else TextSecondary,
                label = "TabTextColor"
            )

            val backgroundBrush = if (isSelected) {
                Brush.horizontalGradient(listOf(PrimaryPurple, PrimaryPurpleLight))
            } else {
                Brush.horizontalGradient(listOf(DarkCardSurface, DarkCardSurface))
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(backgroundBrush)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) GlassBorder else DarkCardBorder,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable { onCategorySelect(category) }
                    .padding(horizontal = 18.dp, vertical = 8.dp)
                    .testTag("filter_tab_${category.name.lowercase()}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = category.displayName,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp
                    ),
                    color = animatedTextColor
                )
            }
        }
    }
}

@Composable
fun AvatarCard(
    avatar: AvatarShopItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "CardScale"
    )

    val isUsing = avatar.status == AvatarStatus.USING
    val cardBorder = if (isUsing) {
        BorderStroke(2.dp, Brush.horizontalGradient(listOf(PrimaryPurpleLight, Color(0xFFE9D5FF))))
    } else {
        BorderStroke(1.dp, DarkCardBorder)
    }

    val cardElevation = if (isUsing) 8.dp else 2.dp

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .testTag("avatar_card_${avatar.id}"),
        shape = RoundedCornerShape(20.dp),
        backgroundColor = if (isUsing) DarkCardSurface.copy(alpha = 0.95f) else DarkCardSurface,
        borderColor = if (isUsing) PrimaryPurpleLight else DarkCardBorder,
        elevation = cardElevation
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Badges Row: Category & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CategoryChip(category = avatar.category)
                StatusBadge(status = avatar.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Avatar Visual Image Box
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(colors = avatar.gradientColors)
                    )
                    .border(
                        width = if (isUsing) 3.dp else 2.dp,
                        color = if (isUsing) PrimaryPurpleLight else GlassBorder,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = avatar.emoji,
                    fontSize = 42.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Avatar Name
            Text(
                text = avatar.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                color = TextWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("avatar_name_${avatar.id}")
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Coin Badge
            CoinBadge(price = avatar.price)
        }
    }
}

@Composable
fun CategoryChip(
    category: AvatarCategory,
    modifier: Modifier = Modifier
) {
    when (category) {
        AvatarCategory.FREE -> {
            Surface(
                modifier = modifier.testTag("category_chip_free"),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF22C55E).copy(alpha = 0.2f),
                border = BorderStroke(0.5.dp, Color(0xFF4ADE80))
            ) {
                Text(
                    text = "FREE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 9.sp,
                        letterSpacing = 0.5.sp
                    ),
                    color = Color(0xFF4ADE80),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        AvatarCategory.COMMON -> {
            Surface(
                modifier = modifier.testTag("category_chip_common"),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF38BDF8).copy(alpha = 0.2f),
                border = BorderStroke(0.5.dp, Color(0xFF38BDF8))
            ) {
                Text(
                    text = "COMMON",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 9.sp,
                        letterSpacing = 0.5.sp
                    ),
                    color = Color(0xFF38BDF8),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        AvatarCategory.RARE -> {
            Surface(
                modifier = modifier.testTag("category_chip_rare"),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFA855F7).copy(alpha = 0.2f),
                border = BorderStroke(0.5.dp, Color(0xFFC084FC))
            ) {
                Text(
                    text = "RARE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 9.sp,
                        letterSpacing = 0.5.sp
                    ),
                    color = Color(0xFFC084FC),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        AvatarCategory.EPIC -> {
            Box(
                modifier = modifier
                    .testTag("category_chip_epic")
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFFF59E0B), Color(0xFFFBBF24))
                        )
                    )
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "EPIC",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 9.sp,
                        letterSpacing = 0.5.sp
                    ),
                    color = Color(0xFF451A03)
                )
            }
        }
        AvatarCategory.ALL -> {
            Surface(
                modifier = modifier.testTag("category_chip_all"),
                shape = RoundedCornerShape(10.dp),
                color = PrimaryPurple.copy(alpha = 0.2f),
                border = BorderStroke(0.5.dp, PrimaryPurpleLight)
            ) {
                Text(
                    text = "ALL",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 9.sp,
                        letterSpacing = 0.5.sp
                    ),
                    color = PrimaryPurpleLight,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun CoinBadge(
    price: Int,
    modifier: Modifier = Modifier
) {
    if (price == 0) {
        Surface(
            modifier = modifier.testTag("coin_badge_free"),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF4ADE80).copy(alpha = 0.15f),
            border = BorderStroke(0.5.dp, Color(0xFF4ADE80).copy(alpha = 0.4f))
        ) {
            Text(
                text = "FREE",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                ),
                color = Color(0xFF4ADE80),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    } else {
        Row(
            modifier = modifier.testTag("coin_badge_price_$price"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.MonetizationOn,
                contentDescription = "Price",
                tint = AccentCoins,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "$price",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                ),
                color = TextWhite
            )
        }
    }
}

@Composable
fun StatusBadge(
    status: AvatarStatus,
    modifier: Modifier = Modifier
) {
    when (status) {
        AvatarStatus.USING -> {
            Surface(
                modifier = modifier.testTag("status_badge_using"),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF22C55E).copy(alpha = 0.25f),
                border = BorderStroke(1.dp, Color(0xFF4ADE80))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Using",
                        tint = Color(0xFF4ADE80),
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "USING",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        ),
                        color = Color(0xFF4ADE80)
                    )
                }
            }
        }
        AvatarStatus.OWNED -> {
            Surface(
                modifier = modifier.testTag("status_badge_owned"),
                shape = RoundedCornerShape(10.dp),
                color = PrimaryPurple.copy(alpha = 0.25f),
                border = BorderStroke(0.5.dp, PrimaryPurpleLight)
            ) {
                Text(
                    text = "OWNED",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    ),
                    color = PrimaryPurpleLight,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        AvatarStatus.BUY -> {
            Surface(
                modifier = modifier.testTag("status_badge_buy"),
                shape = RoundedCornerShape(10.dp),
                color = AccentCoins.copy(alpha = 0.2f),
                border = BorderStroke(0.5.dp, AccentCoins)
            ) {
                Text(
                    text = "BUY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    ),
                    color = AccentCoins,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        AvatarStatus.LOCKED -> {
            Surface(
                modifier = modifier.testTag("status_badge_locked"),
                shape = RoundedCornerShape(10.dp),
                color = DarkCardBorder,
                border = BorderStroke(0.5.dp, TextMuted)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = TextMuted,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "LOCKED",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        ),
                        color = TextMuted
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarDetailBottomSheet(
    avatar: AvatarShopItem,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onBuyClick: (AvatarShopItem) -> Unit = {},
    onEquipClick: (AvatarShopItem) -> Unit = {}
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkBackground,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        modifier = Modifier.testTag("avatar_detail_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Large Avatar Preview Container
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(colors = avatar.gradientColors)
                    )
                    .border(3.dp, GlassBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = avatar.emoji,
                    fontSize = 64.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Avatar Name
            Text(
                text = avatar.name,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                ),
                color = TextWhite,
                modifier = Modifier.testTag("sheet_avatar_name")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Category & Coin Cost Badges Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CategoryChip(category = avatar.category)

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkCardSurface,
                    border = BorderStroke(1.dp, GlassBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Cost: ",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        CoinBadge(price = avatar.price)
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Description Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
                border = BorderStroke(1.dp, DarkCardBorder)
            ) {
                Text(
                    text = avatar.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .testTag("sheet_avatar_description")
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Large Action Button with Premium Gradient Styling
            val buttonModifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("buy_avatar_button")

            val buttonShape = RoundedCornerShape(16.dp)

            when (avatar.status) {
                AvatarStatus.USING -> {
                    Button(
                        onClick = { },
                        enabled = false,
                        modifier = buttonModifier,
                        shape = buttonShape,
                        colors = ButtonDefaults.buttonColors(
                            disabledContainerColor = Color(0xFF3B0764),
                            disabledContentColor = Color(0xFFC084FC)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color(0xFFC084FC))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Using", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC084FC))
                        }
                    }
                }
                AvatarStatus.OWNED -> {
                    Box(
                        modifier = buttonModifier
                            .clip(buttonShape)
                            .background(Brush.horizontalGradient(listOf(PrimaryPurple, PrimaryPurpleLight)))
                            .clickable { onEquipClick(avatar) },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = TextWhite, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Use", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        }
                    }
                }
                AvatarStatus.BUY, AvatarStatus.LOCKED -> {
                    Box(
                        modifier = buttonModifier
                            .clip(buttonShape)
                            .background(Brush.horizontalGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706))))
                            .clickable { onBuyClick(avatar) },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = TextWhite, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (avatar.price > 0) "Unlock for ${avatar.price} Coins" else "Get Free",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun NotEnoughCoinsDialog(
    avatarName: String,
    onDismiss: () -> Unit,
    onPlayQuiz: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkBackground,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.testTag("not_enough_coins_dialog"),
        title = {
            Text(
                text = "Not Enough Coins 🪙",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = TextWhite
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Play quizzes and earn more coins to unlock this avatar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                border = BorderStroke(1.dp, DarkCardBorder),
                modifier = Modifier.testTag("not_enough_coins_cancel_button")
            ) {
                Text("Cancel", color = TextSecondary)
            }
        },
        confirmButton = {
            Button(
                onClick = onPlayQuiz,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurpleLight),
                modifier = Modifier.testTag("not_enough_coins_play_quiz_button")
            ) {
                Text("Play Quiz", color = TextWhite, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun BuyConfirmationDialog(
    avatar: AvatarShopItem,
    onDismiss: () -> Unit,
    onConfirmBuy: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkBackground,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.testTag("buy_confirmation_dialog"),
        title = {
            Text(
                text = "Buy ${avatar.name} Avatar?",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = TextWhite
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(avatar.gradientColors))
                        .border(2.dp, GlassBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = avatar.emoji, fontSize = 36.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("Cost: ", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                    Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = AccentCoins, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${avatar.price} Coins", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = AccentCoins)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "• Coins will be deducted.\n• Avatar will become permanently unlocked.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                border = BorderStroke(1.dp, DarkCardBorder),
                modifier = Modifier.testTag("buy_confirmation_cancel_button")
            ) {
                Text("Cancel", color = TextSecondary)
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmBuy,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurpleLight),
                modifier = Modifier.testTag("buy_confirmation_buy_button")
            ) {
                Text("Buy", color = TextWhite, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun SuccessUnlockedDialog(
    avatar: AvatarShopItem,
    onDismiss: () -> Unit
) {
    var scaleState by remember { mutableStateOf(0.4f) }
    LaunchedEffect(Unit) {
        scaleState = 1f
    }
    val scale by animateFloatAsState(
        targetValue = scaleState,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "SuccessScale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "GlowTransition")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowScale"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkBackground,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.testTag("success_dialog"),
        title = {
            Text(
                text = "🎉 Avatar Unlocked!",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 22.sp),
                color = TextWhite,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(vertical = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .scale(glowScale)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        PrimaryPurpleLight.copy(alpha = 0.5f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .scale(scale)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(avatar.gradientColors))
                            .border(3.dp, PrimaryPurpleLight, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = avatar.emoji, fontSize = 46.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "You have successfully unlocked this avatar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("success_continue_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurpleLight)
            ) {
                Text("Continue", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    )
}
